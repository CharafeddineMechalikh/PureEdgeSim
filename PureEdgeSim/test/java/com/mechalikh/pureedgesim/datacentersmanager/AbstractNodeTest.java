package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;

/**
 * Tests the behaviour inherited from {@link AbstractNode}: type/name accessors,
 * orchestrator wiring, task generation flags and the status update event loop.
 * A {@link DefaultComputingNode} is used as the concrete node.
 */
class AbstractNodeTest extends DatacentersTestSupport {

	private DefaultComputingNode node;

	@BeforeEach
	void setUp() {
		createSimulationManager(1);
		node = new DefaultComputingNode(simulationManager, 1000, 2, 100, 100);
	}

	@Test
	void typeAndNameCanBeChanged() {
		assertEquals(null, node.getType());
		node.setType(TYPES.EDGE_DATACENTER);
		assertEquals(TYPES.EDGE_DATACENTER, node.getType());
		node.setName("dc1");
		assertEquals("dc1", node.getName());
	}

	@Test
	void orchestratorFlagsAreWired() {
		assertFalse(node.isOrchestrator());
		node.setAsOrchestrator(true);
		assertTrue(node.isOrchestrator());
		// setAsOrchestrator also makes the node its own orchestrator.
		node.setAsOrchestrator(false);
		assertFalse(node.isOrchestrator());

		ComputingNode orchestrator = new DefaultComputingNode(simulationManager, 1000, 1, 100, 100);
		assertFalse(orchestrator.isOrchestrator());
		node.setOrchestrator(orchestrator);
		assertTrue(orchestrator.isOrchestrator(), "the target must be flagged as orchestrator");
		SimulationParameters.enableOrchestrators = true;
		assertSame(orchestrator, node.getOrchestrator());
	}

	@Test
	void taskGenerationFlagCanBeToggled() {
		assertFalse(node.isGeneratingTasks());
		node.enableTaskGeneration(true);
		assertTrue(node.isGeneratingTasks());
		node.enableTaskGeneration(false);
		assertFalse(node.isGeneratingTasks());
	}

	@Test
	void onSimulationStartSchedulesAnImmediateStatusUpdate() {
		node.onSimulationStart();
		assertEquals(1, simulation.getEventsQueue().size());
		Event event = simulation.getEventsQueue().first();
		assertEquals(AbstractNode.UPDATE_STATUS, event.getTag());
		assertEquals(0.0, event.getTime());
		assertSame(node, event.getSimEntity());
	}

	@Test
	void updateStatusEventRefreshesEnergyAndReschedules() {
		EnergyModelComputingNode energyModel = mock(EnergyModelComputingNode.class);
		when(energyModel.isBatteryPowered()).thenReturn(false);
		node.setEnergyModel(energyModel);

		node.processEvent(new Event(node, 0.0, AbstractNode.UPDATE_STATUS));

		verify(energyModel).updateStaticEnergyConsumption();
		verify(energyModel, never()).getBatteryLevelWattHour();
		// The node schedules the next status update one interval later.
		assertEquals(1, simulation.getEventsQueue().size());
		Event next = simulation.getEventsQueue().first();
		assertEquals(AbstractNode.UPDATE_STATUS, next.getTag());
		assertEquals(SimulationParameters.updateInterval, next.getTime());
	}

	@Test
	void unrelatedEventsAreIgnored() {
		EnergyModelComputingNode energyModel = mock(EnergyModelComputingNode.class);
		node.setEnergyModel(energyModel);

		node.processEvent(new Event(node, 0.0, 999));

		verify(energyModel, times(0)).updateStaticEnergyConsumption();
		assertTrue(simulation.getEventsQueue().isEmpty());
	}

	@Test
	void linkOrientationsAreExposedByTheInterface() {
		assertEquals(3, LinkOrientation.values().length);
		assertEquals(LinkOrientation.UP_LINK, LinkOrientation.valueOf("UP_LINK"));
	}
}
