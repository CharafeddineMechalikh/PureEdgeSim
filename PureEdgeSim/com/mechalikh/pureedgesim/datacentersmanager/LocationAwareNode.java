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

public abstract class LocationAwareNode extends EnergyAwareNode {
	protected MobilityModel mobilityModel = MobilityModel.NULL;
	protected boolean peripheral = false;
	protected ComputingNode applicationPlacementLocation = ComputingNode.NULL;
	protected boolean isApplicationPlaced = false;

	protected LocationAwareNode(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	public void startInternal() {
		super.startInternal();
		mobilityModel.generatePath();
	}

	@Override
	protected void updateStatus() {
		super.updateStatus();
		// Update location
		if (getMobilityModel().isMobile() && !isDead) {
			getMobilityModel().updateLocation(simulationManager.getSimulation().clock());
			connectWith(getMobilityModel().getClosestEdgeDataCenter());
		}
	}

	protected void connectWith(ComputingNode closestEdgeDataCenter) {
		getCurrentUpLink().setDst(closestEdgeDataCenter);
		getCurrentDownLink().setSrc(closestEdgeDataCenter);

		if (getCurrentWiFiLink().getDst() != ComputingNode.NULL && getMobilityModel()
				.distanceTo(getCurrentWiFiLink().getDst()) >= SimulationParameters.edgeDataCentersRange) {
			setApplicationPlaced(false);
		}
	}

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

	public MobilityModel getMobilityModel() {
		return mobilityModel;
	}

	public void setMobilityModel(MobilityModel mobilityModel) {
		this.mobilityModel = mobilityModel;
	}

	public boolean isPeripheral() {
		return peripheral;
	}

	public void setPeriphery(boolean periphery) {
		this.peripheral = periphery;
	}

	public ComputingNode getApplicationPlacementLocation() {
		return this.applicationPlacementLocation;
	}

	public boolean isApplicationPlaced() {
		return isApplicationPlaced;
	}

	public void setApplicationPlaced(boolean isApplicationPlaced) {
		this.isApplicationPlaced = isApplicationPlaced;
	}

}
