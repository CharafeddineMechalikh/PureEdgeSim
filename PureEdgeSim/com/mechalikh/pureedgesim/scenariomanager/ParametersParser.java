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

	private boolean checkSimulationProperties() {
		SimLog.println(getClass().getSimpleName() + " - Checking simulation properties file...");
		boolean result = false;
		InputStream input = null;
		try {
			input = new FileInputStream(file);

			// Loading properties file.
			Properties prop = new Properties();
			prop.load(input);

			// In seconds
			SimulationParameters.SIMULATION_TIME = (double) 60
					* assertDouble(prop, "simulation_time", value -> (value > 0), "> 0");

			SimulationParameters.PARALLEL = Boolean.parseBoolean(prop.getProperty("parallel_simulation").trim());

			// In seconds
			SimulationParameters.UPDATE_INTERVAL = assertDouble(prop, "update_interval", value -> (value >= 0.01),
					">= 0.01");

			// In seconds
			SimulationParameters.PAUSE_LENGTH = (int) assertDouble(prop, "pause_length", value -> (value >= 0), ">= 0");

			SimulationParameters.DISPLAY_REAL_TIME_CHARTS = Boolean
					.parseBoolean(prop.getProperty("display_real_time_charts").trim());
			SimulationParameters.AUTO_CLOSE_REAL_TIME_CHARTS = Boolean
					.parseBoolean(prop.getProperty("auto_close_real_time_charts").trim());

			SimulationParameters.CHARTS_UPDATE_INTERVAL = assertDouble(prop, "charts_update_interval",
					value -> (value >= 0.01 && value <= 60), "between 0.01 and 60");

			SimulationParameters.SAVE_CHARTS = Boolean.parseBoolean(prop.getProperty("save_charts").trim());

			// Meters
			SimulationParameters.AREA_LENGTH = (int) assertDouble(prop, "length", value -> (value > 0), "> 0");

			// Meters
			SimulationParameters.AREA_WIDTH = (int) assertDouble(prop, "width", value -> (value > 0), "> 0");

			SimulationParameters.EDGE_DEVICES_RANGE = (int) assertDouble(prop, "edge_devices_range",
					value -> (value > 0), "> 0");
			SimulationParameters.EDGE_DATACENTERS_RANGE = (int) assertDouble(prop, "edge_datacenters_coverage",
					value -> (value > 0), "> 0");

			SimulationParameters.ENABLE_REGISTRY = Boolean.parseBoolean(prop.getProperty("enable_registry").trim());
			SimulationParameters.registry_mode = prop.getProperty("registry_mode").trim();
			SimulationParameters.ENABLE_ORCHESTRATORS = Boolean
					.parseBoolean(prop.getProperty("enable_orchestrators").trim());
			SimulationParameters.DEPLOY_ORCHESTRATOR = prop.getProperty("deploy_orchestrator").trim();

			SimulationParameters.WAIT_FOR_TASKS = Boolean.parseBoolean(prop.getProperty("wait_for_all_tasks").trim());
			SimulationParameters.SAVE_LOG = Boolean.parseBoolean(prop.getProperty("save_log_file").trim());
			SimulationParameters.CLEAN_OUTPUT_FOLDER = Boolean
					.parseBoolean(prop.getProperty("clear_output_folder").trim());
			SimulationParameters.DEEP_LOGGING = Boolean.parseBoolean(prop.getProperty("deep_log_enabled").trim());

			SimulationParameters.MIN_NUM_OF_EDGE_DEVICES = (int) assertDouble(prop, "min_number_of_edge_devices",
					value -> (value > 0), "> 0");
			SimulationParameters.MAX_NUM_OF_EDGE_DEVICES = (int) assertDouble(prop, "max_number_of_edge_devices",
					value -> (value > 0), "> 0");

			if (SimulationParameters.MIN_NUM_OF_EDGE_DEVICES > SimulationParameters.MAX_NUM_OF_EDGE_DEVICES) {
				throw new IllegalArgumentException(getClass().getSimpleName()
						+ " - Error,  the entered min number of edge devices is superior to the max number, check the 'simulation.properties' file.");
			}

			SimulationParameters.EDGE_DEVICE_COUNTER_STEP = (int) assertDouble(prop, "edge_device_counter_size",
					value -> (value > 0), "> 0");

			SimulationParameters.REALISTIC_NETWORK_MODEL = Boolean
					.parseBoolean(prop.getProperty("realistic_network_model").trim());
			// Seconds
			SimulationParameters.NETWORK_UPDATE_INTERVAL = assertDouble(prop, "network_update_interval",
					value -> (value >= 0.01), ">= 0.01");
			SimulationParameters.ONE_SHARED_WAN_NETWORK = Boolean
					.parseBoolean(prop.getProperty("one_shared_wan_network").trim());

			// Mbps to bits per second
			SimulationParameters.WAN_BANDWIDTH_BITS_PER_SECOND = 1000000
					* assertDouble(prop, "wan_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.WAN_LATENCY = assertDouble(prop, "wan_latency", value -> (value >= 0), ">= 0");

			// Nanojoules per second (per bit) to Watt Hour (per bit)
			SimulationParameters.WAN_WATTHOUR_PER_BIT = 2.7777777777778e-13
					* assertDouble(prop, "wan_nanojoules_per_bit", value -> (value >= 0), ">= 0");

			SimulationParameters.MAN_BANDWIDTH_BITS_PER_SECOND = 1000000
					* assertDouble(prop, "man_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.MAN_LATENCY = assertDouble(prop, "man_latency", value -> (value >= 0), ">= 0");
			SimulationParameters.MAN_WATTHOUR_PER_BIT = 2.7777777777778e-13
					* assertDouble(prop, "man_nanojoules_per_bit", value -> (value >= 0), ">= 0");

			SimulationParameters.WIFI_BANDWIDTH_BITS_PER_SECOND = 1000000
					* assertDouble(prop, "wifi_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.WIFI_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT = 2.7777777777778e-13
					* assertDouble(prop, "wifi_device_transmission_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.WIFI_DEVICE_RECEPTION_WATTHOUR_PER_BIT = 2.7777777777778e-13 * assertDouble(prop,
					"wifi_device_reception_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.WIFI_ACCESS_POINT_TRANSMISSION_WATTHOUR_PER_BIT = 2.7777777777778e-13 * assertDouble(prop,
					"wifi_access_point_transmission_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.WIFI_ACCESS_POINT_RECEPTION_WATTHOUR_PER_BIT = 2.7777777777778e-13 * assertDouble(prop,
					"wifi_access_point_reception_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			
			SimulationParameters.WIFI_LATENCY = assertDouble(prop, "wifi_latency", value -> (value >= 0), ">= 0");
			SimulationParameters.ETHERNET_BANDWIDTH_BITS_PER_SECOND = 1000000
					* assertDouble(prop, "ethernet_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.ETHERNET_WATTHOUR_PER_BIT = 2.7777777777778e-13
					* assertDouble(prop, "ethernet_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.ETHERNET_LATENCY = assertDouble(prop, "ethernet_latency", value -> (value >= 0),
					">= 0");

			SimulationParameters.CELLULAR_BANDWIDTH_BITS_PER_SECOND = 1000000
					* assertDouble(prop, "cellular_bandwidth", value -> (value > 0), "> 0");
			SimulationParameters.CELLULAR_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT = 2.7777777777778e-13
					* assertDouble(prop, "wifi_device_transmission_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.CELLULAR_DEVICE_RECEPTION_WATTHOUR_PER_BIT = 2.7777777777778e-13
					* assertDouble(prop, "wifi_device_reception_nanojoules_per_bit", value -> (value >= 0), ">= 0");
			SimulationParameters.CELLULAR_BASE_STATION_WATTHOUR_PER_BIT_UP_LINK = 2.7777777777778e-13
					* assertDouble(prop, "cellular_base_station_nanojoules_per_bit_up_link", value -> (value >= 0), ">= 0");
			SimulationParameters.CELLULAR_BASE_STATION_WATTHOUR_PER_BIT_DOWN_LINK = 12.7777777777778e-13 * assertDouble(prop,
					"cellular_base_station_nanojoules_per_bit_down_link", value -> (value >= 0), ">= 0");
			SimulationParameters.CELLULAR_LATENCY = assertDouble(prop, "cellular_latency", value -> (value >= 0), ">= 0");

			SimulationParameters.ORCHESTRATION_ARCHITECTURES = prop.getProperty("orchestration_architectures")
					.split(",");
			SimulationParameters.ORCHESTRATION_AlGORITHMS = prop.getProperty("orchestration_algorithms").split(",");
			input.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
					result = true;
					SimLog.println(getClass().getSimpleName() + " - Properties file successfully Loaded propoerties file!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				throw new IllegalArgumentException(getClass().getSimpleName() + " - Failed to load properties file!");
			}
		}

		return result;

	}

	private double assertDouble(Properties prop, String parameter, Predicate<Double> p, String message) {
		double number = Double.parseDouble(prop.getProperty(parameter).trim());
		if (!p.test(number))
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the value of \"" + parameter
					+ "\" must be " + message + ". Check the simulation_parameters.xml file!.");
		return number;
	}

}
