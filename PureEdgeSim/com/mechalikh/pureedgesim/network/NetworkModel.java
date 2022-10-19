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
package com.mechalikh.pureedgesim.network;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * The main class of the Network module, that handles all network events, and
 * updates the network topology status.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public abstract class NetworkModel extends SimEntity {
	public static final int SEND_REQUEST_FROM_ORCH_TO_DESTINATION = 1;
	protected static final int TRANSFER_FINISHED = 2;
	public static final int DOWNLOAD_CONTAINER = 3;
	public static final int SEND_REQUEST_FROM_DEVICE_TO_ORCH = 4;
	public static final int SEND_RESULT_TO_ORCH = 6;
	public static final int SEND_RESULT_FROM_ORCH_TO_DEV = 7;
	// the list where the current (and the previous)
	// transferred files are stored
	protected SimulationManager simulationManager;
	protected NetworkLinkWanUp wanUp;
	protected NetworkLinkWanDown wanDown;

	protected NetworkModel(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		setSimulationManager(simulationManager);
		simulationManager.setNetworkModel(this);
	}

	protected void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	protected abstract void updateEdgeDevicesRemainingEnergy(TransferProgress transfer, ComputingNode origin,
			ComputingNode destination);

	protected abstract void transferFinished(TransferProgress transfer);

	public void setWanLinks(NetworkLinkWanUp wanUp, NetworkLinkWanDown wanDown) {
		this.wanUp = wanUp;
		this.wanDown = wanDown;
	}

	public double getWanUpUtilization() {
		if (!SimulationParameters.useOneSharedWanLink)
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - The \"one_shared_wan_network\" option needs to be enabled in simulation_parameters.properties file in  in order to call \"getWanUpUtilization()\"");
		return wanUp.getUsedBandwidth();
	}

	public double getWanDownUtilization() {
		if (!SimulationParameters.useOneSharedWanLink)
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - The \"one_shared_wan_network\" option needs to be enabled in simulation_parameters.properties file in order to call \"getWanDownUtilization()\"");
		return wanDown.getUsedBandwidth();
	}

}
