package examples;

import java.util.HashMap;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * Layered placement for a multi-layer fog: mist, then edge, then cloud.
 * <p>
 * The orchestrator walks the layers in that order and stops at the first one
 * that contains a feasible node. Inside that layer it selects the node with the
 * fewest tasks pending, so the work spreads over the home gateways before it
 * spills to the edge data center and then to the cloud. The architecture of the
 * scenario still decides which layers exist; a layer that is not allowed by the
 * architecture is skipped.
 *
 * @since PureEdgeSim 6.0
 */
public class Example13FogLayersOrchestrator extends Orchestrator {

	private static final String[] PREFERENCE = { "Mist", "Edge", "Cloud" };

	private final int[] pending;
	private final Map<Integer, Integer> decisions = new HashMap<>();

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example13FogLayersOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"FOG_LAYERS".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected FOG_LAYERS.");
		pending = new int[nodeList.size()];
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0)
			return -1;
		int chosen = -1;
		for (String tier : PREFERENCE) {
			if (!arrayContains(architectureLayers, tier))
				continue;
			chosen = leastLoadedNodeIn(tier, architectureLayers, task);
			if (chosen >= 0)
				break;
		}
		if (chosen >= 0) {
			pending[chosen]++;
			decisions.put(task.getId(), chosen);
		}
		return chosen;
	}

	@Override
	public void resultsReturned(Task task) {
		Integer index = decisions.remove(task.getId());
		if (index != null && pending[index] > 0)
			pending[index]--;
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/** The number of tasks pending on a node, exposed for the example test. */
	public int pendingOn(int nodeIndex) {
		return pending[nodeIndex];
	}

	private int leastLoadedNodeIn(String tier, String[] layers, Task task) {
		int best = -1;
		int bestLoad = Integer.MAX_VALUE;
		for (int index = 0; index < nodeList.size(); index++) {
			ComputingNode node = nodeList.get(index);
			if (!tier.equals(tierOf(node)) || !offloadingIsPossible(task, node, layers))
				continue;
			if (pending[index] < bestLoad) {
				bestLoad = pending[index];
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
}
