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
 * Tabular Q-learning orchestration over the cloud, edge, and mist tiers.
 * <p>
 * The state of the agent is the application type, a coarse bucket of the task
 * length, and a coarse bucket of the number of pending tasks per node in the
 * tier it is about to use. The actions are the tiers allowed by the scenario
 * architecture. A tier action is executed by the least loaded eligible node of
 * that tier, scored the same way as {@code TRADE_OFF}.
 * <p>
 * After a task returns, the orchestrator observes its total delay and updates
 * the action value of the tier it chose. The next state is rebuilt from the
 * pending counts after the task leaves the tier, so the update follows the
 * standard Q-learning rule. The policy is epsilon-greedy, with a fixed
 * exploration rate of {@value #EXPLORATION_RATE}.
 *
 * @since PureEdgeSim 6.0
 */
public class Example11QLearningOrchestrator extends Orchestrator {

	/** Fraction of the decisions that explore a random tier. */
	static final double EXPLORATION_RATE = 0.1;
	/** Weight of the new reward in the action value. */
	static final double LEARNING_RATE = 0.2;
	/** Weight of the value of the next state. */
	static final double DISCOUNT_RATE = 0.9;
	/** Reward of a task that fails, large enough to dominate any delay saving. */
	static final double FAILURE_PENALTY = 1000.0;

	private final Map<String, double[]> qTable = new LinkedHashMap<>();
	private final Map<Integer, Decision> decisions = new HashMap<>();
	private final Map<String, Integer> pendingPerTier = new HashMap<>();
	private int[] assignments;
	private int actionCount;

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example11QLearningOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"Q_LEARNING".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected Q_LEARNING.");
		assignments = new int[nodeList.size()];
		actionCount = architectureLayers.length;
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;
		List<Integer> available = availableActions(architectureLayers, task);
		if (available.isEmpty())
			return -1;

		int action = chooseAction(task, architectureLayers, available);
		String tier = architectureLayers[action];
		int nodeIndex = nodeInTier(tier, architectureLayers, task);
		if (nodeIndex < 0)
			return -1;

		assignments[nodeIndex]++;
		pendingPerTier.merge(tier, 1, Integer::sum);
		decisions.put(task.getId(), new Decision(stateKey(task, tier), action, tier));
		return nodeIndex;
	}

	@Override
	public void resultsReturned(Task task) {
		Decision decision = decisions.remove(task.getId());
		if (decision == null)
			return;
		pendingPerTier.merge(decision.tier, -1, Integer::sum);
		double reward = task.getStatus() == Task.Status.SUCCESS ? -task.getTotalDelay() : -FAILURE_PENALTY;
		double[] values = qTable.computeIfAbsent(decision.state, key -> new double[actionCount]);
		double next = maxValue(stateKey(task, decision.tier));
		values[decision.action] += LEARNING_RATE * (reward + DISCOUNT_RATE * next - values[decision.action]);
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/** The learned value of the given state and action, exposed for the example test. */
	public double getQValue(String state, int action) {
		double[] values = qTable.get(state);
		return values == null ? 0 : values[action];
	}

	/** The state key used by the table, exposed so the example test can read it. */
	public String getStateKey(Task task, String tier) {
		return stateKey(task, tier);
	}

	private List<Integer> availableActions(String[] layers, Task task) {
		List<Integer> actions = new ArrayList<>();
		for (int action = 0; action < layers.length; action++) {
			if (nodeInTier(layers[action], layers, task) >= 0)
				actions.add(action);
		}
		return actions;
	}

	private int chooseAction(Task task, String[] layers, List<Integer> available) {
		if (Randomness.nextDouble() < EXPLORATION_RATE)
			return available.get(Randomness.nextInt(available.size()));
		int best = available.get(0);
		double bestValue = value(stateKey(task, layers[best]), best);
		for (int action : available) {
			double candidate = value(stateKey(task, layers[action]), action);
			if (candidate > bestValue) {
				bestValue = candidate;
				best = action;
			}
		}
		return best;
	}

	private double value(String state, int action) {
		double[] values = qTable.get(state);
		return values == null ? 0 : values[action];
	}

	private int nodeInTier(String tier, String[] layers, Task task) {
		int best = -1;
		double bestScore = Double.MAX_VALUE;
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (!tier.equals(tierOf(node)) || !offloadingIsPossible(task, node, layers))
				continue;
			double score = (assignments[index] + 1) * task.getLength() / node.getMipsPerCore();
			if (score < bestScore) {
				bestScore = score;
				best = index;
			}
		}
		return best;
	}

	private String stateKey(Task task, String tier) {
		int length = task.getLength() < 1000 ? 0 : task.getLength() < 5000 ? 1 : task.getLength() < 20000 ? 2 : 3;
		int load = tier == null ? 0 : loadBucket(tier);
		return task.getType() + "|" + length + "|" + load;
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
		double[] values = qTable.get(state);
		if (values == null)
			return 0;
		double max = Double.NEGATIVE_INFINITY;
		for (double value : values)
			max = Math.max(max, value);
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

		private Decision(String state, int action, String tier) {
			this.state = state;
			this.action = action;
			this.tier = tier;
		}
	}
}
