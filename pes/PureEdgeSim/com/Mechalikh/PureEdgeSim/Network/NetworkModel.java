package com.Mechalikh.PureEdgeSim.Network;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EnergyModel;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters.TYPES;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimulationManager;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class NetworkModel extends CloudSimEntity {
	public static final int base = 4000;
	public static final int SEND_REQUEST_FROM_ORCH_TO_DESTINATION = base + 1;
	private static final int UPDATE_PROGRESS = base + 2;
	public static final int DOWNLOAD_CONTAINER = base + 3;
	public static final int SEND_REQUEST_FROM_DEVICE_TO_ORCH = base + 4;
	public static final int SEND_RESULT_FROM_ORCH_TO_DEV = base + 5;
	public static final int SEND_UPDATE_FROM_DEVICE_TO_ORCH = base + 6;
	public static final int SEND_RESULT_TO_ORCH = base + 7;

	private List<FileTransferProgress> transferProgressList; // the list where the current (and the previous)
																// transferred file are stored
	private SimulationManager simulationManager;
	int firstIndex = 0;
	double bwUsage = 0;

	public NetworkModel(Simulation simulation, SimulationManager simulationManager) {
		super(simulation);
		this.simulationManager = simulationManager;
		transferProgressList = new ArrayList<FileTransferProgress>();
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
		}
	}

	public List<FileTransferProgress> getTransferProgressList() {
		return transferProgressList;
	}

	public void sendRequestFromOrchToDest(Task task) {
		transferProgressList.add(new FileTransferProgress(task, task.getFileSize() * 8, FileTransferProgress.TASK));
	}

	public void sendResultFromOrchToDev(Task task) {
		transferProgressList
				.add(new FileTransferProgress(task, task.getOutputSize() * 8, FileTransferProgress.RESULTS_TO_DEV));
	}

	public void sendResultFromDevToOrch(Task task) {
		if(task.getOrchestrator()!=task.getEdgeDevice())
		transferProgressList.add(new FileTransferProgress(task, task.getOutputSize() * 8, FileTransferProgress.RESULTS_TO_ORCH));
		else
			scheduleNow(this, NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, task);	
	}

	public void addContainer(Task task) {
		transferProgressList
				.add(new FileTransferProgress(task, task.getContainerSize() * 8, FileTransferProgress.CONTAINER));
	}

	public void sendRequestFromDeviceToOrch(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice()) 
			transferProgressList
					.add(new FileTransferProgress(task, task.getFileSize() * 8, FileTransferProgress.REQUEST));
		else // The device orchestrate its tasks by itself, so, send the request directly to destination 
			scheduleNow(simulationManager, SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task);
	}

	private void updateTasksProgress() {
<<<<<<< HEAD
		// Ignore finished transfers, so we will start looping from the first index of
		// the remaining transfers
		if (firstIndex < transferProgressList.size()
				&& transferProgressList.get(firstIndex).getRemainingFileSize() == 0)
			firstIndex++;
		for (int i = firstIndex; i < transferProgressList.size(); i++) {
			int remainingTransfersCount_Lan = 0;
			int remainingTransfersCount_Wan = 0;
			if (transferProgressList.get(i).getRemainingFileSize() > 0) {
				for (int j = i; j < transferProgressList.size(); j++) {
					if (transferProgressList.get(j).getRemainingFileSize() > 0 && j != i) {
						if (wanIsUsed(transferProgressList.get(j))) {
							remainingTransfersCount_Wan++;
							bwUsage += transferProgressList.get(j).getRemainingFileSize();
						}
						if (sameLanIsUsed(transferProgressList.get(i), transferProgressList.get(j))) {
							// Both transfers use same Lan
							remainingTransfersCount_Lan++;
						}
=======
		 if(index < taskProgressList.size() &&
		  taskProgressList.get(index).getRemainingFileSize()==0) index++; //ignore
		// finished transfers, so we will start looping from the first index of the
		// remaining transfers

		for (int i = index; i < taskProgressList.size(); i++) {
			int remainingTasksCount_Lan = 0;
			int remainingTasksCount_Wan = 0;
			if (taskProgressList.get(i).getRemainingFileSize() > 0) {
				for (int j = i; j < taskProgressList.size(); j++) {
					if (taskProgressList.get(j).getRemainingFileSize() > 0 && j!= i) { 
						if ((taskProgressList.get(j).getType() == taskTransferProgress.TASK
								// if the offloading destination is the cloud
								&& ((EdgeVM) taskProgressList.get(j).getTask().getVm()).getType().equals(TYPES.CLOUD)) 
								// or if containers will be downloaded from registry
								|| taskProgressList.get(j).getType() == taskTransferProgress.CONTAINER 
								//or if the orchestrator is deployed in the cloud
								|| (taskProgressList.get(j).getTask().getOrchestrator()
										.getType() == SimulationParameters.TYPES.CLOUD)) {
							remainingTasksCount_Wan++; // in all these cases the WAN is used 
						} 
						if ((sameLocation(taskProgressList.get(i).getTask().getEdgeDevice(),
								taskProgressList.get(j).getTask().getEdgeDevice())
								&& SimulationParameters.NETWORK_HOTSPOTS)
								
								//compare orchestrator
								|| ((taskProgressList.get(i).getTask().getOrchestrator() == taskProgressList.get(j).getTask().getOrchestrator()) && !SimulationParameters.NETWORK_HOTSPOTS)
								|| ((taskProgressList.get(i).getTask().getOrchestrator() == taskProgressList.get(j).getTask().getVm().getHost().getDatacenter()) && !SimulationParameters.NETWORK_HOTSPOTS)
								|| ((taskProgressList.get(i).getTask().getOrchestrator() == taskProgressList.get(j).getTask().getEdgeDevice()) && !SimulationParameters.NETWORK_HOTSPOTS)
				                
								//compare origin device
								|| ((taskProgressList.get(i).getTask().getEdgeDevice() == taskProgressList.get(j).getTask().getOrchestrator()) && !SimulationParameters.NETWORK_HOTSPOTS)
					         	|| ((taskProgressList.get(i).getTask().getEdgeDevice() == taskProgressList.get(j).getTask().getVm().getHost().getDatacenter()) && !SimulationParameters.NETWORK_HOTSPOTS)
					        	|| ((taskProgressList.get(i).getTask().getEdgeDevice() == taskProgressList.get(j).getTask().getEdgeDevice()) && !SimulationParameters.NETWORK_HOTSPOTS)
		                        
					        	// compare offloading destination
					        	|| ((taskProgressList.get(i).getTask().getVm().getHost().getDatacenter() == taskProgressList.get(j).getTask().getOrchestrator()) && !SimulationParameters.NETWORK_HOTSPOTS)
					         	|| ((taskProgressList.get(i).getTask().getVm().getHost().getDatacenter() == taskProgressList.get(j).getTask().getVm().getHost().getDatacenter()) && !SimulationParameters.NETWORK_HOTSPOTS)
					        	|| ((taskProgressList.get(i).getTask().getVm().getHost().getDatacenter() == taskProgressList.get(j).getTask().getEdgeDevice()) && !SimulationParameters.NETWORK_HOTSPOTS)) {
							// TODO Auto-generated method stub
							// both tasks are generated from same location, which means they share same bandwidth
							//if they are connected to the same access point
							remainingTasksCount_Lan++; 
						 	}
>>>>>>> 147e16d424d30d2a6ee0d9fc0eedace37a168b7d

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

	private void updateTransfer(FileTransferProgress transfer) {

		double oldRemainingSize = transfer.getRemainingFileSize();

		// Update progress (remaining file size)
		transfer.setRemainingFileSize(transfer.getRemainingFileSize()
				- (SimulationParameters.NETWORK_UPDATE_INTERVAL * transfer.getCurrentBandwidth()));

		if (transfer.getRemainingFileSize() <= 0) {// Transfer finished
			transfer.setRemainingFileSize(0);
			transferFinished(transfer);
		}
		// Update LAN network usage delay
		transfer.setLanNetworkUsage(transfer.getLanNetworkUsage()
				+ (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth());

		// Update WAN network usage delay
		if (wanIsUsed(transfer))
			transfer.setWanNetworkUsage(transfer.getWanNetworkUsage()
					+ (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth());

	}

	private void UpdateEnergyConsumption(FileTransferProgress transfer, String type) {
		// update energy consumption
		EdgeDataCenter origin = null;
		EdgeDataCenter destination = null;
		if (type.equals("Orchestrator")) {
			origin = transfer.getTask().getEdgeDevice();
			destination = transfer.getTask().getOrchestrator();
		} else if (type.equals("Destination")) {
			origin = transfer.getTask().getOrchestrator();
			destination = ((EdgeDataCenter) transfer.getTask().getVm().getHost().getDatacenter());
		} else if (type.equals("Container")) {
			origin = simulationManager.getServersManager().getDatacenterList().get(0);//registry,  so set the first cloud datacenter as the origin
			destination = transfer.getTask().getEdgeDevice();
		} else if (type.equals("Result_Orchestrator")) {
			origin = ((EdgeDataCenter) transfer.getTask().getVm().getHost().getDatacenter());
			destination = transfer.getTask().getOrchestrator();
		} else if (type.equals("Result_Origin")) {
			origin = transfer.getTask().getOrchestrator();
			destination = transfer.getTask().getEdgeDevice();
		}
		if (origin != null) {
			origin.getEnergyModel().updatewirelessEnergyConsumption(transfer, origin, destination,
					EnergyModel.TRANSMISSION);
		}
		destination.getEnergyModel().updatewirelessEnergyConsumption(transfer, origin, destination,
				EnergyModel.RECEPTION);
	}

	private void transferFinished(FileTransferProgress transfer) {
		// If it is an offlaoding request that is sent to the orchestrator
		if (transfer.getTransferType() == FileTransferProgress.REQUEST) {
			offloadingRequestRecievedByOrchestrator(transfer);
			UpdateEnergyConsumption(transfer, "Orchestrator");
		}
		// If it is an task (or offloading request) that is sent to the destination
		else if (transfer.getTransferType() == FileTransferProgress.TASK) {
			executeTaskOrDownloadContainer(transfer);
			UpdateEnergyConsumption(transfer, "Destination");
		}
		// If the container has been downloaded, then execute the task now
		else if (transfer.getTransferType() == FileTransferProgress.CONTAINER) {
			containerDownloadFinished(transfer);
			UpdateEnergyConsumption(transfer, "Container");
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == FileTransferProgress.RESULTS_TO_ORCH) {
			returnResultToDevice(transfer);
			UpdateEnergyConsumption(transfer, "Result_Orchestrator");
		}
		// Results transferred to the device
		else {
			resultsReturnedToDevice(transfer);
			UpdateEnergyConsumption(transfer, "Result_Origin");
		}

	}

	private void containerDownloadFinished(FileTransferProgress transfer) {
		scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, transfer.getTask());
	}

	private void resultsReturnedToDevice(FileTransferProgress transfer) {
		scheduleNow(simulationManager, SimulationManager.RESULT_RETURN_FINISHED, transfer.getTask());
	}

	private void returnResultToDevice(FileTransferProgress transfer) {
		// if the results are returned from the cloud, consider the wan propagation
		// delay
		if (transfer.getTask().getOrchestrator().getType().equals(TYPES.CLOUD)
				|| ((EdgeVM) transfer.getTask().getVm()).getType().equals(TYPES.CLOUD))
			schedule(this, SimulationParameters.WAN_PROPAGATION_DELAY, NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV,
					transfer.getTask());
		else
			scheduleNow(this, NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, transfer.getTask());

	}

	private void executeTaskOrDownloadContainer(FileTransferProgress transfer) {
		if (SimulationParameters.ENABLE_REGISTRY
				&& !((EdgeVM) transfer.getTask().getVm()).getType().equals(TYPES.CLOUD)) {
			// if the registry is enabled and the task is offloaded to the fog or the edge,
			// then download the container
			scheduleNow(this, NetworkModel.DOWNLOAD_CONTAINER, transfer.getTask());

		} else {// if the registry is disabled, execute directly the request, as it represents
				// the offloaded task in this case
			if (((EdgeVM) transfer.getTask().getVm()).getType().equals(TYPES.CLOUD))
				schedule(simulationManager, SimulationParameters.WAN_PROPAGATION_DELAY, SimulationManager.EXECUTE_TASK,
						transfer.getTask());
			else
				scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, transfer.getTask());
		}
	}

	private void offloadingRequestRecievedByOrchestrator(FileTransferProgress transfer) {
		// Find the offloading destination and execute the task
		if (transfer.getTask().getOrchestrator().getType().equals(TYPES.CLOUD))
			schedule(simulationManager, SimulationParameters.WAN_PROPAGATION_DELAY,
					SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
		else
			scheduleNow(simulationManager, SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
	}

	private boolean sameLanIsUsed(FileTransferProgress transfer1, FileTransferProgress transfer2) {
		// The trasfers share same Lan of they have one device in common
		// Compare orchestrator
		if ((transfer1.getTask().getOrchestrator() == transfer2.getTask().getOrchestrator())
				|| (transfer1.getTask().getOrchestrator() == transfer2.getTask().getVm().getHost().getDatacenter())
				|| (transfer1.getTask().getOrchestrator() == transfer2.getTask().getEdgeDevice())

				// Compare origin device
				|| (transfer1.getTask().getEdgeDevice() == transfer2.getTask().getOrchestrator())
				|| (transfer1.getTask().getEdgeDevice() == transfer2.getTask().getVm().getHost().getDatacenter())
				|| (transfer1.getTask().getEdgeDevice() == transfer2.getTask().getEdgeDevice())

				// Compare offloading destination
				|| (transfer1.getTask().getVm().getHost().getDatacenter() == transfer2.getTask().getOrchestrator())
				|| (transfer1.getTask().getVm().getHost().getDatacenter() == transfer2.getTask().getVm().getHost()
						.getDatacenter())
				|| (transfer1.getTask().getVm().getHost().getDatacenter() == transfer2.getTask().getEdgeDevice()))
			return true;
		return false;
	}

	private boolean wanIsUsed(FileTransferProgress fileTransferProgress) {
		if ((fileTransferProgress.getTransferType() == FileTransferProgress.TASK
				&& ((EdgeVM) fileTransferProgress.getTask().getVm()).getType().equals(TYPES.CLOUD))
				// If the offloading destination is the cloud

				|| fileTransferProgress.getTransferType() == FileTransferProgress.CONTAINER
				// Or if containers will be downloaded from registry

				|| (fileTransferProgress.getTask().getOrchestrator().getType() == SimulationParameters.TYPES.CLOUD))
			// Or if the orchestrator is deployed in the cloud
			return true;

		return false;
	}

	public void updateBandwidth(FileTransferProgress transfer) {
		double bandwidth = 0;
		if (wanIsUsed(transfer)) {
			// The bandwidth will be limited by the minimum value
			// If the lan bandwidth is 1 mbps and the wan bandwidth is 4 mbps
			// It will be limited by the lan, so we will choose the minimum
			bandwidth = Math.min(transfer.getLanBandwidth(), transfer.getWanBandwidth());
		} else
			bandwidth = transfer.getLanBandwidth();
		transfer.setCurrentBandwidth(bandwidth);
	}

	private double getLanBandwidth(double remainingTasksCount_Lan) {
<<<<<<< HEAD
		if (remainingTasksCount_Lan == 0)
			remainingTasksCount_Lan = 1;
		return (SimulationParameters.BANDWIDTH_WLAN / (remainingTasksCount_Lan));
	}

	private double getWanBandwidth(double remainingTasksCount_Wan) {
		if (remainingTasksCount_Wan == 0)
			remainingTasksCount_Wan = 1;
=======
		//if (SimulationParameters.NETWORK_HOTSPOTS)
		if(remainingTasksCount_Lan==0 )remainingTasksCount_Lan=1; 
			return (SimulationParameters.BANDWIDTH_WLAN / (remainingTasksCount_Lan)); // if edge many devices use same
																						// wlan
	//	else
		//	return SimulationParameters.BANDWIDTH_WLAN; // if peer to peer
	}

	private double getWanBandwidth(double remainingTasksCount_Wan) {
		if(remainingTasksCount_Wan==0 )remainingTasksCount_Wan=1; 
>>>>>>> 147e16d424d30d2a6ee0d9fc0eedace37a168b7d
		return (SimulationParameters.WAN_BANDWIDTH / (remainingTasksCount_Wan));
	}

	@Override
	protected void startEntity() {
		schedule(this, 1, UPDATE_PROGRESS);
	}

	@Override
	public void shutdownEntity() {
	}

	public double getWanUtilization() {
		int wanTasks = 0;
		for (int j = 0; j < transferProgressList.size(); j++) {
			if (transferProgressList.get(j).getRemainingFileSize() > 0) {
				if (wanIsUsed(transferProgressList.get(j))) {
					wanTasks++;
					bwUsage += transferProgressList.get(j).getRemainingFileSize();
				}
			}
		}
		if (wanTasks != 0)
			bwUsage = bwUsage / wanTasks;
		else
			bwUsage = 0;
		bwUsage = bwUsage / 1000;
		double utilization = Math.min(bwUsage, 300);
		return utilization;
	}

}
