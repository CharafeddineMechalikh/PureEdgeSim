/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.scenariomanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationcore.SimLog;
import com.mechalikh.pureedgesim.tasksgenerator.Application;

public class FilesParser {

	// Scan files
	public boolean checkFiles(String simProp, String edgeDevicesFile, String edgeDataCentersFile, String appFile,
			String cloudFile) {
		SimulationParameters.EDGE_DEVICES_FILE = edgeDevicesFile;
		SimulationParameters.EDGE_DATACENTERS_FILE = edgeDataCentersFile;
		SimulationParameters.CLOUD_DATACENTERS_FILE = cloudFile;
		return (checkSimulationProperties(simProp) && checkXmlFiles(edgeDevicesFile, TYPES.EDGE_DEVICE)
				&& checkXmlFiles(edgeDataCentersFile, TYPES.EDGE_DATACENTER) && checkXmlFiles(cloudFile, TYPES.CLOUD)
				&& checkAppFile(appFile));
	}

	private boolean checkSimulationProperties(String simProp) {
		SimLog.println("FilesParser- Checking simulation properties file");
		boolean result = false;
		InputStream input = null;
		try {
			input = new FileInputStream(simProp);

			// loading properties file
			Properties prop = new Properties();
			prop.load(input);
			SimulationParameters.PARALLEL = Boolean.parseBoolean(prop.getProperty("parallel_simulation").trim());

			SimulationParameters.INITIALIZATION_TIME = Double
					.parseDouble(prop.getProperty("initialization_time").trim()); // seconds
			SimulationParameters.SIMULATION_TIME = SimulationParameters.INITIALIZATION_TIME
					+ (double) 60 * Double.parseDouble(prop.getProperty("simulation_time").trim()); // seconds

			SimulationParameters.DISPLAY_REAL_TIME_CHARTS = Boolean
					.parseBoolean(prop.getProperty("display_real_time_charts").trim());
			SimulationParameters.AUTO_CLOSE_REAL_TIME_CHARTS = Boolean
					.parseBoolean(prop.getProperty("auto_close_real_time_charts").trim());
			SimulationParameters.CHARTS_UPDATE_INTERVAL = Double
					.parseDouble(prop.getProperty("charts_update_interval").trim());
			SimulationParameters.SAVE_CHARTS = Boolean.parseBoolean(prop.getProperty("save_charts").trim());

			SimulationParameters.AREA_LENGTH = Integer.parseInt(prop.getProperty("length").trim()); // seconds
			SimulationParameters.AREA_WIDTH = Integer.parseInt(prop.getProperty("width").trim()); // seconds
			SimulationParameters.UPDATE_INTERVAL = Double.parseDouble(prop.getProperty("update_interval").trim()); // seconds
			SimulationParameters.DEEP_LOGGING = Boolean.parseBoolean(prop.getProperty("deep_log_enabled").trim());
			SimulationParameters.SAVE_LOG = Boolean.parseBoolean(prop.getProperty("save_log_file").trim());
			SimulationParameters.CLEAN_OUTPUT_FOLDER = Boolean
					.parseBoolean(prop.getProperty("clear_output_folder").trim());
			SimulationParameters.WAIT_FOR_TASKS = Boolean.parseBoolean(prop.getProperty("wait_for_all_tasks").trim());
			SimulationParameters.ENABLE_REGISTRY = Boolean.parseBoolean(prop.getProperty("enable_registry").trim());
			SimulationParameters.registry_mode = prop.getProperty("registry_mode").trim();
			SimulationParameters.ENABLE_ORCHESTRATORS = Boolean
					.parseBoolean(prop.getProperty("enable_orchestrators").trim());

			SimulationParameters.EDGE_DEVICES_RANGE = Integer.parseInt(prop.getProperty("edge_devices_range").trim()); // meters
			SimulationParameters.EDGE_DATACENTERS_RANGE = Integer
					.parseInt(prop.getProperty("edge_datacenters_coverage").trim()); // meters
			SimulationParameters.PAUSE_LENGTH = Integer.parseInt(prop.getProperty("pause_length").trim());// seconds
			SimulationParameters.MIN_NUM_OF_EDGE_DEVICES = Integer
					.parseInt(prop.getProperty("min_number_of_edge_devices").trim());
			SimulationParameters.MAX_NUM_OF_EDGE_DEVICES = Integer
					.parseInt(prop.getProperty("max_number_of_edge_devices").trim());
			if (SimulationParameters.MIN_NUM_OF_EDGE_DEVICES > SimulationParameters.MAX_NUM_OF_EDGE_DEVICES) {
				SimLog.println(
						"FilelParser, Error,  the entered min number of edge devices is superior than the max number, check the 'simulation.properties' file.");
				return false;
			}
			SimulationParameters.EDGE_DEVICE_COUNTER_STEP = Integer
					.parseInt(prop.getProperty("edge_device_counter_size").trim());
			SimulationParameters.BANDWIDTH_WLAN = 1000 * Integer.parseInt(prop.getProperty("wlan_bandwidth").trim()); // Mbits/s
																														// to
																														// Kbits/s
			SimulationParameters.WAN_BANDWIDTH = 1000 * Integer.parseInt(prop.getProperty("wan_bandwidth").trim());// Mbits/s
																													// to
																													// Kbits/s
			SimulationParameters.WAN_PROPAGATION_DELAY = Double
					.parseDouble(prop.getProperty("wan_propogation_delay").trim()); // seconds
			SimulationParameters.NETWORK_UPDATE_INTERVAL = Double
					.parseDouble(prop.getProperty("network_update_interval").trim()); // seconds
			SimulationParameters.REALISTIC_NETWORK_MODEL = Boolean
					.parseBoolean(prop.getProperty("realistic_network_model").trim()); // seconds

			SimulationParameters.CPU_ALLOCATION_POLICY = prop.getProperty("Applications_CPU_allocation_policy").trim();
			SimulationParameters.ORCHESTRATION_ARCHITECTURES = prop.getProperty("orchestration_architectures")
					.split(",");
			SimulationParameters.ORCHESTRATION_AlGORITHMS = prop.getProperty("orchestration_algorithms").split(",");
			SimulationParameters.DEPLOY_ORCHESTRATOR = prop.getProperty("deploy_orchestrator").trim();

			SimulationParameters.CONSUMED_ENERGY_PER_BIT = Double
					.parseDouble(prop.getProperty("consumed_energy_per_bit").trim()); // J/bit
			SimulationParameters.AMPLIFIER_DISSIPATION_FREE_SPACE = Double
					.parseDouble(prop.getProperty("amplifier_dissipation_free_space").trim()); // J/bit/m^2
			SimulationParameters.AMPLIFIER_DISSIPATION_MULTIPATH = Double
					.parseDouble(prop.getProperty("amplifier_dissipation_multipath").trim()); // J/bit/m^4

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
					result = true;
					SimLog.println("FilesParser- Properties file successfully Loaded propoerties file!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				SimLog.println("FilesParser- Failed to load properties file!");
				result = false;
			}
		}

		return result;

	}

	private boolean checkXmlFiles(String xmlFile, TYPES type) {
		SimLog.println("FilesParser- Checking file: " + xmlFile);

		try {
			File devicesFile = new File(xmlFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document xmlDoc = dBuilder.parse(devicesFile);
			xmlDoc.getDocumentElement().normalize();
			NodeList datacenterList;
			if (type == TYPES.EDGE_DEVICE)
				datacenterList = xmlDoc.getElementsByTagName("device");
			else
				datacenterList = xmlDoc.getElementsByTagName("datacenter");

			double percentage = 0;
			for (int i = 0; i < datacenterList.getLength(); i++) {
				Node datacenterNode = datacenterList.item(i);
				Element datacenterElement = (Element) datacenterNode;
				isElementPresent(datacenterElement, "idleConsumption");
				isElementPresent(datacenterElement, "maxConsumption");

				if (type == TYPES.EDGE_DEVICE) {
					if (!checkEdgeDevice(datacenterElement))
						return false;
					percentage += Double
							.parseDouble(datacenterElement.getElementsByTagName("percentage").item(0).getTextContent());
					isElementPresent(datacenterElement, "batteryCapacity");
					isElementPresent(datacenterElement, "generateTasks");

				} else if (type == TYPES.CLOUD) {
					SimulationParameters.NUM_OF_CLOUD_DATACENTERS++;

				} else {
					SimulationParameters.NUM_OF_EDGE_DATACENTERS++;
					Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
					isElementPresent(location, "x_pos");
					isElementPresent(location, "y_pos");
				}

				NodeList hostList = datacenterElement.getElementsByTagName("host");
				for (int j = 0; j < hostList.getLength(); j++) {
					Node hostNode = hostList.item(j);

					Element hostElement = (Element) hostNode;
					isElementPresent(hostElement, "core");
					isElementPresent(hostElement, "mips");
					isElementPresent(hostElement, "ram");
					isElementPresent(hostElement, "storage");

					NodeList vmList = hostElement.getElementsByTagName("VM");
					for (int k = 0; k < vmList.getLength(); k++) {
						Node vmNode = vmList.item(k);

						Element vmElement = (Element) vmNode;
						isElementPresent(vmElement, "core");
						isElementPresent(vmElement, "mips");
						isElementPresent(vmElement, "ram");
						isElementPresent(vmElement, "storage");
					}
				}
			}
			if (percentage != 100 && type == TYPES.EDGE_DEVICE) {
				SimLog.println(
						"FilesParser- check the edge_devices.xml file!, the sum of percentages must be equal to 100%");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("FilesParser- Failed to load edge devices file!");
			return false;
		}
		SimLog.println("FilesParser- Edge devices XML file successfully Loaded!");
		return true;
	}

	private boolean checkEdgeDevice(Element datacenterElement) {
		isElementPresent(datacenterElement, "mobility");
		isElementPresent(datacenterElement, "battery");
		isElementPresent(datacenterElement, "percentage");
		isElementPresent(datacenterElement, "speed");
		isElementPresent(datacenterElement, "minPauseDuration");
		isElementPresent(datacenterElement, "maxPauseDuration");
		isElementPresent(datacenterElement, "minMobilityDuration");
		isElementPresent(datacenterElement, "maxMobilityDuration");
		double speed = Double.parseDouble(datacenterElement.getElementsByTagName("speed").item(0).getTextContent());
		double percentage = Double
				.parseDouble(datacenterElement.getElementsByTagName("percentage").item(0).getTextContent());
		if (percentage <= 0 || speed < 0) {
			SimLog.println(
					"FilesParser- check the edge_devices.xml file!, the percentage must be > 0 and the speed must be >= 0");
			return false;
		}

		double minPauseDuration = Double
				.parseDouble(datacenterElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
		double maxPauseDuration = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxPauseDuration").item(0).getTextContent());
		if (minPauseDuration > maxPauseDuration) {
			SimLog.println(
					"FilesParser- check the edge_devices.xml file!, the maxPauseDuration value must be greater than (or equal) the minPauseDuration value");
			return false;
		}
		double minMobilityDuration = Double
				.parseDouble(datacenterElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
		double maxMobilityDuration = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxMobilityDuration").item(0).getTextContent());
		if (minMobilityDuration > maxMobilityDuration) {
			SimLog.println(
					"FilesParser- check the edge_devices.xml file!, the maxMobilityDuration value must be greater than (or equal) the minMobilityDuration value");
			return false;
		}
		return true;
	}

	private boolean checkAppFile(String appFile) {
		SimLog.println("FilesParser- Checking applications file");
		Document doc;
		try {
			File devicesFile = new File(appFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(devicesFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttribtuePresent(appElement, "name");
				isElementPresent(appElement, "max_delay");
				isElementPresent(appElement, "usage_percentage");
				isElementPresent(appElement, "container_size");
				isElementPresent(appElement, "request_size");
				isElementPresent(appElement, "results_size");
				isElementPresent(appElement, "task_length");
				isElementPresent(appElement, "required_core");
				isElementPresent(appElement, "rate");

				// max delay in seconds
				double max_delay = Double
						.parseDouble(appElement.getElementsByTagName("max_delay").item(0).getTextContent());

				// the size of the container (KB)
				long container_size = Long
						.parseLong(appElement.getElementsByTagName("container_size").item(0).getTextContent());

				// average request size (KB)
				long request_size = Long
						.parseLong(appElement.getElementsByTagName("request_size").item(0).getTextContent());

				// average downloaded results size (KB)
				long results_size = Long
						.parseLong(appElement.getElementsByTagName("results_size").item(0).getTextContent());

				// average task length (MI)
				double task_length = Double
						.parseDouble(appElement.getElementsByTagName("task_length").item(0).getTextContent());

				// required number of CPU cores
				int required_cores = Integer
						.parseInt(appElement.getElementsByTagName("required_core").item(0).getTextContent());

				// the generation rate (tasks per minute)
				int rate = Integer.parseInt(appElement.getElementsByTagName("rate").item(0).getTextContent());

				// the percentage of devices using this type of applications
				int usage_percentage = Integer
						.parseInt(appElement.getElementsByTagName("usage_percentage").item(0).getTextContent());

				// save apps parameters
				SimulationParameters.APPLICATIONS_LIST.add(new Application(rate, usage_percentage, max_delay,
						container_size, request_size, results_size, task_length, required_cores));

			}

		} catch (Exception e) {
			SimLog.println("FilesParser- Applications XML file cannot be parsed!");
			e.printStackTrace();
			return false;
		}
		SimLog.println("FilesParser- Applications XML file successfully loaded!");
		return true;
	}

	private void isElementPresent(Element element, String key) {
			String value = element.getElementsByTagName(key).item(0).getTextContent();
			checkArgument("Element", key, element, value);
	}

	private void isAttribtuePresent(Element element, String key) {
		String value = element.getAttribute(key);
		checkArgument("Attribure", key, element, value);

	}

	private void checkArgument(String name, String key, Element element, String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException(
					name+ " " + key + "' is not found in '" + element.getNodeName() + "'");
		}
	}

}
