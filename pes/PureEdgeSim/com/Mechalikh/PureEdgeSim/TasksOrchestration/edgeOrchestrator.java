package com.Mechalikh.PureEdgeSim.TasksOrchestration;

import java.io.BufferedReader; 
import java.io.FileOutputStream;
import java.io.FileReader; 
import java.util.ArrayList;
import java.util.List;
import java.util.Random; 

import org.cloudbus.cloudsim.brokers.DatacenterBroker; 
import org.cloudbus.cloudsim.vms.Vm;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.LocationManager.Location;
import com.Mechalikh.PureEdgeSim.ScenarioManager.Scenario;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters; 
import com.Mechalikh.PureEdgeSim.SimulationManager.SimLog;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class edgeOrchestrator extends Orchestrator {
	private List<List<Integer>> history; 
	private SimLog simLog;

	public edgeOrchestrator(DatacenterBroker broker, List<EdgeVM> list, SimLog simLog, Scenario scenario) {
		super(broker, list, scenario);
		history = new ArrayList<List<Integer>>();
		initHistoryList(vmList.size());
		this.simLog = simLog;
	}

	private void initHistoryList(int size) {

		for (int i = 0; i < size; i++) {
			// every i value represents a VM
			// creating a list to store Tasks history for this vm and add it to the history
			// list
			history.add(new ArrayList<Integer>());
		}
	}

	@Override
	public void initialize(Task task) {
		if (scenario.getStringOrchPolicy().equals("CLOUD_ONLY")) {
			cloudOnly(task);
		} else if (scenario.getStringOrchPolicy().equals("EDGE_ONLY")) {
			edgeOnly(task);
		} else if (scenario.getStringOrchPolicy().equals("FOG_AND_CLOUD")) {
			fogAndCloud(task);
		} else if (scenario.getStringOrchPolicy().equals("ALL")) {
			all(task);
		} else if (scenario.getStringOrchPolicy().equals("FOG_ONLY")) {
			fogOnly(task);
		} else if (scenario.getStringOrchPolicy().equals("EDGE_AND_CLOUD")) {
			edgeAndCloud(task);
		}
    if(task.getVm()!= Vm.NULL) //send only if resources are available (i.e. the offloading distination is available) 
		sendTask(task);// final step : send the task to execute it
	}

	// if the orchestration scenario is EDGE_ONLY send Tasks only to
	// edge virtual machines (vms)
	private void edgeOnly(Task task) {
		String[] policy = { "Edge" };
		findVM(policy, task);

	}

	// if the orchestration scenario is ClOUD_ONLY send Tasks (cloudlets) only to
	// cloud virtual machines (vms)
	private void cloudOnly(Task task) {
		String[] policy = { "Cloud" };
		findVM(policy, task);

	}

	// if the orchestration scenario is FOG_AND_CLOUD send Tasks only to
	// fog or cloud virtual machines (vms)
	private void fogAndCloud(Task task) {
		String[] policy = { "Cloud", "Fog" };
		findVM(policy, task);

	}

	// if the orchestration scenario is FOG_AND_CLOUD send Tasks only to
	// fog or cloud virtual machines (vms)
	private void edgeAndCloud(Task task) {
		String[] policy = { "Cloud", "Edge" };
		findVM(policy, task);

	}

	// if the orchestration scenario is FOG_AND_CLOUD send Tasks only to
	// fog or cloud virtual machines (vms)
	private void fogOnly(Task task) {
		String[] policy = { "Fog" };
		findVM(policy, task);

	}

	// if the orchestration scenario is ALL send Tasks (cloudlets) any virtual
	// machine (vm)
	private void all(Task task) {
		String[] policy = { "Cloud", "Fog", "Edge" };
		findVM(policy, task);

	}

	public List<List<Integer>> getHistory() {
		return history;
	}

	public void findVM(String[] policy, Task task) {
		if (scenario.getStringOrchCriteria().equals("RANDOM")) {
			int vm = -1;
			// get best vm for this task
			int j = 1 + new Random().nextInt(history.size() - 1);
			while (j > 0) {
				for (int i = 0; i < history.size(); i++) {
					if ((ArrayContains(policy, "Cloud") && vmList.get(i).getType() == SimulationParameters.TYPES.CLOUD) // cloud
							|| (ArrayContains(policy, "Fog")
									&& vmList.get(i).getType() == SimulationParameters.TYPES.FOG // fog
									// compare destination (fog host) location and origin (edge) location, if they
									// are in same area offload to his device
									&& sameLocation(((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).getLocation() ,task
											.getEdgeDevice().getLocation(),SimulationParameters.FOG_RANGE))
							|| (ArrayContains(policy, "Edge")
									&& vmList.get(i).getType() == SimulationParameters.TYPES.EDGE // edge
									// compare destination (edge device) location and origin (edge) location, if
									// they are in same area offload to his device
									&& sameLocation(((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).getLocation(), task
											.getEdgeDevice().getLocation(),SimulationParameters.EDGE_RANGE)
									&& ((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).isDead() == false)) {
						j--;
						// affect the tasks to the vm found
						if (j == 0) {
							vm = i;
						}
					}
				}

			}
			affectTaskToVm(vm, task);
		}
	 else if (scenario.getStringOrchCriteria().equals("ROUND_ROBIN")) {
			int vm = -1;
			int minTasksCount = -1; // vm with minimum affected tasks;
			// get best vm for this task
			for (int i = 0; i < history.size(); i++) {
				if ((ArrayContains(policy, "Cloud") && vmList.get(i).getType() == SimulationParameters.TYPES.CLOUD) // cloud
						|| (ArrayContains(policy, "Fog") && vmList.get(i).getType() == SimulationParameters.TYPES.FOG // fog
						// compare destination (fog host) location and origin (edge) location, if they
						// are in same area offload to his device
								&& sameLocation(((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).getLocation(),task
										.getEdgeDevice().getLocation(),SimulationParameters.FOG_RANGE))
						|| (ArrayContains(policy, "Edge") && vmList.get(i).getType() == SimulationParameters.TYPES.EDGE // edge
						// compare destination (edge device) location and origin (edge) location, if
						// they are in same area offload to his device
								&& sameLocation(((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).getLocation(), task
										.getEdgeDevice().getLocation(),SimulationParameters.EDGE_RANGE)
								&& ((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).isDead() == false)) {

					if (minTasksCount == -1) {
						minTasksCount = history.get(i).size(); // if this is the first time, set the first vm as the
																// best
																// one
						vm = i;
					} else if (minTasksCount > history.get(i).size()) {
						minTasksCount = history.get(i).size(); // new min found, so we choose it as the best VM
						vm = i;
						break;
					}
				}
			}
			// affect the tasks to the vm found
			affectTaskToVm(vm, task);
		}  else if (scenario.getStringOrchCriteria().equals("INCEREASE_LIFETIME")) {
			int vm = -1;
			double minTasksCount = -1; // vm with minimum affected tasks;
			double vmMips = 0;
			double weight = 0;
			double minWeight = 20;
			// get best vm for this task
			for (int i = 0; i < history.size(); i++) {
				if ((ArrayContains(policy, "Cloud") && vmList.get(i).getType() == SimulationParameters.TYPES.CLOUD) // cloud
						|| (ArrayContains(policy, "Fog") && vmList.get(i).getType() == SimulationParameters.TYPES.FOG // fog
						// compare destination (fog host) location and origin (edge) location, if they
						// are in same area offload to his device
								&& sameLocation(((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).getLocation() ,task
										.getEdgeDevice().getLocation(),SimulationParameters.FOG_RANGE))
						|| (ArrayContains(policy, "Edge") && vmList.get(i).getType() == SimulationParameters.TYPES.EDGE // edge
						// compare destination (edge device) location and origin (edge) location, if
						// they are in same area offload to his device
								&& sameLocation(((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).getLocation() , task
										.getEdgeDevice().getLocation(),SimulationParameters.EDGE_RANGE)
								&& ((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).isDead() == false)) {
					weight = 1;
					if (((EdgeDataCenter) vmList.get(i).getHost().getDatacenter()).isBattery()) {
						if (task.getEdgeDevice()
								.getBatteryLevel() > ((EdgeDataCenter) vmList.get(i).getHost().getDatacenter())
										.getBatteryLevel())
							weight = 20; // the destination device has lower remaining power than the task offloading
											// device,in this case it is better not to offload
											// that's why the weight is high (20)
						else
							weight = 15; // in this case the destination has higher remaining power, so it is okey to
											// offload tasks for it, if the cloud and the fog are absent.
					} else
						weight = 1; // if it is not battery powered

					if (minTasksCount == 0)
						minTasksCount = 1;// avoid devision by 0

					if (minTasksCount == -1) { // if it is the first iteration
						minTasksCount = history.get(i).size()
								- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1;
						// if this is the first time, set the first vm as the
						vm = i; // best one
						vmMips = vmList.get(i).getMips();
						minWeight = weight;
					} else if (vmMips / (minTasksCount * minWeight) < vmList.get(i).getMips() / ((history.get(i).size()
							- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1) * weight)) { // if
																														// this
																														// vm
																														// has
																														// more
																														// cpu
																														// mips
																														// and
																														// less
																														// waiting
																														// tasks

						minWeight = weight;
						vmMips = vmList.get(i).getMips();
						minTasksCount = history.get(i).size()
								- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1;
						vm = i;
					}
				}
			}
			// affect the tasks to the vm found
			affectTaskToVm(vm, task);
		}

	}

	private boolean sameLocation(Location location, Location location2, int RANGE) {
		double distance=Math.abs(Math.sqrt(Math.pow((location.getXPos()-location2.getXPos()),2)+ Math.pow((location.getYPos()-location2.getYPos()),2)));
		
		if(distance<RANGE)
			return true;
		return false;
	}

 

	private boolean ArrayContains(String[] policy, String value) {
		for (int i = 0; i < policy.length; i++) {
			if (policy[i].equals(value))
				return true;
		}
		return false;
	}

	private void affectTaskToVm(int vmIndex, Task task) {
		if (vmIndex==-1) {
			simLog.setFailedDueToResourcesUnavailablity(simLog.getTasksFailedRessourcesUnavailable()+1);
		}else {
		task.setVm(vmList.get(vmIndex)); // send this task to this vm
		simLog.deepLog(broker.getSimulation().clock() + " : EdgeOrchestrator, Task: " + task.getId() + " affected to "
				+ vmList.get(vmIndex).getType() + " vm: " + vmList.get(vmIndex).getId());

		// update history
		history.get(vmIndex).add(task.getId());
		}
	}

	@Override
	public void sendTask(Task task) {
		task.getEdgeDevice().getVmTaskMap().add(new VmTaskMapItem((EdgeVM) task.getVm(), task));

	}

	

	
}
