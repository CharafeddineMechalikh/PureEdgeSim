package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.distribution.Randomness;

class ParametersParserTest extends AbstractTest {

	@BeforeEach
	void setUp() throws Exception {
		TempSettingsFiles.resetDirectory();
	}

	@AfterEach
	void cleanUp() throws Exception {
		TempSettingsFiles.resetDirectory();
		Randomness.reset();
	}

	@Test
	void parsesTheFastReferenceProperties() {
		assertTrue(new ParametersParser(SETTINGS_FOLDER + FAST_SETTINGS).parse());

		assertEquals(60.0, SimulationParameters.simulationDuration, 0.0001);
		assertEquals(100, SimulationParameters.batchSize);
		assertFalse(SimulationParameters.parallelism_enabled);
		assertEquals(1.0, SimulationParameters.updateInterval, 0.0001);
		assertEquals(0, SimulationParameters.pauseLength);
		assertFalse(SimulationParameters.displayRealTimeCharts);
		assertTrue(SimulationParameters.autoCloseRealTimeCharts);
		assertEquals(30.0, SimulationParameters.chartsUpdateInterval, 0.0001);
		assertFalse(SimulationParameters.saveCharts);
		assertEquals(200, SimulationParameters.simulationMapLength);
		assertEquals(200, SimulationParameters.simulationMapWidth);
		assertEquals(50, SimulationParameters.edgeDevicesRange);
		assertEquals(200, SimulationParameters.edgeDataCentersRange);
		assertFalse(SimulationParameters.enableRegistry);
		assertEquals("CACHE", SimulationParameters.registryMode);
		assertFalse(SimulationParameters.enableOrchestrators);
		assertEquals("CLOUD", SimulationParameters.deployOrchestrators);
		assertFalse(SimulationParameters.waitForAllTasksToFinish);
		assertFalse(SimulationParameters.saveLog);
		assertFalse(SimulationParameters.cleanOutputFolder);
		assertFalse(SimulationParameters.deepLoggingEnabled);
		assertEquals(20, SimulationParameters.minNumberOfEdgeDevices);
		assertEquals(20, SimulationParameters.maxNumberOfEdgeDevices);
		assertEquals(100, SimulationParameters.edgeDevicesIncrementationStepSize);
		assertTrue(SimulationParameters.realisticNetworkModel);
		assertEquals(1.0, SimulationParameters.networkUpdateInterval, 0.0001);
		assertTrue(SimulationParameters.useOneSharedWanLink);

		assertEquals(20_000_000.0, SimulationParameters.wanBandwidthBitsPerSecond, 0.0001);
		assertEquals(0.06, SimulationParameters.wanLatency, 0.0001);
		assertEquals(46.7 * 2.7777777777778e-13, SimulationParameters.wanWattHourPerBit, 1e-20);
		assertEquals(1_000_000_000.0, SimulationParameters.manBandwidthBitsPerSecond, 0.0001);
		assertEquals(0.01, SimulationParameters.manLatency, 0.0001);
		assertEquals(0.0, SimulationParameters.manWattHourPerBit, 1e-20);
		assertEquals(1_300_000_000.0, SimulationParameters.wifiBandwidthBitsPerSecond, 0.0001);
		assertEquals(283.17 * 2.7777777777778e-13, SimulationParameters.wifiDeviceTransmissionWattHourPerBit, 1e-20);
		assertEquals(137.01 * 2.7777777777778e-13, SimulationParameters.wifiDeviceReceptionWattHourPerBit, 1e-20);
		assertEquals(23.8 * 2.7777777777778e-13,
				SimulationParameters.wifiAccessPointTransmissionWattHourPerBit, 1e-20);
		assertEquals(23.8 * 2.7777777777778e-13,
				SimulationParameters.wifiAccessPointReceptionWattHourPerBit, 1e-20);
		assertEquals(0.005, SimulationParameters.wifiLatency, 0.0001);
		assertEquals(1_000_000_000.0, SimulationParameters.ethernetBandwidthBitsPerSecond, 0.0001);
		assertEquals(40000 * 2.7777777777778e-13, SimulationParameters.ethernetWattHourPerBit, 1e-20);
		assertEquals(0.002, SimulationParameters.ethernetLatency, 0.0001);
		assertEquals(100_000_000.0, SimulationParameters.cellularBandwidthBitsPerSecond, 0.0001);
		assertEquals(438.4 * 2.7777777777778e-13,
				SimulationParameters.cellularDeviceTransmissionWattHourPerBit, 1e-20);
		assertEquals(51.97 * 2.7777777777778e-13,
				SimulationParameters.cellularDeviceReceptionWattHourPerBit, 1e-20);
		assertEquals(6200 * 2.7777777777778e-13,
				SimulationParameters.cellularBaseStationWattHourPerBitUpLink, 1e-20);
		assertEquals(20500 * 12.7777777777778e-13,
				SimulationParameters.cellularBaseStationWattHourPerBitDownLink, 1e-19);
		assertEquals(0.03, SimulationParameters.cellularLatency, 0.0001);

		assertArrayEquals(new String[] { "ALL" }, SimulationParameters.orchestrationArchitectures);
		assertArrayEquals(new String[] { "TRADE_OFF" }, SimulationParameters.orchestrationAlgorithms);
	}

	@Test
	void parsesBooleanAndTextFlags() throws Exception {
		Map<String, String> overrides = new LinkedHashMap<>();
		overrides.put("parallel_simulation", "true");
		overrides.put("display_real_time_charts", "true");
		overrides.put("auto_close_real_time_charts", "false");
		overrides.put("save_charts", "true");
		overrides.put("enable_registry", "true");
		overrides.put("registry_mode", " PULL ");
		overrides.put("enable_orchestrators", "true");
		overrides.put("deploy_orchestrator", " MIST ");
		overrides.put("wait_for_all_tasks", "true");
		overrides.put("save_log_file", "true");
		overrides.put("clear_output_folder", "true");
		overrides.put("deep_log_enabled", "true");
		overrides.put("realistic_network_model", "false");
		overrides.put("one_shared_wan_network", "false");
		Path file = TempSettingsFiles.propertiesWithOverrides(overrides, "flags.properties");

		assertTrue(new ParametersParser(file.toString()).parse());
		assertTrue(SimulationParameters.parallelism_enabled);
		assertTrue(SimulationParameters.displayRealTimeCharts);
		assertFalse(SimulationParameters.autoCloseRealTimeCharts);
		assertTrue(SimulationParameters.saveCharts);
		assertTrue(SimulationParameters.enableRegistry);
		assertEquals("PULL", SimulationParameters.registryMode);
		assertTrue(SimulationParameters.enableOrchestrators);
		assertEquals("MIST", SimulationParameters.deployOrchestrators);
		assertTrue(SimulationParameters.waitForAllTasksToFinish);
		assertTrue(SimulationParameters.saveLog);
		assertTrue(SimulationParameters.cleanOutputFolder);
		assertTrue(SimulationParameters.deepLoggingEnabled);
		assertFalse(SimulationParameters.realisticNetworkModel);
		assertFalse(SimulationParameters.useOneSharedWanLink);
	}

	@Test
	void numericValuesAreTrimmedBeforeParsing() throws Exception {
		Path file = TempSettingsFiles.propertiesWith("length", " 42 ");
		assertTrue(new ParametersParser(file.toString()).parse());
		assertEquals(42, SimulationParameters.simulationMapLength);
	}

	@Test
	void returnsFalseWhenTheFileDoesNotExist() {
		assertFalse(new ParametersParser(TempSettingsFiles.DIR.resolve("missing.properties").toString()).parse());
	}

	@Test
	void rejectsAllOutOfRangeValues() {
		String[][] invalid = { { "simulation_time", "0" }, { "batch_size", "0" }, { "update_interval", "0.001" },
				{ "pause_length", "-1" }, { "charts_update_interval", "0.001" }, { "charts_update_interval", "61" },
				{ "length", "0" }, { "width", "0" }, { "edge_devices_range", "0" },
				{ "edge_datacenters_coverage", "0" }, { "min_number_of_edge_devices", "0" },
				{ "max_number_of_edge_devices", "0" }, { "edge_device_counter_size", "0" },
				{ "network_update_interval", "0.0001" }, { "wan_bandwidth", "0" }, { "wan_latency", "-1" },
				{ "wan_nanojoules_per_bit", "-1" }, { "man_bandwidth", "0" }, { "man_latency", "-1" },
				{ "man_nanojoules_per_bit", "-1" }, { "wifi_bandwidth", "0" },
				{ "wifi_device_transmission_nanojoules_per_bit", "-1" },
				{ "wifi_device_reception_nanojoules_per_bit", "-1" },
				{ "wifi_access_point_transmission_nanojoules_per_bit", "-1" },
				{ "wifi_access_point_reception_nanojoules_per_bit", "-1" }, { "wifi_latency", "-1" },
				{ "ethernet_bandwidth", "0" }, { "ethernet_nanojoules_per_bit", "-1" }, { "ethernet_latency", "-1" },
				{ "cellular_bandwidth", "0" }, { "cellular_device_transmission_nanojoules_per_bit", "-1" },
				{ "cellular_device_reception_nanojoules_per_bit", "-1" },
				{ "cellular_base_station_nanojoules_per_bit_up_link", "-1" },
				{ "cellular_base_station_nanojoules_per_bit_down_link", "-1" }, { "cellular_latency", "-1" } };

		Executable[] checks = new Executable[invalid.length];
		for (int i = 0; i < invalid.length; i++) {
			final String key = invalid[i][0];
			final String value = invalid[i][1];
			checks[i] = () -> {
				Path file = TempSettingsFiles.propertiesWith(key, value);
				IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
						() -> new ParametersParser(file.toString()).parse());
				assertTrue(exception.getMessage().contains(key), "message should mention " + key);
			};
		}
		assertAll("all simulation properties must be validated", checks);
	}

	@Test
	void rejectsMinimumNumberOfDevicesSuperiorToMaximum() throws Exception {
		Path file = TempSettingsFiles.propertiesWithTwo("min_number_of_edge_devices", "30",
				"max_number_of_edge_devices", "20");
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ParametersParser(file.toString()).parse());
		assertTrue(exception.getMessage().contains("superior to the max number"));
	}

	@Test
	void rejectsMissingMandatoryProperties() throws Exception {
		Path file = TempSettingsFiles.propertiesWithout("wan_bandwidth");
		assertThrows(NullPointerException.class, () -> new ParametersParser(file.toString()).parse());
	}

	@Test
	void rejectsNonNumericProperties() throws Exception {
		Path file = TempSettingsFiles.propertiesWith("length", "not-a-number");
		assertThrows(NumberFormatException.class, () -> new ParametersParser(file.toString()).parse());
	}

	@Test
	void rejectsEmptyNumericProperties() throws Exception {
		Path file = TempSettingsFiles.propertiesWith("width", "");
		assertThrows(NumberFormatException.class, () -> new ParametersParser(file.toString()).parse());
	}

	// ------------------------------------------------------------------
	// The random seed
	// ------------------------------------------------------------------

	private double[] drawFiveValues() {
		Random random = Randomness.getRandom();
		double[] values = new double[5];
		for (int i = 0; i < values.length; i++)
			values[i] = random.nextDouble();
		return values;
	}

	@Test
	void aConfiguredSeedMakesTheDrawsReproducible() throws Exception {
		Path file = TempSettingsFiles.propertiesWith("random_seed", "12345");
		assertTrue(new ParametersParser(file.toString()).parse());
		assertTrue(Randomness.isSeeded());
		assertEquals(12345, Randomness.getSeed());
		double[] first = drawFiveValues();

		// Reparsing the same scenario restarts the stream from the same place.
		assertTrue(new ParametersParser(file.toString()).parse());
		assertArrayEquals(first, drawFiveValues(), 0.0);
	}

	@Test
	void twoDifferentSeedsGiveTwoDifferentStreams() throws Exception {
		assertTrue(new ParametersParser(TempSettingsFiles.propertiesWith("random_seed", "1").toString()).parse());
		double[] withSeedOne = drawFiveValues();

		assertTrue(new ParametersParser(TempSettingsFiles.propertiesWith("random_seed", "2").toString()).parse());
		double[] withSeedTwo = drawFiveValues();

		assertFalse(Arrays.equals(withSeedOne, withSeedTwo));
	}

	@Test
	void anEmptySeedKeepsTheNonDeterministicGenerator() throws Exception {
		Randomness.setSeed(99);
		Path file = TempSettingsFiles.propertiesWith("random_seed", "");
		assertTrue(new ParametersParser(file.toString()).parse());
		assertFalse(Randomness.isSeeded(), "an empty random_seed must not freeze the generator");
	}

	@Test
	void aMissingSeedClearsTheOneLeftByThePreviousScenario() throws Exception {
		Randomness.setSeed(7);
		Path file = TempSettingsFiles.propertiesWithout("random_seed");
		assertTrue(new ParametersParser(file.toString()).parse());
		assertFalse(Randomness.isSeeded());
		assertEquals(0, Randomness.getSeed());
	}

	@Test
	void aNegativeSeedIsAccepted() throws Exception {
		assertTrue(new ParametersParser(TempSettingsFiles.propertiesWith("random_seed", "-1").toString()).parse());
		assertTrue(Randomness.isSeeded());
		assertEquals(-1, Randomness.getSeed());
	}

	@Test
	void rejectsANonNumericSeed() throws Exception {
		Path file = TempSettingsFiles.propertiesWith("random_seed", "42.5");
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ParametersParser(file.toString()).parse());
		assertTrue(exception.getMessage().contains("random_seed"), exception.getMessage());
		assertTrue(exception.getMessage().contains("whole number"), exception.getMessage());
	}
}
