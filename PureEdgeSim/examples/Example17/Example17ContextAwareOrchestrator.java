package examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Context-aware deep reinforcement learning for multi-access offloading.
 * <p>
 * The action value of every layer is approximated by a small neural network
 * whose inputs describe the task and the device context: the application type,
 * the task length, the battery level, the access technology, the device
 * mobility, and the load of each tier. Unlike the tabular agents of the
 * previous examples, a context the agent has never seen still produces a
 * sensible value, because nearby contexts share the same weights.
 * <p>
 * Training is online semi-gradient temporal-difference learning with a target
 * network that is refreshed every {@link #TARGET_SYNC} updates. The reward is
 * the negated delay normalized by the task latency, and a failed task receives
 * a penalty of one. The layer returned by the network is executed by its least
 * loaded eligible node.
 *
 * @since PureEdgeSim 6.0
 */
public class Example17ContextAwareOrchestrator extends Orchestrator {

	/** Number of units in the hidden layer. */
	static final int HIDDEN_UNITS = 12;
	/** Application type one-hot (3), length, battery, cellular, mobile, and 3 tier loads. */
	static final int FEATURE_COUNT = 10;
	/** Index of the battery-level feature. */
	static final int BATTERY_FEATURE = 4;
	/** Index of the cellular-access feature. */
	static final int CELLULAR_FEATURE = 5;
	/** Index of the mobility feature. */
	static final int MOBILE_FEATURE = 6;
	/** The three tiers, in the order used by the network outputs. */
	static final String[] TIERS = { "Mist", "Edge", "Cloud" };
	/** Step size of the stochastic gradient descent. */
	static final double LEARNING_RATE = 0.02;
	/** Weight of the value of the next state. */
	static final double DISCOUNT_RATE = 0.9;
	/** Initial probability of a random action. */
	static final double EPSILON_START = 0.3;
	/** Lowest probability of a random action. */
	static final double EPSILON_END = 0.02;
	/** Multiplicative decay of the exploration probability per decision. */
	static final double EPSILON_DECAY = 0.999;
	/** Reward of a task that fails. */
	static final double FAILURE_PENALTY = -1.0;
	/** Number of updates between two copies of the online network into the target one. */
	static final int TARGET_SYNC = 200;

	private final double[][] hiddenWeights = new double[HIDDEN_UNITS][FEATURE_COUNT];
	private final double[] hiddenBias = new double[HIDDEN_UNITS];
	private final double[][] outputWeights = new double[TIERS.length][HIDDEN_UNITS];
	private final double[] outputBias = new double[TIERS.length];

	private final double[][] targetHiddenWeights = new double[HIDDEN_UNITS][FEATURE_COUNT];
	private final double[] targetHiddenBias = new double[HIDDEN_UNITS];
	private final double[][] targetOutputWeights = new double[TIERS.length][HIDDEN_UNITS];
	private final double[] targetOutputBias = new double[TIERS.length];

	private final Map<String, Integer> pendingPerTier = new HashMap<>();
	private final Map<Integer, Decision> decisions = new LinkedHashMap<>();
	private final int[] pendingPerNode;
	private double epsilon = EPSILON_START;
	private int updates = 0;

	/**
	 * Creates the orchestrator, checks the algorithm name declared in the settings
	 * file, and initializes the two networks with the simulation random source.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example17ContextAwareOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"CONTEXT_AWARE".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected CONTEXT_AWARE.");
		pendingPerNode = new int[nodeList.size()];
		for (int unit = 0; unit < HIDDEN_UNITS; unit++) {
			for (int feature = 0; feature < FEATURE_COUNT; feature++)
				hiddenWeights[unit][feature] = (Randomness.nextDouble() - 0.5) * 0.4;
		}
		for (int tier = 0; tier < TIERS.length; tier++)
			for (int unit = 0; unit < HIDDEN_UNITS; unit++)
				outputWeights[tier][unit] = (Randomness.nextDouble() - 0.5) * 0.2;
		synchronizeTarget();
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;

		double[] state = features(task);
		List<Integer> available = availableActions(architectureLayers, task);
		if (available.isEmpty())
			return -1;

		int action = epsilonGreedy(state, available);
		String tier = TIERS[action];
		int nodeIndex = nodeInTier(tier, architectureLayers, task);
		if (nodeIndex < 0)
			return -1;

		pendingPerNode[nodeIndex]++;
		pendingPerTier.merge(tier, 1, Integer::sum);
		decisions.put(task.getId(), new Decision(state, action, tier, nodeIndex));
		epsilon = Math.max(EPSILON_END, epsilon * EPSILON_DECAY);
		return nodeIndex;
	}

	@Override
	public void resultsReturned(Task task) {
		Decision decision = decisions.remove(task.getId());
		if (decision == null)
			return;
		pendingPerTier.merge(decision.tier, -1, Integer::sum);
		if (pendingPerNode[decision.node] > 0)
			pendingPerNode[decision.node]--;

		double reward = task.getStatus() == Task.Status.SUCCESS
				? -task.getTotalDelay() / Math.max(task.getMaxLatency(), 1e-9)
				: FAILURE_PENALTY;
		train(decision.state, decision.action, reward, features(task));
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/** The context vector of a task, exposed for the example test. */
	public double[] getFeatures(Task task) {
		return features(task);
	}

	/** The value the online network gives to a state and a canonical tier action. */
	public double getQValue(double[] state, int action) {
		return forward(state, hiddenWeights, hiddenBias, outputWeights, outputBias)[action];
	}

	/** Number of gradient updates performed so far, exposed for the example test. */
	public int getUpdates() {
		return updates;
	}

	private List<Integer> availableActions(String[] layers, Task task) {
		List<Integer> actions = new ArrayList<>();
		for (int action = 0; action < TIERS.length; action++)
			if (arrayContains(layers, TIERS[action]) && nodeInTier(TIERS[action], layers, task) >= 0)
				actions.add(action);
		return actions;
	}

	private int epsilonGreedy(double[] state, List<Integer> available) {
		if (Randomness.nextDouble() < epsilon)
			return available.get(Randomness.nextInt(available.size()));
		double[] values = forward(state, hiddenWeights, hiddenBias, outputWeights, outputBias);
		int best = available.get(0);
		for (int action : available)
			if (values[action] > values[best])
				best = action;
		return best;
	}

	private void train(double[] state, int action, double reward, double[] nextState) {
		double target = reward + DISCOUNT_RATE * maxTargetValue(nextState);

		double[] hidden = activation(state, hiddenWeights, hiddenBias);
		double[] output = output(hidden, outputWeights, outputBias);
		double delta = Math.max(-1, Math.min(1, target - output[action]));

		for (int unit = 0; unit < HIDDEN_UNITS; unit++)
			outputWeights[action][unit] += LEARNING_RATE * delta * hidden[unit];
		outputBias[action] += LEARNING_RATE * delta;

		double[] hiddenGradient = new double[HIDDEN_UNITS];
		for (int unit = 0; unit < HIDDEN_UNITS; unit++)
			hiddenGradient[unit] = delta * outputWeights[action][unit] * (1 - hidden[unit] * hidden[unit]);
		for (int unit = 0; unit < HIDDEN_UNITS; unit++) {
			for (int feature = 0; feature < FEATURE_COUNT; feature++)
				hiddenWeights[unit][feature] += LEARNING_RATE * hiddenGradient[unit] * state[feature];
			hiddenBias[unit] += LEARNING_RATE * hiddenGradient[unit];
		}

		updates++;
		if (updates % TARGET_SYNC == 0)
			synchronizeTarget();
	}

	private double maxTargetValue(double[] state) {
		double[] values = forward(state, targetHiddenWeights, targetHiddenBias, targetOutputWeights, targetOutputBias);
		double max = Double.NEGATIVE_INFINITY;
		for (double value : values)
			max = Math.max(max, value);
		return max;
	}

	private void synchronizeTarget() {
		for (int unit = 0; unit < HIDDEN_UNITS; unit++) {
			System.arraycopy(hiddenWeights[unit], 0, targetHiddenWeights[unit], 0, FEATURE_COUNT);
			targetHiddenBias[unit] = hiddenBias[unit];
		}
		for (int tier = 0; tier < TIERS.length; tier++) {
			System.arraycopy(outputWeights[tier], 0, targetOutputWeights[tier], 0, HIDDEN_UNITS);
			targetOutputBias[tier] = outputBias[tier];
		}
	}

	private double[] features(Task task) {
		double[] state = new double[FEATURE_COUNT];
		String type = task.getType();
		state["Hard Real-time".equals(type) ? 0 : "Soft Real-time".equals(type) ? 1 : 2] = 1;
		state[3] = Math.min(1, task.getLength() / 100_000.0);

		ComputingNode device = task.getEdgeDevice();
		if (device != null) {
			EnergyModelComputingNode energy = device.getEnergyModel();
			if (energy != null) {
				state[BATTERY_FEATURE] = energy.isBatteryPowered() ? energy.getBatteryLevelPercentage() / 100.0 : 1.0;
				state[CELLULAR_FEATURE] = "cellular".equals(energy.getConnectivityType()) ? 1 : 0;
			}
			state[MOBILE_FEATURE] = device.getMobilityModel() != null && device.getMobilityModel().isMobile() ? 1 : 0;
		}
		for (int tier = 0; tier < TIERS.length; tier++)
			state[7 + tier] = load(TIERS[tier]);
		return state;
	}

	private double load(String tier) {
		int nodes = 0;
		for (ComputingNode node : nodeList)
			if (tier.equals(tierOf(node)))
				nodes++;
		double average = nodes == 0 ? 0 : (double) pendingPerTier.getOrDefault(tier, 0) / nodes;
		return Math.min(1, average / 2.0);
	}

	private double[] forward(double[] state, double[][] firstWeights, double[] firstBias,
			double[][] secondWeights, double[] secondBias) {
		return output(activation(state, firstWeights, firstBias), secondWeights, secondBias);
	}

	private static double[] activation(double[] state, double[][] weights, double[] bias) {
		double[] hidden = new double[HIDDEN_UNITS];
		for (int unit = 0; unit < HIDDEN_UNITS; unit++) {
			double sum = bias[unit];
			for (int feature = 0; feature < FEATURE_COUNT; feature++)
				sum += weights[unit][feature] * state[feature];
			hidden[unit] = Math.tanh(sum);
		}
		return hidden;
	}

	private static double[] output(double[] hidden, double[][] weights, double[] bias) {
		double[] values = new double[TIERS.length];
		for (int tier = 0; tier < TIERS.length; tier++) {
			double sum = bias[tier];
			for (int unit = 0; unit < HIDDEN_UNITS; unit++)
				sum += weights[tier][unit] * hidden[unit];
			values[tier] = sum;
		}
		return values;
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
		private final double[] state;
		private final int action;
		private final String tier;
		private final int node;

		private Decision(double[] state, int action, String tier, int node) {
			this.state = state;
			this.action = action;
			this.tier = tier;
			this.node = node;
		}
	}
}
