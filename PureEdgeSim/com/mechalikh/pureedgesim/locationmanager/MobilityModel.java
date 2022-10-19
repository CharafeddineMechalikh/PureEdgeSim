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

	protected Location currentLocation;
	protected boolean isMobile = false;
	protected double minPauseDuration;
	protected double maxPauseDuration;
	protected double maxMobilityDuration;
	protected double minMobilityDuration;
	protected double speed;
	protected SimulationManager simulationManager;
	protected ComputingNode closestEdgeDataCenter = ComputingNode.NULL;
	Map<Integer, Location> path = new LinkedHashMap<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	Map<Integer, ComputingNode> datacentersMap = new LinkedHashMap<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));

	/**
	 * An attribute that implements the Null Object Design Pattern to avoid
	 * NullPointerException when using the NULL object instead of attributing null
	 * to MobilityModel variables.
	 */
	public static final MobilityModel NULL = new MobilityModelNull();

	protected MobilityModel(SimulationManager simulationManager, Location location) {
		currentLocation = location;
		setSimulationManager(simulationManager);
	}

	protected MobilityModel() {
	}

	protected abstract Location getNextLocation(Location location);

	public Location updateLocation(double time) {
		if (time <= SimulationParameters.simulationDuration)
			currentLocation = path.get((int) time * 1000);
		return currentLocation;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public boolean isMobile() {
		return isMobile;
	}

	public MobilityModel setMobile(boolean mobile) {
		isMobile = mobile;
		return this;
	}

	public double getMinPauseDuration() {
		return minPauseDuration;
	}

	public MobilityModel setMinPauseDuration(double minPauseDuration) {
		this.minPauseDuration = minPauseDuration;
		return this;
	}

	public double getMaxPauseDuration() {
		return maxPauseDuration;
	}

	public MobilityModel setMaxPauseDuration(double maxPauseDuration) {
		this.maxPauseDuration = maxPauseDuration;
		return this;
	}

	public double getMinMobilityDuration() {
		return minMobilityDuration;
	}

	public MobilityModel setMinMobilityDuration(double minMobilityDuration) {
		this.minMobilityDuration = minMobilityDuration;
		return this;
	}

	public double getMaxMobilityDuration() {
		return maxMobilityDuration;
	}

	public MobilityModel setMaxMobilityDuration(double maxMobilityDuration) {
		this.maxMobilityDuration = maxMobilityDuration;
		return this;
	}

	public double getSpeed() {
		return speed;
	}

	public MobilityModel setSpeed(double speed) {
		this.speed = speed;
		return this;
	}

	public double distanceTo(ComputingNode device2) {
		return Math.abs(Math.sqrt(Math
				.pow((getCurrentLocation().getXPos() - device2.getMobilityModel().getCurrentLocation().getXPos()), 2)
				+ Math.pow((getCurrentLocation().getYPos() - device2.getMobilityModel().getCurrentLocation().getYPos()),
						2)));
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

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

	public ComputingNode getClosestEdgeDataCenter() {
		return (isMobile && getSimulationManager().getSimulation().clock() <= SimulationParameters.simulationDuration)
				? datacentersMap.get((int) (getSimulationManager().getSimulation().clock() * 1000))
				: closestEdgeDataCenter;
	}

}
