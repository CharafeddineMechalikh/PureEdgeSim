package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;

/**
 * Parses the simulation parameters properties file and loads its values into
 * {@link SimulationParameters}. Missing or invalid values fail immediately with
 * an explanatory exception.
 * <p>
 * The optional {@code random_seed} property is applied to {@link Randomness},
 * which every module of the simulation draws its values from.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class ParametersParser extends FileParserAbstract {

	/** Every property name the parser reads, used to point out unknown keys. */
	private static final List<String> KNOWN_PROPERTIES = List.of("random_seed", "simulation_time", "batch_size",
			"parallel_simulation", "update_interval", "pause_length", "display_real_time_charts",
			"auto_close_real_time_charts", "charts_update_interval", "save_charts", "length", "width",
			"edge_devices_range", "edge_datacenters_coverage", "enable_registry", "registry_mode",
			"enable_orchestrators", "deploy_orchestrator", "wait_for_all_tasks", "save_log_file",
			"clear_output_folder", "deep_log_enabled", "min_number_of_edge_devices", "max_number_of_edge_devices",
			"edge_device_counter_size", "realistic_network_model", "network_update_interval", "one_shared_wan_network",
			"wan_bandwidth", "wan_latency", "wan_nanojoules_per_bit", "man_bandwidth", "man_latency",
			"man_nanojoules_per_bit", "wifi_bandwidth", "wifi_device_transmission_nanojoules_per_bit",
			"wifi_device_reception_nanojoules_per_bit", "wifi_access_point_transmission_nanojoules_per_bit",
			"wifi_access_point_reception_nanojoules_per_bit", "wifi_latency", "ethernet_bandwidth",
			"ethernet_nanojoules_per_bit", "ethernet_latency", "cellular_bandwidth",
			"cellular_device_transmission_nanojoules_per_bit", "cellular_device_reception_nanojoules_per_bit",
			"cellular_base_station_nanojoules_per_bit_up_link", "cellular_base_station_nanojoules_per_bit_down_link",
			"cellular_latency", "orchestration_architectures", "orchestration_algorithms");

	/**
	 * Creates a parser for the given properties file.
	 *
	 * @param file path to the simulation parameters file
	 */
	public ParametersParser(String file) {
		super(file);
	}

	/**
	 * Parses the properties file.
	 *
	 * @return {@code true} when the file was loaded successfully, {@code false}
	 *         when it could not be read
	 */
	@Override
	public boolean parse() {
		return checkSimulationProperties();
	}

	/**
	 * Loads and validates every simulation parameter. Any problem found in the
	 * file is reported once with the property name and the file path, and the
	 * parse fails without a stack trace.
	 *
	 * @return {@code true} when all properties were loaded successfully,
	 *         {@code false} when the file could not be read or holds an invalid
	 *         value
	 */
	protected boolean checkSimulationProperties() {
		SimLog.println("%s - Checking simulation properties file...",getClass().getSimpleName());
		boolean result = false;
		try (InputStream input = new FileInputStream(file)) {

			// Loading properties file.
			Properties prop = new Properties();
			prop.load(input);

			warnAboutUnknownProperties(prop);

			// Read first, so that every value generated later comes from the right stream.
			readRandomSeed(prop);

			// In seconds
			SimulationParameters.simulationDuration = (double) 60
					* assertDouble(prop, "simulation_time", value -> (value > 0), "> 0");

			SimulationParameters.batchSize = (int) assertDouble(prop, "batch_size", value -> (value > 0), "> 0");

			SimulationParameters.parallelism_enabled = assertBoolean(prop, "parallel_simulation");

			// In seconds
			SimulationParameters.updateInterval = assertDouble(prop, "update_interval", value -> (value >= 0.01),
					">= 0.01");

			// In seconds
			SimulationParameters.pauseLength = (int) assertDouble(prop, "pause_length", value -> (value >= 0), ">= 0");

			SimulationParameters.displayRealTimeCharts = assertBoolean(prop, "display_real_time_charts");
			SimulationParameters.autoCloseRealTimeCharts = assertBoolean(prop, "auto_close_real_time_charts");

			SimulationParameters.chartsUpdateInterval = assertDouble(prop, "charts_update_interval",
					value -> (value >= 0.01 && value <= 60), "between 0.01 and 60");

			SimulationParameters.saveCharts = assertBoolean(prop, "save_charts");

			// Meters
			SimulationParameters.simulationMapLength = (int) assertDouble(prop, "length", value -> (value > 0), "> 0");

			// Meters
			SimulationParameters.simulationMapWidth = (int) assertDouble(prop, "width", value -> (value > 0), "> 0");

			SimulationParameters.edgeDevicesRange = (int) assertDouble(prop, "edge_devices_range",
					value -> (value > 0), "> 0");
			SimulationParameters.edgeDataCentersRange = (int) assertDouble(prop, "edge_datacenters_coverage",
					value -> (value > 0), "> 0");

			SimulationParameters.enableRegistry = assertBoolean(prop, "enable_registry");
			SimulationParameters.registryMode = requiredProperty(prop, "registry_mode");
			SimulationParameters.enableOrchestrators = assertBoolean(prop, "enable_orchestrators");
			SimulationParameters.deployOrchestrators = requiredProperty(prop, "deploy_orchestrator");

			SimulationParameters.waitForAllTasksToFinish = assertBoolean(prop, "wait_for_all_tasks");
			SimulationParameters.saveLog = assertBoolean(prop, "save_log_file");
			SimulationParameters.cleanOutputFolder = assertBoolean(prop, "clear_output_folder");
			SimulationParameters.deepLoggingEnabled = assertBoolean(prop, "deep_log_enabled");

			SimulationParameters.minNumberOfEdgeDevices = (int) assertDouble(prop, "min_number_of_edge_devices",
					value -> (value > 0), "> 0");
			SimulationParameters.maxNumberOfEdgeDevices = (int) assertDouble(prop, "max_number_of_edge_devices",
					value -> (value > 0), "> 0");

			if (SimulationParameters.minNumberOfEdgeDevices > SimulationParameters.maxNumberOfEdgeDevices) {
				throw new IllegalArgumentException(getClass().getSimpleName()
						+ " - Error, the entered min number of edge devices is superior to the max number, check the "
						+ file + " file.");
			}

			SimulationParameters.edgeDevicesIncrementationStepSize = (int) assertDouble(prop,
					"edge_device_counter_size", value -> (value > 0), "> 0");

			SimulationParameters.realisticNetworkModel = assertBoolean(prop, "realistic_network_model");
			// Seconds
			SimulationParameters.networkUpdateInterval = assertDouble(prop, "network_update_interval",
					value -> (value >= 0.001), ">= 0.001");
			SimulationParameters.useOneSharedWanLink = assertBoolean(prop, "one_shared_wan_network");

			// Mbps to bits per second
			SimulationParameters.wanBandwidthBitsPerSecond = 1000000
					* assertDouble(prop, "wan_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.wanLatency = assertDouble(prop, "wan_latency", value -> (value >= 0), ">= 0");

			// Nanojoules per second (per bit) to Watt Hour (per bit)
			SimulationParameters.wanWattHourPerBit = 2.7777777777778e-13
					* assertDouble(prop, "wan_nanojoules_per_bit", value -> (value >= 0), ">= 0");

			SimulationParameters.manBandwidthBitsPerSecond = 1000000
					* assertDouble(prop, "man_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.manLatency = assertDouble(prop, "man_latency", value -> (value >= 0), ">= 0");
			SimulationParameters.manWattHourPerBit = 2.7777777777778e-13
					* assertDouble(prop, "man_nanojoules_per_bit", value -> (value >= 0), ">= 0");

			SimulationParameters.wifiBandwidthBitsPerSecond = 1000000
					* assertDouble(prop, "wifi_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.wifiDeviceTransmissionWattHourPerBit = 2.7777777777778e-13
					* assertDouble(prop, "wifi_device_transmission_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.wifiDeviceReceptionWattHourPerBit = 2.7777777777778e-13
					* assertDouble(prop, "wifi_device_reception_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.wifiAccessPointTransmissionWattHourPerBit = 2.7777777777778e-13 * assertDouble(
					prop, "wifi_access_point_transmission_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.wifiAccessPointReceptionWattHourPerBit = 2.7777777777778e-13 * assertDouble(prop,
					"wifi_access_point_reception_nanojoules_per_bit", value -> (value >= 0), ">= 0");

			SimulationParameters.wifiLatency = assertDouble(prop, "wifi_latency", value -> (value >= 0), ">= 0");
			SimulationParameters.ethernetBandwidthBitsPerSecond = 1000000
					* assertDouble(prop, "ethernet_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.ethernetWattHourPerBit = 2.7777777777778e-13
					* assertDouble(prop, "ethernet_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.ethernetLatency = assertDouble(prop, "ethernet_latency", value -> (value >= 0),
					">= 0");

			SimulationParameters.cellularBandwidthBitsPerSecond = 1000000
					* assertDouble(prop, "cellular_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.cellularDeviceTransmissionWattHourPerBit = 2.7777777777778e-13 * assertDouble(
					prop, "cellular_device_transmission_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.cellularDeviceReceptionWattHourPerBit = 2.7777777777778e-13
					* assertDouble(prop, "cellular_device_reception_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.cellularBaseStationWattHourPerBitUpLink = 2.7777777777778e-13 * assertDouble(
					prop, "cellular_base_station_nanojoules_per_bit_up_link", value -> (value >= 0), ">= 0");
			SimulationParameters.cellularBaseStationWattHourPerBitDownLink = 12.7777777777778e-13 * assertDouble(
					prop, "cellular_base_station_nanojoules_per_bit_down_link", value -> (value >= 0), ">= 0");
			SimulationParameters.cellularLatency = assertDouble(prop, "cellular_latency", value -> (value >= 0),
					">= 0");

			SimulationParameters.orchestrationArchitectures = requiredProperty(prop, "orchestration_architectures")
					.split(",");
			SimulationParameters.orchestrationAlgorithms = requiredProperty(prop, "orchestration_algorithms").split(",");

			result = true;
			SimLog.println("%s - Properties file successfully loaded!",getClass().getSimpleName());
		} catch (FileNotFoundException e) {
			SimLog.println("%s - Error, the simulation parameters file \"%s\" does not exist. Check the settings folder location.",
					getClass().getSimpleName(), file);
		} catch (IOException e) {
			SimLog.println("%s - Error, cannot read the file \"%s\": %s", getClass().getSimpleName(), file,
					e.getMessage());
		} catch (IllegalArgumentException e) {
			SimLog.println("%s", e.getMessage());
		}
		return result;

	}

	/**
	 * Reports the properties of the file that the parser never reads. A misspelled
	 * key would otherwise be ignored silently, and the simulation would run with
	 * an unintended value.
	 *
	 * @param prop the loaded properties
	 */
	protected void warnAboutUnknownProperties(Properties prop) {
		for (String key : new TreeSet<>(prop.stringPropertyNames())) {
			if (KNOWN_PROPERTIES.contains(key))
				continue;
			String suggestion = suggest(key, KNOWN_PROPERTIES);
			SimLog.println("%s - Warning, unknown property \"%s\" in the %s file%s", getClass().getSimpleName(), key,
					file, suggestion == null ? " is ignored." : ", did you mean \"" + suggestion + "\"?");
		}
	}

	/**
	 * Reads a property that must be present.
	 *
	 * @param prop      the loaded properties
	 * @param parameter the property name
	 * @return the trimmed value of the property
	 * @throws IllegalArgumentException naming the property and the file when the
	 *                                  property is missing or empty
	 */
	protected String requiredProperty(Properties prop, String parameter) {
		String value = prop.getProperty(parameter);
		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the property \"" + parameter
					+ "\" is missing from the " + file + " file.");
		return value.trim();
	}

	/**
	 * Reads a property that must be a boolean. The value is rejected when it is
	 * neither {@code true} nor {@code false}, so that a misspelled value never
	 * silently enables or disables a feature.
	 *
	 * @param prop      the loaded properties
	 * @param parameter the property name
	 * @return the parsed value
	 * @throws IllegalArgumentException naming the property, the value and the file
	 *                                  when the value is not a boolean
	 */
	protected boolean assertBoolean(Properties prop, String parameter) {
		String value = requiredProperty(prop, parameter);
		if (value.equalsIgnoreCase("true"))
			return true;
		if (value.equalsIgnoreCase("false"))
			return false;
		String suggestion = suggest(value, List.of("true", "false"));
		throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the value of \"" + parameter
				+ "\" must be \"true\" or \"false\", found \"" + value + "\". Check the " + file + " file"
				+ (suggestion == null ? "." : ", did you mean \"" + suggestion + "\"?"));
	}

	/**
	 * Reads the optional {@code random_seed} property and configures
	 * {@link Randomness} accordingly.
	 * <p>
	 * When the property is missing or empty, each run draws its own values, as in
	 * previous versions. When it holds a whole number, every generator of the
	 * simulation is derived from it, so that the same scenario produces the same
	 * results again. This is what makes a paper experiment reproducible: task sizes,
	 * arrival times, node capacities and positions all come from the seed.
	 *
	 * @param prop the loaded properties
	 * @throws IllegalArgumentException if the value is not a whole number
	 */
	protected void readRandomSeed(Properties prop) {
		String value = prop.getProperty("random_seed");
		if (value == null || value.trim().isEmpty()) {
			Randomness.reset();
			return;
		}
		long seed;
		try {
			seed = Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - Error, the value of \"random_seed\" must be a whole number, found \"" + value.trim() + "\".");
		}
		Randomness.setSeed(seed);
		SimLog.println("%s - Random values are reproducible, seed = %d", getClass().getSimpleName(), seed);
	}

	/**
	 * Reads a numeric property and checks it against a predicate. The error
	 * messages always name the property and the file, so that a broken value can
	 * be located without reading a stack trace.
	 *
	 * @param prop      the loaded properties
	 * @param parameter the property name
	 * @param p         the condition the value must satisfy
	 * @param message   description of the expected condition, used in the error
	 *                  message
	 * @return the parsed value
	 * @throws IllegalArgumentException if the property is missing, does not hold a
	 *                                  number, or does not satisfy the condition
	 */
	protected double assertDouble(Properties prop, String parameter, Predicate<Double> p, String message) {
		String value = requiredProperty(prop, parameter);
		double number;
		try {
			number = Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the value of \"" + parameter
					+ "\" must be a number, found \"" + value + "\". Check the " + file + " file.");
		}
		if (!p.test(number))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the value of \"" + parameter
					+ "\" must be " + message + ". Check the " + file + " file.");
		return number;
	}

}
