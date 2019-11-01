package com.mechalikh.pureedgesim.TasksOrchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.DataCentersManager.EdgeDataCenter;
import com.mechalikh.pureedgesim.DataCentersManager.EdgeVM;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;

public class EdgeOrchestrator extends Orchestrator {
	private List<List<Integer>> orchestrationHistory;

	public EdgeOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		this.setSimulationManager(simulationManager);
		orchestrationHistory = new ArrayList<List<Integer>>();
		initHistoryList(this.getSimulationManager().getServersManager().getVmList().size());
	}

	private void initHistoryList(int size) {
		for (int vm = 0; vm < size; vm++) {
			// Creating a list to store the orchestration history for each VM (virtual
			// machine)
			orchestrationHistory.add(new ArrayList<Integer>());
		}
	}

	@Override
	public void initialize(Task task) {
		if (this.getSimulationManager().getScenario().getStringOrchArchitecture().equals("CLOUD_ONLY")) {
			cloudOnly(task);
		} else if (this.getSimulationManager().getScenario().getStringOrchArchitecture().equals("EDGE_ONLY")) {
			edgeOnly(task);
		} else if (this.getSimulationManager().getScenario().getStringOrchArchitecture().equals("FOG_AND_CLOUD")) {
			fogAndCloud(task);
		} else if (this.getSimulationManager().getScenario().getStringOrchArchitecture().equals("ALL")) {
			all(task);
		} else if (this.getSimulationManager().getScenario().getStringOrchArchitecture().equals("FOG_ONLY")) {
			fogOnly(task);
		} else if (this.getSimulationManager().getScenario().getStringOrchArchitecture().equals("EDGE_AND_CLOUD")) {
			edgeAndCloud(task);
		}
		// Offload it only if resources are available (i.e. the offloading distination
		// is available)
		if (task.getVm() != Vm.NULL)
			sendTask(task);// Send the task to execute it
	}

	// If the orchestration scenario is EDGE_ONLY send Tasks only to
	// edge virtual machines (vms)
	private void edgeOnly(Task task) {
		String[] Architecture = { "Edge" };
		findVM(Architecture, task);

	}

	// If the orchestration scenario is ClOUD_ONLY send Tasks (cloudlets) only to
	// cloud virtual machines (vms)
	private void cloudOnly(Task task) {
		String[] Architecture = { "Cloud" };
		findVM(Architecture, task);
	}

	// If the orchestration scenario is FOG_AND_CLOUD send Tasks only to
	// fog or cloud virtual machines (vms)
	private void fogAndCloud(Task task) {
		String[] Architecture = { "Cloud", "Fog" };
		findVM(Architecture, task);
	}

	// If the orchestration scenario is FOG_AND_CLOUD send Tasks only to
	// fog or cloud virtual machines (vms)
	private void edgeAndCloud(Task task) {
		String[] Architecture = { "Cloud", "Edge" };
		findVM(Architecture, task);
	}

	// If the orchestration scenario is FOG_AND_CLOUD send Tasks only to
	// fog or cloud virtual machines (vms)
	private void fogOnly(Task task) {
		String[] Architecture = { "Fog" };
		findVM(Architecture, task);
	}

	// If the orchestration scenario is ALL send Tasks (cloudlets) any virtual
	// machine (vm)
	private void all(Task task) {
		String[] Architecture = { "Cloud", "Fog", "Edge" };
		findVM(Architecture, task);
	}

	public List<List<Integer>> getHistory() {
		return orchestrationHistory;
	}

	public void findVM(String[] architecture, Task task) {
		if (this.getSimulationManager().getScenario().getStringOrchAlgorithm().equals("RANDOM")) {
			random(architecture, task);
		} else if (this.getSimulationManager().getScenario().getStringOrchAlgorithm().equals("ROUND_ROBIN")) {
			roundRobin(architecture, task);
		} else if (this.getSimulationManager().getScenario().getStringOrchAlgorithm().equals("INCEREASE_LIFETIME")) {
			increseLifetime(architecture, task);
		} else if (this.getSimulationManager().getScenario().getStringOrchAlgorithm().equals("TRADE_OFF")) {
			tradeOff(architecture, task);
		}

	}

	private void tradeOff(String[] architecture, Task task) {
		int vm = -1;
		double min = -1;
		double new_min;// vm with minimum assigned tasks;
		List<EdgeVM> vmList = this.getSimulationManager().getServersManager().getVmList();

		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				double latency = 1;
				double energy = 1;
				if (vmList.get(i).getType() == simulationParameters.TYPES.CLOUD) {
					latency = 1.6;
					energy = 1.1;
				} else if (vmList.get(i).getType() == simulationParameters.TYPES.EDGE) {
					energy = 1.4;
				}
				new_min = (orchestrationHistory.get(i).size() + 1) * latency * energy / vmList.get(i).getMips();
				if (min == -1) { // if it is the first iteration
					min = new_min;
					// if this is the first time, set the first vm as the
					vm = i; // best one
				} else if (min > new_min) { // if this vm has more cpu mips and less waiting tasks
					// idle vm, no tasks are waiting
					min = new_min;
					vm = i;
				}
			}
		}
		// assign the tasks to the vm found
		assignTaskToVm(vm, task);
	}

	private void increseLifetime(String[] architecture, Task task) {
		int vm = -1;
		double minTasksCount = -1; // vm with minimum assigned tasks;
		double vmMips = 0;
		double weight = 0;
		double minWeight = 20;
		List<EdgeVM> vmList = this.getSimulationManager().getServersManager().getVmList();
		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
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
					minTasksCount = orchestrationHistory.get(i).size()
							- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1;
					// if this is the first time, set the first vm as the
					vm = i; // best one
					vmMips = vmList.get(i).getMips();
					minWeight = weight;
				} else if (vmMips / (minTasksCount * minWeight) < vmList.get(i).getMips()
						/ ((orchestrationHistory.get(i).size()
								- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1)
								* weight)) {
					// if this vm has more cpu mips and less waiting tasks
					minWeight = weight;
					vmMips = vmList.get(i).getMips();
					minTasksCount = orchestrationHistory.get(i).size()
							- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1;
					vm = i;
				}
			}
		}
		// assign the tasks to the vm found
		assignTaskToVm(vm, task);

	}

	private void roundRobin(String[] architecture, Task task) {
		List<EdgeVM> vmList = this.getSimulationManager().getServersManager().getVmList();
		int vm = -1;
		int minTasksCount = -1; // vm with minimum assigned tasks;
		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) { 
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				if (minTasksCount == -1) {
					minTasksCount = orchestrationHistory.get(i).size();
					// if this is the first time, set the first vm as the best one
					vm = i;
				} else if (minTasksCount > orchestrationHistory.get(i).size()) {
					minTasksCount = orchestrationHistory.get(i).size();
					// new min found, so we choose it as the best VM
					vm = i;
					break;
				}
			}
		}
		// assign the tasks to the vm found
		assignTaskToVm(vm, task);

	}

	private void random(String[] architecture, Task task) {
		List<EdgeVM> vmList = this.getSimulationManager().getServersManager().getVmList();
		int vm = -1;
		// get best vm for this task
		int j = 1 + new Random().nextInt(orchestrationHistory.size() - 1);
		while (j > 0) {
			for (int i = 0; i < orchestrationHistory.size(); i++) {
				if (offloadingIsPossible(task, vmList.get(i), architecture)) {
					j--;
					// assign the tasks to the vm found
					if (j == 0) {
						vm = i;
					}
				}
			}

		}
		assignTaskToVm(vm, task);
	}

	private boolean sameLocation(EdgeDataCenter device1, EdgeDataCenter device2, int RANGE) {
		double distance = Math
				.abs(Math.sqrt(Math.pow((device1.getLocation().getXPos() - device2.getLocation().getXPos()), 2)
						+ Math.pow((device1.getLocation().getYPos() - device2.getLocation().getYPos()), 2)));

		return (distance < RANGE);
	}

	private boolean arrayContains(String[] Architecture, String value) {
		for (int i = 0; i < Architecture.length; i++) {
			if (Architecture[i].equals(value))
				return true;
		}
		return false;
	}

	private void assignTaskToVm(int vmIndex, Task task) {
		List<EdgeVM> vmList = this.getSimulationManager().getServersManager().getVmList();
		if (vmIndex == -1) {
			this.getSimulationManager().getSimulationLogger().incrementTasksFailedLackOfRessources(task);
		} else {
			task.setVm(vmList.get(vmIndex)); // send this task to this vm
			this.getSimulationManager().getSimulationLogger()
					.deepLog(this.getSimulationManager().getSimulation().clock() + " : EdgeOrchestrator, Task: "
							+ task.getId() + " assigned to " + vmList.get(vmIndex).getType() + " vm: "
							+ vmList.get(vmIndex).getId());

			// update history
			orchestrationHistory.get(vmIndex).add((int) task.getId());
		}
	}

	private boolean offloadingIsPossible(Task task, EdgeVM edgeVM, String[] architecture) {
		return ((arrayContains(architecture, "Cloud") && edgeVM.getType() == simulationParameters.TYPES.CLOUD) // cloud
				|| (arrayContains(architecture, "Fog") && edgeVM.getType() == simulationParameters.TYPES.FOG // fog
				// compare destination (fog host) location and origin (edge) location, if they
				// are in same area offload to his device
						&& (sameLocation(((EdgeDataCenter) edgeVM.getHost().getDatacenter()), task.getEdgeDevice(),
								simulationParameters.FOG_RANGE)
								// or compare the location of their orchestrators
								|| (simulationParameters.ENABLE_ORCHESTRATORS
										&& sameLocation(((EdgeDataCenter) edgeVM.getHost().getDatacenter()),
												task.getOrchestrator(), simulationParameters.FOG_RANGE))))

				|| (arrayContains(architecture, "Edge") && edgeVM.getType() == simulationParameters.TYPES.EDGE // edge
				// compare destination (edge device) location and origin (edge) location, if
				// they are in same area offload to his device
						&& (sameLocation(((EdgeDataCenter) edgeVM.getHost().getDatacenter()), task.getEdgeDevice(),
								simulationParameters.EDGE_RANGE)
								// or compare the location of their orchestrators
								|| (simulationParameters.ENABLE_ORCHESTRATORS
										&& sameLocation(((EdgeDataCenter) edgeVM.getHost().getDatacenter()),
												task.getOrchestrator(), simulationParameters.EDGE_RANGE))
										&& ((EdgeDataCenter) edgeVM.getHost().getDatacenter()).isDead())));
	}

	@Override
	public void sendTask(Task task) {
		task.getEdgeDevice().getVmTaskMap().add(new VmTaskMapItem((EdgeVM) task.getVm(), task));

	}

}
