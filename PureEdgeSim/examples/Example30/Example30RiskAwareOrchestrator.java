package examples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Subtask;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;

/**
 * Places the subtasks of service graphs with or without regard to the risk that
 * the chosen node fails.
 * <p>
 * When a subtask fails, PureEdgeSim fails every subtask that waits for it, so a
 * failure early in a graph costs the whole request. The algorithm name selects
 * the rule:
 * <ul>
 * <li>{@code LATENCY_GREEDY}: the node with the lowest estimated delay as a share
 * of the subtask's latency budget, using the estimator of Example 19.</li>
 * <li>{@code RELIABLE_INNER}: the same, except that a subtask any other subtask
 * waits for may only go to a node without risk, when one is allowed. Leaves are
 * placed as in {@code LATENCY_GREEDY}.</li>
 * <li>{@code TRADE_OFF} and {@code ROUND_ROBIN}: the default orchestrator.</li>
 * </ul>
 * Cloud virtual machines and edge data centers carry no risk. An edge device
 * carries {@code 0.25}, plus up to {@code 0.5} when it runs on a battery (more as
 * the battery empties), plus {@code 0.5} when it moves, capped at 1.
 */
public class Example30RiskAwareOrchestrator extends DefaultOrchestrator {

	/** The algorithm names this orchestrator accepts. */
	static final List<String> ALGORITHMS = Arrays.asList("TRADE_OFF", "ROUND_ROBIN", "LATENCY_GREEDY",
			"RELIABLE_INNER");

	private final Set<String> innerSubtasks = new HashSet<>();
	private final int[] pendingPerNode;
	private final Map<Integer, Integer> decisions = new HashMap<>();

	/**
	 * Creates the orchestrator and reads the subtask graphs of the applications.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example30RiskAwareOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!ALGORITHMS.contains(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected one of " + ALGORITHMS);
		if (SimulationParameters.applicationList != null)
			registerApplications(SimulationParameters.applicationList);
		pendingPerNode = new int[nodeList.size()];
	}

	/**
	 * Records which subtasks of each application other subtasks depend on.
	 *
	 * @param applications the applications of the scenario
	 */
	void registerApplications(List<Application> applications) {
		for (int id = 0; id < applications.size(); id++) {
			Application application = applications.get(id);
			if (!application.hasSubtasks())
				continue;
			for (Subtask subtask : application.getSubtasks())
				for (String dependency : subtask.getDependencies())
					innerSubtasks.add(key(id, dependency));
		}
	}

	/**
	 * Returns whether another subtask of the same application waits for this one.
	 *
	 * @param applicationId the application of the task
	 * @param subtask       the subtask name, or {@code null}
	 * @return {@code true} for a subtask that is not a leaf of its graph
	 */
	boolean hasDependents(int applicationId, String subtask) {
		return innerSubtasks.contains(key(applicationId, subtask));
	}

	/**
	 * Places the task, then clears the cached placement of its device so the next
	 * subtask is placed on its own, as in Example 9. Every algorithm, the default
	 * ones included, therefore decides once per subtask.
	 *
	 * @param task the task to place
	 */
	@Override
	public void orchestrate(Task task) {
		super.orchestrate(task);
		task.getEdgeDevice().setApplicationPlaced(false);
	}

	@Override
	protected int findComputingNode(String[] architecture, Task task) {
		if ("TRADE_OFF".equals(algorithmName) || "ROUND_ROBIN".equals(algorithmName))
			return super.findComputingNode(architecture, task);
		boolean reliableOnly = "RELIABLE_INNER".equals(algorithmName)
				&& hasDependents(task.getApplicationID(), task.getSubtaskName()) && anyReliable(task, architecture);
		int best = -1;
		double bestCost = Double.MAX_VALUE;
		for (int i = 0; i < nodeList.size(); i++) {
			ComputingNode node = nodeList.get(i);
			if (!offloadingIsPossible(task, node, architecture))
				continue;
			if (reliableOnly && risk(node) > 0)
				continue;
			double cost = estimatedDelay(task, node, i) / Math.max(task.getMaxLatency(), 1e-9);
			if (cost < bestCost) {
				bestCost = cost;
				best = i;
			}
		}
		if (best >= 0) {
			pendingPerNode[best]++;
			decisions.put(task.getId(), best);
		}
		return best;
	}

	/**
	 * Returns the risk that a node fails before a task placed on it finishes.
	 *
	 * @param node the candidate node
	 * @return 0 for cloud and edge data centers, between 0.25 and 1 for devices
	 */
	static double risk(ComputingNode node) {
		if (node.getType() != SimulationParameters.TYPES.EDGE_DEVICE)
			return 0;
		double risk = 0.25;
		if (node.getEnergyModel() != null && node.getEnergyModel().isBatteryPowered()) {
			double level = node.getEnergyModel().getBatteryLevelPercentage() / 100.0;
			risk += 0.25 + 0.25 * (1 - Math.max(0, Math.min(1, level)));
		}
		if (node.getMobilityModel() != null && node.getMobilityModel().isMobile())
			risk += 0.5;
		return Math.min(1, risk);
	}

	private boolean anyReliable(Task task, String[] architecture) {
		for (ComputingNode node : nodeList)
			if (risk(node) == 0 && offloadingIsPossible(task, node, architecture))
				return true;
		return false;
	}

	/**
	 * Estimated time to finish a task on a node: its compute time, multiplied by
	 * the tasks already sent there and not finished, plus the access latency and,
	 * for the cloud, the WAN latency. This is the estimator of Example 19.
	 */
	private double estimatedDelay(Task task, ComputingNode node, int index) {
		double capacity = node.getMipsPerCore() * node.getNumberOfCPUCores();
		double delay = task.getLength() / Math.max(1, capacity) * (1 + pendingPerNode[index]);
		if (task.getEdgeDevice() != null && task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK) != null)
			delay += task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK).getLatency();
		if (node.getType() == SimulationParameters.TYPES.CLOUD)
			delay += SimulationParameters.wanLatency;
		return delay;
	}

	@Override
	public void resultsReturned(Task task) {
		release(task);
	}

	/**
	 * Forgets the placement of a task that ended without returning results, so the
	 * delay estimate does not count it any longer.
	 *
	 * @param task the failed task
	 */
	public void taskEnded(Task task) {
		release(task);
	}

	/** Returns the tasks sent to a node and not finished yet, for tests. */
	int pending(int node) {
		return pendingPerNode[node];
	}

	private void release(Task task) {
		Integer node = decisions.remove(task.getId());
		if (node != null && pendingPerNode[node] > 0)
			pendingPerNode[node]--;
	}

	private static String key(int applicationId, String subtask) {
		return applicationId + "#" + (subtask == null ? "" : subtask);
	}
}
