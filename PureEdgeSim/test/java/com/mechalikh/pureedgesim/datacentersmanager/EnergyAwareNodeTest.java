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

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.simulationengine.Event;

/**
 * Tests the energy/life-cycle logic of {@link EnergyAwareNode}: static
 * consumption bookkeeping, battery depletion and the dead state.
 */
class EnergyAwareNodeTest extends DatacentersTestSupport {

	private DefaultComputingNode node;
	private EnergyModelComputingNode energyModel;

	@BeforeEach
	void setUp() {
		createSimulationManager(1);
		node = new DefaultComputingNode(simulationManager, 10, 2, 100, 100);
		energyModel = mock(EnergyModelComputingNode.class);
		node.setEnergyModel(energyModel);
	}

	private void sendStatusUpdate() {
		node.processEvent(new Event(node, simulationManager.getSimulation().clock(), AbstractNode.UPDATE_STATUS));
	}

	@Test
	void freshNodeIsAliveWithTheNullEnergyModel() {
		DefaultComputingNode fresh = new DefaultComputingNode(simulationManager, 1, 1, 10, 10);
		assertFalse(fresh.isDead());
		assertEquals(0, fresh.getDeathTime(), 0.0001);
		assertSame(EnergyModelComputingNode.NULL, fresh.getEnergyModel());
	}

	@Test
	void nonBatteryPoweredNodeOnlyUpdatesStaticConsumption() {
		when(energyModel.isBatteryPowered()).thenReturn(false);

		sendStatusUpdate();

		verify(energyModel).updateStaticEnergyConsumption();
		verify(energyModel, never()).getBatteryLevelWattHour();
		assertFalse(node.isDead());
	}

	@Test
	void batteryPoweredNodeWithRemainingChargeStaysAlive() {
		when(energyModel.isBatteryPowered()).thenReturn(true);
		when(energyModel.getBatteryLevelWattHour()).thenReturn(12.5);

		sendStatusUpdate();

		verify(energyModel).updateStaticEnergyConsumption();
		verify(energyModel).getBatteryLevelWattHour();
		assertFalse(node.isDead());
	}

	@Test
	void depletedBatteryKillsTheNodeAtTheCurrentClock() {
		when(energyModel.isBatteryPowered()).thenReturn(true);
		when(energyModel.getBatteryLevelWattHour()).thenReturn(0.0);
		setClock(42.5);

		sendStatusUpdate();

		assertTrue(node.isDead());
		assertEquals(42.5, node.getDeathTime(), 0.0001);
	}

	@Test
	void deadNodeNoLongerUpdatesItsEnergyModel() {
		when(energyModel.isBatteryPowered()).thenReturn(true);
		when(energyModel.getBatteryLevelWattHour()).thenReturn(0.0);

		sendStatusUpdate();
		assertTrue(node.isDead());

		sendStatusUpdate();

		verify(energyModel, times(1)).updateStaticEnergyConsumption();
		verify(energyModel, times(1)).getBatteryLevelWattHour();
	}

	@Test
	void deathStateCanBeSetDirectly() {
		node.setDeath(true, 3.25);
		assertTrue(node.isDead());
		assertEquals(3.25, node.getDeathTime(), 0.0001);

		node.setDeath(false, 0);
		assertFalse(node.isDead());
		assertEquals(0, node.getDeathTime(), 0.0001);
	}
}
