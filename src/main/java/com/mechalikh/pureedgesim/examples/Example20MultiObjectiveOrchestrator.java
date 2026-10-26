package examples;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * Multi-objective reinforcement learning with two scalarized value tables.
 * <p>
 * The agent learns the delay and the energy of every layer separately, then
 * combines them with fixed weights when it chooses. Two tables keep the
 * objectives distinguishable, so the weights can be changed without retraining
 * from scratch, and the energy table makes the agent aware that a cloud task
 * consumes far more than a mist one. The delay reward is the negated delay
 * normalized by the task latency; the energy reward is the negated estimated
 * energy normalized by a reference of {@link #REFERENCE_ENERGY_WH}. A failed
 * task applies the same failure penalty to both tables.
 *
 * @since PureEdgeSim 6.0
 */
public class Example20MultiObjectiveOrchestrator extends Orchestrator {

	/** Weight of the delay value in the decision. */
	static final double DELAY_WEIGHT = 1.0;
	/** Weight of the energy value in the decision. */
	static final double ENERGY_WEIGHT = 1.0;
	/** Energy, in watt-hours, that maps to a reward of minus one. */
	static final double REFERENCE_ENERGY_WH = 0.01;
	/** Weight of the new reward in the action value. */
	static final double LEARNING_RATE = 0.2;
	/** Weight of the value of the next state. */
	static final double DISCOUNT_RATE = 0.9;
	/** Probability of a random action. */
	static final double EPSILON = 0.1;
	/** Value of a failed task in both tables. */
	static final double FAILURE_PENALTY = 200.0;

	private final Map<String, double[]> delayValues = new LinkedHashMap<>();
	private final Map<String, double[]> energyValues = new LinkedHashMap<>();
	private final Map<Integer, Decision> decisions = new HashMap<>();
	private final int[] pendingPerNode;
	private final int actionCount;

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example20MultiObjectiveOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"SCALARIZED_Q".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected SCALARIZED_Q.");
		pendingPerNode = new int[nodeList.size()];
		actionCount = architectureLayers.length;
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;

		String state = stateKey(task);
		int action = -1;
		if (Randomness.nextDouble() < EPSILON) {
			action = randomFeasibleAction(architectureLayers, task);
		} else {
			double bestScore = Double.NEGATIVE_INFINITY;
			for (int index = 0; index < architectureLayers.length; index++) {
				if (nodeInTier(architectureLayers[index], architectureLayers, task) < 0)
					continue;
				double score = DELAY_WEIGHT * value(delayValues, state, index)
						+ ENERGY_WEIGHT * value(energyValues, state, index);
				if (score > bestScore) {
					bestScore = score;
					action = index;
				}
			}
		}
		if (action < 0)
			return -1;

		String tier = architectureLayers[action];
		int nodeIndex = nodeInTier(tier, architectureLayers, task);
		pendingPerNode[nodeIndex]++;
		decisions.put(task.getId(), new Decision(state, action, nodeIndex));
		return nodeIndex;
	}

	@Override
	public void resultsReturned(Task task) {
		Decision decision = decisions.remove(task.getId());
		if (decision == null)
			return;
		if (pendingPerNode[decision.node] > 0)
			pendingPerNode[decision.node]--;

		double delayReward;
		double energyReward;
		if (task.getStatus() == Task.Status.SUCCESS) {
			delayReward = -task.getTotalDelay() / Math.max(task.getMaxLatency(), 1e-9);
			energyReward = -estimatedEnergy(task, decision.node) / REFERENCE_ENERGY_WH;
		} else {
			delayReward = -FAILURE_PENALTY;
			energyReward = -FAILURE_PENALTY;
		}
		update(delayValues, decision.state, decision.action, delayReward);
		update(energyValues, decision.state, decision.action, energyReward);
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/** The delay value of a state and action, exposed for the example test. */
	public double getDelayValue(String state, int action) {
		return value(delayValues, state, action);
	}

	/** The energy value of a state and action, exposed for the example test. */
	public double getEnergyValue(String state, int action) {
		return value(energyValues, state, action);
	}

	/** The state key used by both tables, exposed so the example test can read it. */
	public String getStateKey(Task task) {
		return stateKey(task);
	}

	/** The estimated energy, in watt-hours, of a task on the node at the given index. */
	public double estimatedEnergy(Task task, int nodeIndex) {
		ComputingNode node = nodeList.get(nodeIndex);
		EnergyModelComputingNode energy = node.getEnergyModel();
		double power = energy == null ? 0 : energy.getMaxActiveConsumption();
		double capacity = Math.max(1, node.getMipsPerCore() * node.getNumberOfCPUCores());
		return power * (task.getLength() / capacity) / 3600.0;
	}

	private void update(Map<String, double[]> table, String state, int action, double reward) {
		double[] actionValues = table.computeIfAbsent(state, key -> new double[actionCount]);
		double next = maxValue(table, state);
		actionValues[action] += LEARNING_RATE * (reward + DISCOUNT_RATE * next - actionValues[action]);
	}

	private int randomFeasibleAction(String[] layers, Task task) {
		int feasible = 0;
		int chosen = -1;
		for (int index = 0; index < layers.length; index++) {
			if (nodeInTier(layers[index], layers, task) < 0)
				continue;
			feasible++;
			if (Randomness.nextInt(feasible) == 0)
				chosen = index;
		}
		return chosen;
	}

	private int nodeInTier(String tier, String[] layers, Task task) {
		int best = -1;
		int bestPending = Integer.MAX_VALUE;
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (!tier.equals(tierOf(node)) || !offloadingIsPossible(task, node, layers))
				continue;
			if (pendingPerNode[index] < bestPending) {
				bestPending = pendingPerNode[index];
				best = index;
			}
		}
		return best;
	}

	private String stateKey(Task task) {
		double length = task.getLength();
		int bucket = length < 1000 ? 0 : length < 5000 ? 1 : length < 20000 ? 2 : 3;
		return task.getType() + "|" + bucket;
	}

	private static double value(Map<String, double[]> table, String state, int action) {
		double[] actionValues = table.get(state);
		return actionValues == null ? 0 : actionValues[action];
	}

	private static double maxValue(Map<String, double[]> table, String state) {
		double[] actionValues = table.get(state);
		if (actionValues == null)
			return 0;
		double max = Double.NEGATIVE_INFINITY;
		for (double actionValue : actionValues)
			max = Math.max(max, actionValue);
		return max == Double.NEGATIVE_INFINITY ? 0 : max;
	}

	private static String tierOf(ComputingNode node) {
		if (node.getType() == SimulationParameters.TYPES.CLOUD)
			return "Cloud";
		if (node.getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
			return "Edge";
		if (node.getType() == SimulationParameters.TYPES.EDGE_DEVICE)
			return "Mist";
		return "Unknown";
	}

	private static final class Decision {
		private final String state;
		private final int action;
		private final int node;

		private Decision(String state, int action, int node) {
			this.state = state;
			this.action = action;
			this.node = node;
		}
	}
}
