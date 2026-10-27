package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GraphPartitionerTest {

	private static ServiceGraph.Step step(String name, double length, double resultSize, String... dependencies) {
		return new ServiceGraph.Step(name, Arrays.asList(dependencies), length, resultSize);
	}

	private static ServiceGraph chain(double... lengths) {
		List<ServiceGraph.Step> steps = new ArrayList<>();
		for (int index = 0; index < lengths.length; index++) {
			String name = "s" + index;
			List<String> dependencies = index == 0 ? List.of() : List.of("s" + (index - 1));
			steps.add(new ServiceGraph.Step(name, dependencies, lengths[index], 100 * (index + 1)));
		}
		return new ServiceGraph(steps);
	}

	@Test
	void aChainIsCutIntoTwoBalancedContiguousHalves() {
		ServiceGraph graph = chain(1, 1, 1, 1);

		GraphPartitioner.Plan plan = new GraphPartitioner().partition(graph, 2);

		assertEquals(2, plan.getPartitions().size());
		assertEquals(Set.of(Set.of("s0", "s1"), Set.of("s2", "s3")),
				Set.of(new HashSet<>(plan.getPartitions().get(0).getMembers()),
						new HashSet<>(plan.getPartitions().get(1).getMembers())));
		assertEquals(1, plan.getCutEdges(), "only the s1 -> s2 edge crosses the two partitions");
	}

	@Test
	void theCrossingEdgeCarriesTheResultsOfItsParentOnly() {
		ServiceGraph graph = chain(1, 1, 1, 1);

		GraphPartitioner.Plan plan = new GraphPartitioner().partition(graph, 2);

		// s1 carries 200 bits, and it is the parent of the single crossing edge.
		assertEquals(200, plan.getCrossTrafficBits());
	}

	@Test
	void disconnectedComponentsArePartitionedIndependently() {
		ServiceGraph graph = new ServiceGraph(List.of(
				step("a", 2, 10),
				step("b", 1, 10, "a"),
				step("x", 2, 10),
				step("y", 1, 10, "x")));

		GraphPartitioner.Plan plan = new GraphPartitioner().partition(graph, 2);

		assertEquals(4, plan.getPartitions().size(), "two components of two steps give two partitions each");
		assertEquals(2, plan.getCutEdges(), "one edge of each component crosses its two partitions");
		for (GraphPartitioner.Partition partition : plan.getPartitions()) {
			boolean fromFirstComponent = partition.getMembers().stream().anyMatch(name -> name.equals("a") || name.equals("b"));
			boolean fromSecondComponent = partition.getMembers().stream().anyMatch(name -> name.equals("x") || name.equals("y"));
			assertTrue(fromFirstComponent ^ fromSecondComponent,
					"a partition must not mix two connected components: " + partition);
		}
	}

	@Test
	void everySubtaskIsAssignedExactlyOnce() {
		ServiceGraph graph = new ServiceGraph(List.of(
				step("root", 1, 10),
				step("left", 2, 10, "root"),
				step("right", 3, 10, "root"),
				step("join", 1, 10, "left", "right"),
				step("tail", 4, 10, "join")));

		GraphPartitioner.Plan plan = new GraphPartitioner().partition(graph, 3);

		Map<String, Integer> assignment = plan.getPartitionOf();
		assertEquals(graph.size(), assignment.size());
		Set<String> members = new HashSet<>();
		for (GraphPartitioner.Partition partition : plan.getPartitions())
			for (String name : partition.getMembers())
				assertTrue(members.add(name), name + " is assigned twice");
		assertEquals(new HashSet<>(assignment.keySet()), members);
		for (int partition : assignment.values())
			assertTrue(partition >= 0 && partition < plan.getPartitions().size());
	}

	@Test
	void morePartitionsThanStepsGivesOneStepPerPartition() {
		ServiceGraph graph = chain(1, 1, 1);

		GraphPartitioner.Plan plan = new GraphPartitioner().partition(graph, 10);

		assertEquals(3, plan.getPartitions().size());
		assertEquals(2, plan.getCutEdges());
		for (GraphPartitioner.Partition partition : plan.getPartitions())
			assertEquals(1, partition.getMembers().size());
	}

	@Test
	void aSinglePartitionKeepsTheWholeGraphTogether() {
		ServiceGraph graph = chain(1, 2, 3);

		GraphPartitioner.Plan plan = new GraphPartitioner().partition(graph, 1);

		assertEquals(List.of("s0", "s1", "s2"), plan.getPartitions().get(0).getMembers());
		assertEquals(0, plan.getCutEdges());
	}

	@Test
	void aPartitionCountBelowOneIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new GraphPartitioner().partition(chain(1, 1), 0));
	}

	@Test
	void aProgrammaticCycleIsRejected() {
		ServiceGraph cyclic = new ServiceGraph(List.of(
				step("a", 1, 1, "b"),
				step("b", 1, 1, "a")));

		assertThrows(IllegalArgumentException.class, () -> new GraphPartitioner().partition(cyclic, 2));
	}

	@Test
	void duplicateNamesAndUnknownDependenciesAreRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new ServiceGraph(List.of(step("a", 1, 1), step("a", 1, 1))));
		assertThrows(IllegalArgumentException.class,
				() -> new ServiceGraph(List.of(step("a", 1, 1, "missing"))));
	}

	@Test
	void theTopologicalOrderPutsEveryDependencyFirst() {
		ServiceGraph graph = new ServiceGraph(List.of(
				step("join", 1, 1, "left", "right"),
				step("left", 1, 1, "root"),
				step("root", 1, 1),
				step("right", 1, 1, "root")));

		List<Integer> order = graph.topologicalOrder();

		assertEquals(4, order.size());
		assertTrue(order.indexOf(graph.indexOf("root")) < order.indexOf(graph.indexOf("left")));
		assertTrue(order.indexOf(graph.indexOf("root")) < order.indexOf(graph.indexOf("right")));
		assertTrue(order.indexOf(graph.indexOf("left")) < order.indexOf(graph.indexOf("join")));
		assertTrue(order.indexOf(graph.indexOf("right")) < order.indexOf(graph.indexOf("join")));
	}
}
