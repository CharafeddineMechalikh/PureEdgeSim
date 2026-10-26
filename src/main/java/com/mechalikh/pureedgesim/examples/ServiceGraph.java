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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Subtask;

/**
 * The dependency graph of an application whose requests are split into
 * subtasks, with the mean values of each subtask.
 * <p>
 * Both partitioning examples need the graph before the first task reaches the
 * orchestrator, when no {@code Task} instance exists yet. This class reads the
 * templates parsed from the applications file, so a partition plan can be
 * computed once per application and reused by every request.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Application#getSubtasks()
 * @see GraphPartitioner
 * @see GeneticPartitioner
 */
public final class ServiceGraph {

	/** One subtask of the graph, with the mean values of its distributions. */
	public static final class Step {

		private final String name;
		private final List<String> dependencies;
		private final double length;
		private final double resultSize;

		/**
		 * Creates a step.
		 *
		 * @param name         name of the subtask, unique in the application
		 * @param dependencies names of the steps it depends on
		 * @param length       mean task length, in MI
		 * @param resultSize   mean result size, in bits
		 */
		public Step(String name, List<String> dependencies, double length, double resultSize) {
			this.name = name;
			this.dependencies = List.copyOf(dependencies);
			this.length = length;
			this.resultSize = resultSize;
		}

		/**
		 * Returns the name of this step.
		 *
		 * @return the subtask name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the names of the steps this one depends on.
		 *
		 * @return the dependency names, empty for a root
		 */
		public List<String> getDependencies() {
			return dependencies;
		}

		/**
		 * Returns the mean task length of this step.
		 *
		 * @return the length, in MI
		 */
		public double getLength() {
			return length;
		}

		/**
		 * Returns the mean size of the results this step sends to its dependents.
		 *
		 * @return the result size, in bits
		 */
		public double getResultSize() {
			return resultSize;
		}

		@Override
		public String toString() {
			return name + "(" + Math.round(length) + " MI)";
		}
	}

	private final List<Step> steps;
	private final Map<String, Integer> index = new LinkedHashMap<>();

	/**
	 * Creates a graph from its steps and validates its names and dependencies.
	 *
	 * @param steps the steps, in any order
	 * @throws IllegalArgumentException if a name is repeated or a dependency is
	 *                                  unknown
	 */
	public ServiceGraph(List<Step> steps) {
		this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
		for (int i = 0; i < this.steps.size(); i++) {
			Step step = this.steps.get(i);
			if (index.put(step.getName(), i) != null)
				throw new IllegalArgumentException("Duplicate subtask name '" + step.getName() + "'");
		}
		for (Step step : this.steps)
			for (String dependency : step.getDependencies())
				if (!index.containsKey(dependency))
					throw new IllegalArgumentException(
							"Subtask '" + step.getName() + "' depends on unknown subtask '" + dependency + "'");
	}

	/**
	 * Builds the graph of an application from the mean values of its subtasks.
	 *
	 * @param application the application, which must declare subtasks
	 * @return the dependency graph of the application
	 */
	public static ServiceGraph of(Application application) {
		List<Step> steps = new ArrayList<>();
		for (Subtask subtask : application.getSubtasks())
			steps.add(new Step(subtask.getName(), subtask.getDependencies(),
					subtask.getTaskLengthDistribution().mean(), subtask.getResultsSizeDistribution().mean()));
		return new ServiceGraph(steps);
	}

	/**
	 * Returns every step of the graph.
	 *
	 * @return the steps, in declaration order
	 */
	public List<Step> getSteps() {
		return steps;
	}

	/**
	 * Returns the number of steps.
	 *
	 * @return the number of subtask templates
	 */
	public int size() {
		return steps.size();
	}

	/**
	 * Returns the declaration index of a step.
	 *
	 * @param name the subtask name
	 * @return its index, or -1 when the name is unknown
	 */
	public int indexOf(String name) {
		Integer found = index.get(name);
		return found == null ? -1 : found;
	}

	/**
	 * Orders the steps so that every step comes after its dependencies.
	 * <p>
	 * The applications parser already rejects cycles; this method validates the
	 * graph again so the partitioners can rely on the order and gives a clear
	 * message when a cycle is passed programmatically.
	 *
	 * @return the step indices, in a deterministic topological order
	 * @throws IllegalArgumentException if the graph contains a cycle
	 */
	public List<Integer> topologicalOrder() {
		int[] remaining = new int[steps.size()];
		List<List<Integer>> dependents = new ArrayList<>();
		for (int i = 0; i < steps.size(); i++)
			dependents.add(new ArrayList<>());
		for (int i = 0; i < steps.size(); i++) {
			remaining[i] = steps.get(i).getDependencies().size();
			for (String dependency : steps.get(i).getDependencies())
				dependents.get(indexOf(dependency)).add(i);
		}

		Deque<Integer> ready = new ArrayDeque<>();
		for (int i = 0; i < remaining.length; i++)
			if (remaining[i] == 0)
				ready.add(i);

		List<Integer> order = new ArrayList<>(steps.size());
		while (!ready.isEmpty()) {
			int step = ready.remove();
			order.add(step);
			for (int dependent : dependents.get(step))
				if (--remaining[dependent] == 0)
					ready.add(dependent);
		}
		if (order.size() != steps.size())
			throw new IllegalArgumentException("The service graph contains a cycle");
		return order;
	}

	/**
	 * Groups the steps into weakly connected components, using a depth-first
	 * search over the dependencies in both directions.
	 * <p>
	 * Two steps that are not connected by any path can be placed independently:
	 * no message ever travels between their partitions.
	 *
	 * @return the components, each sorted by declaration index
	 */
	public List<List<Integer>> components() {
		List<List<Integer>> neighbours = new ArrayList<>();
		for (int i = 0; i < steps.size(); i++)
			neighbours.add(new ArrayList<>());

		Map<String, Integer> position = new HashMap<>();
		for (int i = 0; i < steps.size(); i++)
			position.put(steps.get(i).getName(), i);
		for (int i = 0; i < steps.size(); i++)
			for (String dependency : steps.get(i).getDependencies()) {
				int parent = position.get(dependency);
				neighbours.get(i).add(parent);
				neighbours.get(parent).add(i);
			}

		boolean[] visited = new boolean[steps.size()];
		List<List<Integer>> components = new ArrayList<>();
		for (int start = 0; start < steps.size(); start++) {
			if (visited[start])
				continue;
			List<Integer> component = new ArrayList<>();
			Deque<Integer> stack = new ArrayDeque<>();
			stack.push(start);
			visited[start] = true;
			while (!stack.isEmpty()) {
				int step = stack.pop();
				component.add(step);
				for (int neighbour : neighbours.get(step))
					if (!visited[neighbour]) {
						visited[neighbour] = true;
						stack.push(neighbour);
					}
			}
			Collections.sort(component);
			components.add(component);
		}
		return components;
	}

	/**
	 * Returns a description of the graph, one dependency arrow per edge.
	 *
	 * @return a multi-line description
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Step step : steps) {
			builder.append(step);
			if (!step.getDependencies().isEmpty())
				builder.append(" after ").append(step.getDependencies());
			builder.append(System.lineSeparator());
		}
		return builder.toString();
	}
}
