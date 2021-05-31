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

import org.cloudbus.cloudsim.core.events.SimEvent;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultEnergyModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;

public class DefaultNetworkModel extends NetworkModel { 
	public DefaultNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case SEND_REQUEST_FROM_DEVICE_TO_ORCH:
			// Send the offloading request to the orchestrator
			sendRequestFromDeviceToOrch((Task) ev.getData());
			break;
		case SEND_REQUEST_FROM_ORCH_TO_DESTINATION:
			// Forward the offloading request from orchestrator to offloading destination
			sendRequestFromOrchToDest((Task) ev.getData());
			break;
		case DOWNLOAD_CONTAINER: 
			// Pull the container from the registry
			addContainer((Task) ev.getData());
			break;
		case SEND_RESULT_TO_ORCH:
			// Send the execution results to the orchestrator
			sendResultFromDevToOrch((Task) ev.getData());
			break;
		case SEND_RESULT_FROM_ORCH_TO_DEV:
			// Transfer the execution results from the orchestrators to the device
			sendResultFromOrchToDev((Task) ev.getData());
			break;
		case UPDATE_PROGRESS:
			// update the progress of the current transfers and their allocated bandwidth 
			updateTasksProgress(); 
			schedule(this, SimulationParameters.NETWORK_UPDATE_INTERVAL, UPDATE_PROGRESS);
			break;
		default:
			break;
		}
	}


	public void sendRequestFromOrchToDest(Task task) {
		transferProgressList
				.add(new FileTransferProgress(task, task.getFileSize() * 8, FileTransferProgress.Type.TASK));
	}

	public void sendResultFromOrchToDev(Task task) {
		transferProgressList.add(
				new FileTransferProgress(task, task.getOutputSize() * 8, FileTransferProgress.Type.RESULTS_TO_DEV));
	}

	public void sendResultFromDevToOrch(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice())
			transferProgressList.add(new FileTransferProgress(task, task.getOutputSize() * 8,
					FileTransferProgress.Type.RESULTS_TO_ORCH));
		else
			scheduleNow(this, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, task);
	}

	public void addContainer(Task task) {
		transferProgressList
				.add(new FileTransferProgress(task, task.getContainerSize() * 8, FileTransferProgress.Type.CONTAINER));
	}

	public void sendRequestFromDeviceToOrch(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice())
			transferProgressList
					.add(new FileTransferProgress(task, task.getFileSize() * 8, FileTransferProgress.Type.REQUEST));
		else // The device orchestrate its tasks by itself, so, send the request directly to
				// destination
			scheduleNow(simulationManager, SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task);
	}

	protected void updateTasksProgress() {
		// Ignore finished transfers, so we will start looping from the first index of
		// the remaining transfers
		int remainingTransfersCount_Lan;
		int remainingTransfersCount_Wan; 
		for (int i = 0; i < transferProgressList.size(); i++) {
			if (transferProgressList.get(i).getRemainingFileSize() > 0) {
				remainingTransfersCount_Lan = 0;
				remainingTransfersCount_Wan = 0;
				for (int j = 0; j < transferProgressList.size(); j++) {
					if (transferProgressList.get(j).getRemainingFileSize() > 0 && j != i
							&& wanIsUsed(transferProgressList.get(j))) {
						remainingTransfersCount_Wan++; 
					}
					if (transferProgressList.get(j).getRemainingFileSize() > 0 && j != i && sameLanIsUsed(
							transferProgressList.get(i).getTask(), transferProgressList.get(j).getTask())) {
						// Both transfers use same Lan
						remainingTransfersCount_Lan++;
					}
				}

				// allocate bandwidths
				transferProgressList.get(i).setLanBandwidth(getLanBandwidth(remainingTransfersCount_Lan));
				transferProgressList.get(i).setWanBandwidth(getWanBandwidth(remainingTransfersCount_Wan));
				updateBandwidth(transferProgressList.get(i));
				updateTransfer(transferProgressList.get(i));
			}

		}
	}

	protected void updateTransfer(FileTransferProgress transfer) {

		double oldRemainingSize = transfer.getRemainingFileSize();

		// Update progress (remaining file size)
		if (SimulationParameters.REALISTIC_NETWORK_MODEL)
			transfer.setRemainingFileSize(transfer.getRemainingFileSize()
					- (SimulationParameters.NETWORK_UPDATE_INTERVAL * transfer.getCurrentBandwidth()));
		else
			transfer.setRemainingFileSize(0);

		// Update LAN network usage delay
		transfer.setLanNetworkUsage(transfer.getLanNetworkUsage()
				+ (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth());

		// Update WAN network usage delay 
		if (wanIsUsed(transfer))
			transfer.setWanNetworkUsage(transfer.getWanNetworkUsage()
					+ (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth());
		if (transfer.getRemainingFileSize() <= 0) { // Transfer finished
			transfer.setRemainingFileSize(0); // if < 0 set it to 0
			transferFinished(transfer);
		}
	}

	protected void updateEnergyConsumption(FileTransferProgress transfer, String type) {
		// update energy consumption
		if ("Orchestrator".equals(type)) {
			calculateEnergyConsumption(transfer.getTask().getEdgeDevice(), transfer.getTask().getOrchestrator(),
					transfer);
		} else if ("Destination".equals(type)) {
			calculateEnergyConsumption(transfer.getTask().getOrchestrator(),
					((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()), transfer);
		} else if ("Container".equals(type)) {
			// update the energy consumption of the registry and the device
			calculateEnergyConsumption(transfer.getTask().getRegistry(), transfer.getTask().getEdgeDevice(), transfer);
		} else if ("Result_Orchestrator".equals(type)) {
			calculateEnergyConsumption(((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()),
					transfer.getTask().getOrchestrator(), transfer);
		} else if ("Result_Origin".equals(type)) {
			calculateEnergyConsumption(transfer.getTask().getOrchestrator(), transfer.getTask().getEdgeDevice(),
					transfer);
		}

	}

	private void calculateEnergyConsumption(DataCenter origin, DataCenter destination, FileTransferProgress transfer) {
		if (origin != null) {
			origin.getEnergyModel().updatewirelessEnergyConsumption(transfer, origin, destination,
					DefaultEnergyModel.TRANSMISSION);
		}
		destination.getEnergyModel().updatewirelessEnergyConsumption(transfer, origin, destination,
				DefaultEnergyModel.RECEPTION);
	}

	protected void transferFinished(FileTransferProgress transfer) {
		// Update logger parameters
		simulationManager.getSimulationLogger().updateNetworkUsage(transfer);

		// Delete the transfer from the queue
		transferProgressList.remove(transfer);

		// If it is an offlaoding request that is sent to the orchestrator
		if (transfer.getTransferType() == FileTransferProgress.Type.REQUEST) {
			offloadingRequestRecievedByOrchestrator(transfer);
			updateEnergyConsumption(transfer, "Orchestrator");
		}
		// If it is a task (or offloading request) that is sent to the destination
		else if (transfer.getTransferType() == FileTransferProgress.Type.TASK) {
			transfer.getTask().setReceptionTime(simulationManager.getSimulation().clock());
			executeTaskOrDownloadContainer(transfer);
			updateEnergyConsumption(transfer, "Destination");
		}
		// If the container has been downloaded, then execute the task now
		else if (transfer.getTransferType() == FileTransferProgress.Type.CONTAINER) { 
			transfer.getTask().setReceptionTime(simulationManager.getSimulation().clock());
			containerDownloadFinished(transfer);
			updateEnergyConsumption(transfer, "Container");
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == FileTransferProgress.Type.RESULTS_TO_ORCH) {
			returnResultToDevice(transfer);
			updateEnergyConsumption(transfer, "Result_Orchestrator");
		}
		// Results transferred to the device
		else {
			resultsReturnedToDevice(transfer);
			updateEnergyConsumption(transfer, "Result_Origin");
		}

	}

	protected void containerDownloadFinished(FileTransferProgress transfer) {
		scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, transfer.getTask());
	}

	protected void resultsReturnedToDevice(FileTransferProgress transfer) {
		scheduleNow(simulationManager, SimulationManager.RESULT_RETURN_FINISHED, transfer.getTask());
	}

	protected void returnResultToDevice(FileTransferProgress transfer) {
		// if the results are returned from the cloud, consider the wan propagation
		// delay
		if (transfer.getTask().getOrchestrator().getType().equals(TYPES.CLOUD)
				|| ((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()).getType().equals(TYPES.CLOUD))
			schedule(this, SimulationParameters.WAN_PROPAGATION_DELAY, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV,
					transfer.getTask());
		else
			scheduleNow(this, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, transfer.getTask());

	}

	protected void executeTaskOrDownloadContainer(FileTransferProgress transfer) { 
		if (SimulationParameters.ENABLE_REGISTRY && "CLOUD".equals(SimulationParameters.registry_mode)
				&& !((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()).getType().equals(TYPES.CLOUD)) {
			// if the registry is enabled and the task is offloaded to the edge data centers
			// or the mist nodes (edge devices),
			// then download the container
			scheduleNow(this, DefaultNetworkModel.DOWNLOAD_CONTAINER, transfer.getTask());

		} else {// if the registry is disabled, execute directly the request, as it represents
				// the offloaded task in this case
			if (((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()).getType().equals(TYPES.CLOUD))
				schedule(simulationManager, SimulationParameters.WAN_PROPAGATION_DELAY, SimulationManager.EXECUTE_TASK,
						transfer.getTask());
			else
				scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, transfer.getTask());
		}
	}

	protected void offloadingRequestRecievedByOrchestrator(FileTransferProgress transfer) {
		// Find the offloading destination and execute the task
		if (transfer.getTask().getOrchestrator().getType().equals(TYPES.CLOUD))
			schedule(simulationManager, SimulationParameters.WAN_PROPAGATION_DELAY,
					SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
		else
			scheduleNow(simulationManager, SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
	}

	@Override
	protected void startInternal() {
		schedule(this, SimulationParameters.NETWORK_UPDATE_INTERVAL, UPDATE_PROGRESS);
	}
	public double getWanUtilization() {
		int wanTasks = 0;
		double bwUsage = 0;
		for (FileTransferProgress fileTransferProgress : transferProgressList) {
			if (fileTransferProgress.getRemainingFileSize() > 0 && wanIsUsed(fileTransferProgress)) {
				wanTasks++;
				bwUsage += fileTransferProgress.getRemainingFileSize();
			}
		}
		bwUsage = (wanTasks > 0 ? bwUsage / (wanTasks * 1000) : 0);
		return Math.min(bwUsage, SimulationParameters.WAN_BANDWIDTH / 1000);
	}
}
