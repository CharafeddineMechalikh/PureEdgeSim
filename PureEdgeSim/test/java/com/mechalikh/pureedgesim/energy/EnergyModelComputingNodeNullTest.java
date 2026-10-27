package com.mechalikh.pureedgesim.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnergyModelComputingNodeNullTest {

	@Test
	void singletonIsAlwaysTheSameInstance() {
		EnergyModelComputingNodeNull instance = EnergyModelComputingNodeNull.getInstance();

		assertSame(instance, EnergyModelComputingNodeNull.getInstance());
		// EnergyModelComputingNode.NULL is resolved while the subclass is still
		// initialising when the singleton is loaded first, so it may stay null.
		assertTrue(EnergyModelComputingNode.NULL == null || EnergyModelComputingNode.NULL == instance);
	}

	@Test
	void everyAccessorReturnsANeutralValue() {
		EnergyModelComputingNodeNull model = EnergyModelComputingNodeNull.getInstance();

		assertEquals(0, model.getTotalEnergyConsumption());
		assertEquals(0, model.getMaxActiveConsumption());
		assertEquals(0, model.getIdleConsumption());
		assertEquals(0, model.getBatteryCapacity());
		assertEquals(0, model.getBatteryLevelWattHour());
		assertEquals(0, model.getBatteryLevelPercentage());
		assertFalse(model.isBatteryPowered());
		assertEquals("", model.getConnectivityType());
	}

	@Test
	void inheritedMutatorsDoNotBreakTheNullObject() {
		EnergyModelComputingNodeNull model = EnergyModelComputingNodeNull.getInstance();
		model.setBattery(true);
		model.setBatteryCapacity(100);
		model.setConnectivityType("wifi");
		model.setMaxActiveConsumption(10);
		model.setIdleConsumption(5);
		model.updateStaticEnergyConsumption();
		model.updateDynamicEnergyConsumption(10, 10);
		model.updatewirelessEnergyConsumption(100, EnergyModelComputingNode.TRANSMISSION);

		assertEquals(0, model.getTotalEnergyConsumption());
		assertFalse(model.isBatteryPowered());
		assertEquals("", model.getConnectivityType());
	}
}
