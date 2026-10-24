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

public abstract class TopologyCreator {
	protected ComputingNodesGenerator computingNodesGenerator;
	protected SimulationManager simulationManager;
	protected InfrastructureGraph infrastructureTopology;

	public TopologyCreator(SimulationManager simulationManager, ComputingNodesGenerator computingNodesGenerator) {
		infrastructureTopology = new InfrastructureGraph();
		this.simulationManager = simulationManager;
		this.computingNodesGenerator = computingNodesGenerator;
	}

	public abstract void generateTopologyGraph();
	
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public InfrastructureGraph getTopology() {
		return infrastructureTopology;
	}
}