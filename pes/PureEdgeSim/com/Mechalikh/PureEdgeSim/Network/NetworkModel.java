package com.Mechalikh.PureEdgeSim.Network;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters.TYPES;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimulationManager;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class NetworkModel extends CloudSimEntity {
	private List<taskTransferProgress> taskProgressList;
	private SimulationManager sm;
	public static final int SEND_REQUEST_FROM_ORCH_TO_DESTINATION = 0;
	private static final int UPDATE_PROGRESS = 1;
	public static final int ADD_RESULT_TO_ORCH = 2;
	public static final int ADD_CONTAINER = 3;
	public static final int SEND_REQUEST_FROM_DEVICE_TO_ORCH = 4;
	public static final int ADD_RESULT_TO_DEV = 5;
	int index = 0;

	public NetworkModel(Simulation simulation, SimulationManager simulationManager) {
		super(simulation);
		this.sm = simulationManager;
		taskProgressList = new ArrayList<taskTransferProgress>(); // list where the tasks being transferred are stored
	}

	public List<taskTransferProgress> getTaskProgressList() {
		return taskProgressList;
	}

	public void sendRequestFromOrchToDest(Task task) {
		// add the new task to the list of the tasks being transferred (uploaded)
		taskProgressList.add(new taskTransferProgress(task, task.getFileSize() * 8, taskTransferProgress.TASK)); // request
																													// size
																													// in
																													// kilobits
	}

	public void sendResultFromOrchToDev(Task task) {
		// sen the resutls from the orchestrator to the device that offloaded the task
		// add the new task to the list of the files being transferred (downloaded)
		taskProgressList
				.add(new taskTransferProgress(task, task.getOutputSize() * 8, taskTransferProgress.RESULTS_TO_DEV)); // results
		// size
		// in
		// kilobits
	}

	public void sendResultFromDevToOrch(Task task) {
		// send the results from the device that executed the task to the orchestrator
		// add the new task to the list of the files being transferred (downloaded)
		taskProgressList
				.add(new taskTransferProgress(task, task.getOutputSize() * 8, taskTransferProgress.RESULTS_TO_ORCH)); // results
		// size
		// in
		// kilobits
	}

	public void addContainer(Task task) {
		// add the new container to the list of files being transferred (downloaded)
		taskProgressList
				.add(new taskTransferProgress(task, task.getContainerSize() * 8, taskTransferProgress.CONTAINER)); // results
																													// size
																													// in
																													// kilobits
	}

	public void sendRequestFromDeviceToOrch(Task task) {
		// add the new container to the list of files being transferred (downloaded)
		taskProgressList.add(new taskTransferProgress(task, task.getFileSize() * 8, taskTransferProgress.REQUEST)); // results
																													// size
																													// in
																													// kilobits
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case SEND_REQUEST_FROM_ORCH_TO_DESTINATION:
			// add the task to the list of tasks being transferred
			sendRequestFromOrchToDest((Task) ev.getData());
			break;
		case ADD_RESULT_TO_ORCH:
			// add the execution results transferred to the orchestrator to the list of the
			// results being transferred
			sendResultFromDevToOrch((Task) ev.getData());
			break;
		case ADD_RESULT_TO_DEV:
			// add the execution results returned to the edge device to the list of the
			// results being transferred
			sendResultFromOrchToDev((Task) ev.getData());
			break;
		case ADD_CONTAINER:
			// add the execution results to the list of the results being transferred
			addContainer((Task) ev.getData());
			break;
		case SEND_REQUEST_FROM_DEVICE_TO_ORCH:
			// add the execution results to the list of the results being transferred
			sendRequestFromDeviceToOrch((Task) ev.getData());
			break;
		case UPDATE_PROGRESS:
			// update the tasks transfer progress and the allocated bandwidth
			updateTasksProgress();
			schedule(this, SimulationParameters.NETWORK_UPDATE_INTERVAL, UPDATE_PROGRESS);
			break;
		}
	}

	private void updateTasksProgress() {
		// if(index < taskProgressList.size() &&
		// taskProgressList.get(index).getRemainingFileSize()==0) index++; //ignore
		// finished transfers, so we will start looping from the first index of the
		// remaining transfers

		for (int i = index; i < taskProgressList.size(); i++) {
			int remainingTasksCount_Lan = 0;
			int remainingTasksCount_Wan = 0;
			if (taskProgressList.get(i).getRemainingFileSize() > 0) {
				for (int j = index; j < taskProgressList.size(); j++) {
					if (taskProgressList.get(j).getRemainingFileSize() > 0) { 
						if ((taskProgressList.get(j).getType() == taskTransferProgress.TASK
								// if the offloading destination is the cloud
								&& ((EdgeVM) taskProgressList.get(j).getTask().getVm()).getType().equals(TYPES.CLOUD)) 
								// or if containers will be downloaded from registry
								|| taskProgressList.get(j).getType() == taskTransferProgress.CONTAINER 
								//or if the orchestrator is deployed in the cloud
								|| (taskProgressList.get(j).getTask().getOrchestrator()
										.getType() == SimulationParameters.TYPES.CLOUD)) {
							remainingTasksCount_Wan++; // in all these cases the WAN is used
							remainingTasksCount_Lan++;// using the WAN includes using the LAN
						} else if (sameLocation(taskProgressList.get(i).getTask().getEdgeDevice(),
								taskProgressList.get(j).getTask().getEdgeDevice())) {
							// both tasks are generated from same location, which means they share same bandwidth
							//if they are connected to the same access point
							remainingTasksCount_Lan++;
						}

					}
				}

				// since the lan upload and download are not separated this means that tasks and
				// results use the same bandwidth
				// therefore we need to count the results being transferred before updating the
				// tasks allocated bandwidth

				// allocate bandwidths
				taskProgressList.get(i).setLanBandwidth(getLanBandwidth(remainingTasksCount_Lan));
				taskProgressList.get(i).setWanBandwidth(getWanBandwidth(remainingTasksCount_Wan));
				update(taskProgressList.get(i));
			}

		}
	}

	public void update(taskTransferProgress progress) {
		double bandwidth = 0;
		if ((progress.getType() == taskTransferProgress.TASK
				&& ((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD))
				|| progress.getType() == taskTransferProgress.CONTAINER
				|| progress.getTask().getOrchestrator().getType() == SimulationParameters.TYPES.CLOUD) {
			// the bandwidth will be limited by the minimum value
			// if the lan bandwidth is 1 mbps and the wan bandwidth is 4 mbps
			// it will be limited by the lan, so we will choose the minimum
			bandwidth = Math.min(progress.getLanBandwidth(), progress.getWanBandwidth());
		} else
			bandwidth = progress.getLanBandwidth(); // no wan usage
		double oldRemainingSize = progress.getRemainingFileSize();
		
		// update progress (remaining file size) 
		progress.setRemainingFileSize(progress.getRemainingFileSize()
				-(SimulationParameters.NETWORK_UPDATE_INTERVAL * bandwidth)); 
		
		if (progress.getRemainingFileSize() < 0) {// task upload finished
			progress.setRemainingFileSize(0);

			
		}
		// update  lan  network  usage delay
		progress.setLanNetworkUsage(
				progress.getLanNetworkUsage() + (oldRemainingSize - progress.getRemainingFileSize()) / bandwidth); 
		if ((progress.getType() == taskTransferProgress.TASK
				&& ((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD))
				|| progress.getType() == taskTransferProgress.CONTAINER
				|| progress.getTask().getOrchestrator().getType() == SimulationParameters.TYPES.CLOUD) {
			progress.setWanNetworkUsage(
					progress.getWanNetworkUsage() + (oldRemainingSize - progress.getRemainingFileSize()) / bandwidth); // update
																														// wan
																														// network
																														// usage
																														// delay
		}
		if (progress.getRemainingFileSize() == 0) {
			if (progress.getType() == taskTransferProgress.REQUEST) { // it is an offlaoding request (and not a result)

				if (progress.getTask().getOrchestrator().getType().equals(TYPES.CLOUD))
					schedule(sm, SimulationParameters.WAN_PROPAGATION_DELAY,
							SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, progress.getTask());
				else
					scheduleNow(sm, SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, progress.getTask());
			} else if (progress.getType() == taskTransferProgress.TASK) { // it is an offlaoding request (and not a
																			// result)
				if (SimulationParameters.ENABLE_REGISTRY
						&& !((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD)) {
					// if the registry is enabled and the task is offloaded to the fog or the edge
					// ,then download the container after receiving the request
					scheduleNow(this, NetworkModel.ADD_CONTAINER, progress.getTask()); // begin downloading the
																						// container

				} else {// if the registry is disabled, execute directly the request, as it represents
						// the offloaded task in this case

					if (((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD))
						schedule(sm, SimulationParameters.WAN_PROPAGATION_DELAY, SimulationManager.EXECUTE_TASK,
								progress.getTask());
					else
						scheduleNow(sm, SimulationManager.EXECUTE_TASK, progress.getTask());
				}
			} else if (progress.getType() == taskTransferProgress.CONTAINER) { // the container has been
																				// downloaded,execute the task now

				scheduleNow(sm, SimulationManager.EXECUTE_TASK, progress.getTask());

			} else if (progress.getType() == taskTransferProgress.RESULTS_TO_ORCH) {// if the transfer of results to the
																					// orchestrator finished
				if (progress.getTask().getOrchestrator().getType().equals(TYPES.CLOUD)
						|| ((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD))
					schedule(this, SimulationParameters.WAN_PROPAGATION_DELAY, NetworkModel.ADD_RESULT_TO_DEV,
							progress.getTask());
				else
					scheduleNow(this, NetworkModel.ADD_RESULT_TO_DEV, progress.getTask());
			} else {// return results to the edge device
				if (progress.getTask().getOrchestrator().getType().equals(TYPES.CLOUD)
						|| ((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD))
					schedule(sm, SimulationParameters.WAN_PROPAGATION_DELAY, SimulationManager.RESULT_RETURN_FINISHED,
							progress.getTask());
				else
					scheduleNow(sm, SimulationManager.RESULT_RETURN_FINISHED, progress.getTask());
			}
		}

	}

	private double getLanBandwidth(double remainingTasksCount_Lan) {
		if (SimulationParameters.NETWORK_HOTSPOTS)
			return (SimulationParameters.BANDWIDTH_WLAN / (remainingTasksCount_Lan)); // if edge many devices use same
																						// wlan
		else
			return SimulationParameters.BANDWIDTH_WLAN; // if peer to peer
	}

	private double getWanBandwidth(double remainingTasksCount_Wan) {
		return (SimulationParameters.WAN_BANDWIDTH / (remainingTasksCount_Wan));
	}

	@Override
	protected void startEntity() {
		schedule(this, 1, UPDATE_PROGRESS);
	}

	private boolean sameLocation(EdgeDataCenter Dev1, EdgeDataCenter Dev2) {
		double distance = Math.abs(Math.sqrt(Math.pow((Dev1.getLocation().getXPos() - Dev2.getLocation().getXPos()), 2)
				+ Math.pow((Dev1.getLocation().getYPos() - Dev2.getLocation().getYPos()), 2)));
		int RANGE = SimulationParameters.EDGE_RANGE;
		if (Dev1.getType() != Dev2.getType())// one of them is fog and the other is edge
			RANGE = SimulationParameters.FOG_RANGE;
		if (distance < RANGE)
			return true;
		return false;
	}

	@Override
	public void shutdownEntity() {
	}
}
