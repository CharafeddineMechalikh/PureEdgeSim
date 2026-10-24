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
package com.mechalikh.pureedgesim.locationmanager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * The main class of the Mobility Manager module, that generates the mobility
 * path for different edge devices. It implements the Null Object Design Pattern
 * in order to start avoiding {@link NullPointerException} when using the NULL
 * object instead of attributing null to EnergyModelNetworkLink variables.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */

public abstract class MobilityModel {

	/** The current position of the node holding this mobility model. */
	protected Location currentLocation;
	/** Whether the node moves during the simulation. */
	protected boolean isMobile = false;
	/** Minimum pause duration between two movements, in seconds. */
	protected double minPauseDuration;
	/** Maximum pause duration between two movements, in seconds. */
	protected double maxPauseDuration;
	/** Maximum duration of a single movement, in seconds. */
	protected double maxMobilityDuration;
	/** Minimum duration of a single movement, in seconds. */
	protected double minMobilityDuration;
	/** Movement speed in meters per second. */
	protected double speed;
	/** The simulation manager used to query time, nodes, and the topology. */
	protected SimulationManager simulationManager;
	/** The closest peripheral edge data center at simulation start. */
	protected ComputingNode closestEdgeDataCenter = ComputingNode.NULL;
	/** Precomputed locations, keyed by simulation time in milliseconds. */
	Map<Integer, Location> path = new LinkedHashMap<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	/** Precomputed closest edge data center, keyed by simulation time in milliseconds. */
	Map<Integer, ComputingNode> datacentersMap = new LinkedHashMap<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));

	/**
	 * An attribute that implements the Null Object Design Pattern to avoid
	 * NullPointerException when using the NULL object instead of attributing null
	 * to MobilityModel variables.
	 */
	public static final MobilityModel NULL = new MobilityModelNull();

	/**
	 * Creates a mobility model for a node at the given initial location.
	 *
	 * @param simulationManager the simulation manager
	 * @param location          the initial location
	 */
	protected MobilityModel(SimulationManager simulationManager, Location location) {
		currentLocation = location;
		setSimulationManager(simulationManager);
	}

	/**
	 * Creates a mobility model without an initial location or simulation manager.
	 * Used by the {@link #NULL} object.
	 */
	protected MobilityModel() {
	}

	/**
	 * Computes the next location of the node. Called once per update interval
	 * while the path table is generated.
	 *
	 * @param location the current location.
	 * @return the next location.
	 */
	protected abstract Location getNextLocation(Location location);

	/**
	 * Updates and returns the current location from the precomputed path for the
	 * given simulation time. Times beyond the simulation duration leave the
	 * location unchanged.
	 *
	 * @param time the simulation time in seconds
	 * @return the current location after the update
	 */
	public Location updateLocation(double time) {
		if (time <= SimulationParameters.simulationDuration)
			currentLocation = path.get((int) time * 1000);
		return currentLocation;
	}

	/**
	 * Returns the current location of the node.
	 *
	 * @return the current location
	 */
	public Location getCurrentLocation() {
		return currentLocation;
	}

	/**
	 * Returns whether the node moves during the simulation.
	 *
	 * @return {@code true} if the node is mobile
	 */
	public boolean isMobile() {
		return isMobile;
	}

	/**
	 * Sets whether the node moves during the simulation.
	 *
	 * @param mobile {@code true} to make the node mobile
	 * @return this model, for method chaining
	 */
	public MobilityModel setMobile(boolean mobile) {
		isMobile = mobile;
		return this;
	}

	/**
	 * Returns the minimum pause duration between two movements.
	 *
	 * @return the minimum pause duration in seconds
	 */
	public double getMinPauseDuration() {
		return minPauseDuration;
	}

	/**
	 * Sets the minimum pause duration between two movements.
	 *
	 * @param minPauseDuration the minimum pause duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMinPauseDuration(double minPauseDuration) {
		this.minPauseDuration = minPauseDuration;
		return this;
	}

	/**
	 * Returns the maximum pause duration between two movements.
	 *
	 * @return the maximum pause duration in seconds
	 */
	public double getMaxPauseDuration() {
		return maxPauseDuration;
	}

	/**
	 * Sets the maximum pause duration between two movements.
	 *
	 * @param maxPauseDuration the maximum pause duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMaxPauseDuration(double maxPauseDuration) {
		this.maxPauseDuration = maxPauseDuration;
		return this;
	}

	/**
	 * Returns the minimum duration of a single movement.
	 *
	 * @return the minimum mobility duration in seconds
	 */
	public double getMinMobilityDuration() {
		return minMobilityDuration;
	}

	/**
	 * Sets the minimum duration of a single movement.
	 *
	 * @param minMobilityDuration the minimum mobility duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMinMobilityDuration(double minMobilityDuration) {
		this.minMobilityDuration = minMobilityDuration;
		return this;
	}

	/**
	 * Returns the maximum duration of a single movement.
	 *
	 * @return the maximum mobility duration in seconds
	 */
	public double getMaxMobilityDuration() {
		return maxMobilityDuration;
	}

	/**
	 * Sets the maximum duration of a single movement.
	 *
	 * @param maxMobilityDuration the maximum mobility duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMaxMobilityDuration(double maxMobilityDuration) {
		this.maxMobilityDuration = maxMobilityDuration;
		return this;
	}

	/**
	 * Returns the movement speed.
	 *
	 * @return the speed in meters per second
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Sets the movement speed.
	 *
	 * @param speed the speed in meters per second
	 * @return this model, for method chaining
	 */
	public MobilityModel setSpeed(double speed) {
		this.speed = speed;
		return this;
	}

	/**
	 * Returns the Euclidean distance between this node and another node, based on
	 * their current locations.
	 *
	 * @param device2 the other node
	 * @return the distance in meters
	 */
	public double distanceTo(ComputingNode device2) {
		return Math.abs(Math.sqrt(Math
				.pow((getCurrentLocation().getXPos() - device2.getMobilityModel().getCurrentLocation().getXPos()), 2)
				+ Math.pow((getCurrentLocation().getYPos() - device2.getMobilityModel().getCurrentLocation().getYPos()),
						2)));
	}

	/**
	 * Returns the simulation manager.
	 *
	 * @return the simulation manager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Sets the simulation manager.
	 *
	 * @param simulationManager the simulation manager
	 */
	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	/**
	 * Generates the location table for the whole simulation at update-interval
	 * granularity. Immobile nodes keep their initial location. For mobile nodes,
	 * the closest edge data center is also recorded at each interval so that
	 * handovers can be resolved instantly later.
	 */
	public void generatePath() {

		closestEdgeDataCenter = getDataCenter();

		if (!isMobile())
			return;
		Location newLocation = getCurrentLocation();

		// Working around the double imprecision
		int interval = (int) (SimulationParameters.updateInterval * 1000);
		int simulationTime = (int) (SimulationParameters.simulationDuration * 1000);

		for (int i = 0; i <= simulationTime; i = i + interval) {
			path.put(i, newLocation);
			newLocation = getNextLocation(newLocation);
			datacentersMap.put(i, getDataCenter());
		}

	}

	/**
	 * Returns the closest peripheral edge data center within
	 * {@link SimulationParameters#edgeDataCentersRange}.
	 *
	 * @return the closest edge data center, or {@link ComputingNode#NULL} when
	 *         none is in range
	 */
	protected ComputingNode getDataCenter() {
		List<ComputingNode> list = getSimulationManager().getDataCentersManager().getComputingNodesGenerator()
				.getEdgeOnlyList();
		double range = SimulationParameters.edgeDataCentersRange;
		ComputingNode closestDC = ComputingNode.NULL;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isPeripheral() && distanceTo(list.get(i)) <= range) {
				range = distanceTo(list.get(i));
				closestDC = list.get(i);
			}
		}
		return closestDC;
	}

	/**
	 * Returns the edge data center closest to the node at the current simulation
	 * time. Mobile nodes within the simulation duration use the value recorded in
	 * the precomputed table; otherwise the value computed at simulation start is
	 * returned.
	 *
	 * @return the closest edge data center, or {@link ComputingNode#NULL} when none
	 *         is in range
	 */
	public ComputingNode getClosestEdgeDataCenter() {
		return (isMobile && getSimulationManager().getSimulation().clock() <= SimulationParameters.simulationDuration)
				? datacentersMap.get((int) (getSimulationManager().getSimulation().clock() * 1000))
				: closestEdgeDataCenter;
	}

}
