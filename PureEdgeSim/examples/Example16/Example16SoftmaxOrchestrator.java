package examples;

import java.util.ArrayList;
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
 * Softmax action selection for smart-city offloading.
 * <p>
 * The agent keeps one value per application type, task-length bucket, and
 * allowed layer. Instead of the epsilon-greedy rule of Example 11, it samples
 * the layer from a Boltzmann distribution over the values, so better layers are
 * picked more often and exploration decreases as the values separate. A layer
 * is executed by its least loaded eligible node. The reward is the negated
 * delay, normalized by the task latency, and a failed task receives a large
 * penalty.
 *
 * @since PureEdgeSim 6.0
 */
public class Example16SoftmaxOrchestrator extends Orchestrator {

	/** Temperature of the Boltzmann distribution, in normalized reward units. */
	static final double TEMPERATURE = 0.05;
	/** Weight of the new reward in the action value. */
	static final double LEARNING_RATE = 0.2;
	/** Weight of the value of the next state. */
	static final double DISCOUNT_RATE = 0.9;
	/** Reward of a task that fails, large enough to dominate any delay saving. */
	static final double FAILURE_PENALTY = 10.0;

	private final Map<String, double[]> values = new LinkedHashMap<>();
	private final Map<String, Integer> pendingPerTier = new HashMap<>();
	private final Map<Integer, Decision> decisions = new HashMap<>();
	private final int[] pendingPerNode;
	private final int actionCount;

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example16SoftmaxOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"SOFTMAX".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected SOFTMAX.");
		pendingPerNode = new int[nodeList.size()];
		actionCount = architectureLayers.length;
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;
		List<Integer> available = availableActions(architectureLayers, task);
		if (available.isEmpty())
			return -1;

		int action = sampleAction(task, architectureLayers, available);
		String tier = architectureLayers[action];
		int nodeIndex = nodeInTier(tier, architectureLayers, task);
		if (nodeIndex < 0)
			return -1;

		pendingPerNode[nodeIndex]++;
		pendingPerTier.merge(tier, 1, Integer::sum);
		decisions.put(task.getId(), new Decision(stateKey(task, tier), action, tier, nodeIndex));
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
				: -FAILURE_PENALTY;
		double[] actionValues = values.computeIfAbsent(decision.state, key -> new double[actionCount]);
		double next = maxValue(stateKey(task, decision.tier));
		actionValues[decision.action] += LEARNING_RATE * (reward + DISCOUNT_RATE * next - actionValues[decision.action]);
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/** The learned value of the given state and action, exposed for the example test. */
	public double getQValue(String state, int action) {
		double[] actionValues = values.get(state);
		return actionValues == null ? 0 : actionValues[action];
	}

	/** The state key used by the table, exposed so the example test can read it. */
	public String getStateKey(Task task, String tier) {
		return stateKey(task, tier);
	}

	private List<Integer> availableActions(String[] layers, Task task) {
		List<Integer> actions = new ArrayList<>();
		for (int action = 0; action < layers.length; action++)
			if (nodeInTier(layers[action], layers, task) >= 0)
				actions.add(action);
		return actions;
	}

	private int sampleAction(Task task, String[] layers, List<Integer> available) {
		double[] weights = new double[available.size()];
		double total = 0;
		double highest = Double.NEGATIVE_INFINITY;
		for (int action : available)
			highest = Math.max(highest, value(stateKey(task, layers[action]), action));
		for (int i = 0; i < available.size(); i++) {
			weights[i] = Math.exp((value(stateKey(task, layers[available.get(i)]), available.get(i)) - highest)
					/ TEMPERATURE);
			total += weights[i];
		}
		double draw = Randomness.nextDouble() * total;
		for (int i = 0; i < weights.length; i++) {
			draw -= weights[i];
			if (draw <= 0 || i == weights.length - 1)
				return available.get(i);
		}
		return available.get(available.size() - 1);
	}

	private double value(String state, int action) {
		double[] actionValues = values.get(state);
		return actionValues == null ? 0 : actionValues[action];
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

	private String stateKey(Task task, String tier) {
		double length = task.getLength();
		int bucket = length < 1000 ? 0 : length < 5000 ? 1 : length < 20000 ? 2 : 3;
		return task.getType() + "|" + bucket + "|" + (tier == null ? 0 : loadBucket(tier));
	}

	private int loadBucket(String tier) {
		int nodes = 0;
		for (ComputingNode node : nodeList)
			if (tier.equals(tierOf(node)))
				nodes++;
		int perNode = nodes == 0 ? 0 : pendingPerTier.getOrDefault(tier, 0) / nodes;
		return perNode == 0 ? 0 : perNode <= 1 ? 1 : 2;
	}

	private double maxValue(String state) {
		double[] actionValues = values.get(state);
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
		private final String tier;
		private final int node;

		private Decision(String state, int action, String tier, int node) {
			this.state = state;
			this.action = action;
			this.tier = tier;
			this.node = node;
		}
	}
}
