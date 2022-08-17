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

import java.util.List;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * The main class of the Data Centers Manager module, that manages the different
 * resources and generates the infrastructure topology.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */

public class DataCentersManager {
	/**
	 * The simulation manager.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationManager
	 */
	protected SimulationManager simulationManager;
	
	/**
	 * The Mobility Model to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(SimulationManager)
	 */
	private Class<? extends MobilityModel> mobilityModel; 
	
	/**
	 * The Computing Node Class to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(SimulationManager)
	 */
	private Class<? extends ComputingNode> computingNodeClass;
	
	/**
	 * The computing nodes generator used to generate all resources from the xml files.
	 * 
	 * @see #generateComputingNodes()
	 * @see com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator
	 */
	private ComputingNodesGenerator computingNodesGenerator;
	
	/**
	 * The topology creator that is used to generate the network topology.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator
	 */
	private TopologyCreator topologyCreator;

	/**
	 * Initializes the DataCentersManager
	 *
	 * @param simulationManager  The simulation Manager
	 * @param mobilityModelClass The mobility model that will be used in the
	 *                           simulation
	 * @param computingNodeClass The computing node class that will be used to
	 *                           generate computing resources
	 */
	public DataCentersManager(SimulationManager simulationManager, Class<? extends MobilityModel> mobilityModel,
			 Class<? extends ComputingNode> computingNodeClass) {
		this.simulationManager = simulationManager;
		this.mobilityModel = mobilityModel;
		this.computingNodeClass = computingNodeClass;

		// Generate all data centers, servers, an devices
		generateComputingNodes();

		// Add this to the simulation manager and submit computing nodes to broker
		simulationManager.setDataCentersManager(this);

		// Generate topology
		createTopology();
	}

	/**
	 * Generates all computing nodes.
	 */
	private void generateComputingNodes() {
		computingNodesGenerator = new ComputingNodesGenerator(simulationManager, mobilityModel,
				computingNodeClass);
		computingNodesGenerator.generateDatacentersAndDevices();
	} 

	/**
	 * Creates the network topology.
	 */
	public void createTopology() {
		topologyCreator = new TopologyCreator(simulationManager, this.computingNodesGenerator);
		topologyCreator.generateTopologyGraph();
	}

	/**
	 * Gets the current infrastructure topology.
	 * 
	 * @return network topology.
	 */
	public InfrastructureGraph getTopology() {
		return topologyCreator.getTopology();
	}

	/**
	 * Gets the list of generated computing nodes.
	 * 
	 * @return the list of computing nodes.
	 */
	public List<ComputingNode> getNodesList() {
		return computingNodesGenerator.getComputingNodes();
	}

	/**
	 * Gets the list of workload orchestrators.
	 * 
	 * @return the list of orchestrators.
	 */
	public List<ComputingNode> getOrchestratorsList() {
		return computingNodesGenerator.getOrchestratorsList();
	}

	/**
	 * Gets the list of edge data centers/ servers / fog nodes...
	 * 
	 * @return the list of edge servers or fog nodes.
	 */
	public List<ComputingNode> getEdgeDatacenterList() {
		return computingNodesGenerator.getEdgeDatacenterList();
	}

	/**
	 * Gets the list of generated edge devices.
	 * 
	 * @return the list of edge devices.
	 */
	public List<ComputingNode> getEdgeDevicesList() {
		return computingNodesGenerator.getEdgeDevicesList();
	}

	/**
	 * Gets the list of the generated cloud data centers.
	 * 
	 * @return the list of cloud data centers.
	 */
	public List<ComputingNode> getCloudDatacentersList() {
		return computingNodesGenerator.getCloudDatacenterList();
	}
}
