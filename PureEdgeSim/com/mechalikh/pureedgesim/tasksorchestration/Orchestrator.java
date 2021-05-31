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
package com.mechalikh.pureedgesim.tasksorchestration;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimLog;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;

public abstract class Orchestrator {
	protected List<List<Integer>> orchestrationHistory;
	protected List<Vm> vmList;
	protected SimulationManager simulationManager;
	protected SimLog simLog;
	protected String algorithm;
	protected String architecture;

	public Orchestrator(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
		simLog = simulationManager.getSimulationLogger();
		orchestrationHistory = new ArrayList<>();
		vmList = simulationManager.getDataCentersManager().getVmList();
		algorithm = simulationManager.getScenario().getStringOrchAlgorithm();
		architecture = simulationManager.getScenario().getStringOrchArchitecture();
		initHistoryList(vmList.size());
	}

	private void initHistoryList(int size) {
		for (int vm = 0; vm < size; vm++) {
			// Creating a list to store the orchestration history for each VM (virtual
			// machine)
			orchestrationHistory.add(new ArrayList<>());
		}
	}

	public void initialize(Task task) {
		if ("CLOUD_ONLY".equals(architecture)) {
			cloudOnly(task);
		} else if ("MIST_ONLY".equals(architecture)) {
			mistOnly(task);
		} else if ("EDGE_AND_CLOUD".equals(architecture)) {
			edgeAndCloud(task);
		} else if ("ALL".equals(architecture)) {
			all(task);
		} else if ("EDGE_ONLY".equals(architecture)) {
			edgeOnly(task);
		} else if ("MIST_AND_CLOUD".equals(architecture)) {
			mistAndCloud(task);
		}
	}

	// If the orchestration scenario is MIST_ONLY send Tasks only to edge devices
	private void mistOnly(Task task) {
		String[] Architecture = { "Mist" };
		assignTaskToVm(findVM(Architecture, task), task);
	}

	// If the orchestration scenario is ClOUD_ONLY send Tasks (cloudlets) only to
	// cloud virtual machines (vms)
	private void cloudOnly(Task task) {
		String[] Architecture = { "Cloud" };
		assignTaskToVm(findVM(Architecture, task), task);
	}

	// If the orchestration scenario is EDGE_AND_CLOUD send Tasks only to edge data
	// centers or cloud virtual machines (vms)
	private void edgeAndCloud(Task task) {
		String[] Architecture = { "Cloud", "Edge" };
		assignTaskToVm(findVM(Architecture, task), task);
	}

	// If the orchestration scenario is MIST_AND_CLOUD send Tasks only to edge
	// devices or cloud virtual machines (vms)
	private void mistAndCloud(Task task) {
		String[] Architecture = { "Cloud", "Mist" };
		assignTaskToVm(findVM(Architecture, task), task);
	}

	// If the orchestration scenario is EDGE_ONLY send Tasks only to edge data
	// centers
	private void edgeOnly(Task task) {
		String[] Architecture = { "Edge" };
		assignTaskToVm(findVM(Architecture, task), task);
	}

	// If the orchestration scenario is ALL send Tasks to any virtual machine (vm)
	// or device
	private void all(Task task) {
		String[] Architecture = { "Cloud", "Edge", "Mist" };
		assignTaskToVm(findVM(Architecture, task), task);
	}

	protected abstract int findVM(String[] architecture, Task task);
 

	protected void assignTaskToVm(int vmIndex, Task task) {
		if (vmIndex == -1) {
			simLog.incrementTasksFailedLackOfRessources(task);
		} else {
			task.setVm(vmList.get(vmIndex)); // send this task to this vm
			simLog.deepLog(simulationManager.getSimulation().clock() + " : EdgeOrchestrator, Task: " + task.getId()
					+ " assigned to " + ((DataCenter) vmList.get(vmIndex).getHost().getDatacenter()).getType() + " vm: "
					+ vmList.get(vmIndex).getId());

			// update history
			orchestrationHistory.get(vmIndex).add((int) task.getId());
		}
	}

	protected boolean sameLocation(DataCenter device1, DataCenter device2, int RANGE) {
		if (device2.getType() == SimulationParameters.TYPES.CLOUD)
			return true;
		double distance = device1.getMobilityManager().distanceTo(device2);
		return (distance < RANGE);
	}

	protected boolean arrayContains(String[] Architecture, String value) {
		for (String s : Architecture) {
			if (s.equals(value))
				return true;
		}
		return false;
	}

	protected boolean offloadingIsPossible(Task task, Vm vm, String[] architecture) {
		SimulationParameters.TYPES vmType = ((DataCenter) vm.getHost().getDatacenter()).getType();
		return ((arrayContains(architecture, "Cloud") && vmType == SimulationParameters.TYPES.CLOUD) // cloud computing
				|| (arrayContains(architecture, "Edge") && vmType == SimulationParameters.TYPES.EDGE_DATACENTER // Edge
																												// computing
				// compare destination (edge data center) location and origin (edge device)
				// location, if they
				// are in same area offload to his device
						&& (sameLocation(((DataCenter) vm.getHost().getDatacenter()), task.getEdgeDevice(),
								SimulationParameters.EDGE_DATACENTERS_RANGE)
								// or compare the location of their orchestrators
								|| (SimulationParameters.ENABLE_ORCHESTRATORS
										&& sameLocation(((DataCenter) vm.getHost().getDatacenter()),
												task.getOrchestrator(), SimulationParameters.EDGE_DATACENTERS_RANGE))))

				|| (arrayContains(architecture, "Mist") && vmType == SimulationParameters.TYPES.EDGE_DEVICE // Mist
																											// computing
				// compare destination (edge device) location and origin (edge device) location,
				// if they are in same area offload to this device
						&& (sameLocation(((DataCenter) vm.getHost().getDatacenter()), task.getEdgeDevice(),
								SimulationParameters.EDGE_DEVICES_RANGE)
								// or compare the location of their orchestrators
								|| (SimulationParameters.ENABLE_ORCHESTRATORS
										&& sameLocation(((DataCenter) vm.getHost().getDatacenter()),
												task.getOrchestrator(), SimulationParameters.EDGE_DEVICES_RANGE)))
						&& !((DataCenter) vm.getHost().getDatacenter()).isDead()));
	}

	public abstract void resultsReturned(Task task);

}
