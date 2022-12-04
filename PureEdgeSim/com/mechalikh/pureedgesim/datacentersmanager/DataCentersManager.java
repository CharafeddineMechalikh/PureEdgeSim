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

import java.lang.reflect.Constructor;

import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
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
	 * @see com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager
	 */
	protected SimulationManager simulationManager;

	/**
	 * The computing nodes generator used to generate all resources from the xml
	 * files.
	 * 
	 * @see #generateComputingNodes()
	 * @see com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator
	 */
	protected ComputingNodesGenerator computingNodesGenerator;

	/**
	 * The topology creator that is used to generate the network topology.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator
	 */
	protected TopologyCreator topologyCreator;

	/**
	 * Initializes the DataCentersManager
	 *
	 * @param simulationManager  The simulation Manager
	 * @param mobilityModelClass The mobility model that will be used in the
	 *                           simulation
	 * @param computingNodeClass The computing node class that will be used to
	 *                           generate computing resources
	 * @param topologyCreator
	 */
	public DataCentersManager(SimulationManager simulationManager, Class<? extends MobilityModel> mobilityModelClass,
			Class<? extends ComputingNode> computingNodeClass, Class<? extends TopologyCreator> topologyCreatorClass) {
		this.simulationManager = simulationManager;
		// Add this to the simulation manager and submit computing nodes to broker
		simulationManager.setDataCentersManager(this);

		// Generate all data centers, servers, an devices
		generateComputingNodes(mobilityModelClass, computingNodeClass);

		// Generate topology
		createTopology(topologyCreatorClass);
	}

	/**
	 * Generates all computing nodes.
	 * 
	 * @param computingNodeClass
	 * @param mobilityModelClass
	 */
	protected void generateComputingNodes(Class<? extends MobilityModel> mobilityModelClass,
			Class<? extends ComputingNode> computingNodeClass) {
		SimLog.println("%s - Generating computing nodes...",this.getClass().getSimpleName());
		computingNodesGenerator = new ComputingNodesGenerator(simulationManager, mobilityModelClass,
				computingNodeClass);
		computingNodesGenerator.generateDatacentersAndDevices();
	}

	/**
	 * Creates the network topology.
	 * 
	 * @param topologyCreatorClass
	 */
	public void createTopology(Class<? extends TopologyCreator> topologyCreatorClass) {
		SimLog.println("%s - Creating the network topology...",this.getClass().getSimpleName());
		Constructor<?> topologyCreatorConstructor;
		try {
			topologyCreatorConstructor = topologyCreatorClass.getConstructor(SimulationManager.class,
					ComputingNodesGenerator.class);

			topologyCreator = (TopologyCreator) topologyCreatorConstructor.newInstance(simulationManager,
					computingNodesGenerator);
		} catch (Exception e) {
			e.printStackTrace();
		}
		topologyCreator.generateTopologyGraph();
	}

	/**
	 * Gets the topology creator.
	 * 
	 * @return topologyCreator the topology creator.
	 */
	public InfrastructureGraph getTopology() {
		return topologyCreator.getTopology();
	}

	/**
	 * Gets the computing nodes generator.
	 * 
	 * @return computingNodesGenerator the computing nodes generator..
	 */
	public ComputingNodesGenerator getComputingNodesGenerator() {
		return computingNodesGenerator;
	}

}
