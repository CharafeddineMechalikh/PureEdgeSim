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

import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Base class for topology creators. A creator receives the generated computing
 * nodes and adds the network links that connect them.
 */
public abstract class TopologyCreator {
	/** Generator that provides the generated cloud, edge, and mist nodes. */
	protected ComputingNodesGenerator computingNodesGenerator;
	/** Simulation manager. */
	protected SimulationManager simulationManager;
	/** Topology under construction. */
	protected InfrastructureGraph infrastructureTopology;

	/**
	 * Creates a topology creator.
	 *
	 * @param simulationManager        the simulation manager.
	 * @param computingNodesGenerator the generator that provides the nodes.
	 */
	public TopologyCreator(SimulationManager simulationManager, ComputingNodesGenerator computingNodesGenerator) {
		infrastructureTopology = new InfrastructureGraph();
		this.simulationManager = simulationManager;
		this.computingNodesGenerator = computingNodesGenerator;
	}

	/** Builds the topology graph. Called once during initialization. */
	public abstract void generateTopologyGraph();
	
	/**
	 * Returns the simulation manager.
	 *
	 * @return the simulation manager.
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Returns the generated topology.
	 *
	 * @return the infrastructure graph.
	 */
	public InfrastructureGraph getTopology() {
		return infrastructureTopology;
	}
}