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
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Default network model. It routes requests, containers, and results through
 * the topology created by
 * {@link com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator},
 * updates link utilization and energy, and triggers the next simulation step
 * when a transfer finishes.
 */
public class DefaultNetworkModel extends NetworkModel {

	/**
	 * Creates the network model for a simulation.
	 *
	 * @param simulationManager the simulation manager.
	 */
	public DefaultNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Dispatches network events to the matching transfer method.
	 *
	 * @param ev the event to process.
	 */
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

	/**
	 * Builds the path from one node to another and starts a transfer over its first
	 * link. Direct device-to-device and device-to-edge-data-center paths are
	 * assembled explicitly; other pairs use the shortest path stored in the
	 * topology.
	 *
	 * @param from     the source node.
	 * @param to       the destination node.
	 * @param task     the task concerned by the transfer.
	 * @param fileSize the file size, in bits.
	 * @param type     the kind of data being transferred.
	 */
	public void send(ComputingNode from, ComputingNode to, Task task, double fileSize, TransferProgress.Type type) {
		List<ComputingNode> vertexList = new ArrayList<>(5);
		List<NetworkLink> edgeList = new ArrayList<>(5);

		// If both are edge devices (one hop far from each other), send directly.
		if (from.getType() == TYPES.EDGE_DEVICE && to.getType() == TYPES.EDGE_DEVICE) {
			from.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE).setDst(to);
			vertexList.addAll(List.of(from, to));
			edgeList.add(from.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));

		} // Otherwise, if the first is a mobile edge device
		else if (from.getType() == TYPES.EDGE_DEVICE && to.getType() == TYPES.EDGE_DATACENTER) { 
			long id = simulationManager.getDataCentersManager().getTopology()
					.getUniqueId(from.getCurrentLink(LinkOrientation.UP_LINK).getDst().getId(), to.getId());
			GraphPath<ComputingNode, NetworkLink> path = simulationManager.getDataCentersManager().getTopology()
					.getPathsMap().get(id);
			vertexList.add(from);
			vertexList.addAll(path.getVertexList());
			edgeList.add(from.getCurrentLink(LinkOrientation.UP_LINK));
			edgeList.addAll(path.getEdgeList());

		} // Else, if the second is a mobile edge device
		else if (from.getType() == TYPES.EDGE_DATACENTER && to.getType() == TYPES.EDGE_DEVICE) {
			long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(),
					to.getCurrentLink(LinkOrientation.DOWN_LINK).getSrc().getId());
			GraphPath<ComputingNode, NetworkLink> path = simulationManager.getDataCentersManager().getTopology()
					.getPathsMap().get(id);
			vertexList.addAll(path.getVertexList());
			vertexList.add(from);
			edgeList.addAll(path.getEdgeList());
			edgeList.add(to.getCurrentLink(LinkOrientation.DOWN_LINK)); 
			
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

	/**
	 * Sends an offloading request from the orchestrator to the destination, or
	 * starts execution locally when the destination is the orchestrator or the
	 * device itself.
	 *
	 * @param task the task to send.
	 */
	public void sendRequestFromOrchToDest(Task task) {
		if (task.getOrchestrator() != task.getOffloadingDestination()
				&& task.getOffloadingDestination() != task.getEdgeDevice())

			send(task.getOrchestrator(), task.getOffloadingDestination(), task, task.getFileSizeInBits(),
					TransferProgress.Type.TASK);
		else // The device will execute the task locally
			executeTaskOrDownloadContainer(
					new TransferProgress(task, task.getFileSizeInBits(), TransferProgress.Type.TASK));
	}

	/**
	 * Sends execution results from the orchestrator to the originating device, or
	 * finishes the task immediately when the orchestrator is that device.
	 *
	 * @param task the completed task.
	 */
	public void sendResultFromOrchToDev(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice())
			send(task.getOrchestrator(), task.getEdgeDevice(), task, task.getOutputSizeInBits(),
					TransferProgress.Type.RESULTS_TO_DEV);
		else
			scheduleNow(simulationManager, DefaultSimulationManager.RESULT_RETURN_FINISHED, task);

	}

	/**
	 * Sends execution results from the offloading destination to the orchestrator,
	 * or forwards them directly to the device when the orchestrator is the
	 * destination.
	 *
	 * @param task the completed task.
	 */
	public void sendResultFromDevToOrch(Task task) {
		if (task.getOffloadingDestination() != task.getOrchestrator())
			send(task.getOffloadingDestination(), task.getOrchestrator(), task, task.getOutputSizeInBits(),
					TransferProgress.Type.RESULTS_TO_ORCH);
		else
			scheduleNow(this, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, task);

	}

	/**
	 * Transfers the application container from the registry to the offloading
	 * destination, or executes the task directly when the destination is the
	 * registry.
	 *
	 * @param task the task whose container must be available.
	 */
	public void addContainer(Task task) {
		if (task.getRegistry() != task.getOffloadingDestination())
			send(task.getRegistry(), task.getOffloadingDestination(), task, task.getContainerSizeInBits(),
					TransferProgress.Type.CONTAINER);
		else
			scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, task);
	}

	/**
	 * Sends an offloading request from the device to its orchestrator, or straight
	 * to the destination when the device orchestrates its own tasks.
	 *
	 * @param task the task to offload.
	 */
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

	/**
	 * Continues the workflow after a transfer reaches its destination, based on
	 * the transfer type: forward a request, execute or download a container,
	 * return results, or mark the results as returned.
	 *
	 * @param transfer the finished transfer.
	 */
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

	/**
	 * Charges the wireless radio energy of the edge devices involved in a transfer,
	 * if either endpoint is an edge device.
	 *
	 * @param transfer    the finished transfer.
	 * @param origin      the sender.
	 * @param destination the receiver.
	 */
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

	/**
	 * Called when the container transfer finishes: asks the simulation manager to
	 * execute the task.
	 *
	 * @param transfer the finished container transfer.
	 */
	protected void containerDownloadFinished(TransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, transfer.getTask());
	}

	/**
	 * Marks the task as completed because its results reached the device.
	 *
	 * @param transfer the finished results transfer.
	 */
	protected void resultsReturnedToDevice(TransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.RESULT_RETURN_FINISHED, transfer.getTask());
	}

	/**
	 * Starts the transfer of the results from the orchestrator to the device.
	 *
	 * @param transfer the finished results-to-orchestrator transfer.
	 */
	protected void returnResultsToDevice(TransferProgress transfer) {
		scheduleNow(this, NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, transfer.getTask());
	}

	/**
	 * Decides whether a task can start executing immediately or must first download
	 * its container. The container is downloaded only when the registry is enabled,
	 * the registry mode is {@code CLOUD}, and the destination is not in the cloud.
	 *
	 * @param transfer the finished request transfer.
	 */
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

	/**
	 * Called when the orchestrator receives an offloading request: forwards the
	 * task to the selected offloading destination.
	 *
	 * @param transfer the finished request transfer.
	 */
	protected void offloadingRequestRecievedByOrchestrator(TransferProgress transfer) {
		// Find the offloading destination and execute the task
		scheduleNow(simulationManager, DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
	}

	
}
