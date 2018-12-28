package com.Mechalikh.PureEdgeSim.Network;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters.TYPES;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimulationManager;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class NetworkModel extends CloudSimEntity {
	private List<taskTransferProgress> taskProgressList; 
	private SimulationManager sm;
	public static final int ADD_TASK = 0;
	private static final int UPDATE_PROGRESS = 1;
	public static final int ADD_RESULT = 2; 
	public NetworkModel(Simulation simulation, SimulationManager simulationManager) {
		super(simulation);
		this.sm = simulationManager;
		taskProgressList = new ArrayList<taskTransferProgress>(); // list where the tasks being transferred are stored 
	}

	public List<taskTransferProgress> getTaskProgressList() {
		return taskProgressList;
	}

	public void addTask(Task task) {
		// add the new task to the list of the tasks being transferred
		taskProgressList.add(new taskTransferProgress(task, task.getFileSize() * 8, true)); // file size in kilobits
	}

	public void addResult(Task task) {
		// add the new task to the list of the results being transferred
		taskProgressList.add(new taskTransferProgress(task, task.getOutputSize() * 8, false)); // file size in kilobits
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case ADD_TASK:    
			Task t = (Task) ev.getData();
			// add the task to the list of tasks being transferred
			addTask(t);
			break;
		case ADD_RESULT:   
			Task task = (Task) ev.getData();
			// add the execution results to the list of the results being transferred
			addResult(task);
			break;
		case UPDATE_PROGRESS:
			// update the tasks transfer progress and the allocated bandwidth 
			updateTasksProgress(); 
			schedule(this, SimulationParameters.INTERVAL_TO_SEND_EVENT, UPDATE_PROGRESS);
			break;
		}
	}

	private void updateTasksProgress() {
		for (int i = 0; i < taskProgressList.size(); i++) {
			int remainingTasksCount_Lan = 0;
			int remainingTasksCount_Wan = 0;
			if (taskProgressList.get(i).getRemainingFileSize() > 0) {
				for (int j = 0; j < taskProgressList.size(); j++) {
					if (taskProgressList.get(j).getRemainingFileSize() > 0
							&& taskProgressList.get(i).getTask().getEdgeDevice().getLocation()
									.equals(taskProgressList.get(j).getTask().getEdgeDevice().getLocation())) {
						// both tasks are generated from same location, which means they share same
						// bandwidth
						remainingTasksCount_Lan++;
						if (((EdgeVM) taskProgressList.get(j).getTask().getVm()).getType().equals(TYPES.CLOUD))
							remainingTasksCount_Wan++;
					}

				}

				// since the lan upload and download are not separated this means that tasks and
				// results use the same bandwidth
				// therefore we need to count the results being transferred before updating the
				// tasks allocated bandwidth 
				
				// allocate bandwidths
				taskProgressList.get(i)
						.setLanBandwidth(getLanBandwidth(remainingTasksCount_Lan));
				taskProgressList.get(i).setWanBandwidth(getWanBandwidth(remainingTasksCount_Wan));
				update(taskProgressList.get(i));
			}

		}
	}

	 
	 

	 

	public void update(taskTransferProgress progress) {
		double bandwidth = 0;
		if (((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD)) {
			// the bandwidth will be limited by the minimum value
			// if the lan bandwidth is 1 mbps and the wan bandwidth is 4 mbps
			// it will be limited by the lan, so we will choose the minimum
			bandwidth = Math.min(progress.getLanBandwidth(), progress.getWanBandwidth());
		} else
			bandwidth = progress.getLanBandwidth();

		double oldRemainingSize = progress.getRemainingFileSize();
		progress.setRemainingFileSize(progress.getRemainingFileSize() - (bandwidth * (double) 60)); // update progress
																									// (remaining file
																									// size)

		if (progress.getRemainingFileSize() < 0) {// task upload finished
			progress.setRemainingFileSize(0);

		}
		progress.setLanNetworkUsage(
				progress.getLanNetworkUsage() + (oldRemainingSize - progress.getRemainingFileSize()) / bandwidth); // update
																													// lan
																													// network
																													// usage
																													// delay
		if (((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD)) {
			progress.setWanNetworkUsage(
					progress.getWanNetworkUsage() + (oldRemainingSize - progress.getRemainingFileSize()) / bandwidth); // update
																														// wan
																														// network
																														// usage
																														// delay
		}
		if (progress.getRemainingFileSize() == 0) {
			if (progress.isExecutable()) { // it is a task (and not a result)
				
				if (((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD))
					schedule(sm, SimulationParameters.WAN_PROPAGATION_DELAY, SimulationManager.EXECUTE_TASK,
							progress.getTask());
				else
					scheduleNow(sm, SimulationManager.EXECUTE_TASK, progress.getTask());
			} else {// if the results transfer finished   
				if (((EdgeVM) progress.getTask().getVm()).getType().equals(TYPES.CLOUD))
					schedule(sm, SimulationParameters.WAN_PROPAGATION_DELAY, SimulationManager.RESULT_RETURN_FINISHED,
							progress.getTask());
				else
					scheduleNow(sm, SimulationManager.RESULT_RETURN_FINISHED, progress.getTask());
			}
		}

	}

	private double getLanBandwidth(double remainingTasksCount_Lan) {
		return (SimulationParameters.BANDWIDTH_WLAN / (remainingTasksCount_Lan));
	}

	private double getWanBandwidth(double remainingTasksCount_Wan) {
		return (SimulationParameters.WAN_BANDWIDTH / (remainingTasksCount_Wan));
	}
 

	@Override
	protected void startEntity() {
		schedule(this, 1, UPDATE_PROGRESS);
	}

	 

	@Override
	public void shutdownEntity() {  
	}
}
