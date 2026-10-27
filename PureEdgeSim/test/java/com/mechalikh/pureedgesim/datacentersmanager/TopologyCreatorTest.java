package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the state and template methods of the abstract {@link TopologyCreator}.
 */
class TopologyCreatorTest extends DatacentersTestSupport {

	private DefaultComputingNodesGenerator generator;
	private RecordingTopologyCreator creator;

	@BeforeEach
	void setUp() {
		createSimulationManager(5);
		generator = newGenerator();
		creator = new RecordingTopologyCreator(simulationManager, generator);
	}

	@Test
	void constructorWiresManagerGeneratorAndAnEmptyTopology() {
		assertSame(simulationManager, creator.getSimulationManager());
		assertNotNull(creator.getTopology());
		assertTrue(creator.getTopology().getGraph().vertexSet().isEmpty());
	}

	@Test
	void generateTopologyGraphIsADelegatedTemplateMethod() {
		assertFalse(creator.generated);
		creator.generateTopologyGraph();
		assertTrue(creator.generated);
	}
}
