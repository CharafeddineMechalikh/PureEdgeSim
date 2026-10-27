package examples;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * Deadline-aware placement for tasks with different criticality.
 * <p>
 * The application type decides the objective. A hard real-time task goes to the
 * eligible node whose estimated finish time leaves the largest latency margin,
 * and only falls back to the fastest node when no node can meet the deadline. A
 * soft real-time task goes where the estimated delay is the smallest fraction of
 * its latency. A non real-time task goes where the estimated energy is the
 * lowest, so a long-running background job does not consume an edge server that
 * latency-sensitive tasks may need.
 * <p>
 * The estimate covers the compute time, the tasks already queued on the node,
 * the device access latency, and the WAN latency for the cloud. The energy
 * estimate multiplies the active consumption of the node by the compute time.
 *
 * @since PureEdgeSim 6.0
 */
public class Example19PriorityOrchestrator extends Orchestrator {

	/** Penalty added to a deadline miss, so a feasible node always wins. */
	static final double DEADLINE_MISS_PENALTY = 1e6;

	private final int[] pendingPerNode;
	private final Map<Integer, Integer> decisions = new LinkedHashMap<>();

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example19PriorityOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"PRIORITY_AWARE".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected PRIORITY_AWARE.");
		pendingPerNode = new int[nodeList.size()];
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;

		boolean hard = "Hard Real-time".equals(task.getType());
		boolean soft = "Soft Real-time".equals(task.getType());
		int best = -1;
		double bestScore = Double.MAX_VALUE;
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (!offloadingIsPossible(task, node, architectureLayers))
				continue;
			double delay = estimatedDelay(task, node, index);
			double score;
			if (hard) {
				double slack = task.getMaxLatency() - delay;
				score = slack >= 0 ? -slack : DEADLINE_MISS_PENALTY + delay;
			} else if (soft) {
				score = delay / Math.max(task.getMaxLatency(), 1e-9);
			} else {
				score = estimatedEnergy(task, node) + delay * 1e-9;
			}
			if (score < bestScore) {
				bestScore = score;
				best = index;
			}
		}
		if (best >= 0) {
			pendingPerNode[best]++;
			decisions.put(task.getId(), best);
		}
		return best;
	}

	@Override
	public void resultsReturned(Task task) {
		Integer node = decisions.remove(task.getId());
		if (node != null && pendingPerNode[node] > 0)
			pendingPerNode[node]--;
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/** The estimated delay of a task on a node, exposed for the example test. */
	public double estimatedDelay(Task task, ComputingNode node) {
		int index = nodeList.indexOf(node);
		return estimatedDelay(task, node, index < 0 ? 0 : index);
	}

	/** The estimated energy of a task on a node in watt-hours, exposed for the example test. */
	public double estimatedEnergy(Task task, ComputingNode node) {
		EnergyModelComputingNode energy = node.getEnergyModel();
		double power = energy == null ? 0 : energy.getMaxActiveConsumption();
		return power * computeTime(task, node) / 3600.0;
	}

	private double estimatedDelay(Task task, ComputingNode node, int index) {
		double delay = computeTime(task, node) * (1 + pendingPerNode[index]);
		if (task.getEdgeDevice() != null && task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK) != null)
			delay += task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK).getLatency();
		if (node.getType() == SimulationParameters.TYPES.CLOUD)
			delay += SimulationParameters.wanLatency;
		return delay;
	}

	private static double computeTime(Task task, ComputingNode node) {
		double capacity = node.getMipsPerCore() * node.getNumberOfCPUCores();
		return task.getLength() / Math.max(1, capacity);
	}
}
