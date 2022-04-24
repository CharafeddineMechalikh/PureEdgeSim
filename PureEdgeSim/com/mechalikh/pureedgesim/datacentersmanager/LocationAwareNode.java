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

	public LocationAwareNode(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	public void startInternal() {
		super.startInternal();
		getMobilityModel().generatePath();
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
				.distanceTo(getCurrentWiFiLink().getDst()) >= SimulationParameters.EDGE_DEVICES_RANGE) {
			setApplicationPlaced(false);
		}
	}

	@Override
	public ComputingNode getOrchestrator() {
		if (orchestrator == ComputingNode.NULL && SimulationParameters.ENABLE_ORCHESTRATORS) {
			if ("".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)
					|| ("CLOUD".equals(SimulationParameters.DEPLOY_ORCHESTRATOR))) {
				orchestrator = simulationManager.getDataCentersManager().getCloudDatacentersList().get(0);
			} else if ("EDGE".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)) {
				orchestrator = getMobilityModel().getClosestEdgeDataCenter();
			} else if ("MIST".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)) {
				orchestrator = (ComputingNode) this;
			} else {
				double min = Double.POSITIVE_INFINITY;
				int orch = 0;
				for (int i = 0; i < simulationManager.getDataCentersManager().getOrchestratorsList().size(); i++) {
					double delay = simulationManager.getDataCentersManager().getTopology().getDelay(
							(ComputingNode) this,
							simulationManager.getDataCentersManager().getOrchestratorsList().get(i));
					if (delay < min) {
						min = delay;
						orch = i;
					}
				}

				orchestrator = (ComputingNode) simulationManager.getDataCentersManager().getOrchestratorsList()
						.get(orch);
			}
		} else if (!SimulationParameters.ENABLE_ORCHESTRATORS)
			orchestrator = this;
		return orchestrator;
	}

	public MobilityModel getMobilityModel() {
		return mobilityModel;
	}

	public void setMobilityModel(MobilityModel mobilityModel) {
		this.mobilityModel = (MobilityModel) mobilityModel;
	}

	public boolean isPeripheral() {
		return peripheral;
	}

	public void setPeriphery(boolean periphery) {
		this.peripheral = periphery;
	}

	public abstract void setApplicationPlacementLocation(ComputingNode node);

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
