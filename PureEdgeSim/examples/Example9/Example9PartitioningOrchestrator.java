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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;

/**
 * Orchestrator of the graph partitioning example.
 * <p>
 * Before the simulation starts, it partitions the service graph of every
 * application with {@link GraphPartitioner}. Each partition is then mapped to a
 * tier of the architecture, in the order mist, edge, cloud, so the stages of a
 * connected component are spread from the device to the cloud. When a task
 * arrives, its subtask name gives its partition, and the partition gives the
 * tier where the task must run.
 * <p>
 * The partitions, their loads and the edge cut are printed when the
 * orchestrator is created. Compare that cut with the one printed by
 * {@link Example10GeneticOrchestrator}: both examples solve the same problem
 * with a different search strategy.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example9
 */
public class Example9PartitioningOrchestrator extends DefaultOrchestrator {

	/** Partitions per connected component of the service graph. */
	public static final int PARTITIONS_PER_COMPONENT = 3;

	private static final String[] TIER_ORDER = { "Mist", "Edge", "Cloud" };

	private final Map<Integer, GraphPartitioner.Plan> plans = new LinkedHashMap<>();
	private final Map<Integer, Integer> cursors = new HashMap<>();
	private final String[] tiers;

	/**
	 * Creates the orchestrator and computes the partition plan of every
	 * application that declares subtasks.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example9PartitioningOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		tiers = allowedTiers();
		for (int application = 0; application < SimulationParameters.applicationList.size(); application++) {
			Application profile = SimulationParameters.applicationList.get(application);
			if (!profile.hasSubtasks())
				continue;
			GraphPartitioner.Plan plan = new GraphPartitioner().partition(ServiceGraph.of(profile),
					PARTITIONS_PER_COMPONENT);
			plans.put(application, plan);
			SimLog.println("%s - Application %d (%s): %s", getClass().getSimpleName(), application,
					profile.getType(), plan);
		}
	}

	/**
	 * Clears the per-application placement cache after every decision.
	 * <p>
	 * The simulation manager asks the orchestrator only while the application of
	 * a device is not placed, then reuses that placement for every later task of
	 * the device. A partition plan routes the subtasks of one request to
	 * different nodes, so each subtask must be decided on its own.
	 *
	 * @param task the task to place
	 */
	@Override
	public void orchestrate(Task task) {
		super.orchestrate(task);
		task.getEdgeDevice().setApplicationPlaced(false);
	}

	/**
	 * Returns the node of the tier assigned to the partition of the task.
	 *
	 * @param architecture the layers allowed by the architecture
	 * @param task         the task to place
	 * @return the index of the selected node, or -1 when the tier has no eligible
	 *         node and the caller should fall back to the default policy
	 */
	@Override
	protected int findComputingNode(String[] architecture, Task task) {
		GraphPartitioner.Plan plan = plans.get(task.getApplicationID());
		Integer partition = plan == null || task.getSubtaskName() == null ? null
				: plan.getPartitionOf().get(task.getSubtaskName());
		if (partition == null)
			return tradeOff(architecture, task);

		int selected = nodeOfTier(architecture, task, partition);
		return selected != -1 ? selected : tradeOff(architecture, task);
	}

	private int nodeOfTier(String[] architecture, Task task, int partition) {
		if (tiers.length == 0)
			return -1;
		SimulationParameters.TYPES expected = typeOf(tiers[partition % tiers.length]);
		List<Integer> eligible = new ArrayList<>();
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (node.getType() == expected && offloadingIsPossible(task, node, architecture))
				eligible.add(index);
		}
		if (eligible.isEmpty())
			return -1;

		int cursor = cursors.getOrDefault(partition, 0);
		int selected = eligible.get(cursor % eligible.size());
		cursors.put(partition, cursor + 1);
		historyMap.put(selected, historyMap.getOrDefault(selected, 0) + 1);
		return selected;
	}

	private String[] allowedTiers() {
		List<String> allowed = new ArrayList<>();
		for (String tier : TIER_ORDER)
			if (arrayContains(architectureLayers, tier))
				allowed.add(tier);
		return allowed.toArray(new String[0]);
	}

	private static SimulationParameters.TYPES typeOf(String tier) {
		if ("Mist".equals(tier))
			return SimulationParameters.TYPES.EDGE_DEVICE;
		if ("Edge".equals(tier))
			return SimulationParameters.TYPES.EDGE_DATACENTER;
		return SimulationParameters.TYPES.CLOUD;
	}
}
