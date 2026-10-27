package com.mechalikh.pureedgesim.network;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Small helpers shared by the network package tests. They build real
 * {@link PureEdgeSim} engines (required by {@link com.mechalikh.pureedgesim.simulationengine.SimEntity})
 * behind mocked simulation managers, and Mockito based computing nodes.
 */
final class NetworkTestSupport {

	private NetworkTestSupport() {
	}

	static PureEdgeSim newSimulation() {
		return new PureEdgeSim();
	}

	static SimulationManager newSimulationManager() {
		return newSimulationManager(newSimulation());
	}

	static SimulationManager newSimulationManager(PureEdgeSim simulation) {
		SimulationManager simulationManager = mock(SimulationManager.class);
		when(simulationManager.getSimulation()).thenReturn(simulation);
		return simulationManager;
	}

	static ComputingNode node(int id, TYPES type) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getId()).thenReturn(id);
		when(node.getType()).thenReturn(type);
		return node;
	}
}
