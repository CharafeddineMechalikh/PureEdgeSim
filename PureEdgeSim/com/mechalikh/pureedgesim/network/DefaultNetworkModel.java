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

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DefaultNetworkModel extends NetworkModel {

	public DefaultNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	public void processEvent(Event ev) {
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
		case TRANSFER_FINISHED:
			// Transfer the execution results from the orchestrators to the device
			transferFinished((TransferProgress) ev.getData());
			break;
		default:
			break;
		}
	}

	public void send(ComputingNode from, ComputingNode to, Task task, double fileSize, TransferProgress.Type type) {
		List<ComputingNode> vertexList = new ArrayList<>(5);
		List<NetworkLink> edgeList = new ArrayList<>(5);

		// If both are edge devices (one hop far from each other), send directly.
		if (from.getType() == TYPES.EDGE_DEVICE && to.getType() == TYPES.EDGE_DEVICE) {
			from.getCurrentWiFiLink().setDst(to);
			vertexList.addAll(List.of(from, to));
			edgeList.add(from.getCurrentWiFiLink());

		} // Otherwise, if the first is a mobile edge device
		else if (from.getType() == TYPES.EDGE_DEVICE && to.getType() == TYPES.EDGE_DATACENTER) { 
			long id = simulationManager.getDataCentersManager().getTopology()
					.getUniqueId(from.getCurrentUpLink().getDst().getId(), to.getId());
			GraphPath<ComputingNode, NetworkLink> path = simulationManager.getDataCentersManager().getTopology()
					.getPathsMap().get(id);
			vertexList.add(from);
			vertexList.addAll(path.getVertexList());
			edgeList.add(from.getCurrentUpLink());
			edgeList.addAll(path.getEdgeList());

		} // Else, if the second is a mobile edge device
		else if (from.getType() == TYPES.EDGE_DATACENTER && to.getType() == TYPES.EDGE_DEVICE) {
			long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(),
					to.getCurrentDownLink().getSrc().getId());
			GraphPath<ComputingNode, NetworkLink> path = simulationManager.getDataCentersManager().getTopology()
					.getPathsMap().get(id);
			vertexList.addAll(path.getVertexList());
			vertexList.add(from);
			edgeList.addAll(path.getEdgeList());
			edgeList.add(to.getCurrentDownLink()); 
			
		} 
		else { // Otherwise, if one of them is and edge device but not mobile, or the other is a cloud, or any other cases.
			GraphPath<ComputingNode, NetworkLink> path;
			long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(), to.getId());

			if (simulationManager.getDataCentersManager().getTopology().getPathsMap().containsKey(id)) {
				path = simulationManager.getDataCentersManager().getTopology().getPathsMap().get(id);
			} else {
				path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
				simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
			}
			vertexList.addAll(path.getVertexList());
			edgeList.addAll(path.getEdgeList());
			
		}
		edgeList.get(0).addTransfer(
				new TransferProgress(task, fileSize, type).setVertexList(vertexList).setEdgeList(edgeList));

	}

	public void sendRequestFromOrchToDest(Task task) {
		if (task.getOrchestrator() != task.getOffloadingDestination()
				&& task.getOffloadingDestination() != task.getEdgeDevice())

			send(task.getOrchestrator(), task.getOffloadingDestination(), task, task.getFileSizeInBits(),
					TransferProgress.Type.TASK);
		else // The device will execute the task locally
			executeTaskOrDownloadContainer(
					new TransferProgress(task, task.getFileSizeInBits(), TransferProgress.Type.TASK));
	}

	public void sendResultFromOrchToDev(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice())
			send(task.getOrchestrator(), task.getEdgeDevice(), task, task.getOutputSizeInBits(),
					TransferProgress.Type.RESULTS_TO_DEV);
		else
			scheduleNow(simulationManager, DefaultSimulationManager.RESULT_RETURN_FINISHED, task);

	}

	public void sendResultFromDevToOrch(Task task) {
		if (task.getOffloadingDestination() != task.getOrchestrator())
			send(task.getOffloadingDestination(), task.getOrchestrator(), task, task.getOutputSizeInBits(),
					TransferProgress.Type.RESULTS_TO_ORCH);
		else
			scheduleNow(this, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, task);

	}

	public void addContainer(Task task) {
		if (task.getRegistry() != task.getOffloadingDestination())
			send(task.getRegistry(), task.getOffloadingDestination(), task, task.getContainerSizeInBits(),
					TransferProgress.Type.CONTAINER);
		else
			scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, task);
	}

	public void sendRequestFromDeviceToOrch(Task task) {
		if (SimulationParameters.enableOrchestrators && task.getEdgeDevice() != task.getOrchestrator()) {
			send(task.getEdgeDevice(), task.getOrchestrator(), task, task.getFileSizeInBits(),
					TransferProgress.Type.REQUEST);

		} else // The device orchestrates its tasks by itself, so, send the request directly to
				// destination
		{
			scheduleNow(simulationManager, DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task);
		}
	}

	protected void transferFinished(TransferProgress transfer) {

		// If it is an offloading request that is sent to the orchestrator
		if (transfer.getTransferType() == TransferProgress.Type.REQUEST) {
			// in case this node is the orchestrator

			if (transfer.getVertexList().get(0) == transfer.getTask().getOrchestrator()) {
				updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getEdgeDevice(),
						transfer.getTask().getOrchestrator());
			}
			offloadingRequestRecievedByOrchestrator(transfer);
		}
		// If it is a task (or offloading request) that is sent to the destination
		else if (transfer.getTransferType() == TransferProgress.Type.TASK) {
			// in case this node is the destination
			if (transfer.getVertexList().get(0) == transfer.getTask().getOffloadingDestination()) {
				updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getEdgeDevice(),
						transfer.getTask().getOffloadingDestination());
			}

			executeTaskOrDownloadContainer(transfer);
		}
		// If the container has been downloaded, then execute the task now
		else if (transfer.getTransferType() == TransferProgress.Type.CONTAINER) {
			containerDownloadFinished(transfer);
			updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getRegistry(),
					transfer.getTask().getEdgeDevice());
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == TransferProgress.Type.RESULTS_TO_ORCH) {
			returnResultsToDevice(transfer);
			updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getOffloadingDestination(),
					transfer.getTask().getOrchestrator());
		}
		// Results transferred to the device
		else {
			resultsReturnedToDevice(transfer);
			updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getOrchestrator(),
					transfer.getTask().getEdgeDevice());
		}

	}

	protected void updateEdgeDevicesRemainingEnergy(TransferProgress transfer, ComputingNode origin,
			ComputingNode destination) {
		if (origin != ComputingNode.NULL && origin.getType() == TYPES.EDGE_DEVICE) {
			origin.getEnergyModel().updatewirelessEnergyConsumption(transfer.getFileSize(),
					EnergyModelComputingNode.TRANSMISSION);
		}
		if (destination.getType() == TYPES.EDGE_DEVICE)
			destination.getEnergyModel().updatewirelessEnergyConsumption(transfer.getFileSize(),
					EnergyModelComputingNode.RECEPTION);
	}

	protected void containerDownloadFinished(TransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, transfer.getTask());
	}

	protected void resultsReturnedToDevice(TransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.RESULT_RETURN_FINISHED, transfer.getTask());
	}

	protected void returnResultsToDevice(TransferProgress transfer) {
		scheduleNow(this, NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, transfer.getTask());
	}

	protected void executeTaskOrDownloadContainer(TransferProgress transfer) {
		if (SimulationParameters.enableRegistry && "CLOUD".equals(SimulationParameters.registryMode)
				&& !(transfer.getTask().getOffloadingDestination()).getType().equals(TYPES.CLOUD)) {
			// If the registry is enabled and the task is offloaded to the edge data centers
			// or the mist nodes (edge devices),
			// then download the container
			scheduleNow(this, DefaultNetworkModel.DOWNLOAD_CONTAINER, transfer.getTask());

		} else// if the registry is disabled, execute directly the task
			scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, transfer.getTask());
	}

	protected void offloadingRequestRecievedByOrchestrator(TransferProgress transfer) {
		// Find the offloading destination and execute the task
		scheduleNow(simulationManager, DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
	}

	/**
	 * Defines the logic to be performed by the default network model when the
	 * simulation starts.
	 */
	@Override
	public void startInternal() {
		// Do nothing.
	}

	/**
	 * Defines the logic to be performed by the default network model when the
	 * simulation ends.
	 */
	@Override
	public void onSimulationEnd() {
		// Do something when the simulation finishes.
	}
}
