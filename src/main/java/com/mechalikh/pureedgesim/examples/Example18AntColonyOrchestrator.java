package examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * Ant colony orchestration for cloud, edge, and mist offloading.
 * <p>
 * Every tier lays a pheromone trail that grows when the tasks sent there meet
 * their latency and evaporates when they fail, so the colony converges on the
 * tiers that work for the current workload. A placement is sampled from a
 * probability that multiplies the pheromone by a heuristic desirability, the
 * inverse of the estimated delay, so a good tier is preferred from the first
 * task while the trail keeps adapting. The chosen tier is executed by its least
 * loaded eligible node.
 *
 * @since PureEdgeSim 6.0
 */
public class Example18AntColonyOrchestrator extends Orchestrator {

	/** Part of a tier's pheromone replaced by the latest outcome. */
	static final double EVAPORATION = 0.1;
	/** Weight of the pheromone in the selection probability. */
	static final double ALPHA = 1.0;
	/** Weight of the delay heuristic in the selection probability. */
	static final double BETA = 2.0;
	/** Pheromone of every tier at the start of the run. */
	static final double INITIAL_PHEROMONE = 0.5;
	/** Lower bound of a trail, so no tier becomes unreachable. */
	static final double MIN_PHEROMONE = 0.05;
	/** Upper bound of a trail, so one early success cannot lock the choice. */
	static final double MAX_PHEROMONE = 5.0;

	private final Map<String, Double> pheromone = new HashMap<>();
	private final Map<Integer, Decision> decisions = new LinkedHashMap<>();
	private final int[] pendingPerNode;

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example18AntColonyOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"ANT_COLONY".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected ANT_COLONY.");
		pendingPerNode = new int[nodeList.size()];
		for (String tier : new String[] { "Mist", "Edge", "Cloud" })
			pheromone.put(tier, INITIAL_PHEROMONE);
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;

		List<Candidate> candidates = new ArrayList<>();
		double total = 0;
		for (String tier : new String[] { "Mist", "Edge", "Cloud" }) {
			if (!arrayContains(architectureLayers, tier))
				continue;
			int nodeIndex = bestNode(tier, architectureLayers, task);
			if (nodeIndex < 0)
				continue;
			double delay = estimatedDelay(task, nodeList.get(nodeIndex)) * (1 + pendingPerNode[nodeIndex]);
			double weight = Math.pow(pheromone.getOrDefault(tier, INITIAL_PHEROMONE), ALPHA)
					/ Math.pow(1 + delay, BETA);
			if (weight <= 0)
				continue;
			candidates.add(new Candidate(tier, nodeIndex, weight));
			total += weight;
		}
		if (candidates.isEmpty())
			return -1;

		double draw = Randomness.nextDouble() * total;
		Candidate chosen = candidates.get(candidates.size() - 1);
		for (Candidate candidate : candidates) {
			draw -= candidate.weight;
			if (draw <= 0) {
				chosen = candidate;
				break;
			}
		}

		pendingPerNode[chosen.node]++;
		decisions.put(task.getId(), new Decision(chosen.tier, chosen.node));
		return chosen.node;
	}

	@Override
	public void resultsReturned(Task task) {
		Decision decision = decisions.remove(task.getId());
		if (decision == null)
			return;
		if (pendingPerNode[decision.node] > 0)
			pendingPerNode[decision.node]--;

		double quality = task.getStatus() == Task.Status.SUCCESS
				? task.getMaxLatency() / (task.getMaxLatency() + task.getTotalDelay())
				: 0;
		double trail = (1 - EVAPORATION) * pheromone.getOrDefault(decision.tier, INITIAL_PHEROMONE)
				+ EVAPORATION * quality;
		pheromone.put(decision.tier, Math.max(MIN_PHEROMONE, Math.min(MAX_PHEROMONE, trail)));
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/** The current pheromone of a tier, exposed for the example test. */
	public double getPheromone(String tier) {
		return pheromone.getOrDefault(tier, INITIAL_PHEROMONE);
	}

	/** The delay the colony expects when a task runs on the given node. */
	public double estimatedDelay(Task task, ComputingNode node) {
		double cores = Math.max(1, node.getMipsPerCore() * node.getNumberOfCPUCores());
		double compute = task.getLength() / cores;
		double access = 0;
		if (task.getEdgeDevice() != null && task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK) != null)
			access = task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK).getLatency();
		return compute + access + (node.getType() == SimulationParameters.TYPES.CLOUD
				? SimulationParameters.wanLatency
				: 0);
	}

	private int bestNode(String tier, String[] architectureLayers, Task task) {
		int best = -1;
		double bestDelay = Double.MAX_VALUE;
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (!tier.equals(tierOf(node)) || !offloadingIsPossible(task, node, architectureLayers))
				continue;
			double delay = estimatedDelay(task, node) * (1 + pendingPerNode[index]);
			if (delay < bestDelay) {
				bestDelay = delay;
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

	private static final class Candidate {
		private final String tier;
		private final int node;
		private final double weight;

		private Candidate(String tier, int node, double weight) {
			this.tier = tier;
			this.node = node;
			this.weight = weight;
		}
	}

	private static final class Decision {
		private final String tier;
		private final int node;

		private Decision(String tier, int node) {
			this.tier = tier;
			this.node = node;
		}
	}
}
