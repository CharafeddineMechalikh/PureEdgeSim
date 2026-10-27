package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GeneticPartitionerTest {

	private static ServiceGraph pipeline() {
		// capture -> preprocess -> detect -> (classify, track) -> aggregate -> notify
		return new ServiceGraph(List.of(
				new ServiceGraph.Step("capture", List.of(), 200, 5000),
				new ServiceGraph.Step("preprocess", List.of("capture"), 600, 20000),
				new ServiceGraph.Step("detect", List.of("preprocess"), 4000, 10000),
				new ServiceGraph.Step("classify", List.of("detect"), 2500, 50000),
				new ServiceGraph.Step("track", List.of("detect"), 800, 100000),
				new ServiceGraph.Step("aggregate", List.of("classify", "track"), 300, 20000),
				new ServiceGraph.Step("notify", List.of("aggregate"), 50, 1000)));
	}

	private static List<GeneticPartitioner.Candidate> nodes() {
		return List.of(
				new GeneticPartitioner.Candidate(1, "Cloud", 20000, 1_000_000_000),
				new GeneticPartitioner.Candidate(2, "Edge", 8000, 1_000_000_000),
				new GeneticPartitioner.Candidate(3, "Mist", 2000, 100_000_000));
	}

	@Test
	void theSeedMakesTheSearchReproducible() {
		ServiceGraph graph = pipeline();

		GeneticPartitioner.Plan first = new GeneticPartitioner(11).partition(graph, nodes());
		GeneticPartitioner.Plan second = new GeneticPartitioner(11).partition(graph, nodes());

		assertEquals(first.placement(), second.placement());
		assertEquals(first.getFitness(), second.getFitness(), 0.0);
	}

	@Test
	void everySubtaskReceivesANode() {
		ServiceGraph graph = pipeline();

		GeneticPartitioner.Plan plan = new GeneticPartitioner(7).partition(graph, nodes());

		Map<String, String> placement = plan.placement();
		assertEquals(graph.size(), placement.size());
		for (int step = 0; step < graph.size(); step++)
			assertTrue(placement.containsKey(graph.getSteps().get(step).getName()));
	}

	@Test
	void moreGenerationsNeverReturnAWorsePlan() {
		ServiceGraph graph = pipeline();

		double none = new GeneticPartitioner(5, 20, 0).partition(graph, nodes()).getFitness();
		double many = new GeneticPartitioner(5, 20, 100).partition(graph, nodes()).getFitness();

		assertTrue(many <= none + 1e-9, "the best chromosome is kept across generations");
	}

	@Test
	void aFastSingleNodeWinsWhenTransfersAreCheap() {
		ServiceGraph graph = pipeline();
		List<GeneticPartitioner.Candidate> nodes = List.of(
				new GeneticPartitioner.Candidate(1, "Fast", 50000, 10_000_000_000.0),
				new GeneticPartitioner.Candidate(2, "Slow", 1000, 10_000_000_000.0));

		GeneticPartitioner.Plan plan = new GeneticPartitioner(3).partition(graph, nodes);

		for (String node : plan.placement().values())
			assertEquals("Fast#1", node);
	}

	@Test
	void thePlanReportsItsEstimate() {
		GeneticPartitioner.Plan plan = new GeneticPartitioner(3).partition(pipeline(), nodes());

		assertTrue(plan.getMakespanSeconds() > 0);
		assertTrue(plan.getCommunicationSeconds() >= 0);
		assertTrue(plan.getImbalance() >= 0);
		assertTrue(plan.getFitness() > 0);
	}

	@Test
	void anEmptyCandidateListIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new GeneticPartitioner(1).partition(pipeline(), List.of()));
	}

	@Test
	void aPopulationOfLessThanTwoIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new GeneticPartitioner(1, 1, 10));
		assertThrows(IllegalArgumentException.class, () -> new GeneticPartitioner(1, 10, -1));
	}

	@Test
	void aCyclicGraphIsRejected() {
		ServiceGraph cyclic = new ServiceGraph(Arrays.asList(
				new ServiceGraph.Step("a", List.of("b"), 1, 1),
				new ServiceGraph.Step("b", List.of("a"), 1, 1)));

		assertThrows(IllegalArgumentException.class, () -> new GeneticPartitioner(1).partition(cyclic, nodes()));
	}
}
