package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

/**
 * Tests the life cycle of {@link DataCentersManager}: generation of computing
 * nodes, topology creation, and access to both modules.
 */
class DataCentersManagerTest extends DatacentersTestSupport {

	@Test
	void constructorGeneratesNodesAndTopology() {
		DataCentersManager manager = newDataCentersManager(20, true);

		assertSame(manager, simulationManager.getDataCentersManager());
		assertNotNull(manager.getComputingNodesGenerator());
		assertNotNull(manager.getTopology());
		assertEquals(1, manager.getComputingNodesGenerator().getCloudOnlyList().size());
		assertEquals(1, manager.getComputingNodesGenerator().getEdgeOnlyList().size());
		// The XML percentages generate the 20 devices of the scenario.
		assertEquals(20, manager.getComputingNodesGenerator().getMistOnlyList().size());

		InfrastructureGraph topology = manager.getTopology();
		assertTrue(topology.getWanLinks().size() > 0);
		assertTrue(topology.getLanLinks().size() > 0);
		assertTrue(topology.getManLinks().isEmpty());
		// Cloud, edge data center, shared-WAN router and the 20 edge devices.
		assertEquals(23, topology.getGraph().vertexSet().size());
		assertEquals(4, topology.getPathsMap().size());
	}

	@Test
	void sharedWanOptionRegistersWanLinksOnTheNetworkModel() {
		newDataCentersManager(20, true);

		assertEquals(0, simulationManager.getNetworkModel().getWanUpUtilization(), 0.0001);
		assertEquals(0, simulationManager.getNetworkModel().getWanDownUtilization(), 0.0001);
	}

	@Test
	void withoutSharedWanTheCloudIsUsedDirectly() {
		DataCentersManager manager = newDataCentersManager(20, false);

		assertEquals(22, manager.getTopology().getGraph().vertexSet().size());
		assertThrows(IllegalArgumentException.class,
				() -> simulationManager.getNetworkModel().getWanUpUtilization());
	}

	@Test
	void topologyCanBeRegeneratedWithAnotherCreator() {
		DataCentersManager manager = newDataCentersManager(20, true);
		InfrastructureGraph previous = manager.getTopology();

		manager.createTopology(RecordingTopologyCreator.class);

		assertNotSame(previous, manager.getTopology());
		assertTrue(manager.getTopology().getGraph().vertexSet().isEmpty());
	}

	@Test
	void gettersReturnTheSameInstances() {
		DataCentersManager manager = newDataCentersManager(10, true);

		assertSame(manager.getComputingNodesGenerator(), manager.getComputingNodesGenerator());
		assertSame(manager.getTopology(), manager.getTopology());
	}

	@Test
	void classesWithoutTheExpectedReflectiveConstructorAreRejected() {
		createSimulationManager(1);
		SimulationParameters.useOneSharedWanLink = true;
		attachDefaultNetworkModel();
		assertThrows(NullPointerException.class, () -> new DataCentersManager(simulationManager,
				DefaultMobilityModel.class, DefaultComputingNode.class, ComputingNodesGenerator.class,
				DefaultTopologyCreator.class));

		createSimulationManager(1);
		SimulationParameters.useOneSharedWanLink = true;
		attachDefaultNetworkModel();
		assertThrows(NullPointerException.class, () -> new DataCentersManager(simulationManager,
				DefaultMobilityModel.class, DefaultComputingNode.class, DefaultComputingNodesGenerator.class,
				TopologyCreator.class));
	}
}
