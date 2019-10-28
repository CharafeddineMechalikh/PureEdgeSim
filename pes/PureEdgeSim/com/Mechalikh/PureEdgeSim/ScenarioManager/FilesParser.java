package com.Mechalikh.PureEdgeSim.ScenarioManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.Mechalikh.PureEdgeSim.SimulationManager.SimLog;

public class FilesParser {

	public FilesParser() {
	}

	// Scan files
	public boolean checkFiles(String simProp, String edgeFile, String fogFile, String appFile, String cloudFile)
			throws SAXException, IOException, ParserConfigurationException { 
		SimulationParameters.fogDevicesDoc = loadDocument(fogFile);
		SimulationParameters.edgeDevicesDoc = loadDocument(edgeFile);
		return (checkSimulationProperties(simProp) && checkEdgeDevicesFile(edgeFile) && checkFogDataCentersFile(fogFile)
				&& checkCloudDataCentersFile(cloudFile) && checkAppFile(appFile));

	}

	private Document loadDocument(String filePath) throws SAXException, IOException, ParserConfigurationException {
		File file = new File(filePath);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(file);
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

			SimulationParameters.DISPLAY_REAL_TIME_CHARTS = Boolean.parseBoolean(prop.getProperty("display_real_time_charts").trim());  
			SimulationParameters.AUTO_CLOSE_REAL_TIME_CHARTS = Boolean.parseBoolean(prop.getProperty("auto_close_real_time_charts").trim());  
			SimulationParameters.CHARTS_UPDATE_INTERVAL = Double.parseDouble(prop.getProperty("charts_update_interval").trim());  
			SimulationParameters.SAVE_CHARTS = Boolean.parseBoolean(prop.getProperty("save_charts").trim());  
			
			SimulationParameters.AREA_LENGTH = Integer.parseInt(prop.getProperty("length").trim()); // seconds
			SimulationParameters.AREA_WIDTH = Integer.parseInt(prop.getProperty("width").trim()); // seconds
			SimulationParameters.UPDATE_INTERVAL = Double.parseDouble(prop.getProperty("update_interval").trim()); // seconds
			SimulationParameters.DEEP_LOGGING = Boolean.parseBoolean(prop.getProperty("deep_log_enabled").trim());
			SimulationParameters.SAVE_LOG = Boolean.parseBoolean(prop.getProperty("save_log_file").trim());
			SimulationParameters.CLEAN_OUTPUT_FOLDER = Boolean.parseBoolean(prop.getProperty("clear_output_folder").trim());
			SimulationParameters.WAIT_FOR_TASKS = Boolean.parseBoolean(prop.getProperty("wait_for_all_tasks").trim());
			SimulationParameters.ENABLE_REGISTRY = Boolean.parseBoolean(prop.getProperty("enable_registry").trim());
			SimulationParameters.ENABLE_ORCHESTRATORS = Boolean.parseBoolean(prop.getProperty("enable_orchestrators").trim());
			
			SimulationParameters.EDGE_RANGE = Integer.parseInt(prop.getProperty("edge_range").trim()); //meters
			SimulationParameters.FOG_RANGE = Integer.parseInt(prop.getProperty("fog_coverage").trim()); //meters
			SimulationParameters.PAUSE_LENGTH = Integer.parseInt(prop.getProperty("pause_length").trim());//seconds
			SimulationParameters.MIN_NUM_OF_EDGE_DEVICES = Integer.parseInt(prop.getProperty("min_number_of_edge_devices").trim());
			SimulationParameters.MAX_NUM_OF_EDGE_DEVICES = Integer.parseInt(prop.getProperty("max_number_of_edge_devices").trim());
			if (SimulationParameters.MIN_NUM_OF_EDGE_DEVICES > SimulationParameters.MAX_NUM_OF_EDGE_DEVICES) {
				SimLog.println(
						"FilelParser, Error,  the entered min number of edge devices is superior than the max number, check the 'simulation.properties' file.");
				System.exit(0);
			}
			SimulationParameters.EDGE_DEVICE_COUNTER_STEP = Integer.parseInt(prop.getProperty("edge_device_counter_size").trim());
			SimulationParameters.SPEED = Double.parseDouble(prop.getProperty("speed").trim()); //meters per second m/s
			SimulationParameters.BANDWIDTH_WLAN = 1000 * Integer.parseInt(prop.getProperty("wlan_bandwidth").trim()); // Mbits/s to Kbits/s 
			SimulationParameters.WAN_BANDWIDTH = 1000 * Integer.parseInt(prop.getProperty("wan_bandwidth").trim());// Mbits/s to Kbits/s 
			SimulationParameters.WAN_PROPAGATION_DELAY = Double.parseDouble(prop.getProperty("wan_propogation_delay").trim()); //seconds
			SimulationParameters.NETWORK_UPDATE_INTERVAL = Double.parseDouble(prop.getProperty("network_update_interval").trim()); // seconds
			SimulationParameters.CPU_ALLOCATION_POLICY = prop.getProperty("Applications_CPU_allocation_policy").trim();
			SimulationParameters.TASKS_PER_EDGE_DEVICE_PER_MINUTES = Integer.parseInt(prop.getProperty("tasks_generation_rate").trim());
			SimulationParameters.ORCHESTRATION_ARCHITECTURES = prop.getProperty("orchestration_architectures").split(",");
			SimulationParameters.ORCHESTRATION_AlGORITHMS = prop.getProperty("orchestration_algorithms").split(",");
			SimulationParameters.DEPLOY_ORCHESTRATOR = prop.getProperty("deploy_orchestrator").trim();
			
			SimulationParameters.CONSUMED_ENERGY_PER_BIT = Double.parseDouble(prop.getProperty("consumed_energy_per_bit").trim()); // J/bit
			SimulationParameters.AMPLIFIER_DISSIPATION_FREE_SPACE = Double.parseDouble(prop.getProperty("amplifier_dissipation_free_space").trim()); // J/bit/m^2
			SimulationParameters.AMPLIFIER_DISSIPATION_MULTIPATH = Double.parseDouble(prop.getProperty("amplifier_dissipation_multipath").trim()); // J/bit/m^4
					

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

	private boolean checkEdgeDevicesFile(String edgeFile) {
		SimLog.println("FilesParser- Checking edge devices file");

		try {
			File devicesFile = new File(edgeFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			SimulationParameters.edgeDevicesDoc = dBuilder.parse(devicesFile);
			SimulationParameters.edgeDevicesDoc.getDocumentElement().normalize();

			NodeList datacenterList = SimulationParameters.edgeDevicesDoc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {

				Node datacenterNode = datacenterList.item(i);

				Element datacenterElement = (Element) datacenterNode;
				isAttribtuePresent(datacenterElement, "arch");
				isAttribtuePresent(datacenterElement, "os");
				isAttribtuePresent(datacenterElement, "vmm");
				isElementPresent(datacenterElement, "mobility");
				isElementPresent(datacenterElement, "battery");
				isElementPresent(datacenterElement, "percentage");
				isElementPresent(datacenterElement, "batterycapacity");
				isElementPresent(datacenterElement, "idleConsumption");
				isElementPresent(datacenterElement, "maxConsumption");

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
						isAttribtuePresent(vmElement, "vmm");
						isElementPresent(vmElement, "core");
						isElementPresent(vmElement, "mips");
						isElementPresent(vmElement, "ram");
						isElementPresent(vmElement, "storage");
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("FilesParser- Failed to load edge devices file!");
			return false;
		}
		SimLog.println("FilesParser- Edge devices XML file successfully Loaded!");
		return true;
	}

	private boolean checkCloudDataCentersFile(String cloudFile) {
		SimLog.println("FilesParser- Checking cloud devices file");
		try {
			File devicesFile = new File(cloudFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			SimulationParameters.cloudDevicesDoc = dBuilder.parse(devicesFile);
			SimulationParameters.cloudDevicesDoc.getDocumentElement().normalize();

			NodeList datacenterList = SimulationParameters.cloudDevicesDoc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {

				SimulationParameters.NUM_OF_CLOUD_DATACENTERS++;
				Node datacenterNode = datacenterList.item(i);

				Element datacenterElement = (Element) datacenterNode;
				isAttribtuePresent(datacenterElement, "arch");
				isAttribtuePresent(datacenterElement, "os");
				isAttribtuePresent(datacenterElement, "vmm");
				isElementPresent(datacenterElement, "idleConsumption");
				isElementPresent(datacenterElement, "maxConsumption");

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
						isAttribtuePresent(vmElement, "vmm");
						isElementPresent(vmElement, "core");
						isElementPresent(vmElement, "mips");
						isElementPresent(vmElement, "ram");
						isElementPresent(vmElement, "storage");
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("FilesParser- Failed to load cloud datacenters XML file!");
			return false;
		}
		SimLog.println("FilesParser- Cloud datacenters XML file successfully loaded!");
		return true;
	}

	private boolean checkFogDataCentersFile(String fogFile) {
		SimLog.println("FilesParser- Checking fog devices file");
		try {
			File devicesFile = new File(fogFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			SimulationParameters.fogDevicesDoc = dBuilder.parse(devicesFile);
			SimulationParameters.fogDevicesDoc.getDocumentElement().normalize();

			NodeList datacenterList = SimulationParameters.fogDevicesDoc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {

				SimulationParameters.NUM_OF_FOG_DATACENTERS++;
				Node datacenterNode = datacenterList.item(i);

				Element datacenterElement = (Element) datacenterNode;
				isAttribtuePresent(datacenterElement, "arch");
				isAttribtuePresent(datacenterElement, "os");
				isAttribtuePresent(datacenterElement, "vmm");
				isElementPresent(datacenterElement, "idleConsumption");
				isElementPresent(datacenterElement, "maxConsumption");

				Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
				isElementPresent(location, "x_pos");
				isElementPresent(location, "y_pos");

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
						isAttribtuePresent(vmElement, "vmm");
						isElementPresent(vmElement, "core");
						isElementPresent(vmElement, "mips");
						isElementPresent(vmElement, "ram");
						isElementPresent(vmElement, "storage");
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("FilesParser- Failed to load fog datacenters XML file!");
			return false;
		}
		NodeList datacenterList = SimulationParameters.fogDevicesDoc.getElementsByTagName("datacenter");
		if (datacenterList.getLength() == 0) {
			SimLog.println(
					"FilesParser- Error, Please keep at least one fog data center in the XML file even if you will not use it !");
			SimLog.println("             PureEdgeSim uses Fog nodes as access points too !");
			SimLog.println(
					"             Thus, if you want to put all edge devices in the same location  you must create one fog data center. ");
			SimLog.println(
					"             Otherwise, you have to add more fog data centers in order to distrubute the edge devices on  multiple locations !");
			SimLog.println("             The simulation is  aborted!.");
			return false;
		}
		SimLog.println("FilesParser- Fog datacenters XML file successfully loaded!");
		return true;
	}

	private boolean checkAppFile(String appFile) {
		SimLog.println("FilesParser- Checking applications file");
		Document doc = null;
		try {
			File devicesFile = new File(appFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(devicesFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			SimulationParameters.APPS_COUNT = appList.getLength();// save the number of apps, this will be used later by
																	// the tasks generator
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttribtuePresent(appElement, "name");
				isElementPresent(appElement, "max_delay");
				isElementPresent(appElement, "container_size");
				isElementPresent(appElement, "request_size");
				isElementPresent(appElement, "results_size");
				isElementPresent(appElement, "task_length");
				isElementPresent(appElement, "required_core");

				String appName = appElement.getAttribute("name");
				// SimualtionParamters.APP_TYPES appType =
				// SimualtionParamters.APP_TYPES.valueOf(appName);
				double max_delay = Double
						.parseDouble(appElement.getElementsByTagName("max_delay").item(0).getTextContent());
				double container_size = Double
						.parseDouble(appElement.getElementsByTagName("container_size").item(0).getTextContent());
				double request_size = Double
						.parseDouble(appElement.getElementsByTagName("request_size").item(0).getTextContent());
				double results_size = Double
						.parseDouble(appElement.getElementsByTagName("results_size").item(0).getTextContent());
				double task_length = Double
						.parseDouble(appElement.getElementsByTagName("task_length").item(0).getTextContent());
				double required_core = Double
						.parseDouble(appElement.getElementsByTagName("required_core").item(0).getTextContent());
				int index = getAppIndex(appName);
				// save apps parameters
				SimulationParameters.APPLICATIONS_TABLE[index][0] = max_delay; // max delay in seconds
				SimulationParameters.APPLICATIONS_TABLE[index][1] = request_size; // avg request size (KB)
				SimulationParameters.APPLICATIONS_TABLE[index][2] = results_size; // avg downloaded results size (KB)
				SimulationParameters.APPLICATIONS_TABLE[index][3] = task_length; // avg task length (MI)
				SimulationParameters.APPLICATIONS_TABLE[index][4] = required_core; // required # of core
				SimulationParameters.APPLICATIONS_TABLE[index][5] = container_size; // the size of the container (KB)
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
		try {
			String value = element.getElementsByTagName(key).item(0).getTextContent();
			if (value.isEmpty() || value == null) {
				throw new IllegalArgumentException(
						"Element '" + key + "' is not found in '" + element.getNodeName() + "'");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() + "'");
		}
	}

	private void isAttribtuePresent(Element element, String key) {
		String value = element.getAttribute(key);
		if (value.isEmpty() || value == null) {
			throw new IllegalArgumentException(
					"Attribure '" + key + "' is not found in '" + element.getNodeName() + "'");
		}
	}

	public int getAppIndex(String appname) {
		for (int i = 0; i < SimulationParameters.APPLICATIONS.length; i++) {
			if (appname.equals(SimulationParameters.APPLICATIONS[i]))
				return i;
		}
		SimLog.println("Invalid application scenario " + appname + " check Applications.xml file");
		System.exit(0);
		return -1;
	}
}
