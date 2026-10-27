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
package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * An energy-aware node that has a position and, optionally, a mobility model.
 * Peripheral nodes are the ones mobile devices can connect to when they are in
 * range.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 2.0
 */
public abstract class LocationAwareNode extends EnergyAwareNode {
	/** Mobility model that provides the node position over simulation time. */
	protected MobilityModel mobilityModel = MobilityModel.NULL;
	/** Whether this node is peripheral and can serve as an uplink candidate. */
	protected boolean peripheral = false;
	/** The node currently executing this device's application. */
	protected ComputingNode applicationPlacementLocation = ComputingNode.NULL;
	/** Whether this device's application has already been placed on a node. */
	protected boolean isApplicationPlaced = false;

	/**
	 * Creates a location-aware node.
	 *
	 * @param simulationManager the simulation manager
	 */
	protected LocationAwareNode(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Generates the mobility path when the simulation starts.
	 */
	@Override
	public void onSimulationStart() {
		super.onSimulationStart();
		mobilityModel.generatePath();
	}

	/**
	 * Updates the location of mobile nodes and reconnects them to the closest edge
	 * data center.
	 */
	@Override
	protected void updateStatus() {
		super.updateStatus();
		// Update location
		if (getMobilityModel().isMobile() && !isDead) {
			getMobilityModel().updateLocation(simulationManager.getSimulation().clock());
			connectWith(getMobilityModel().getClosestEdgeDataCenter());
		}
	}

	/**
	 * Points the current uplink and downlink toward the given edge data center.
	 * When this device has a device-to-device link and has moved beyond the edge
	 * data center range from its peer, the application is marked as no longer
	 * placed so that it will be placed again on the next task.
	 *
	 * @param closestEdgeDataCenter the edge data center to connect to
	 */
	protected void connectWith(ComputingNode closestEdgeDataCenter) {
		getCurrentLink(LinkOrientation.UP_LINK).setDst(closestEdgeDataCenter);
		getCurrentLink(LinkOrientation.DOWN_LINK).setSrc(closestEdgeDataCenter);

		if (getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE).getDst() != ComputingNode.NULL && getMobilityModel()
				.distanceTo(getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE).getDst()) >= SimulationParameters.edgeDataCentersRange) {
			setApplicationPlaced(false);
		}
	}

	/**
	 * Returns the node that orchestrates tasks for this node.
	 *
	 * The location depends on the {@code deployOrchestrators} parameter: an empty
	 * value or {@code CLOUD} uses the first cloud node, {@code EDGE} uses the
	 * closest edge data center, {@code MIST} uses this node itself, and any other
	 * value selects the node in the orchestrators list with the lowest topology
	 * delay. When orchestrators are disabled, this node orchestrates its own
	 * tasks. The result is cached after the first call.
	 *
	 * @return the orchestrator node
	 */
	@Override
	public ComputingNode getOrchestrator() {
		if (orchestrator == ComputingNode.NULL && SimulationParameters.enableOrchestrators) {
			if ("".equals(SimulationParameters.deployOrchestrators)
					|| ("CLOUD".equals(SimulationParameters.deployOrchestrators))) {
				orchestrator = simulationManager.getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList()
						.get(0);
			} else if ("EDGE".equals(SimulationParameters.deployOrchestrators)) {
				orchestrator = getMobilityModel().getClosestEdgeDataCenter();
			} else if ("MIST".equals(SimulationParameters.deployOrchestrators)) {
				orchestrator = this;
			} else {
				double min = Double.POSITIVE_INFINITY;
				for (ComputingNode node : simulationManager.getDataCentersManager().getComputingNodesGenerator()
						.getOrchestratorsList()) {
					double delay = simulationManager.getDataCentersManager().getTopology().getDelay(this, node);
					if (delay < min) {
						min = delay;
						orchestrator = node;
					}
				}
			}
		} else if (!SimulationParameters.enableOrchestrators)
			orchestrator = this;
		return orchestrator;
	}

	/**
	 * Returns the mobility model of this node.
	 *
	 * @return the mobility model
	 */
	public MobilityModel getMobilityModel() {
		return mobilityModel;
	}

	/**
	 * Sets the mobility model of this node.
	 *
	 * @param mobilityModel the mobility model
	 */
	public void setMobilityModel(MobilityModel mobilityModel) {
		this.mobilityModel = mobilityModel;
	}

	/**
	 * Returns whether this node is peripheral.
	 *
	 * @return {@code true} if the node is peripheral
	 */
	public boolean isPeripheral() {
		return peripheral;
	}

	/**
	 * Sets whether this node is peripheral.
	 *
	 * @param periphery {@code true} if the node is peripheral
	 */
	public void setPeriphery(boolean periphery) {
		this.peripheral = periphery;
	}

	/**
	 * Returns the node where this device's application is placed.
	 *
	 * @return the application placement location
	 */
	public ComputingNode getApplicationPlacementLocation() {
		return this.applicationPlacementLocation;
	}

	/**
	 * Returns whether this device's application has been placed.
	 *
	 * @return {@code true} if the application is placed
	 */
	public boolean isApplicationPlaced() {
		return isApplicationPlaced;
	}

	/**
	 * Sets whether this device's application has been placed.
	 *
	 * @param isApplicationPlaced {@code true} if the application is placed
	 */
	public void setApplicationPlaced(boolean isApplicationPlaced) {
		this.isApplicationPlaced = isApplicationPlaced;
	}

}
