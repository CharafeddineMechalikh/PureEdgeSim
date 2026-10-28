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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * Placement rules learned by tabular Q-learning on a pure edge fleet.
 * <p>
 * There is no server in the scenario, so the only decision left to the
 * orchestrator is which device of the fleet runs the task. That decision is
 * taken through a rule: a way of ranking the devices that are currently
 * reachable. Four rules are offered to the agent, the load of the device, its
 * speed, the energy it has left, and the distance the data has to travel. The
 * agent does not place tasks, it learns which of those four ways of ranking the
 * fleet brings the most tasks back for the kind of traffic it is shown, which is
 * the adaptive part of the paper.
 * <p>
 * The state of the agent is the class of the task, a bucket of its length, and a
 * bucket of how much of the fleet is already busy. The reward is the one of
 * FTR-AdWOrch: a task that comes back is worth one, a task that is lost is worth
 * nothing minus a penalty scaled by the class of the task (a hard real-time miss
 * costs three times, a soft real-time one 1.5 times), by how far the results
 * missed their budget, and by how much the container overflowed the storage that
 * was free when the decision was taken. The step is 1 over the number of times
 * the rule was tried for that state, capped at 200, and the exploration rate is
 * six over the simulated clock, so the agent keeps exploring while the fleet is
 * still empty of knowledge and settles once it is not. There is no discount:
 * every task is a decision of its own.
 * <p>
 * The learning is wrapped around the robustness mechanisms of the scenario. A
 * device that cannot host the container any more, or that is almost flat and is
 * asked for elastic work, refuses it, and the orchestrator moves the task to the
 * next device of the same ranking: that is the reallocation. Every task that had
 * to move is counted, so a run can be compared with a variant where devices keep
 * work they can no longer afford.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example26
 * @see Example26OrchestratedDevice
 */
public class Example26FaultTolerantOrchestrator extends Orchestrator {

	/** Type of the traffic that must never be late. */
	static final String HARD_REAL_TIME = "Hard Real-time";
	/** Type of the traffic that may be late once in a while. */
	static final String SOFT_REAL_TIME = "Soft Real-time";

	/** Numerator of the exploration rate, which is this constant over the clock. */
	static final double EXPLORATION_CONSTANT = 6.0;
	/** The learning rate is one over the number of tries, never smaller than one over this. */
	static final int MAX_LEARNING_RATE_DECAY = 200;
	/** Weight of the latency penalty of a hard real-time task that failed. */
	static final double HRT_FAILURE_WEIGHT = 3.0;
	/** Weight of the latency penalty of a soft real-time task that failed. */
	static final double SRT_FAILURE_WEIGHT = 1.5;
	/** Remaining energy, in percent, under which a device reallocates its elastic work. */
	static final int REALLOCATION_BATTERY_LEVEL = 10;

	/** The ways of ranking the fleet that the agent chooses between. */
	enum Rule {
		/** Serve the device with the least work already in hand. */
		LEAST_LOADED,
		/** Serve the fastest device. */
		FASTEST,
		/** Serve the device with the most energy left. */
		MOST_POWER,
		/** Serve the device closest to the one that sent the task. */
		NEAREST
	}

	private final Map<String, double[]> qTable = new LinkedHashMap<>();
	private final Map<String, int[]> visitCounts = new LinkedHashMap<>();
	private final Map<Integer, Decision> decisions = new HashMap<>();
	private final Map<Rule, int[]> ruleUsage = new EnumMap<>(Rule.class);
	private final int[] assignments;
	private final int ruleCount = Rule.values().length;
	private int pendingTasks;
	private int reallocatedTasks;
	private int explorations;
	private int unplacedTasks;

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example26FaultTolerantOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"FTR_ADWORCH".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected FTR_ADWORCH.");
		assignments = new int[nodeList.size()];
		for (Rule rule : Rule.values())
			ruleUsage.put(rule, new int[3]);
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;
		List<Integer> eligible = eligibleNodes(architectureLayers, task);
		if (eligible.isEmpty()) {
			unplacedTasks++;
			return -1;
		}

		String state = stateKey(task, eligible.size());
		Rule rule = chooseRule(state);
		List<Integer> ranked = rank(rule, eligible, task);
		int chosen = firstAccepting(ranked, task);
		if (chosen < 0)
			// Nothing can take the task: keep the best device of the ranking and let the
			// simulation report why the task failed.
			chosen = ranked.get(0);
		if (chosen != ranked.get(0))
			reallocatedTasks++;

		assignments[chosen]++;
		pendingTasks++;
		decisions.put(task.getId(),
				new Decision(state, rule, chosen, nodeList.get(chosen).getAvailableStorage()));
		return chosen;
	}

	@Override
	public void resultsReturned(Task task) {
		Decision decision = decisions.remove(task.getId());
		if (decision == null)
			return;
		assignments[decision.nodeIndex]--;
		pendingTasks--;

		boolean succeeded = task.getStatus() == Task.Status.SUCCESS;
		double value = (succeeded ? 1 : 0) - failurePenalty(task, decision);
		double[] values = qTable.computeIfAbsent(decision.state, key -> new double[ruleCount]);
		int[] visits = visitCounts.computeIfAbsent(decision.state, key -> new int[ruleCount]);
		int action = decision.rule.ordinal();
		visits[action]++;
		values[action] += (value - values[action]) / Math.min(visits[action], MAX_LEARNING_RATE_DECAY);

		int[] usage = ruleUsage.get(decision.rule);
		usage[0]++;
		usage[succeeded ? 1 : 2]++;
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/**
	 * Returns the learned value of one rule in one state, used by the example test
	 * and printed by the run summary.
	 *
	 * @param state the state key, as returned by {@link #getStateKey(Task, int)}
	 * @param rule  the placement rule
	 * @return the value of that rule for that state, zero when it was never tried
	 */
	public double getQValue(String state, Rule rule) {
		double[] values = qTable.get(state);
		return values == null ? 0 : values[rule.ordinal()];
	}

	/**
	 * Builds the state key the agent uses for the given task.
	 *
	 * @param task          the task being placed
	 * @param eligibleCount how many devices could take it
	 * @return the state key
	 */
	public String getStateKey(Task task, int eligibleCount) {
		return stateKey(task, eligibleCount);
	}

	/**
	 * @param rule the placement rule to look up
	 * @return how many tasks it was chosen for, how many of them came back, and how
	 *         many were lost
	 */
	public int[] getRuleUsage(Rule rule) {
		return ruleUsage.get(rule).clone();
	}

	/**
	 * @return how many times the agent explored instead of exploiting
	 */
	public int getExplorations() {
		return explorations;
	}

	/**
	 * @return how many tasks had to be moved to another device than the first one
	 *         of the ranking
	 */
	public int getReallocatedTasks() {
		return reallocatedTasks;
	}

	/**
	 * @return how many tasks found no device that could take them at all
	 */
	public int getUnplacedTasks() {
		return unplacedTasks;
	}

	/**
	 * Lists the devices that may execute the task under the architecture of the run.
	 *
	 * @param architectureLayers the layers allowed by the architecture
	 * @param task               the task being placed
	 * @return the indexes of the eligible devices in {@code nodeList}
	 */
	private List<Integer> eligibleNodes(String[] architectureLayers, Task task) {
		List<Integer> eligible = new ArrayList<>();
		for (int index = 0; index < nodeList.size(); index++)
			if (offloadingIsPossible(task, nodeList.get(index), architectureLayers))
				eligible.add(index);
		return eligible;
	}

	/**
	 * Picks the rule with the highest learned value, except when the run is still
	 * exploring.
	 *
	 * @param state the state of the task
	 * @return the rule used for this decision
	 */
	private Rule chooseRule(String state) {
		double clock = Math.max(1, simulationManager.getSimulation().clock());
		if (Randomness.nextDouble() < Math.min(1, EXPLORATION_CONSTANT / clock)) {
			explorations++;
			return Rule.values()[Randomness.nextInt(ruleCount)];
		}
		double[] values = qTable.get(state);
		if (values == null)
			return Rule.values()[0];
		int best = 0;
		for (int action = 1; action < ruleCount; action++)
			if (values[action] > values[best])
				best = action;
		return Rule.values()[best];
	}

	/**
	 * Orders the eligible devices the way the given rule ranks them.
	 *
	 * @param rule     the rule being applied
	 * @param eligible the indexes of the devices that may take the task
	 * @param task     the task being placed
	 * @return the same indexes, best candidate first
	 */
	private List<Integer> rank(Rule rule, List<Integer> eligible, Task task) {
		List<Integer> ranked = new ArrayList<>(eligible);
		ranked.sort(comparator(rule, task));
		return ranked;
	}

	/**
	 * Builds the comparator of a rule. The devices that were already given work by
	 * this orchestrator and have not returned it yet are the ones carrying that
	 * work, which is what the load of a fleet means between decisions.
	 * <p>
	 * Every rule is broken by the load in hand, so that a rule that ranks the fleet
	 * by a property shared by many devices, the speed of a class or the charge of
	 * devices that are not used, spreads its traffic over that class instead of
	 * emptying it onto its first member.
	 *
	 * @param rule the rule being applied
	 * @param task the task being placed, needed for the distance
	 * @return the comparator that ranks the fleet for that rule
	 */
	private Comparator<Integer> comparator(Rule rule, Task task) {
		Comparator<Integer> byLoad = Comparator.comparingInt(this::load);
		switch (rule) {
		case FASTEST:
			return Comparator.<Integer>comparingDouble(index -> -nodeList.get(index).getMipsPerCore())
					.thenComparing(byLoad);
		case MOST_POWER:
			return Comparator.<Integer>comparingDouble(index -> -battery(nodeList.get(index)))
					.thenComparing(byLoad);
		case NEAREST:
			return Comparator.<Integer>comparingDouble(index -> distance(task, nodeList.get(index)))
					.thenComparing(byLoad);
		default:
			return byLoad.thenComparingDouble(index -> -nodeList.get(index).getMipsPerCore());
		}
	}

	/**
	 * Walks the ranked devices and returns the first one that accepts the task.
	 *
	 * @param ranked the eligible devices, best candidate first
	 * @param task   the task being placed
	 * @return the index of the first device that accepts it, or -1 when none does
	 */
	private int firstAccepting(List<Integer> ranked, Task task) {
		for (int index : ranked) {
			ComputingNode node = nodeList.get(index);
			if (node instanceof Example26OrchestratedDevice) {
				if (((Example26OrchestratedDevice) node).accepts(task))
					return index;
			} else if (!node.isDead())
				return index;
		}
		return -1;
	}

	/**
	 * The state of the fleet for a task: what the task is, how big it is, and how
	 * much of the fleet is already busy with the tasks placed here.
	 *
	 * @param task          the task being placed
	 * @param eligibleCount how many devices could take it
	 * @return the state key
	 */
	private String stateKey(Task task, int eligibleCount) {
		int length = lengthBucket(task.getLength());
		int load = loadBucket(eligibleCount);
		return task.getType() + "|" + length + "|" + load;
	}

	/**
	 * Buckets the length of a task with the thresholds of the paper, which are the
	 * bounds of its fuzzy terms: very low under 2000 million instructions, then up
	 * to very high above 40000.
	 *
	 * @param length the length of the task, in million instructions
	 * @return the bucket, from 0 to 5
	 */
	private int lengthBucket(double length) {
		if (length < 2000)
			return 0;
		if (length < 5000)
			return 1;
		if (length < 10000)
			return 2;
		if (length < 20000)
			return 3;
		return length < 40000 ? 4 : 5;
	}

	/**
	 * Buckets how many of the eligible devices already have work in hand.
	 *
	 * @param eligibleCount how many devices could take the task
	 * @return the bucket, 0 when the fleet is free, 1 when each device carries one
	 *         task, 2 above that
	 */
	private int loadBucket(int eligibleCount) {
		if (pendingTasks == 0 || eligibleCount == 0)
			return 0;
		return pendingTasks <= eligibleCount ? 1 : 2;
	}

	/**
	 * Number of tasks this orchestrator gave to a device and has not seen come back.
	 *
	 * @param index the index of the device in the candidate list
	 * @return the number of tasks in hand
	 */
	private int load(int index) {
		return assignments[index];
	}

	/**
	 * Remaining energy of a device, in percent. A device that is not battery powered
	 * is taken as fully charged, so that the energy rule never prefers an empty
	 * phone over a machine that cannot run out.
	 *
	 * @param node the device to measure
	 * @return the remaining energy, in percent
	 */
	private double battery(ComputingNode node) {
		if (!node.getEnergyModel().isBatteryPowered())
			return 100;
		return node.getEnergyModel().getBatteryLevelPercentage();
	}

	/**
	 * Distance between the device that sent the task and a candidate device.
	 *
	 * @param task   the task being placed
	 * @param candidate a device of the fleet
	 * @return the distance, in meters
	 */
	private double distance(Task task, ComputingNode candidate) {
		if (candidate.getType() != SimulationParameters.TYPES.EDGE_DEVICE
				|| task.getEdgeDevice() == null || task.getEdgeDevice() == candidate)
			return 0;
		return task.getEdgeDevice().getMobilityModel().distanceTo(candidate);
	}

	/**
	 * The penalty the reward of a failed task is reduced by.
	 * <p>
	 * A task that came back costs nothing. A lost task costs the weight of its
	 * class
	 * times how far its results missed the budget, plus how much its container
	 * overflowed the storage that was free when the decision was taken. A failure
	 * that is neither of those, a device that died for example, costs the loss of
	 * the task itself and nothing more.
	 *
	 * @param task     the task whose results came back
	 * @param decision the placement that was chosen for it
	 * @return the penalty, zero for a task that succeeded
	 */
	private double failurePenalty(Task task, Decision decision) {
		if (task.getStatus() == Task.Status.SUCCESS)
			return 0;
		double weight = 1;
		if (HARD_REAL_TIME.equals(task.getType()))
			weight = HRT_FAILURE_WEIGHT;
		else if (SOFT_REAL_TIME.equals(task.getType()))
			weight = SRT_FAILURE_WEIGHT;
		double delayPenalty = 0;
		if (task.getFailureReason() == Task.FailureReason.FAILED_DUE_TO_LATENCY && task.getMaxLatency() > 0)
			delayPenalty = task.getTotalDelay() / task.getMaxLatency();
		double storagePenalty = 0;
		if (task.getFailureReason() == Task.FailureReason.INSUFFICIENT_RESOURCES && decision.availableStorage > 0) {
			double overflow = task.getContainerSizeInMBytes() / decision.availableStorage;
			if (overflow > 1)
				storagePenalty = overflow;
		}
		return weight * delayPenalty + storagePenalty;
	}

	/** The placement chosen for one task, kept until its results come back. */
	private static final class Decision {
		private final String state;
		private final Rule rule;
		private final int nodeIndex;
		private final double availableStorage;

		private Decision(String state, Rule rule, int nodeIndex, double availableStorage) {
			this.state = state;
			this.rule = rule;
			this.nodeIndex = nodeIndex;
			this.availableStorage = availableStorage;
		}
	}
}
