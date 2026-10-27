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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * Places mist-only tasks inside the cluster that the origin device joined.
 * <p>
 * The candidates are the cluster head and the members of its cluster that the
 * origin can reach, plus the origin itself. Each candidate costs
 * {@code (1 + pending) * length / capacity}, an estimate of the queueing plus
 * execution time, and the head gets a 25 percent discount so that it aggregates
 * the load that the cluster was formed to share. The candidate with the lowest
 * cost wins, and ties keep the order of the candidate list, which puts the head
 * first. When the origin has not joined a cluster yet, or it is a sensor, the
 * orchestrator falls back to any eligible mist device in range.
 * <p>
 * At the end of the run the orchestrator prints how many tasks went to heads,
 * members, the origin, and outside the cluster, then the number and size of the
 * clusters formed by the devices.
 *
 * @since PureEdgeSim 6.0
 */
public class Example22ClusterOrchestrator extends Orchestrator implements OnSimulationEndListener {

	/** Multiplier applied to the head cost, so the head wins at equal capacity. */
	public static final double HEAD_COST_FACTOR = 0.75;

	private final Map<Integer, Integer> pendingPerNode = new HashMap<>();
	private final Map<Integer, ComputingNode> decisions = new HashMap<>();
	private long headPlacements;
	private long memberPlacements;
	private long localPlacements;
	private long outsidePlacements;

	/**
	 * Creates the orchestrator and checks the algorithm name declared in the
	 * settings file.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example22ClusterOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"CLUSTER_LOCAL".equals(algorithmName))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', expected CLUSTER_LOCAL.");
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		if (architectureLayers == null || architectureLayers.length == 0
				|| !arrayContains(architectureLayers, "Mist"))
			return -1;

		ComputingNode origin = task.getEdgeDevice();
		ComputingNode head = clusterHeadOf(origin);
		List<ComputingNode> candidates = clusterCandidates(origin);

		int bestIndex = -1;
		double bestCost = Double.MAX_VALUE;
		for (ComputingNode candidate : candidates) {
			int index = nodeList.indexOf(candidate);
			if (index < 0 || !offloadingIsPossible(task, candidate, architectureLayers))
				continue;
			double cost = placementCost(task, candidate);
			if (candidate == head)
				cost *= HEAD_COST_FACTOR;
			if (cost < bestCost) {
				bestCost = cost;
				bestIndex = index;
			}
		}

		if (bestIndex >= 0) {
			ComputingNode chosen = nodeList.get(bestIndex);
			pendingPerNode.merge(chosen.getId(), 1, Integer::sum);
			decisions.put(task.getId(), chosen);
			countPlacement(origin, head, chosen);
		}
		return bestIndex;
	}

	@Override
	public void resultsReturned(Task task) {
		ComputingNode node = decisions.remove(task.getId());
		if (node == null)
			return;
		pendingPerNode.computeIfPresent(node.getId(), (id, pending) -> pending <= 1 ? null : pending - 1);
	}

	@Override
	public void processEvent(Event e) {
		// This orchestrator does not schedule events.
	}

	/**
	 * Returns the nodes that may receive a task from the given origin: the cluster
	 * head, the members of the head's cluster, and the origin itself. When the
	 * origin has not joined a cluster, every mist node is eligible.
	 *
	 * @param origin the device that generated the task
	 * @return the candidate destinations
	 */
	public List<ComputingNode> clusterCandidates(ComputingNode origin) {
		List<ComputingNode> candidates = new ArrayList<>();
		ComputingNode head = clusterHeadOf(origin);
		if (head != null) {
			candidates.add(head);
			for (ComputingNode member : ((Example22SelfClusteringDevice) head).getClusterMembers())
				if (!candidates.contains(member))
					candidates.add(member);
		} else {
			candidates.addAll(nodeList);
		}
		if (!candidates.contains(origin))
			candidates.add(origin);
		return candidates;
	}

	/**
	 * Estimates the time to run a task on a node, transfer and queueing included.
	 * The transfer term is the wireless latency plus the time to send the task
	 * over the device-to-device link, and queueing is the number of tasks already
	 * placed on the node.
	 *
	 * @param task the task to place
	 * @param node the candidate destination
	 * @return the estimated service time in seconds
	 */
	public double placementCost(Task task, ComputingNode node) {
		double capacity = Math.max(1, node.getMipsPerCore() * node.getNumberOfCPUCores());
		int pending = pendingPerNode.getOrDefault(node.getId(), 0);
		double compute = (1 + pending) * task.getLength() / capacity;
		if (node == task.getEdgeDevice())
			return compute;
		double transfer = SimulationParameters.wifiLatency
				+ task.getFileSizeInBits() / Math.max(1, SimulationParameters.wifiBandwidthBitsPerSecond);
		return transfer + compute;
	}

	private static ComputingNode clusterHeadOf(ComputingNode origin) {
		if (!(origin instanceof Example22SelfClusteringDevice))
			return null;
		return ((Example22SelfClusteringDevice) origin).getClusterHead();
	}

	private void countPlacement(ComputingNode origin, ComputingNode head, ComputingNode chosen) {
		if (head == null) {
			outsidePlacements++;
		} else if (chosen == origin) {
			localPlacements++;
		} else if (chosen == head) {
			headPlacements++;
		} else if (((Example22SelfClusteringDevice) head).getClusterMembers().contains(chosen)) {
			memberPlacements++;
		} else {
			outsidePlacements++;
		}
	}

	/**
	 * Prints the placement distribution and the clusters formed by the devices.
	 * Runs after the simulation log has been written, so it writes directly to the
	 * console.
	 */
	@Override
	public void onSimulationEnd() {
		long total = headPlacements + memberPlacements + localPlacements + outsidePlacements;
		SimLog.println("Example22 - cluster placement: %s placements, %s %% to heads, %s %% to members, %s %% local, %s %% outside the cluster",
				total, percent(headPlacements, total), percent(memberPlacements, total), percent(localPlacements, total),
				percent(outsidePlacements, total));

		List<ComputingNode> mistDevices = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistOnlyListSensorsExcluded();
		int clusters = 0;
		int clustered = 0;
		int largest = 0;
		int rounds = 0;
		for (ComputingNode node : mistDevices) {
			if (!(node instanceof Example22SelfClusteringDevice))
				continue;
			Example22SelfClusteringDevice device = (Example22SelfClusteringDevice) node;
			rounds = Math.max(rounds, device.getClusterUpdates());
			if (!device.isClusterHead())
				continue;
			clusters++;
			clustered += device.getClusterMembers().size();
			largest = Math.max(largest, device.getClusterMembers().size());
		}
		double average = clusters == 0 ? 0 : clustered / (double) clusters;
		SimLog.println("Example22 - clustering: %s clusters over %s devices, %s rounds, average size %s, largest %s",
				clusters, mistDevices.size(), rounds, String.format(Locale.ROOT, "%.1f", average), largest);
	}

	private static String percent(long count, long total) {
		return total == 0 ? "0.0" : String.format(Locale.ROOT, "%.1f", 100.0 * count / total);
	}
}
