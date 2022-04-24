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
	protected SimulationManager simulationManager;
	private Class<? extends MobilityModel> mobilityModel; 
	private Class<? extends ComputingNode> computingNodeClass;
	private ComputingNodesGenerator computingNodesGenerator;
	private TopologyCreator topologyCreator;

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

	private void generateComputingNodes() {
		computingNodesGenerator = new ComputingNodesGenerator(simulationManager, mobilityModel,
				computingNodeClass);
		computingNodesGenerator.generateDatacentersAndDevices();
	} 

	public void createTopology() {
		topologyCreator = new TopologyCreator(simulationManager, this.computingNodesGenerator);
		topologyCreator.generateTopologyGraph();
	}

	public InfrastructureGraph getTopology() {
		return topologyCreator.getTopology();
	}

	public List<ComputingNode> getNodesList() {
		return computingNodesGenerator.getComputingNodes();
	}

	public List<ComputingNode> getOrchestratorsList() {
		return computingNodesGenerator.getOrchestratorsList();
	}

	public List<ComputingNode> getEdgeDatacenterList() {
		return computingNodesGenerator.getEdgeDatacenterList();
	}

	public List<ComputingNode> getEdgeDevicesList() {
		return computingNodesGenerator.getEdgeDevicesList();
	}

	public List<ComputingNode> getCloudDatacentersList() {
		return computingNodesGenerator.getCloudDatacenterList();
	}
}
