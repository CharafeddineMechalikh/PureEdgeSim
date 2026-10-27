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
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Base class for task orchestration algorithms.
 *
 * An orchestrator decides where each task runs: it selects a computing node from
 * the candidates allowed by the scenario architecture, then assigns that node to
 * the task. Subclasses implement {@link #findComputingNode(String[], Task)} to
 * define the placement policy and {@link #resultsReturned(Task)} to observe the
 * end of the task lifecycle.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public abstract class Orchestrator extends SimEntity {
	/** Candidate destinations matching the scenario architecture. */
	protected List<ComputingNode> nodeList;
	/** The simulation manager used to access nodes, scenario, and simulation state. */
	protected SimulationManager simulationManager;
	/** The simulation logger used for detailed orchestration traces. */
	protected SimLog simLog;
	/** Name of the orchestration algorithm, as set in the scenario file. */
	protected String algorithmName;
	/** Name of the orchestration architecture, as set in the scenario file. */
	protected String architectureName;
	/** Layers allowed by the architecture, for example {@code Cloud}, {@code Edge}, and {@code Mist}. */
	protected String[] architectureLayers;

	/**
	 * Creates an orchestrator, registers it with the simulation manager, reads the
	 * algorithm and architecture names from the scenario, and initializes the
	 * candidate node list.
	 *
	 * @param simulationManager the simulation manager
	 */
	protected Orchestrator(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
		simulationManager.setOrchestrator(this);
		simLog = simulationManager.getSimulationLogger();
		algorithmName = simulationManager.getScenario().getStringOrchAlgorithm();
		architectureName = simulationManager.getScenario().getStringOrchArchitecture();
		initialize();
	}

	/**
	 * Computes the offloading destination of a task and assigns it to the task.
	 * The actual selection policy is provided by
	 * {@link #findComputingNode(String[], Task)}.
	 *
	 * @param task the task to place
	 */
	public void orchestrate(Task task) {
		assignTaskToComputingNode(task, architectureLayers);
	}
	/**
	 * Builds the candidate node list and the allowed layers from the architecture
	 * name. Recognized architectures are {@code CLOUD_ONLY}, {@code MIST_ONLY},
	 * {@code EDGE_AND_CLOUD}, {@code ALL}, {@code EDGE_ONLY},
	 * {@code MIST_AND_CLOUD}, and {@code MIST_AND_EDGE}. An unknown name leaves the
	 * candidate list unset.
	 */
	public void initialize() {
		if ("CLOUD_ONLY".equals(architectureName)) {
			cloudOnly();
		} else if ("MIST_ONLY".equals(architectureName)) {
			mistOnly();
		} else if ("EDGE_AND_CLOUD".equals(architectureName)) {
			edgeAndCloud();
		} else if ("ALL".equals(architectureName)) {
			all();
		} else if ("EDGE_ONLY".equals(architectureName)) {
			edgeOnly();
		} else if ("MIST_AND_CLOUD".equals(architectureName)) {
			mistAndCloud();
		} else if ("MIST_AND_EDGE".equals(architectureName)) {
			mistAndEdge();
		}
	}

	/**
	 * Configures the {@code MIST_ONLY} architecture: tasks can run only on edge
	 * devices, with sensors excluded from the candidate list.
	 */
	protected void mistOnly() {
		architectureLayers = new String[]{ "Mist" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistOnlyListSensorsExcluded();
		
	}

	/**
	 * Configures the {@code CLOUD_ONLY} architecture: tasks can run only on cloud
	 * virtual machines.
	 */
	protected void cloudOnly() {
		architectureLayers = new String[] { "Cloud" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getCloudOnlyList();
	}

	/**
	 * Configures the {@code EDGE_AND_CLOUD} architecture: tasks can run on edge
	 * data centers or cloud virtual machines.
	 */
	protected void edgeAndCloud() {
		architectureLayers = new String[]{ "Cloud", "Edge" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getEdgeAndCloudList();
	}

	/**
	 * Configures the {@code MIST_AND_CLOUD} architecture: tasks can run on edge
	 * devices or cloud virtual machines, with sensors excluded.
	 */
	protected void mistAndCloud() {
		architectureLayers = new String[] { "Cloud", "Mist" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistAndCloudListSensorsExcluded();
	}

	/**
	 * Configures the {@code EDGE_ONLY} architecture: tasks can run only on edge
	 * data centers.
	 */
	protected void edgeOnly() {
		architectureLayers = new String[]{ "Edge" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getEdgeOnlyList();
	}

	/**
	 * Configures the {@code MIST_AND_EDGE} architecture: tasks can run on edge
	 * devices or edge data centers, with sensors excluded.
	 */
	protected void mistAndEdge() {
		architectureLayers = new String[] { "Mist", "Edge" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistAndEdgeListSensorsExcluded();
	}

	/**
	 * Configures the {@code ALL} architecture: tasks can run on cloud virtual
	 * machines, edge data centers, or edge devices, with sensors excluded.
	 */
	protected void all() {
		architectureLayers = new String[]{ "Cloud", "Edge", "Mist" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getAllNodesListSensorsExcluded();
	}

	/**
	 * Chooses the computing node that will execute the task.
	 *
	 * @param architectureLayers the layers allowed by the architecture
	 * @param task               the task to place
	 * @return the index of the selected node in {@link #nodeList}, or -1 when the
	 *         task cannot be placed
	 */
	protected abstract int findComputingNode(String[] architectureLayers, Task task);

	/**
	 * Finds a destination with {@link #findComputingNode(String[], Task)} and
	 * assigns it to the task. When a node is selected, it becomes the task's
	 * offloading destination, the edge device records it as the application
	 * placement location, and a trace is written to the deep log. The destination
	 * is validated with {@link #checkComputingNode(ComputingNode)}; validation
	 * errors are printed and do not stop the simulation.
	 *
	 * @param task               the task to place
	 * @param architectureLayers the layers allowed by the architecture
	 */
	protected void assignTaskToComputingNode(Task task, String[] architectureLayers) {

		int nodeIndex = findComputingNode(architectureLayers, task);

		if (nodeIndex != -1) {
			ComputingNode node = nodeList.get(nodeIndex);
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

	/**
	 * Ensures that the selected destination is a valid computing node. Sensors
	 * cannot execute tasks, so selecting one indicates a bug in the orchestration
	 * algorithm.
	 *
	 * @param computingNode the selected destination
	 * @throws IllegalArgumentException if the destination is a sensor
	 */
	protected void checkComputingNode(ComputingNode computingNode) {
		if (computingNode.isSensor())
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - The forbidden happened! The orchestration algorithm \"" + algorithmName
							+ "\" has selected a sensor as an offloading destination. Kindly check it.");
	}

	/**
	 * Returns whether two nodes are within the given range. Cloud nodes are always
	 * considered to be in range.
	 *
	 * @param device1 the first node
	 * @param device2 the second node
	 * @param RANGE   the maximum distance, in meters
	 * @return {@code true} if the nodes are close enough
	 */
	protected boolean sameLocation(ComputingNode device1, ComputingNode device2, int RANGE) {
		if (device2.getType() == SimulationParameters.TYPES.CLOUD)
			return true;
		double distance = device1.getMobilityModel().distanceTo(device2);
		return (distance <= RANGE);
	}

	/**
	 * Returns whether the given array contains the value.
	 *
	 * @param architectureLayers the array to search
	 * @param value              the value to find
	 * @return {@code true} if the value is present
	 */
	protected boolean arrayContains(String[] architectureLayers, String value) {
		for (String s : architectureLayers) {
			if (s.equals(value))
				return true;
		}
		return false;
	}

	/**
	 * Returns whether the task may be offloaded to the given node under the
	 * allowed architecture layers.
	 *
	 * Cloud nodes are always possible when {@code Cloud} is allowed. Edge data
	 * centers are possible when {@code Edge} is allowed and the node is the current
	 * uplink destination of the edge device or of its orchestrator. Edge devices
	 * are possible when {@code Mist} is allowed, the node is within communication
	 * range of the task's edge device (or, when orchestrators are enabled, of the
	 * orchestrator), and the node is neither dead nor a sensor.
	 *
	 * @param task               the task being placed
	 * @param node               the candidate destination
	 * @param architectureLayers the layers allowed by the architecture
	 * @return {@code true} if the task can be offloaded to the node
	 */
	protected boolean offloadingIsPossible(Task task, ComputingNode node, String[] architectureLayers) {
		SimulationParameters.TYPES nodeType = node.getType();
		return ((arrayContains(architectureLayers, "Cloud") && nodeType == SimulationParameters.TYPES.CLOUD) // cloud
																										// computing
				|| (arrayContains(architectureLayers, "Edge") && nodeType == SimulationParameters.TYPES.EDGE_DATACENTER // Edge
																													// computing
				// Compare destination (edge data server) and origin (edge device)
				// locations, if they are in same area offload to this edge data server
						&& (node == task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK).getDst()
								// or compare the location of the orchestrator
								|| (node == task.getOrchestrator().getCurrentLink(LinkOrientation.UP_LINK).getDst())))

				|| (arrayContains(architectureLayers, "Mist") && nodeType == SimulationParameters.TYPES.EDGE_DEVICE // Mist
																												// computing
				// compare destination (edge device) location and origin (edge device) location,
				// if they are in same area offload to this device
						&& (sameLocation(node, task.getEdgeDevice(), SimulationParameters.edgeDevicesRange)
								// or compare the location of their orchestrators
								|| (SimulationParameters.enableOrchestrators && sameLocation(node,
										task.getOrchestrator(), SimulationParameters.edgeDevicesRange)))
						&& !node.isDead() && !node.isSensor()));
	}

	/**
	 * Called when the results of a task have returned to its edge device.
	 * Subclasses use this callback to update algorithm state and metrics.
	 *
	 * @param task the completed task
	 */
	public abstract void resultsReturned(Task task);

}
