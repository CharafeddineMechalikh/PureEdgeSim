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
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * This class is responsible for generating the computing resources from the
 * input files ( @see
 * com.mechalikh.pureedgesim.simulationcore.SimulationAbstract#setCustomSettingsFolder(String))
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public abstract class ComputingNodesGenerator {

	/**
	 * The list that contains all orchestrators. It is used by the computing node.
	 * In this case, the tasks are sent over the network to one of the orchestrators
	 * to make decisions.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager#sendTaskToOrchestrator(Task)
	 */
	protected List<ComputingNode> orchestratorsList;

	/**
	 * The simulation manager.
	 */
	protected SimulationManager simulationManager;

	/**
	 * The Mobility Model to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager)
	 */
	protected Class<? extends MobilityModel> mobilityModelClass;

	/**
	 * The Computing Node Class to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager)
	 */
	protected Class<? extends ComputingNode> computingNodeClass;

	/**
	 * A list that contains all edge devices including sensors (i.e., devices
	 * without computing capacities).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> mistOnlyList;

	/**
	 * A list that contains all edge devices except sensors (i.e., devices without
	 * computing capacities).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> mistOnlyListSensorsExcluded;

	/**
	 * A list that contains only edge data centers and servers.
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#edgeOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> edgeOnlyList = new ArrayList<>(SimulationParameters.numberOfEdgeDataCenters);

	/**
	 * A list that contains only cloud data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#cloudOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> cloudOnlyList = new ArrayList<>(SimulationParameters.numberOfCloudDataCenters);

	/**
	 * A list that contains cloud data centers and edge devices (except sensors).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistAndCloud(Task
	 *      task)
	 */
	protected List<ComputingNode> mistAndCloudListSensorsExcluded;

	/**
	 * A list that contains cloud and edge data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#edgeAndCloud(Task
	 *      task)
	 */
	protected List<ComputingNode> edgeAndCloudList = new ArrayList<>(
			SimulationParameters.numberOfCloudDataCenters + SimulationParameters.numberOfEdgeDataCenters);

	/**
	 * A list that contains edge data centers and edge devices (except sensors).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistAndEdge(Task
	 *      task)
	 */
	protected List<ComputingNode> mistAndEdgeListSensorsExcluded;

	/**
	 * A list that contains all generated nodes including sensors
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#all(Task task)
	 */
	protected List<ComputingNode> allNodesList;

	/**
	 * A list that contains all generated nodes (sensors excluded)
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#all(Task task)
	 */
	protected List<ComputingNode> allNodesListSensorsExcluded;

	/**
	 * Constructs a new instance of the computing nodes generator.
	 * 
	 * @param simulationManager  The simulation manager to use.
	 * @param mobilityModelClass The mobility model to use.
	 * @param computingNodeClass The computing node class to use.
	 */
	public ComputingNodesGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		this.simulationManager = simulationManager;
		this.mobilityModelClass = mobilityModelClass;
		this.computingNodeClass = computingNodeClass;
		this.orchestratorsList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		this.mistOnlyList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		this.mistOnlyListSensorsExcluded = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		this.mistAndCloudListSensorsExcluded = new ArrayList<>(
				simulationManager.getScenario().getDevicesCount() + SimulationParameters.numberOfCloudDataCenters);
		this.mistAndEdgeListSensorsExcluded = new ArrayList<>(
				simulationManager.getScenario().getDevicesCount() + SimulationParameters.numberOfEdgeDataCenters);
		this.allNodesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount()
				+ SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters);
		this.allNodesListSensorsExcluded = new ArrayList<>(simulationManager.getScenario().getDevicesCount()
				+ SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters);
	}

	/**
	 * Generates all computing nodes, including the Cloud data centers, the edge
	 * ones, and the edge devices.
	 */ 
public abstract void generateDatacentersAndDevices();
	

	/**
	 * Returns the list containing computing nodes that have been selected as
	 * orchestrators (i.e. to make offloading decisions).
	 * 
	 * @return The list of orchestrators
	 */
	public List<ComputingNode> getOrchestratorsList() {
		return orchestratorsList;
	}

	/**
	 * Returns the simulation Manager.
	 * 
	 * @return The simulation manager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Gets the list containing all generated computing nodes.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDatacentersAndDevices()
	 * 
	 * @return the list containing all generated computing nodes.
	 */
	public List<ComputingNode> getAllNodesList() {
		return this.allNodesList;
	}

	/**
	 * Gets the list containing all generated edge devices including sensors (i.e.,
	 * devices with no computing resources).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing all edge devices including sensors.
	 */
	public List<ComputingNode> getMistOnlyList() {
		return this.mistOnlyList;
	}

	/**
	 * Gets the list containing all generated edge data centers / servers.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing all edge data centers and servers.
	 */
	public List<ComputingNode> getEdgeOnlyList() {
		return this.edgeOnlyList;
	}

	/**
	 * Gets the list containing only cloud data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing all generated cloud data centers.
	 */
	public List<ComputingNode> getCloudOnlyList() {
		return this.cloudOnlyList;
	}

	/**
	 * Gets the list containing cloud data centers and edge devices (except
	 * sensors).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing cloud data centers and edge devices.
	 */
	public List<ComputingNode> getMistAndCloudListSensorsExcluded() {
		return this.mistAndCloudListSensorsExcluded;
	}

	/**
	 * Gets the list containing cloud and edge data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing cloud and edge data centers.
	 */
	public List<ComputingNode> getEdgeAndCloudList() {
		return this.edgeAndCloudList;
	}

	/**
	 * Gets the list containing edge data centers and edge devices (except sensors).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing edge data centers and edge devices.
	 */
	public List<ComputingNode> getMistAndEdgeListSensorsExcluded() {
		return this.mistAndEdgeListSensorsExcluded;
	}

	/**
	 * Gets the list containing all generated edge devices except sensors (i.e.,
	 * devices with no computing resources).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing all edge devices except sensors.
	 */
	public List<ComputingNode> getMistOnlyListSensorsExcluded() {
		return this.mistOnlyListSensorsExcluded;
	}

	/**
	 * Gets the list containing all computing nodes (except sensors).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDataCenters(String, TYPES)
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator#generateDevicesInstances(Element)
	 * 
	 * @return the list containing all data centers and devices except sensors.
	 */
	public List<ComputingNode> getAllNodesListSensorsExcluded() {
		return this.allNodesListSensorsExcluded;
	}

}
