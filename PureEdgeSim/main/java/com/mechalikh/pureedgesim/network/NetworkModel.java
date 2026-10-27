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
	/** Event tag: send an offloading request from the orchestrator to the destination. */
	public static final int SEND_REQUEST_FROM_ORCH_TO_DESTINATION = 1;
	/** Event tag: a transfer reached its destination. */
	protected static final int TRANSFER_FINISHED = 2;
	/** Event tag: download an application container from the registry. */
	public static final int DOWNLOAD_CONTAINER = 3;
	/** Event tag: send an offloading request from a device to its orchestrator. */
	public static final int SEND_REQUEST_FROM_DEVICE_TO_ORCH = 4;
	/** Event tag: send execution results to the orchestrator. */
	public static final int SEND_RESULT_TO_ORCH = 6;
	/** Event tag: send execution results from the orchestrator to the device. */
	public static final int SEND_RESULT_FROM_ORCH_TO_DEV = 7;
	// the list where the current (and the previous)
	// transferred files are stored
	/** Simulation manager that provides access to the topology and logger. */
	protected SimulationManager simulationManager;
	/** Shared uplink, set only when {@code use_one_shared_wan_link} is enabled. */
	protected NetworkLinkWanUp wanUp;
	/** Shared downlink, set only when {@code use_one_shared_wan_link} is enabled. */
	protected NetworkLinkWanDown wanDown;

	/**
	 * Creates the network model and registers it in the simulation manager.
	 *
	 * @param simulationManager the simulation manager.
	 */
	protected NetworkModel(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		setSimulationManager(simulationManager);
		simulationManager.setNetworkModel(this);
	}

	/**
	 * Sets the simulation manager.
	 *
	 * @param simulationManager the simulation manager.
	 */
	protected void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	/**
	 * Updates the battery of the edge devices involved in a transfer.
	 *
	 * @param transfer    the finished transfer.
	 * @param origin      the sender.
	 * @param destination the receiver.
	 */
	protected abstract void updateEdgeDevicesRemainingEnergy(TransferProgress transfer, ComputingNode origin,
			ComputingNode destination);

	/**
	 * Called when a transfer reaches its final destination.
	 *
	 * @param transfer the finished transfer.
	 */
	protected abstract void transferFinished(TransferProgress transfer);

	/**
	 * Sets the shared WAN links used by {@link #getWanUpUtilization()} and
	 * {@link #getWanDownUtilization()}.
	 *
	 * @param wanUp   the shared uplink.
	 * @param wanDown the shared downlink.
	 */
	public void setWanLinks(NetworkLinkWanUp wanUp, NetworkLinkWanDown wanDown) {
		this.wanUp = wanUp;
		this.wanDown = wanDown;
	}

	/**
	 * Returns the current utilization of the shared WAN uplink.
	 *
	 * @return the used bandwidth, in bits per second.
	 * @throws IllegalArgumentException when {@code use_one_shared_wan_link} is
	 *                                  disabled.
	 */
	public double getWanUpUtilization() {
		if (!SimulationParameters.useOneSharedWanLink)
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - The \"one_shared_wan_network\" option needs to be enabled in simulation_parameters.properties file in order to call \"getWanUpUtilization()\"");
		return wanUp.getUsedBandwidth();
	}

	/**
	 * Returns the current utilization of the shared WAN downlink.
	 *
	 * @return the used bandwidth, in bits per second.
	 * @throws IllegalArgumentException when {@code use_one_shared_wan_link} is
	 *                                  disabled.
	 */
	public double getWanDownUtilization() {
		if (!SimulationParameters.useOneSharedWanLink)
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - The \"one_shared_wan_network\" option needs to be enabled in simulation_parameters.properties file in order to call \"getWanDownUtilization()\"");
		return wanDown.getUsedBandwidth();
	}

}
