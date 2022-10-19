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
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public abstract class Orchestrator extends SimEntity {
	protected List<ComputingNode> mistOnlyListSensorsExcluded;
	protected List<ComputingNode> edgeOnlyList;
	protected List<ComputingNode> cloudOnlyList;
	protected List<ComputingNode> mistAndCloudListSensorsExcluded;
	protected List<ComputingNode> edgeAndCloudList;
	protected List<ComputingNode> mistAndEdgeListSensorsExcluded;
	protected List<ComputingNode> allNodesListSensorsExcluded;
	protected SimulationManager simulationManager;
	protected SimLog simLog;
	public String algorithm;
	protected String architecture;

	protected Orchestrator(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
		simulationManager.setOrchestrator(this);
		simLog = simulationManager.getSimulationLogger();
		allNodesListSensorsExcluded = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getAllNodesListSensorsExcluded();
		mistOnlyListSensorsExcluded = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistOnlyListSensorsExcluded();
		edgeOnlyList = simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList();
		cloudOnlyList = simulationManager.getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList();
		mistAndCloudListSensorsExcluded = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistAndCloudListSensorsExcluded();
		edgeAndCloudList = simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeAndCloudList();
		mistAndEdgeListSensorsExcluded = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistAndEdgeListSensorsExcluded();

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
		} else if ("MIST_AND_EDGE".equals(architecture)) {
			mistAndEdge(task);
		}
	}

	// If the orchestration scenario is MIST_ONLY send Tasks only to edge devices
	protected void mistOnly(Task task) {
		String[] architecture = { "Mist" };
		assignTaskToComputingNode(task, architecture, mistOnlyListSensorsExcluded);
	}

	// If the orchestration scenario is ClOUD_ONLY send Tasks (cloudlets) only to
	// cloud virtual machines (vms)
	protected void cloudOnly(Task task) {
		String[] architecture = { "Cloud" };
		assignTaskToComputingNode(task, architecture, cloudOnlyList);
	}

	// If the orchestration scenario is EDGE_AND_CLOUD send Tasks only to edge data
	// centers or cloud virtual machines (vms)
	protected void edgeAndCloud(Task task) {
		String[] architecture = { "Cloud", "Edge" };
		assignTaskToComputingNode(task, architecture, edgeAndCloudList);
	}

	// If the orchestration scenario is MIST_AND_CLOUD send Tasks only to edge
	// devices or cloud virtual machines (vms)
	protected void mistAndCloud(Task task) {
		String[] architecture = { "Cloud", "Mist" };
		assignTaskToComputingNode(task, architecture, mistAndCloudListSensorsExcluded);
	}

	// If the orchestration scenario is EDGE_ONLY send Tasks only to edge data
	// centers
	protected void edgeOnly(Task task) {
		String[] architecture = { "Edge" };
		assignTaskToComputingNode(task, architecture, edgeOnlyList);
	}

	// If the orchestration scenario is MIST_AND_Edge send Tasks only to edge
	// devices or Fog virtual machines (vms)
	protected void mistAndEdge(Task task) {
		String[] architecture = { "Mist", "Edge" };
		assignTaskToComputingNode(task, architecture, mistAndEdgeListSensorsExcluded);
	}

	// If the orchestration scenario is ALL send Tasks to any virtual machine (vm)
	// or device
	protected void all(Task task) {
		String[] architecture = { "Cloud", "Edge", "Mist" };
		assignTaskToComputingNode(task, architecture, allNodesListSensorsExcluded);
	}

	protected abstract int findComputingNode(String[] architecture, Task task, List<ComputingNode> nodesList);

	protected void assignTaskToComputingNode(Task task, String[] architecture, List<ComputingNode> nodesList) {

		int nodeIndex = findComputingNode(architecture, task, nodesList);

		if (nodeIndex != -1) {
			ComputingNode node = nodesList.get(nodeIndex);
			try {
				checkComputingNode(node);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Send this task to this computing node
			task.setOffloadingDestination(node);

			// Application has been deployed
			task.getEdgeDevice().setApplicationPlacementLocation(node);
			simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId()
					+ " assigned to " + node.getType() + " Computing Node: " + node.getId());

		}
	}

	protected void checkComputingNode(ComputingNode computingNode) {
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

	protected boolean arrayContains(String[] architecture, String value) {
		for (String s : architecture) {
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
						&& (node == task.getEdgeDevice().getCurrentUpLink().getDst()
								// or compare the location of the orchestrator
								|| (node == task.getOrchestrator().getCurrentUpLink().getDst())))

				|| (arrayContains(architecture, "Mist") && nodeType == SimulationParameters.TYPES.EDGE_DEVICE // Mist
																												// computing
				// compare destination (edge device) location and origin (edge device) location,
				// if they are in same area offload to this device
						&& (sameLocation(node, task.getEdgeDevice(), SimulationParameters.edgeDevicesRange)
								// or compare the location of their orchestrators
								|| (SimulationParameters.enableOrchestrators && sameLocation(node,
										task.getOrchestrator(), SimulationParameters.edgeDevicesRange)))
						&& !node.isDead() && !node.isSensor()));
	}

	public abstract void resultsReturned(Task task);

}
