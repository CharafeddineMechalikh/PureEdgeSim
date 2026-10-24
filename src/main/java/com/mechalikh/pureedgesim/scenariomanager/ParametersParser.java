package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Predicate;

import com.mechalikh.pureedgesim.simulationmanager.SimLog;

public class ParametersParser extends FileParserAbstract {

	public ParametersParser(String file) {
		super(file);
	}

	@Override
	public boolean parse() {
		return checkSimulationProperties();
	}

	protected boolean checkSimulationProperties() {
		SimLog.println("%s - Checking simulation properties file...",getClass().getSimpleName());
		boolean result = false;
		try (InputStream input = new FileInputStream(file)) {

			// Loading properties file.
			Properties prop = new Properties();
			prop.load(input);

			// In seconds
			SimulationParameters.simulationDuration = (double) 60
					* assertDouble(prop, "simulation_time", value -> (value > 0), "> 0");
			
			SimulationParameters.batchSize = (int) assertDouble(prop, "batch_size", value -> (value > 0), "> 0");
			
			SimulationParameters.parallelism_enabled = Boolean.parseBoolean(prop.getProperty("parallel_simulation").trim());

			// In seconds
			SimulationParameters.updateInterval = assertDouble(prop, "update_interval", value -> (value >= 0.01),
					">= 0.01");
			
			// In seconds
			SimulationParameters.pauseLength = (int) assertDouble(prop, "pause_length", value -> (value >= 0), ">= 0");

			SimulationParameters.displayRealTimeCharts = Boolean
					.parseBoolean(prop.getProperty("display_real_time_charts").trim());
			SimulationParameters.autoCloseRealTimeCharts = Boolean
					.parseBoolean(prop.getProperty("auto_close_real_time_charts").trim());

			SimulationParameters.chartsUpdateInterval = assertDouble(prop, "charts_update_interval",
					value -> (value >= 0.01 && value <= 60), "between 0.01 and 60");

			SimulationParameters.saveCharts = Boolean.parseBoolean(prop.getProperty("save_charts").trim());

			// Meters
			SimulationParameters.simulationMapLength = (int) assertDouble(prop, "length", value -> (value > 0), "> 0");

			// Meters
			SimulationParameters.simulationMapWidth = (int) assertDouble(prop, "width", value -> (value > 0), "> 0");

			SimulationParameters.edgeDevicesRange = (int) assertDouble(prop, "edge_devices_range",
					value -> (value > 0), "> 0");
			SimulationParameters.edgeDataCentersRange = (int) assertDouble(prop, "edge_datacenters_coverage",
					value -> (value > 0), "> 0");

			SimulationParameters.enableRegistry = Boolean.parseBoolean(prop.getProperty("enable_registry").trim());
			SimulationParameters.registryMode = prop.getProperty("registry_mode").trim();
			SimulationParameters.enableOrchestrators = Boolean
					.parseBoolean(prop.getProperty("enable_orchestrators").trim());
			SimulationParameters.deployOrchestrators = prop.getProperty("deploy_orchestrator").trim();

			SimulationParameters.waitForAllTasksToFinish = Boolean.parseBoolean(prop.getProperty("wait_for_all_tasks").trim());
			SimulationParameters.saveLog = Boolean.parseBoolean(prop.getProperty("save_log_file").trim());
			SimulationParameters.cleanOutputFolder = Boolean
					.parseBoolean(prop.getProperty("clear_output_folder").trim());
			SimulationParameters.deepLoggingEnabled = Boolean.parseBoolean(prop.getProperty("deep_log_enabled").trim());

			SimulationParameters.minNumberOfEdgeDevices = (int) assertDouble(prop, "min_number_of_edge_devices",
					value -> (value > 0), "> 0");
			SimulationParameters.maxNumberOfEdgeDevices = (int) assertDouble(prop, "max_number_of_edge_devices",
					value -> (value > 0), "> 0");

			if (SimulationParameters.minNumberOfEdgeDevices > SimulationParameters.maxNumberOfEdgeDevices) {
				throw new IllegalArgumentException(getClass().getSimpleName()
						+ " - Error,  the entered min number of edge devices is superior to the max number, check the 'simulation.properties' file.");
			}

			SimulationParameters.edgeDevicesIncrementationStepSize = (int) assertDouble(prop, "edge_device_counter_size",
					value -> (value > 0), "> 0");

			SimulationParameters.realisticNetworkModel = Boolean
					.parseBoolean(prop.getProperty("realistic_network_model").trim());
			// Seconds
			SimulationParameters.networkUpdateInterval = assertDouble(prop, "network_update_interval",
					value -> (value >= 0.001), ">= 0.001");
			SimulationParameters.useOneSharedWanLink = Boolean
					.parseBoolean(prop.getProperty("one_shared_wan_network").trim());

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

			SimulationParameters.orchestrationArchitectures = prop.getProperty("orchestration_architectures")
					.split(",");
			SimulationParameters.orchestrationAlgorithms = prop.getProperty("orchestration_algorithms").split(",");

			result = true;
			SimLog.println("%s - Properties file successfully Loaded propoerties file!",getClass().getSimpleName());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;

	}

	protected double assertDouble(Properties prop, String parameter, Predicate<Double> p, String message) {
		double number = Double.parseDouble(prop.getProperty(parameter).trim());
		if (!p.test(number))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the value of \"" + parameter
					+ "\" must be " + message + ". Check the simulation_parameters.xml file!.");
		return number;
	}

}
