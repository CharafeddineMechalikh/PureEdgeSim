package com.Mechalikh.PureEdgeSim.DataCentersManager;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;

import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task.Status;

public class TasksSchedulerSpaceShared extends CloudletSchedulerSpaceShared {

	private static final long serialVersionUID = 1L;

	public TasksSchedulerSpaceShared() {
	}

	@Override
	public void cloudletFinish(final CloudletExecution ce) {
		Task task = ((Task) ce.getCloudlet());
		EdgeDataCenter edc = (EdgeDataCenter) task.getVm().getHost().getDatacenter();

		// Task failed due to long delay
		if ((task.getSimulation().clock() - task.getTime()) > task.getMaxLatency()) {
			task.setFailureReason(Task.Status.FAILED_DUE_TO_LATENCY);
			task.setStatus(Cloudlet.Status.FAILED);
		} else
		// Task failed because one of the devices died
		if (edc.isDead() || task.getEdgeDevice().isDead()) {
			// The destination (where the task is executed)
			// or the origin of the task(the device which offloaded the task)
			// if one of them is dead
			task.setFailureReason(Status.FAILED_BECAUSE_DEVICE_DEAD);
			ce.setCloudletStatus(Cloudlet.Status.FAILED);
		} else
		// A simple representation of task failure due to
		// device mobility, if the vm location doesn't match
		// the edge device location (that generated this task)
		if (edc.getType() != SimulationParameters.TYPES.CLOUD && !sameLocation(edc, task.getEdgeDevice())) {
			task.setFailureReason(Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY);
			ce.setCloudletStatus(Cloudlet.Status.FAILED);
		} else {
			ce.setCloudletStatus(Cloudlet.Status.SUCCESS);
		}
		ce.finalizeCloudlet();
		addCloudletToFinishedList(ce);
	}

	private boolean sameLocation(EdgeDataCenter Dev1, EdgeDataCenter Dev2) {
		double distance = Math.abs(Math.sqrt(Math.pow((Dev1.getLocation().getXPos() - Dev2.getLocation().getXPos()), 2)
				+ Math.pow((Dev1.getLocation().getYPos() - Dev2.getLocation().getYPos()), 2)));
		int RANGE = SimulationParameters.EDGE_RANGE;
		if (Dev1.getType() != Dev2.getType()) // One of them is fog and the other is edge
			RANGE = SimulationParameters.FOG_RANGE;
		if (distance < RANGE)
			return true;
		return false;
	}

}
