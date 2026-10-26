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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;

/**
 * Orchestrator of the genetic partitioning example.
 * <p>
 * Before the simulation starts, it builds one candidate per computing node of
 * the scenario and runs {@link GeneticPartitioner} on the service graph of every
 * application that declares subtasks. The best chromosome gives the node of
 * every subtask, and the subtasks placed on the same node form a partition.
 * <p>
 * The plan is static: it is computed once, from the mean task lengths and
 * result sizes, before any device moves. When the chosen node is not eligible
 * for a request, for example a mist device that is now out of range, the
 * orchestrator places that subtask with the default policy instead.
 * <p>
 * The console prints the estimated fitness of the best plan and the node chosen
 * for each subtask. Compare it with {@link Example9PartitioningOrchestrator},
 * which derives a partition from the graph structure instead of searching for
 * one.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example10
 * @see GeneticPartitioner
 */
public class Example10GeneticOrchestrator extends DefaultOrchestrator {

	/** Seed used when the scenario does not set {@code random_seed}. */
	public static final long DEFAULT_SEED = 20261026L;

	private final Map<Integer, Map<String, Integer>> placement = new LinkedHashMap<>();

	/**
	 * Creates the orchestrator and searches the placement of every service graph.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example10GeneticOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		List<Integer> indices = new ArrayList<>();
		List<GeneticPartitioner.Candidate> candidates = new ArrayList<>();
		Set<Integer> seen = new LinkedHashSet<>();
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (node.isSensor() || !seen.add(node.getId()))
				continue;
			indices.add(index);
			candidates.add(new GeneticPartitioner.Candidate(node.getId(), labelOf(node), node.getMipsPerCore(),
					bandwidthOf(node)));
		}

		long seed = Randomness.isSeeded() ? Randomness.getSeed() : DEFAULT_SEED;
		for (int application = 0; application < SimulationParameters.applicationList.size(); application++) {
			Application profile = SimulationParameters.applicationList.get(application);
			if (!profile.hasSubtasks())
				continue;
			ServiceGraph graph = ServiceGraph.of(profile);
			GeneticPartitioner.Plan plan = new GeneticPartitioner(seed).partition(graph, candidates);
			Map<String, Integer> nodes = new LinkedHashMap<>();
			for (String subtask : plan.placement().keySet())
				nodes.put(subtask, plan.candidateIndex(graph.indexOf(subtask)));
			placement.put(application, nodes);
			SimLog.println("%s - Application %d (%s): %s", getClass().getSimpleName(), application,
					profile.getType(), plan);
		}
		this.indices = indices;
	}

	private final List<Integer> indices;

	/**
	 * Clears the per-application placement cache after every decision.
	 * <p>
	 * The plan assigns different subtasks of one request to different nodes, so
	 * the simulation manager must ask the orchestrator for each of them instead
	 * of reusing the placement of the previous one.
	 *
	 * @param task the task to place
	 */
	@Override
	public void orchestrate(Task task) {
		super.orchestrate(task);
		task.getEdgeDevice().setApplicationPlaced(false);
	}

	/**
	 * Returns the index of the node chosen by the genetic algorithm.
	 *
	 * @param architecture the layers allowed by the architecture
	 * @param task         the task to place
	 * @return the index of the node in the candidate list, or the default policy
	 *         when the chosen node is not eligible for this request
	 */
	@Override
	protected int findComputingNode(String[] architecture, Task task) {
		Map<String, Integer> nodes = placement.get(task.getApplicationID());
		if (nodes == null || task.getSubtaskName() == null)
			return tradeOff(architecture, task);

		Integer gene = nodes.get(task.getSubtaskName());
		if (gene == null || gene < 0 || gene >= indices.size())
			return tradeOff(architecture, task);

		int index = indices.get(gene);
		if (!offloadingIsPossible(task, nodeList.get(index), architecture))
			return tradeOff(architecture, task);
		historyMap.put(index, historyMap.getOrDefault(index, 0) + 1);
		return index;
	}

	private static String labelOf(ComputingNode node) {
		if (node.getType() == SimulationParameters.TYPES.CLOUD)
			return "Cloud";
		if (node.getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
			return "Edge";
		return "Mist";
	}

	private static double bandwidthOf(ComputingNode node) {
		if (node.getType() == SimulationParameters.TYPES.CLOUD)
			return SimulationParameters.wanBandwidthBitsPerSecond;
		if (node.getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
			return SimulationParameters.manBandwidthBitsPerSecond;
		return SimulationParameters.wifiBandwidthBitsPerSecond;
	}
}
