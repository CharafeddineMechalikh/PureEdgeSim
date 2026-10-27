package com.mechalikh.pureedgesim.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

class EnergyModelComputingNodeTest extends AbstractTest {

	@BeforeEach
	void setUp() {
		SimulationParameters.updateInterval = 2;
		SimulationParameters.cellularDeviceTransmissionWattHourPerBit = 10;
		SimulationParameters.cellularDeviceReceptionWattHourPerBit = 20;
		SimulationParameters.wifiDeviceTransmissionWattHourPerBit = 30;
		SimulationParameters.wifiDeviceReceptionWattHourPerBit = 40;
		SimulationParameters.ethernetWattHourPerBit = 100;
	}

	@Test
	void constructorAndSimpleAccessors() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);

		assertEquals(100, model.getMaxActiveConsumption());
		assertEquals(20, model.getIdleConsumption());
		assertEquals(0, model.getCpuEnergyConsumption());
		assertEquals(0, model.getTotalEnergyConsumption());
		assertEquals(0, model.getBatteryCapacity());
		assertFalse(model.isBatteryPowered());
		assertNull(model.getConnectivityType());

		model.setMaxActiveConsumption(150);
		model.setIdleConsumption(25);
		model.setBatteryCapacity(5000);
		model.setBattery(true);
		model.setConnectivityType("wifi");
		assertEquals(150, model.getMaxActiveConsumption());
		assertEquals(25, model.getIdleConsumption());
		assertEquals(5000, model.getBatteryCapacity());
		assertTrue(model.isBatteryPowered());
		assertEquals("wifi", model.getConnectivityType());
	}

	@Test
	void updateStaticEnergyConsumptionUsesIdleConsumptionAndInterval() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 3600);

		model.updateStaticEnergyConsumption();

		assertEquals(2.0, model.getCpuEnergyConsumption());
		assertEquals(2.0, model.getTotalEnergyConsumption());
	}

	@Test
	void updateDynamicEnergyConsumptionScalesWithTaskLengthAndMips() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);

		model.updateDynamicEnergyConsumption(10, 80);

		assertEquals(80.0 / 3600.0 * 10.0 / 80.0, model.getCpuEnergyConsumption(), 1e-12);
	}

	@Test
	void totalEnergyConsumptionAddsCpuAndNetworkConsumption() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 3600);
		model.setConnectivityType("cellular");

		model.updateStaticEnergyConsumption();
		model.updatewirelessEnergyConsumption(100, EnergyModelComputingNode.TRANSMISSION);

		assertEquals(2.0 + 100 * 10, model.getTotalEnergyConsumption());
	}

	@Test
	void batteryLevelIsMinusOneWhenNotBatteryPowered() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);
		model.setBatteryCapacity(1000);

		assertEquals(-1, model.getBatteryLevelWattHour());
		assertEquals(-0.1, model.getBatteryLevelPercentage(), 1e-12);
	}

	@Test
	void batteryLevelIsZeroWhenConsumptionExceedsCapacity() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 3600);
		model.setBattery(true);
		model.setBatteryCapacity(1);
		model.updateStaticEnergyConsumption();

		assertEquals(0, model.getBatteryLevelWattHour());
		assertEquals(0, model.getBatteryLevelPercentage());
	}

	@Test
	void batteryLevelAndPercentageAreComputedFromCapacity() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 3600);
		model.setBattery(true);
		model.setBatteryCapacity(1000);
		model.updateStaticEnergyConsumption();

		assertEquals(998, model.getBatteryLevelWattHour());
		assertEquals(99.8, model.getBatteryLevelPercentage(), 1e-12);
	}

	@Test
	void initialBatteryPercentageChangesAvailableBattery() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 3600);
		model.setBattery(true);
		model.setBatteryCapacity(1000);
		model.setIntialBatteryPercentage(50);
		model.updateStaticEnergyConsumption();

		assertEquals(498, model.getBatteryLevelWattHour());
		assertEquals(49.8, model.getBatteryLevelPercentage(), 1e-12);
	}

	@Test
	void cellularConnectivityUsesCellularEnergyPerBit() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);
		model.setConnectivityType("cellular");

		model.updatewirelessEnergyConsumption(100, EnergyModelComputingNode.TRANSMISSION);

		assertEquals(100 * 10, model.getTotalEnergyConsumption());
	}

	@Test
	void wifiConnectivityUsesWifiEnergyPerBitForReception() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);
		model.setConnectivityType("wifi");

		model.updatewirelessEnergyConsumption(100, EnergyModelComputingNode.RECEPTION);

		assertEquals(100 * 40, model.getTotalEnergyConsumption());
	}

	@Test
	void wifiConnectivityUsesWifiEnergyPerBitForTransmission() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);
		model.setConnectivityType("wifi");

		model.updatewirelessEnergyConsumption(100, EnergyModelComputingNode.TRANSMISSION);

		assertEquals(100 * 30, model.getTotalEnergyConsumption());
	}

	@Test
	void otherConnectivitySplitsEthernetEnergyBetweenUpAndDown() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);
		model.setConnectivityType("ethernet");

		model.updatewirelessEnergyConsumption(100, EnergyModelComputingNode.RECEPTION);
		assertEquals(100 * 50, model.getTotalEnergyConsumption());

		model.updatewirelessEnergyConsumption(100, EnergyModelComputingNode.TRANSMISSION);
		assertEquals(200 * 50, model.getTotalEnergyConsumption());
	}

	@Test
	void nullConnectivityFallsBackToEthernetModel() {
		EnergyModelComputingNode model = new EnergyModelComputingNode(100, 20);
		model.setConnectivityType(null);

		model.updatewirelessEnergyConsumption(10, EnergyModelComputingNode.TRANSMISSION);

		assertEquals(10 * 50, model.getTotalEnergyConsumption());
	}

	@Test
	void energyFlagsHaveExpectedValues() {
		assertEquals(0, EnergyModelComputingNode.TRANSMISSION);
		assertEquals(1, EnergyModelComputingNode.RECEPTION);
	}
}
