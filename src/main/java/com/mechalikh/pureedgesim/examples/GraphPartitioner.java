/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Partitions a service graph with graph-theory techniques.
 * <p>
 * The algorithm has three steps:
 * <ol>
 * <li>Split the graph into weakly connected components. No dependency edge
 * crosses two components, so each one is partitioned on its own.</li>
 * <li>Inside a component, assign the heaviest steps first to the least loaded
 * partition (the longest-processing-time rule), which gives an initial balanced
 * partition.</li>
 * <li>Refine it with Kernighan-Lin style pairwise swaps: a pair of steps in
 * different partitions is exchanged whenever the exchange removes more
 * dependency edges than it adds, while keeping every partition within
 * {@value #BALANCE_TOLERANCE} times the average load.</li>
 * </ol>
 * The objective is the edge cut, the number of dependency edges whose two ends
 * land in different partitions, because those edges become network transfers
 * when the partitions run on different nodes. The refinement stops at a local
 * minimum of the cut.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see ServiceGraph
 * @see Example9PartitioningOrchestrator
 */
public class GraphPartitioner {

	/** Maximum partition load, as a multiple of the average load, after a swap. */
	public static final double BALANCE_TOLERANCE = 1.5;

	/** Upper bound on refinement passes, so a pathological graph cannot loop. */
	public static final int MAX_REFINEMENT_PASSES = 100;

	/** One partition of one connected component. */
	public static final class Partition {

		private final int component;
		private final int index;
		private final List<String> members;
		private final double load;

		Partition(int component, int index, List<String> members, double load) {
			this.component = component;
			this.index = index;
			this.members = Collections.unmodifiableList(members);
			this.load = load;
		}

		/**
		 * Returns the connected component this partition belongs to.
		 *
		 * @return the component index
		 */
		public int getComponent() {
			return component;
		}

		/**
		 * Returns the index of this partition inside its component.
		 *
		 * @return the local partition index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * Returns the names of the subtasks in this partition.
		 *
		 * @return the member names
		 */
		public List<String> getMembers() {
			return members;
		}

		/**
		 * Returns the total mean task length of this partition.
		 *
		 * @return the load, in MI
		 */
		public double getLoad() {
			return load;
		}

		@Override
		public String toString() {
			return "P" + component + "." + index + " " + members + " (" + Math.round(load) + " MI)";
		}
	}

	/** The partition plan of a whole graph. */
	public static final class Plan {

		private final Map<String, Integer> partitionOf;
		private final List<Partition> partitions;
		private final int cutEdges;
		private final double crossTrafficBits;

		Plan(Map<String, Integer> partitionOf, List<Partition> partitions, int cutEdges, double crossTrafficBits) {
			this.partitionOf = Collections.unmodifiableMap(new LinkedHashMap<>(partitionOf));
			this.partitions = Collections.unmodifiableList(partitions);
			this.cutEdges = cutEdges;
			this.crossTrafficBits = crossTrafficBits;
		}

		/**
		 * Returns the partition of every subtask, by name.
		 *
		 * @return an immutable subtask name to partition identifier map
		 */
		public Map<String, Integer> getPartitionOf() {
			return partitionOf;
		}

		/**
		 * Returns every partition of the plan.
		 *
		 * @return the partitions, component by component
		 */
		public List<Partition> getPartitions() {
			return partitions;
		}

		/**
		 * Returns the number of dependency edges that cross two partitions.
		 *
		 * @return the edge cut
		 */
		public int getCutEdges() {
			return cutEdges;
		}

		/**
		 * Returns the total size of the results that cross two partitions.
		 *
		 * @return the cross-partition traffic per request, in bits
		 */
		public double getCrossTrafficBits() {
			return crossTrafficBits;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(partitions.size()).append(" partitions, cut ").append(cutEdges).append(" edges (")
					.append(Math.round(crossTrafficBits)).append(" bits per request)");
			for (Partition partition : partitions)
				builder.append(System.lineSeparator()).append("  ").append(partition);
			return builder.toString();
		}
	}

	/**
	 * Partitions a graph into balanced groups, minimizing the number of dependency
	 * edges between groups.
	 *
	 * @param graph          the service graph, which must be acyclic
	 * @param partitionCount the requested partitions per connected component
	 * @return the partition plan
	 * @throws IllegalArgumentException if {@code partitionCount} is lower than one
	 */
	public Plan partition(ServiceGraph graph, int partitionCount) {
		if (partitionCount < 1)
			throw new IllegalArgumentException("A graph needs at least one partition");

		// Also validates that the graph is acyclic before any array is sized.
		graph.topologicalOrder();

		int[] partitionOfStep = new int[graph.size()];
		Arrays.fill(partitionOfStep, -1);
		Map<String, Integer> partitionOfName = new LinkedHashMap<>();
		List<Partition> partitions = new ArrayList<>();
		List<List<Integer>> components = graph.components();

		for (int component = 0; component < components.size(); component++) {
			List<Integer> members = components.get(component);
			int count = Math.min(partitionCount, members.size());
			int[] local = initialAssignment(graph, members, count);
			refine(graph, members, local, count);
			double[] loads = loads(graph, members, local, count);

			int[] globalIndex = new int[count];
			for (int p = 0; p < count; p++) {
				List<String> names = new ArrayList<>();
				for (int i = 0; i < members.size(); i++)
					if (local[i] == p)
						names.add(graph.getSteps().get(members.get(i)).getName());
				globalIndex[p] = partitions.size();
				partitions.add(new Partition(component, p, names, loads[p]));
			}
			for (int i = 0; i < members.size(); i++) {
				int global = globalIndex[local[i]];
				partitionOfStep[members.get(i)] = global;
				partitionOfName.put(graph.getSteps().get(members.get(i)).getName(), global);
			}
		}

		int cutEdges = 0;
		double crossTraffic = 0;
		for (int step = 0; step < graph.size(); step++) {
			ServiceGraph.Step current = graph.getSteps().get(step);
			for (String dependency : current.getDependencies()) {
				int parent = graph.indexOf(dependency);
				if (partitionOfStep[parent] != partitionOfStep[step]) {
					cutEdges++;
					crossTraffic += graph.getSteps().get(parent).getResultSize();
				}
			}
		}
		return new Plan(partitionOfName, partitions, cutEdges, crossTraffic);
	}

	/**
	 * Assigns the heaviest steps first to the least loaded partition.
	 *
	 * @param graph   the graph
	 * @param members the steps of the component
	 * @param count   the number of partitions of this component
	 * @return the partition of every member, in member order
	 */
	private int[] initialAssignment(ServiceGraph graph, List<Integer> members, int count) {
		Integer[] heaviestFirst = new Integer[members.size()];
		for (int i = 0; i < heaviestFirst.length; i++)
			heaviestFirst[i] = i;
		Arrays.sort(heaviestFirst, (a, b) -> {
			int byLoad = Double.compare(graph.getSteps().get(members.get(b)).getLength(),
					graph.getSteps().get(members.get(a)).getLength());
			return byLoad != 0 ? byLoad : Integer.compare(members.get(a), members.get(b));
		});

		double[] loads = new double[count];
		int[] assignment = new int[members.size()];
		for (int index : heaviestFirst) {
			int lightest = 0;
			for (int p = 1; p < count; p++)
				if (loads[p] < loads[lightest])
					lightest = p;
			assignment[index] = lightest;
			loads[lightest] += graph.getSteps().get(members.get(index)).getLength();
		}
		return assignment;
	}

	/**
	 * Exchanges pairs of steps between partitions while the exchange lowers the
	 * edge cut and keeps the partitions balanced.
	 *
	 * @param graph   the graph
	 * @param members the steps of the component
	 * @param local   the partition of every member, modified in place
	 * @param count   the number of partitions of this component
	 */
	private void refine(ServiceGraph graph, List<Integer> members, int[] local, int count) {
		if (count < 2)
			return;
		double average = 0;
		for (int member : members)
			average += graph.getSteps().get(member).getLength();
		average /= count;
		double allowed = average * BALANCE_TOLERANCE;

		int cut = cut(graph, members, local);
		for (int pass = 0; pass < MAX_REFINEMENT_PASSES; pass++) {
			int bestDelta = 0;
			int bestLeft = -1;
			int bestRight = -1;

			for (int left = 0; left < members.size(); left++) {
				for (int right = left + 1; right < members.size(); right++) {
					if (local[left] == local[right])
						continue;
					swap(local, left, right);
					if (maximumLoad(graph, members, local, count) <= allowed + 1e-9) {
						int delta = cut(graph, members, local) - cut;
						if (delta < bestDelta) {
							bestDelta = delta;
							bestLeft = left;
							bestRight = right;
						}
					}
					swap(local, left, right);
				}
			}

			if (bestLeft < 0)
				return;
			swap(local, bestLeft, bestRight);
			cut += bestDelta;
		}
	}

	private static void swap(int[] values, int left, int right) {
		int temporary = values[left];
		values[left] = values[right];
		values[right] = temporary;
	}

	private static int cut(ServiceGraph graph, List<Integer> members, int[] local) {
		int[] partitionOfStep = new int[graph.size()];
		Arrays.fill(partitionOfStep, -1);
		for (int i = 0; i < members.size(); i++)
			partitionOfStep[members.get(i)] = local[i];

		int cut = 0;
		for (int i = 0; i < members.size(); i++) {
			int step = members.get(i);
			for (String dependency : graph.getSteps().get(step).getDependencies())
				if (partitionOfStep[graph.indexOf(dependency)] != local[i])
					cut++;
		}
		return cut;
	}

	private static double maximumLoad(ServiceGraph graph, List<Integer> members, int[] local, int count) {
		double[] loads = loads(graph, members, local, count);
		double maximum = 0;
		for (double load : loads)
			maximum = Math.max(maximum, load);
		return maximum;
	}

	private static double[] loads(ServiceGraph graph, List<Integer> members, int[] local, int count) {
		double[] loads = new double[count];
		for (int i = 0; i < members.size(); i++)
			loads[local[i]] += graph.getSteps().get(members.get(i)).getLength();
		return loads;
	}
}
