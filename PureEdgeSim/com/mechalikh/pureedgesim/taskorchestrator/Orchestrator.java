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
 *     @author Charaf Eddine Mechalikh
 **/
package com.mechalikh.pureedgesim.taskorchestrator;

import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public abstract class Orchestrator {
	protected List<? extends ComputingNode> nodeList;
	protected SimulationManager simulationManager;
	protected SimLog simLog;
	protected String algorithm;
	protected String architecture;

	public Orchestrator(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
		simLog = simulationManager.getSimulationLogger();
		nodeList = simulationManager.getDataCentersManager().getNodesList();
		algorithm = simulationManager.getScenario().getStringOrchAlgorithm();
		architecture = simulationManager.getScenario().getStringOrchArchitecture();
	}

	public void orchestrate(Task task) {
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
		assignTaskToComputingNode(findComputingNode(Architecture, task), task);
	}

	// If the orchestration scenario is ClOUD_ONLY send Tasks (cloudlets) only to
	// cloud virtual machines (vms)
	private void cloudOnly(Task task) {
		String[] Architecture = { "Cloud" };
		assignTaskToComputingNode(findComputingNode(Architecture, task), task);
	}

	// If the orchestration scenario is EDGE_AND_CLOUD send Tasks only to edge data
	// centers or cloud virtual machines (vms)
	private void edgeAndCloud(Task task) {
		String[] Architecture = { "Cloud", "Edge" };
		assignTaskToComputingNode(findComputingNode(Architecture, task), task);
	}

	// If the orchestration scenario is MIST_AND_CLOUD send Tasks only to edge
	// devices or cloud virtual machines (vms)
	private void mistAndCloud(Task task) {
		String[] Architecture = { "Cloud", "Mist" };
		assignTaskToComputingNode(findComputingNode(Architecture, task), task);
	}

	// If the orchestration scenario is EDGE_ONLY send Tasks only to edge data
	// centers
	private void edgeOnly(Task task) {
		String[] Architecture = { "Edge" };
		assignTaskToComputingNode(findComputingNode(Architecture, task), task);
	}

	// If the orchestration scenario is ALL send Tasks to any virtual machine (vm)
	// or device
	private void all(Task task) {
		String[] Architecture = { "Cloud", "Edge", "Mist" };
		assignTaskToComputingNode(findComputingNode(Architecture, task), task);
	}

	protected abstract int findComputingNode(String[] architecture, Task task);

	protected void assignTaskToComputingNode(int nodeIndex, Task task) {

		if (nodeIndex != -1) {
			ComputingNode node = nodeList.get(nodeIndex);
			try {
				checkComputingNode(node);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Send this task to this computing node
			task.setComputingNode(node);

			// Application has been deployed
			task.getEdgeDevice().setApplicationPlacementLocation(node);

			simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId()
					+ " assigned to " + node.getType() + " Computing Node: " + node.getId());

		}
	}

	protected void checkComputingNode(ComputingNode computingNode) throws Exception {
		if (computingNode.isSensor())
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - The forbidden happened! The orchestration algorithm \"" + algorithm
							+ "\" has selected a sensor as an offloading destination. Kindly check it.");
	}

	protected boolean sameLocation(ComputingNode device1, ComputingNode device2, int RANGE) {
		if (device2.getType() == SimulationParameters.TYPES.CLOUD)
			return true;
		double distance = device1.getMobilityModel().distanceTo(device2);
		return (distance <= RANGE);
	}

	protected boolean arrayContains(String[] Architecture, String value) {
		for (String s : Architecture) {
			if (s.equals(value))
				return true;
		}
		return false;
	}

	protected boolean offloadingIsPossible(Task task, ComputingNode node, String[] architecture) {
		SimulationParameters.TYPES nodeType = node.getType();
		return ((arrayContains(architecture, "Cloud") && nodeType == SimulationParameters.TYPES.CLOUD) // cloud
																										// computing
				|| (arrayContains(architecture, "Edge") && nodeType == SimulationParameters.TYPES.EDGE_DATACENTER // Edge
																													// computing
				// Compare destination (edge data server) and origin (edge device)
				// locations, if they are in same area offload to this edge data server
						&& ( node== task.getEdgeDevice().getCurrentUpLink().getDst()
								// or compare the location of the orchestrator
								|| (node== task.getOrchestrator().getCurrentUpLink().getDst())))

				|| (arrayContains(architecture, "Mist") && nodeType == SimulationParameters.TYPES.EDGE_DEVICE // Mist
																												// computing
				// compare destination (edge device) location and origin (edge device) location,
				// if they are in same area offload to this device
						&& (sameLocation(node, task.getEdgeDevice(), SimulationParameters.EDGE_DEVICES_RANGE)
								// or compare the location of their orchestrators
								|| (SimulationParameters.ENABLE_ORCHESTRATORS && sameLocation(node,
										task.getOrchestrator(), SimulationParameters.EDGE_DEVICES_RANGE)))
						&& !node.isDead() && !node.isSensor()));
	}

	public abstract void resultsReturned(Task task);

}
