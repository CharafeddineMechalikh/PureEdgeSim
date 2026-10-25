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
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.datacentersmanager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.locationmanager.MobilityModelFactory;
import com.mechalikh.pureedgesim.scenariomanager.DistributionReader;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Default generator of cloud data centers, edge data centers, and edge devices.
 * It reads the three XML files referenced by {@link SimulationParameters} and
 * creates the computing nodes and their mobility models.
 * <p>
 * Every numeric attribute of a node may be a probability distribution, as
 * described by {@link DistributionReader}. Each instance then draws its own
 * value, which is how a heterogeneous fleet of devices or edge servers is
 * modelled. The {@code mobility} element may also select a mobility model by
 * name through the {@code model} attribute, see {@link MobilityModelFactory}.
 */
public class DefaultComputingNodesGenerator extends ComputingNodesGenerator {

	/**
	 * Creates a generator for the given simulation.
	 *
	 * @param simulationManager        the simulation manager.
	 * @param mobilityModelClass       the mobility model class used for every
	 *                                 node.
	 * @param computingNodeClass       the computing node class used for every
	 *                                 node.
	 */
	public DefaultComputingNodesGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		super(simulationManager, mobilityModelClass, computingNodeClass);
	}
	
	@Override
	public void generateDatacentersAndDevices() {

		// Generate Edge and Cloud data centers.
		generateDataCenters(SimulationParameters.cloudDataCentersFile, SimulationParameters.TYPES.CLOUD); 

		generateDataCenters(SimulationParameters.edgeDataCentersFile, SimulationParameters.TYPES.EDGE_DATACENTER); 

		// Generate edge devices.
		generateEdgeDevices();

		getSimulationManager().getSimulationLogger()
				.print("%s - Datacenters and devices were generated", getClass().getSimpleName());

	}

	/**
	 * Generates edge devices
	 */
	public void generateEdgeDevices() {
		// Generate edge devices instances from edge devices types in xml file.
		try (InputStream devicesFile = new FileInputStream(SimulationParameters.edgeDevicesFile)) {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(devicesFile);
			NodeList nodeList = doc.getElementsByTagName("device");

			// Load all devices types in edge_devices.xml file.
			List<Element> deviceTypes = new ArrayList<>();
			List<Double> percentages = new ArrayList<>();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node edgeNode = nodeList.item(i);
				Element edgeElement = (Element) edgeNode;
				deviceTypes.add(edgeElement);
				percentages.add(sample(edgeElement, "percentage", 0, Double.MAX_VALUE));
			}

			// Expand every type with the number of instances its share earns.
			int[] instances = allocateInstances(percentages);
			for (int i = 0; i < deviceTypes.size(); i++)
				generateDevicesInstances(deviceTypes.get(i), instances[i]);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new IllegalStateException(getClass().getSimpleName()
					+ " - Error, cannot read the edge devices file \"" + SimulationParameters.edgeDevicesFile + "\": "
					+ e.getMessage(), e);
		}

	}

	/**
	 * Puts the newly generated edge device in corresponding lists.
	 *
	 * @param newDevice the device to register.
	 */
	protected void insertEdgeDevice(ComputingNode newDevice) {
		mistOnlyList.add(newDevice);
		allNodesList.add(newDevice);
		if (!newDevice.isSensor()) {
			mistOnlyListSensorsExcluded.add(newDevice);
			mistAndCloudListSensorsExcluded.add(newDevice);
			mistAndEdgeListSensorsExcluded.add(newDevice);
			allNodesListSensorsExcluded.add(newDevice);
		}
	}
	
	/**
	 * Generates the required number of instances of one type of edge devices.
	 * 
	 * @param type The type of edge devices.
	 */
	protected void generateDevicesInstances(Element type) {
		double percentage = sample(type, "percentage", 0, Double.MAX_VALUE);
		generateDevicesInstances(type,
				allocateInstances(Collections.singletonList(percentage))[0]);
	}

	/**
	 * Generates the given number of instances of one type of edge devices.
	 * 
	 * @param type      The type of edge devices.
	 * @param instances The number of devices to create.
	 */
	protected void generateDevicesInstances(Element type, int instances) {
		for (int j = 0; j < instances; j++) {
			insertEdgeDevice(createComputingNode(type, SimulationParameters.TYPES.EDGE_DEVICE,
					SimulationParameters.edgeDevicesFile));
		}
	}

	/**
	 * Creates a computing node and reports which XML element could not be used.
	 *
	 * @param element the XML element describing the node type
	 * @param type    the type of the node
	 * @param source  the file the element comes from, used in the error message
	 * @return the created node
	 * @throws IllegalStateException if no node can be created from the element
	 */
	private ComputingNode createComputingNode(Element element, TYPES type, String source) {
		try {
			return createComputingNode(element, type);
		} catch (NoSuchAlgorithmException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(getClass().getSimpleName() + " - Error, cannot create the " + type
					+ " described by " + describeElement(element) + " in \"" + source + "\": " + e.getMessage(), e);
		}
	}

	/**
	 * Describes an XML element with its attributes, for error messages.
	 *
	 * @param element the element to describe
	 * @return a short representation such as {@code <device arch="x86">}
	 */
	private static String describeElement(Element element) {
		StringBuilder attributes = new StringBuilder();
		NamedNodeMap map = element.getAttributes();
		for (int i = 0; i < map.getLength(); i++) {
			Node attribute = map.item(i);
			attributes.append(' ').append(attribute.getNodeName()).append("=\"").append(attribute.getNodeValue())
					.append('"');
		}
		return "<" + element.getNodeName() + attributes + ">";
	}

	/**
	 * Distributes the devices of the scenario over the declared device types.
	 * <p>
	 * Each percentage is a share of the fleet, so the exact number of devices of a
	 * type is <code>devicesCount * percentage / 100</code>, which is rarely an
	 * integer. Every share is rounded down first, then the types with the largest
	 * fractional part each receive one of the remaining devices (the largest
	 * remainder method). This respects the declared shares as closely as possible
	 * and makes the total exactly equal to the scenario's device count, instead of
	 * truncating every type and padding the list with extra devices of the last
	 * type.
	 * <p>
	 * When the percentages do not sum to 100, a warning is printed and the
	 * declared total is used, capped at the scenario's device count.
	 *
	 * @param percentages the percentage of each device type, in file order
	 * @return the number of instances to create for each type, in file order
	 */
	protected int[] allocateInstances(List<Double> percentages) {
		int devicesCount = getSimulationManager().getScenario().getDevicesCount();
		int[] allocation = new int[percentages.size()];
		double total = 0;
		for (double percentage : percentages)
			total += Math.max(0, percentage);
		if (total <= 0 || devicesCount <= 0)
			return allocation;

		if (Math.abs(total - 100) > 1e-9)
			getSimulationManager().getSimulationLogger().print(
					"%s - The device percentages should sum to 100, found " + total
							+ ", check edge_devices.xml file !",
					getClass().getSimpleName());

		int target = (int) Math.round(devicesCount * total / 100);
		target = Math.min(devicesCount, Math.max(0, target));

		double[] remainders = new double[percentages.size()];
		int allocated = 0;
		for (int i = 0; i < percentages.size(); i++) {
			double share = target * Math.max(0, percentages.get(i)) / total;
			allocation[i] = (int) Math.floor(share);
			remainders[i] = share - allocation[i];
			allocated += allocation[i];
		}
		while (allocated < target) {
			int largest = 0;
			for (int i = 1; i < remainders.length; i++)
				if (remainders[i] > remainders[largest])
					largest = i;
			allocation[largest]++;
			remainders[largest] = -1;
			allocated++;
		}
		return allocation;
	}
	
	/**
	 * Generates the Cloud and Edge data centers.
	 * 
	 * @param file The configuration file.
	 * @param type The type, whether a CLOUD data center or an EDGE one.
	 */
	protected void generateDataCenters(String file, TYPES type) {
		// Fill list with edge data centers
		try (InputStream serversFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(serversFile);
			NodeList datacenterList = doc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {
				Element datacenterElement = (Element) datacenterList.item(i);
				ComputingNode computingNode = createComputingNode(datacenterElement, type, file);
				if (computingNode.getType() == TYPES.CLOUD) {
					cloudOnlyList.add(computingNode);
					mistAndCloudListSensorsExcluded.add(computingNode);
					if (SimulationParameters.enableOrchestrators
							&& "CLOUD".equals(SimulationParameters.deployOrchestrators)) {
						orchestratorsList.add(computingNode);
					}
				} else {
					edgeOnlyList.add(computingNode);
					mistAndEdgeListSensorsExcluded.add(computingNode);
					if (SimulationParameters.enableOrchestrators
							&& "EDGE".equals(SimulationParameters.deployOrchestrators)) {
						orchestratorsList.add(computingNode);
					}
				}
				allNodesList.add(computingNode);
				allNodesListSensorsExcluded.add(computingNode);
				edgeAndCloudList.add(computingNode);
			}
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new IllegalStateException(getClass().getSimpleName() + " - Error, cannot read the data center file \""
					+ file + "\": " + e.getMessage(), e);
		}

	}
	
	/**
	 * Creates the computing nodes.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @param datacenterElement The XML element describing the node type.
	 * @param type              The type, whether an MIST (edge) device, an EDGE
	 *                          data center, or a CLOUD one.
	 * @throws NoSuchAlgorithmException  never raised by this implementation, declared
	 *                                   so that subclasses may use their own secure
	 *                                   random source.
	 * @throws NoSuchMethodException     if the model class lacks the expected
	 *                                   constructor.
	 * @throws SecurityException         if the model constructor cannot be accessed.
	 * @throws InvocationTargetException if the model constructor throws.
	 * @throws IllegalArgumentException  if the constructor arguments are invalid or a
	 *                                   required value is missing.
	 * @throws IllegalAccessException    if the model class or constructor is not
	 *                                   accessible.
	 * @throws InstantiationException    if the model class cannot be instantiated.
	 * @return the generated computing node.
	 */
	protected ComputingNode createComputingNode(Element datacenterElement, SimulationParameters.TYPES type)
			throws NoSuchAlgorithmException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// The random source is shared with the rest of the simulation, so the
		// "random_seed" parameter also pins the capacity and the placement of the nodes.
		Random random = Randomness.getRandom();
		Boolean mobile = false;
		Element mobilityElement = null;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		double idleConsumption = sample(datacenterElement, "idleConsumption", 0, Double.MAX_VALUE);
		double maxConsumption = sample(datacenterElement, "maxConsumption", 0, Double.MAX_VALUE);
		Location datacenterLocation = new Location(-1, -1);
		int numOfCores = (int) sample(datacenterElement, "cores", 1, Double.MAX_VALUE);
		double mips = sample(datacenterElement, "mips", 1, Double.MAX_VALUE);
		double storage = sample(datacenterElement, "storage", 0, Double.MAX_VALUE);
		double ram = sample(datacenterElement, "ram", 0, Double.MAX_VALUE);

		Constructor<?> datacenterConstructor = computingNodeClass.getConstructor(SimulationManager.class, double.class,
				int.class, double.class, double.class);
		ComputingNode computingNode = (ComputingNode) datacenterConstructor.newInstance(getSimulationManager(), mips,
				numOfCores, storage, ram);

		computingNode.setAsOrchestrator(Boolean
				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));

		if (computingNode.isOrchestrator())
			orchestratorsList.add(computingNode);

		computingNode.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));

		if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			String name = datacenterElement.getAttribute("name");
			computingNode.setName(name);
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			datacenterLocation = new Location(sample(location, "x_pos", 0, SimulationParameters.simulationMapWidth),
					sample(location, "y_pos", 0, SimulationParameters.simulationMapLength));

			for (int i = 0; i < edgeOnlyList.size(); i++)
				if (datacenterLocation.equals(edgeOnlyList.get(i).getMobilityModel().getCurrentLocation()))
					throw new IllegalArgumentException(
							" Each Edge Data Center must have a different location, check the \"edge_datacenters.xml\" file!");

			computingNode.setPeriphery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("periphery").item(0).getTextContent()));

		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			mobilityElement = (Element) datacenterElement.getElementsByTagName("mobility").item(0);
			mobile = Boolean.parseBoolean(mobilityElement.getTextContent());
			speed = sample(datacenterElement, "speed", 0, Double.MAX_VALUE);
			minPauseDuration = sample(datacenterElement, "minPauseDuration", 0, Double.MAX_VALUE);
			maxPauseDuration = sample(datacenterElement, "maxPauseDuration", minPauseDuration, Double.MAX_VALUE);
			minMobilityDuration = sample(datacenterElement, "minMobilityDuration", 0, Double.MAX_VALUE);
			maxMobilityDuration = sample(datacenterElement, "maxMobilityDuration", minMobilityDuration,
					Double.MAX_VALUE);
			computingNode.getEnergyModel().setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			computingNode.getEnergyModel().setBatteryCapacity(sample(datacenterElement, "batteryCapacity", 0,
					Double.MAX_VALUE));
			computingNode.getEnergyModel().setIntialBatteryPercentage(
					sample(datacenterElement, "initialBatteryLevel", 0, 100));
			computingNode.getEnergyModel().setConnectivityType(
					datacenterElement.getElementsByTagName("connectivity").item(0).getTextContent());
			computingNode.enableTaskGeneration(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("generateTasks").item(0).getTextContent()));
			// Generate random location for edge devices
			datacenterLocation = getDeviceLocation(datacenterElement, random);
			getSimulationManager().getSimulationLogger()
					.deepLog("DefaultComputingNodesGenerator- Edge device:" + mistOnlyList.size() + "    location: ( "
							+ datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
		}
		computingNode.setType(type);
		computingNode.setMobilityModel(createMobilityModel(mobilityElement, datacenterLocation, mobile, speed,
				minPauseDuration, maxPauseDuration, minMobilityDuration, maxMobilityDuration));

		return computingNode;
	}

	/**
	 * Creates the mobility model of a node. When the {@code mobility} element
	 * declares a {@code model} attribute, the model is looked up by name, so that
	 * each type of device can move differently. Otherwise the class configured with
	 * {@code setCustomMobilityModel} is used, as in previous versions. The remaining
	 * attributes of the element are passed to the model as its options.
	 *
	 * @param mobilityElement     the {@code mobility} element, {@code null} for data
	 *                            centers
	 * @param location            the initial location of the node
	 * @param mobile              whether the node moves
	 * @param speed               the movement speed, in meters per second
	 * @param minPauseDuration    the minimum pause between two movements
	 * @param maxPauseDuration    the maximum pause between two movements
	 * @param minMobilityDuration the minimum duration of a movement
	 * @param maxMobilityDuration the maximum duration of a movement
	 * @return the mobility model of the node
	 */
	protected MobilityModel createMobilityModel(Element mobilityElement, Location location, boolean mobile,
			double speed, double minPauseDuration, double maxPauseDuration, double minMobilityDuration,
			double maxMobilityDuration) {
		String modelName = mobilityElement == null ? "" : mobilityElement.getAttribute("model");
		MobilityModel mobilityModel = modelName.isEmpty() ? instantiateMobilityModel(mobilityModelClass, location)
				: MobilityModelFactory.create(modelName, simulationManager, location);
		if (mobilityElement != null)
			mobilityModel.setParameters(readMobilityOptions(mobilityElement));
		return mobilityModel.setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);
	}

	/**
	 * Instantiates the configured mobility model class with its default constructor.
	 *
	 * @param modelClass the class to instantiate
	 * @param location   the initial location of the node
	 * @return a new instance of the model
	 */
	protected MobilityModel instantiateMobilityModel(Class<? extends MobilityModel> modelClass, Location location) {
		try {
			return modelClass.getConstructor(SimulationManager.class, Location.class).newInstance(simulationManager,
					location);
		} catch (ReflectiveOperationException | SecurityException e) {
			throw new IllegalArgumentException("The mobility model \"" + modelClass.getName() + "\" must provide a "
					+ "public constructor taking a SimulationManager and a Location", e);
		}
	}

	/**
	 * Collects the options of a {@code mobility} element. The {@code model} attribute
	 * is excluded, since it only selects the class to use.
	 *
	 * @param mobilityElement the element describing the mobility of the device
	 * @return a map of option name to value
	 */
	protected Map<String, String> readMobilityOptions(Element mobilityElement) {
		Map<String, String> options = new HashMap<>();
		NamedNodeMap attributes = mobilityElement.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attribute = attributes.item(i);
			if (!"model".equals(attribute.getNodeName()))
				options.put(attribute.getNodeName(), attribute.getNodeValue());
		}
		return options;
	}

	/**
	 * Draws the value of a numeric child element for the node being created. The
	 * element may hold a plain number, which every instance then reuses, or a
	 * probability distribution, which gives each instance its own value:
	 * 
	 * <pre>
	 * &lt;mips distribution="normal" mean="2000" std="300" min="500"/&gt;
	 * &lt;ram distribution="uniform" min="2048" max="8192"/&gt;
	 * &lt;storage&gt;1000000&lt;/storage&gt;
	 * &lt;/pre&gt;
	 * 
	 * Use this to model a heterogeneous fleet instead of identical machines. The
	 * sampled value is always clamped to the given range, so a distribution with
	 * unbounded support cannot produce a negative capacity.
	 *
	 * @param parent the element that contains the value
	 * @param tag    the tag name of the value
	 * @param min    the smallest value a node may get
	 * @param max    the largest value a node may get
	 * @return the value of this node
	 * @throws IllegalArgumentException when the element is missing or declares an
	 *                                  invalid distribution
	 */
	protected double sample(Element parent, String tag, double min, double max) {
		Element valueElement = (Element) parent.getElementsByTagName(tag).item(0);
		if (valueElement == null)
			throw new IllegalArgumentException("The \"" + tag + "\" element is missing from the \""
					+ parent.getNodeName() + "\" definition, check the scenario files");
		return Math.min(max, Math.max(min, DistributionReader.read(valueElement,
				"\"" + tag + "\" of the \"" + parent.getNodeName() + "\"").sample()));
	}

	/**
	 * Returns the initial location of an edge device.
	 * <p>
	 * By default the devices of a type are spread uniformly over the whole map, as
	 * in previous versions. A device type may also pin its instances to an area by
	 * adding a {@code location} element, whose coordinates may be distributions.
	 * Sampling them places the devices around a hotspot instead of scattering them:
	 * 
	 * <pre>
	 * &lt;location&gt;
	 *     &lt;x_pos distribution="normal" mean="150" std="30" min="0" max="300"/&gt;
	 *     &lt;y_pos distribution="normal" mean="150" std="30" min="0" max="300"/&gt;
	 * &lt;/location&gt;
	 * &lt;/pre&gt;
	 *
	 * @param deviceElement the element describing the device type
	 * @param random        the random source used for the uniform placement
	 * @return a location inside the simulation map
	 */
	protected Location getDeviceLocation(Element deviceElement, Random random) {
		Element location = (Element) deviceElement.getElementsByTagName("location").item(0);
		if (location == null)
			return new Location(random.nextInt(SimulationParameters.simulationMapWidth),
					random.nextInt(SimulationParameters.simulationMapLength));
		return new Location(sample(location, "x_pos", 0, SimulationParameters.simulationMapWidth),
				sample(location, "y_pos", 0, SimulationParameters.simulationMapLength));
	}

}
