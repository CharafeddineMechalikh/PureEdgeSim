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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationStartListener;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * An edge device that organizes itself with its neighbors into clusters.
 * <p>
 * Every device scores itself from four local signals: computing capacity,
 * remaining battery, stability (fixed devices score higher than fast moving
 * ones), and the number of neighbors within the mist communication range. Each
 * device then points to the best scored node among its neighbors, including
 * itself. A device that points to itself is a cluster head, and every other
 * device follows the pointers until it reaches one. This is a local-maximum
 * election: no central authority assigns the heads, and no message is exchanged
 * between devices. When the best neighbor of a device is itself not a head, the
 * device joins that neighbor's head, so clusters grow by following the chain to
 * the local maximum.
 * <p>
 * Clusters are recomputed every 30 seconds by the first alive device of the
 * fleet, which acts as a coordinator for that round. If the coordinator dies,
 * the next alive device takes over on the following round. The elected state is
 * exposed through {@link #getClusterHead()}, {@link #isClusterHead()}, and
 * {@link #getClusterMembers()} so that an orchestrator can place tasks within
 * the cluster.
 *
 * @since PureEdgeSim 6.0
 */
public class Example22SelfClusteringDevice extends DefaultComputingNode implements OnSimulationStartListener {

	/** Event used to trigger a clustering round. */
	protected static final int UPDATE_CLUSTERS = 11001;

	/** Seconds between two clustering rounds. */
	public static final double CLUSTER_UPDATE_INTERVAL = 30;

	/** Weight of the normalized capacity in the election score. */
	public static final double CAPACITY_WEIGHT = 0.4;
	/** Weight of the remaining battery in the election score. */
	public static final double BATTERY_WEIGHT = 0.25;
	/** Weight of the mobility stability in the election score. */
	public static final double STABILITY_WEIGHT = 0.2;
	/** Weight of the neighbor degree in the election score. */
	public static final double DEGREE_WEIGHT = 0.15;

	/** Neighbor count at which the degree term reaches its maximum. */
	public static final int DEGREE_SATURATION = 10;

	/** Speed, in meters per second, at which the stability term reaches zero. */
	public static final double STABILITY_REFERENCE_SPEED = 10;

	private final List<ComputingNode> mistDevices;
	private final List<ComputingNode> clusterMembers = new ArrayList<>();
	private ComputingNode clusterHead;
	private int clusterUpdates;

	/**
	 * Creates a device and captures the fleet it will cluster with.
	 *
	 * @param simulationManager the simulation manager
	 * @param mipsPerCore       capacity of one core, in million instructions per second
	 * @param numberOfCPUCores  number of CPU cores
	 * @param storage           total storage, in megabytes
	 * @param ram               total RAM, in megabytes
	 */
	public Example22SelfClusteringDevice(SimulationManager simulationManager, double mipsPerCore,
			int numberOfCPUCores, double storage, double ram) {
		super(simulationManager, mipsPerCore, numberOfCPUCores, storage, ram);
		mistDevices = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistOnlyListSensorsExcluded();
	}

	/**
	 * Starts the clustering rounds. Only devices can be cluster members, so data
	 * centers do not schedule the event.
	 */
	@Override
	public void onSimulationStart() {
		super.onSimulationStart();
		if (getType() == SimulationParameters.TYPES.EDGE_DEVICE && !isSensor())
			schedule(this, 1, UPDATE_CLUSTERS);
	}

	/**
	 * Runs the clustering round when this device is the coordinator, then
	 * schedules the next round.
	 *
	 * @param ev the event to process
	 */
	@Override
	public void processEvent(Event ev) {
		if (ev.getTag() == UPDATE_CLUSTERS) {
			if (isCoordinator())
				refreshClusters(mistDevices, SimulationParameters.edgeDevicesRange);
			if (!isDead())
				schedule(this, CLUSTER_UPDATE_INTERVAL, UPDATE_CLUSTERS);
			return;
		}
		super.processEvent(ev);
	}

	/**
	 * Returns whether this device is the cluster coordinator of the current round,
	 * that is, the first alive device in the fleet.
	 *
	 * @return {@code true} when this device should run the election
	 */
	public boolean isCoordinator() {
		for (ComputingNode device : mistDevices)
			if (!device.isDead())
				return device == this;
		return false;
	}

	/**
	 * Elects the heads of the given devices and updates every device with its head
	 * and members.
	 *
	 * @param devices the devices that may cluster
	 * @param range   the maximum distance between two neighbors, in meters
	 */
	public static void refreshClusters(List<ComputingNode> devices, double range) {
		Map<ComputingNode, ComputingNode> heads = electClusters(devices, range);
		for (ComputingNode device : devices) {
			Example22SelfClusteringDevice self = asSelfClusteringDevice(device);
			if (self == null)
				continue;
			self.clusterHead = null;
			self.clusterMembers.clear();
			self.clusterUpdates++;
		}
		for (Map.Entry<ComputingNode, ComputingNode> entry : heads.entrySet()) {
			Example22SelfClusteringDevice member = asSelfClusteringDevice(entry.getKey());
			Example22SelfClusteringDevice head = asSelfClusteringDevice(entry.getValue());
			if (member == null || head == null)
				continue;
			member.clusterHead = head;
			head.clusterMembers.add(member);
		}
	}

	/**
	 * Runs the election without touching the devices, for inspection and tests.
	 * <p>
	 * Dead devices and sensors are left out. Each remaining device points to the
	 * best scored node within range, itself included, and ties go to the lower
	 * identifier. The returned map associates every device with the head reached
	 * by following those pointers.
	 *
	 * @param devices the devices that may cluster
	 * @param range   the maximum distance between two neighbors, in meters
	 * @return the head of every eligible device
	 */
	public static Map<ComputingNode, ComputingNode> electClusters(List<ComputingNode> devices, double range) {
		Map<ComputingNode, ComputingNode> heads = new LinkedHashMap<>();
		List<ComputingNode> candidates = new ArrayList<>();
		for (ComputingNode device : devices)
			if (!device.isDead() && !device.isSensor())
				candidates.add(device);

		int size = candidates.size();
		if (size == 0)
			return heads;

		double maxCapacity = 0;
		for (ComputingNode device : candidates)
			maxCapacity = Math.max(maxCapacity, device.getTotalMipsCapacity());

		boolean[][] neighbors = new boolean[size][size];
		int[] degrees = new int[size];
		for (int i = 0; i < size; i++) {
			MobilityModel mobility = candidates.get(i).getMobilityModel();
			for (int j = 0; j < size; j++) {
				boolean neighbor = i == j || (mobility != null && mobility.distanceTo(candidates.get(j)) <= range);
				neighbors[i][j] = neighbor;
				if (neighbor)
					degrees[i]++;
			}
		}

		Map<ComputingNode, ComputingNode> best = new LinkedHashMap<>();
		for (int i = 0; i < size; i++) {
			ComputingNode chosen = candidates.get(i);
			double chosenScore = clusterScore(chosen, degrees[i], maxCapacity);
			for (int j = 0; j < size; j++) {
				if (!neighbors[i][j])
					continue;
				ComputingNode candidate = candidates.get(j);
				double score = clusterScore(candidate, degrees[j], maxCapacity);
				if (score > chosenScore + 1e-12 || (Math.abs(score - chosenScore) <= 1e-12
						&& candidate.getId() < chosen.getId())) {
					chosen = candidate;
					chosenScore = score;
				}
			}
			best.put(candidates.get(i), chosen);
		}

		for (ComputingNode device : candidates)
			heads.put(device, follow(device, best));
		return heads;
	}

	/**
	 * Follows the best-neighbor pointers until a device that points to itself.
	 *
	 * @param device the starting device
	 * @param best   the best neighbor of every device
	 * @return the head reached from the starting device
	 */
	private static ComputingNode follow(ComputingNode device, Map<ComputingNode, ComputingNode> best) {
		ComputingNode current = device;
		for (int step = 0; step <= best.size(); step++) {
			ComputingNode next = best.get(current);
			if (next == null || next == current)
				return current;
			current = next;
		}
		return current;
	}

	/**
	 * Scores a device for the election.
	 *
	 * @param device      the device to score
	 * @param neighbors   the number of devices within range, itself included
	 * @param maxCapacity the capacity of the most powerful device of the fleet
	 * @return a score between 0 and 1
	 */
	public static double clusterScore(ComputingNode device, int neighbors, double maxCapacity) {
		double battery = 1;
		EnergyModelComputingNode energy = device.getEnergyModel();
		if (energy != null && energy.isBatteryPowered())
			battery = Math.max(0, Math.min(1, energy.getBatteryLevelPercentage() / 100.0));
		double stability = 1;
		MobilityModel mobility = device.getMobilityModel();
		if (mobility != null && mobility.isMobile())
			stability = Math.max(0, Math.min(1, 1 - mobility.getSpeed() / STABILITY_REFERENCE_SPEED));
		return clusterScore(device.getTotalMipsCapacity(), maxCapacity, battery, stability, neighbors);
	}

	/**
	 * Scores a device from its normalized inputs.
	 *
	 * @param capacity      total capacity, in million instructions per second
	 * @param maxCapacity   capacity of the most powerful device of the fleet
	 * @param battery       remaining battery between 0 and 1
	 * @param stability     mobility stability between 0 and 1
	 * @param neighbors     the number of devices within range, itself included
	 * @return a score between 0 and 1
	 */
	public static double clusterScore(double capacity, double maxCapacity, double battery, double stability,
			int neighbors) {
		double capacityNorm = maxCapacity <= 0 ? 0 : Math.min(1, capacity / maxCapacity);
		double degreeNorm = Math.min(1, neighbors / (double) DEGREE_SATURATION);
		return CAPACITY_WEIGHT * capacityNorm + BATTERY_WEIGHT * battery + STABILITY_WEIGHT * stability
				+ DEGREE_WEIGHT * degreeNorm;
	}

	private static Example22SelfClusteringDevice asSelfClusteringDevice(ComputingNode node) {
		return node instanceof Example22SelfClusteringDevice ? (Example22SelfClusteringDevice) node : null;
	}

	/**
	 * Returns the head of this device's cluster, or {@code null} before the first
	 * election.
	 *
	 * @return the cluster head
	 */
	public ComputingNode getClusterHead() {
		return clusterHead;
	}

	/**
	 * Returns whether this device is a cluster head.
	 *
	 * @return {@code true} when this device leads its cluster
	 */
	public boolean isClusterHead() {
		return clusterHead == this;
	}

	/**
	 * Returns the members of this device's cluster, itself included when it leads
	 * one.
	 *
	 * @return the cluster members
	 */
	public List<ComputingNode> getClusterMembers() {
		return clusterMembers;
	}

	/**
	 * Returns the number of clustering rounds this device took part in.
	 *
	 * @return the number of rounds
	 */
	public int getClusterUpdates() {
		return clusterUpdates;
	}
}
