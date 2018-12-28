package com.Mechalikh.PureEdgeSim.DataCentersManager;

import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;

import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task.Status;

public class TasksSchedulerTimeShared extends CloudletSchedulerTimeShared {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public TasksSchedulerTimeShared() {
		// TODO Auto-generated constructor stub
	} 
		@Override
		public void cloudletFinish(final CloudletExecution ce) {
			Task task = ((Task) ce.getCloudlet());
			EdgeDataCenter edc = (EdgeDataCenter) task.getVm().getHost().getDatacenter();
			
			if (edc.isDead() || task.getEdgeDevice().isDead()) { 
				//the destination (where the task is executed) 
				//or the origin of the task(the device which offloaded the task)
	           // if one of them is dead
				task.setFailureReason(Status.FAILED_BECAUSE_DEVICE_DEAD);
				ce.setCloudletStatus(Cloudlet.Status.FAILED);
			} 
		 
			
			else
			// a simple representation of task failure due to device mobility, if there is
			// no vm migration
			// if vm location doesn't equal the edge device location (that generated this
			// task)
			if (edc.getType()!=SimulationParameters.TYPES.CLOUD && !edc.getLocation().equals(task.getEdgeDevice().getLocation())) {
				task.setFailureReason(Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY);
				ce.setCloudletStatus(Cloudlet.Status.FAILED);
			} else {
			  ce.setCloudletStatus(Cloudlet.Status.SUCCESS);
			  task.getEdgeDevice().addConsumption(task.getOutputSize()*SimulationParameters.POWER_CONS_PER_MEGABYTE);
			  edc.addConsumption(task.getOutputSize()*SimulationParameters.POWER_CONS_PER_MEGABYTE);
			}
			ce.finalizeCloudlet();
			addCloudletToFinishedList(ce);
		}



	}
 
