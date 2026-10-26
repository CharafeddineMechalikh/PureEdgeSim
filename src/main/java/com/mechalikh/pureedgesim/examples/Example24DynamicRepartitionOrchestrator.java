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
import com.mechalikh.pureedgesim.taskgenerator.Subtask;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;

/**
 * Orchestrator that repartitions a service graph while the simulation runs.
 * <p>
 * A static plan decides once where each stage of the graph runs and keeps that
 * decision for the whole run. This orchestrator instead recomputes the plan of
 * every application every {@link #REPLAN_INTERVAL} seconds, from the state of
 * the nodes at that moment: the pending tasks, the CPU utilization, the
 * capacity of each node, and the tiers that are still alive. It also replans
 * immediately when a tier of the current plan no longer has an available node.
 * <p>
 * The plan assigns a tier to every subtask, not a node. The node inside the
 * tier is chosen per task among the reachable nodes of that tier, so mobile
 * devices that move out of range of a data center are served by another node
 * without invalidating the plan. The number of subtasks that changed tier in
 * each replan is printed as a migration count: a high count means the workload
 * moved between mist, edge, and cloud.
 * <p>
 * Run the example with {@code -Dexample24.frozenPlan=true} to keep the plan
 * computed on the first request and compare it with the periodic replanning in
 * the same scenario.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example24
 */
public class Example24DynamicRepartitionOrchestrator extends DefaultOrchestrator {

	/** Seconds between two replans of the service graph. */
	public static final double REPLAN_INTERVAL = 30;

	/** Set to {@code true} to compute the plan once and never update it. */
	protected static final boolean FROZEN_PLAN = Boolean.getBoolean("example24.frozenPlan");

	/** Tier assigned to every subtask, per application. */
	private final Map<Integer, Map<String, SimulationParameters.TYPES>> plans = new LinkedHashMap<>();
	/** Time of the last replan, per application. */
	private final Map<Integer, Double> lastPlanTime = new HashMap<>();
	/** Subtasks whose tier changed across all the replans of the run. */
	private long migrations;
	/** Number of replans performed during the run. */
	private int replans;

	/**
	 * Creates the orchestrator. The plans are built from the first request of each
	 * application, because the reachable nodes are only known once a device sends
	 * a task.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example24DynamicRepartitionOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Clears the per-application placement cache after every decision so that each
	 * subtask is routed to the tier of the current plan.
	 *
	 * @param task the task to place
	 */
	@Override
	public void orchestrate(Task task) {
		super.orchestrate(task);
		task.getEdgeDevice().setApplicationPlaced(false);
	}

	/**
	 * Returns the node selected by the plan of the application, replanning first
	 * when the plan is missing, expired, or points to a tier without available
	 * nodes.
	 *
	 * @param architecture the layers allowed by the architecture
	 * @param task         the task to place
	 * @return the index of the selected node, or -1 when the plan cannot serve the
	 *         task and the caller should fall back to the default policy
	 */
	@Override
	protected int findComputingNode(String[] architecture, Task task) {
		if (task.getSubtaskName() == null)
			return tradeOff(architecture, task);

		int application = task.getApplicationID();
		Map<String, SimulationParameters.TYPES> plan = plans.get(application);
		if (plan == null || (!FROZEN_PLAN && shouldReplan(application, plan))) {
			Map<String, SimulationParameters.TYPES> next = computePlan(architecture, task);
			if (next.isEmpty())
				return tradeOff(architecture, task);
			if (plan != null)
				migrations += countMigrations(plan, next);
			plans.put(application, next);
			double now = simulationManager.getSimulation().clock();
			lastPlanTime.put(application, now);
			replans++;
			SimLog.println("%s - %.1f s: plan of application %d recomputed, %d subtasks moved, %d migrations so far",
					getClass().getSimpleName(), now, application, plan == null ? 0 : countMigrations(plan, next),
					migrations);
			plan = next;
		}

		SimulationParameters.TYPES tier = plan.get(task.getSubtaskName());
		int selected = tier == null ? -1 : nodeOfTier(architecture, task, tier);
		return selected != -1 ? selected : tradeOff(architecture, task);
	}

	/**
	 * Returns whether the plan of an application has to be recomputed.
	 *
	 * @param application the application index
	 * @param plan        the plan in use
	 * @return {@code true} when the interval elapsed or a planned tier lost all of
	 *         its nodes
	 */
	protected boolean shouldReplan(int application, Map<String, SimulationParameters.TYPES> plan) {
		if (simulationManager.getSimulation().clock() - lastPlanTime.getOrDefault(application, 0.0) >= REPLAN_INTERVAL)
			return true;
		for (SimulationParameters.TYPES tier : plan.values())
			if (!hasAvailableNode(tier))
				return true;
		return false;
	}

	/**
	 * Returns whether at least one node of the tier is alive and not a sensor.
	 *
	 * @param tier the tier to check
	 * @return {@code true} when the tier can still host a task
	 */
	protected boolean hasAvailableNode(SimulationParameters.TYPES tier) {
		for (ComputingNode node : nodeList)
			if (node.getType() == tier && !node.isDead() && !node.isSensor())
				return true;
		return false;
	}

	/**
	 * Assigns a tier to every subtask of the application from the current load.
	 * <p>
	 * Each subtask goes to the tier whose least loaded node gives the smallest
	 * estimated cost, and that node is provisionally charged with the subtask, so
	 * the next subtask of the same graph avoids piling onto the same tier.
	 *
	 * @param architecture the layers allowed by the architecture
	 * @param origin       the task that triggered the plan, used for reachability
	 * @return the tier of every subtask, empty when no tier can host the graph
	 */
	protected Map<String, SimulationParameters.TYPES> computePlan(String[] architecture, Task origin) {
		Map<String, SimulationParameters.TYPES> plan = new LinkedHashMap<>();
		List<SimulationParameters.TYPES> tiers = availableTiers();
		Map<SimulationParameters.TYPES, Integer> assigned = new HashMap<>();
		int application = origin.getApplicationID();
		if (application < 0 || application >= SimulationParameters.applicationList.size())
			return plan;
		Application profile = SimulationParameters.applicationList.get(application);
		if (!profile.hasSubtasks())
			return plan;

		for (Subtask subtask : profile.getSubtasks()) {
			SimulationParameters.TYPES best = null;
			double bestScore = Double.MAX_VALUE;
			for (SimulationParameters.TYPES tier : tiers) {
				double score = tierScore(architecture, origin, subtask, tier, assigned.getOrDefault(tier, 0));
				if (score < bestScore) {
					bestScore = score;
					best = tier;
				}
			}
			if (best != null) {
				plan.put(subtask.getName(), best);
				assigned.merge(best, 1, Integer::sum);
			}
		}
		return plan;
	}

	/**
	 * Estimates the cost of running a subtask on a tier. The estimate follows the
	 * weights of the trade-off policy: cloud is penalized for its distance, mist
	 * for its lower capacity, and pending tasks and current CPU utilization raise
	 * the cost of a node.
	 *
	 * @param architecture the layers allowed by the architecture
	 * @param origin       the task that triggered the plan
	 * @param subtask      the subtask to estimate
	 * @param tier         the candidate tier
	 * @param virtualLoad  the number of subtasks of the same graph already planned
	 *                     on this tier
	 * @return the estimated cost, or {@link Double#MAX_VALUE} when the tier cannot
	 *         host the subtask
	 */
	protected double tierScore(String[] architecture, Task origin, Subtask subtask,
			SimulationParameters.TYPES tier, int virtualLoad) {
		double best = Double.MAX_VALUE;
		for (ComputingNode node : nodeList) {
			if (node.getType() != tier || !offloadingIsPossible(origin, node, architecture))
				continue;
			double weight = weightOf(tier);
			double load = node.getTasksQueue().size() + virtualLoad + 1;
			double cpu = 1 + node.getCurrentCpuUtilization() / 100.0;
			double cost = load * weight * cpu * subtask.getTaskLengthDistribution().mean()
					/ Math.max(1, node.getMipsPerCore());
			best = Math.min(best, cost);
		}
		return best;
	}

	/**
	 * Returns the allowed tiers that have at least one node.
	 *
	 * @return the tiers of the architecture, filtered by node availability
	 */
	protected List<SimulationParameters.TYPES> availableTiers() {
		List<SimulationParameters.TYPES> tiers = new ArrayList<>();
		for (SimulationParameters.TYPES tier : SimulationParameters.TYPES.values())
			if (hasAvailableNode(tier) && tierAllowed(tier))
				tiers.add(tier);
		return tiers;
	}

	/**
	 * Returns whether the architecture allows the tier.
	 *
	 * @param tier the tier to check
	 * @return {@code true} when the tier is part of the architecture
	 */
	protected boolean tierAllowed(SimulationParameters.TYPES tier) {
		if (tier == SimulationParameters.TYPES.CLOUD)
			return arrayContains(architectureLayers, "Cloud");
		if (tier == SimulationParameters.TYPES.EDGE_DATACENTER)
			return arrayContains(architectureLayers, "Edge");
		return arrayContains(architectureLayers, "Mist");
	}

	/**
	 * Selects the least loaded reachable node of a tier.
	 *
	 * @param architecture the layers allowed by the architecture
	 * @param task         the task to place
	 * @param tier         the tier chosen by the plan
	 * @return the index of the selected node, or -1 when no node of the tier can
	 *         host the task
	 */
	protected int nodeOfTier(String[] architecture, Task task, SimulationParameters.TYPES tier) {
		int selected = -1;
		double leastLoaded = Double.MAX_VALUE;
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (node.getType() != tier || !offloadingIsPossible(task, node, architecture))
				continue;
			if (node.getAvailableRam() < task.getContainerSizeInMBytes()
					|| node.getAvailableStorage() < task.getContainerSizeInMBytes())
				continue;
			double load = node.getTasksQueue().size() + 0.01 * node.getCurrentCpuUtilization()
					+ 0.5 * historyMap.getOrDefault(index, 0);
			if (load < leastLoaded) {
				leastLoaded = load;
				selected = index;
			}
		}
		if (selected != -1)
			historyMap.put(selected, historyMap.getOrDefault(selected, 0) + 1);
		return selected;
	}

	/**
	 * Counts the subtasks whose tier differs between two plans.
	 *
	 * @param previous the plan in use
	 * @param next     the new plan
	 * @return the number of subtasks that moved
	 */
	protected int countMigrations(Map<String, SimulationParameters.TYPES> previous,
			Map<String, SimulationParameters.TYPES> next) {
		int moved = 0;
		for (Map.Entry<String, SimulationParameters.TYPES> entry : next.entrySet())
			if (previous.containsKey(entry.getKey()) && previous.get(entry.getKey()) != entry.getValue())
				moved++;
		return moved;
	}

	private static double weightOf(SimulationParameters.TYPES tier) {
		if (tier == SimulationParameters.TYPES.CLOUD)
			return 1.8;
		if (tier == SimulationParameters.TYPES.EDGE_DEVICE)
			return 1.3;
		return 1.2;
	}
}
