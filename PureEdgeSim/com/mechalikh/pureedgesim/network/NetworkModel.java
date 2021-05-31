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
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.network;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.events.SimEvent;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;

public abstract class NetworkModel extends CloudSimEntity {
	public static final int base = 4000;
	public static final int SEND_REQUEST_FROM_ORCH_TO_DESTINATION = base + 1;
	protected static final int UPDATE_PROGRESS = base + 2;
	public static final int DOWNLOAD_CONTAINER = base + 3;
	public static final int SEND_REQUEST_FROM_DEVICE_TO_ORCH = base + 4;
	public static final int SEND_RESULT_FROM_ORCH_TO_DEV = base + 5;
	public static final int SEND_UPDATE_FROM_DEVICE_TO_ORCH = base + 6;
	public static final int SEND_RESULT_TO_ORCH = base + 7;
	// the list where the current (and the previous)
	// transferred file are stored
	protected List<FileTransferProgress> transferProgressList;
	protected SimulationManager simulationManager;

	public NetworkModel(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		setSimulationManager(simulationManager);
		transferProgressList = new ArrayList<>();
	}

	private void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	protected abstract void updateTasksProgress();

	protected abstract void updateTransfer(FileTransferProgress transfer);

	protected abstract void updateEnergyConsumption(FileTransferProgress transfer, String type);

	protected abstract void transferFinished(FileTransferProgress transfer);

	protected boolean sameLanIsUsed(Task task1, Task task2) {
		// The transfers share same Lan of they have one device in common
		// Compare orchestrator
		return ((task1.getOrchestrator() == task2.getOrchestrator())
				|| (task1.getOrchestrator() == task2.getVm().getHost().getDatacenter())
				|| (task1.getOrchestrator() == task2.getEdgeDevice())
				|| (task1.getOrchestrator() == task2.getRegistry())

				// Compare origin device
				|| (task1.getEdgeDevice() == task2.getOrchestrator())
				|| (task1.getEdgeDevice() == task2.getVm().getHost().getDatacenter())
				|| (task1.getEdgeDevice() == task2.getEdgeDevice()) || (task1.getEdgeDevice() == task2.getRegistry())

				// Compare offloading destination
				|| (task1.getVm().getHost().getDatacenter() == task2.getOrchestrator())
				|| (task1.getVm().getHost().getDatacenter() == task2.getVm().getHost().getDatacenter())
				|| (task1.getVm().getHost().getDatacenter() == task2.getEdgeDevice())
				|| (task1.getVm().getHost().getDatacenter() == task2.getRegistry()));
	}

	protected boolean wanIsUsed(FileTransferProgress fileTransferProgress) {
		return ((fileTransferProgress.getTransferType() == FileTransferProgress.Type.TASK
				&& ((DataCenter) fileTransferProgress.getTask().getVm().getHost().getDatacenter()).getType()
						.equals(TYPES.CLOUD))
				// If the offloading destination is the cloud

				|| (fileTransferProgress.getTransferType() == FileTransferProgress.Type.CONTAINER
						&& (fileTransferProgress.getTask().getRegistry() == null
								|| fileTransferProgress.getTask().getRegistry().getType() == TYPES.CLOUD))
				// Or if containers will be downloaded from registry

				|| (fileTransferProgress.getTask().getOrchestrator().getType() == SimulationParameters.TYPES.CLOUD));
		// Or if the orchestrator is deployed in the cloud

	}

	protected void updateBandwidth(FileTransferProgress transfer) {
		double bandwidth;
		if (wanIsUsed(transfer)) {
			// The bandwidth will be limited by the minimum value
			// If the lan bandwidth is 1 mbps and the wan bandwidth is 4 mbps
			// It will be limited by the lan, so we will choose the minimum
			bandwidth = Math.min(transfer.getLanBandwidth(), transfer.getWanBandwidth());
		} else
			bandwidth = transfer.getLanBandwidth();
		transfer.setCurrentBandwidth(bandwidth);
	}

	protected double getLanBandwidth(double remainingTasksCount_Lan) {
		return (SimulationParameters.BANDWIDTH_WLAN / (remainingTasksCount_Lan > 0 ? remainingTasksCount_Lan : 1));
	}

	protected double getWanBandwidth(double remainingTasksCount_Wan) {
		return (SimulationParameters.WAN_BANDWIDTH / (remainingTasksCount_Wan > 0 ? remainingTasksCount_Wan : 1));
	}

	@Override
	protected void startInternal() {
	}

	@Override
	public void processEvent(SimEvent ev) {
	}

	public abstract double getWanUtilization();

}
