package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;

/**
 * Tests the state exposed by the abstract {@link ComputingNodesGenerator}
 * through the default generator.
 */
class ComputingNodesGeneratorTest extends DatacentersTestSupport {

	private DefaultComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		createSimulationManager(20);
		generator = newGenerator();
	}

	@Test
	void constructorCreatesEmptyListsAndKeepsTheManager() {
		assertSame(simulationManager, generator.getSimulationManager());
		assertNotNull(generator.getOrchestratorsList());
		assertNotNull(generator.getAllNodesList());
		assertNotNull(generator.getAllNodesListSensorsExcluded());
		assertNotNull(generator.getMistOnlyList());
		assertNotNull(generator.getMistOnlyListSensorsExcluded());
		assertNotNull(generator.getMistAndCloudListSensorsExcluded());
		assertNotNull(generator.getMistAndEdgeListSensorsExcluded());
		assertNotNull(generator.getEdgeOnlyList());
		assertNotNull(generator.getCloudOnlyList());
		assertNotNull(generator.getEdgeAndCloudList());

		assertTrue(generator.getOrchestratorsList().isEmpty());
		assertTrue(generator.getAllNodesList().isEmpty());
		assertTrue(generator.getMistOnlyList().isEmpty());
		assertTrue(generator.getEdgeOnlyList().isEmpty());
		assertTrue(generator.getCloudOnlyList().isEmpty());
		assertTrue(generator.getEdgeAndCloudList().isEmpty());
	}

	@Test
	void generatorKeepsCustomMobilityAndNodeClasses() {
		DefaultComputingNodesGenerator custom = new DefaultComputingNodesGenerator(simulationManager,
				DefaultMobilityModel.class, DefaultComputingNode.class);
		assertSame(simulationManager, custom.getSimulationManager());
	}

	@Test
	void generatedListsAreBackedByTheGenerator() {
		generator.generateDatacentersAndDevices();

		assertEquals(22, generator.getAllNodesList().size());
		assertEquals(2, generator.getEdgeAndCloudList().size());
		assertEquals(1, generator.getCloudOnlyList().size());
		assertEquals(1, generator.getEdgeOnlyList().size());
	}
}
