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
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom; 
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class DefaultComputingNodesGenerator extends ComputingNodesGenerator {

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
			Element edgeElement = null;

			// Load all devices types in edge_devices.xml file.
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node edgeNode = nodeList.item(i);
				edgeElement = (Element) edgeNode;
				generateDevicesInstances(edgeElement);
			}

			// if percentage of generated devices is < 100%.
			if (mistOnlyList.size() < getSimulationManager().getScenario().getDevicesCount())
				getSimulationManager().getSimulationLogger().print("%s - Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !", getClass().getSimpleName());
			// Add more devices.
			if (edgeElement != null) {
				int missingInstances = getSimulationManager().getScenario().getDevicesCount() - mistOnlyList.size();
				for (int k = 0; k < missingInstances; k++) {
					ComputingNode newDevice = createComputingNode(edgeElement, SimulationParameters.TYPES.EDGE_DEVICE);
					insertEdgeDevice(newDevice);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Puts the newly generated edge device in corresponding lists.
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
	 * Generates the required number of instances for each type of edge devices.
	 * 
	 * @param type The type of edge devices.
	 */
	protected void generateDevicesInstances(Element type) {

		int instancesPercentage = Integer.parseInt(type.getElementsByTagName("percentage").item(0).getTextContent());

		// Find the number of instances of this type of devices
		int devicesInstances = getSimulationManager().getScenario().getDevicesCount() * instancesPercentage / 100;

		for (int j = 0; j < devicesInstances; j++) {
			if (mistOnlyList.size() > getSimulationManager().getScenario().getDevicesCount()) {
				getSimulationManager().getSimulationLogger().print("%s - Wrong percentages values (the sum is superior than 100%), check edge_devices.xml file !",getClass().getSimpleName());
				break;
			}

			try {
				insertEdgeDevice(createComputingNode(type, SimulationParameters.TYPES.EDGE_DEVICE));
			} catch (NoSuchAlgorithmException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

		}
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
				ComputingNode computingNode = createComputingNode(datacenterElement, type);
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
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Creates the computing nodes.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @param datacenterElement The configuration file.
	 * @param type              The type, whether an MIST (edge) device, an EDGE
	 *                          data center, or a CLOUD one.
	 * @throws NoSuchAlgorithmException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected ComputingNode createComputingNode(Element datacenterElement, SimulationParameters.TYPES type)
			throws NoSuchAlgorithmException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// SecureRandom is preferred to generate random values.
		Random random = SecureRandom.getInstanceStrong();
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int xPosition = -1;
		int yPosition = -1;
		double idleConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
		Location datacenterLocation = new Location(xPosition, yPosition);
		int numOfCores = Integer.parseInt(datacenterElement.getElementsByTagName("cores").item(0).getTextContent());
		double mips = Double.parseDouble(datacenterElement.getElementsByTagName("mips").item(0).getTextContent());
		double storage = Double.parseDouble(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
		double ram = Double.parseDouble(datacenterElement.getElementsByTagName("ram").item(0).getTextContent());

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
			xPosition = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			yPosition = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			datacenterLocation = new Location(xPosition, yPosition);

			for (int i = 0; i < edgeOnlyList.size(); i++)
				if (datacenterLocation.equals(edgeOnlyList.get(i).getMobilityModel().getCurrentLocation()))
					throw new IllegalArgumentException(
							" Each Edge Data Center must have a different location, check the \"edge_datacenters.xml\" file!");

			computingNode.setPeriphery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("periphery").item(0).getTextContent()));

		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			mobile = Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent());
			speed = Double.parseDouble(datacenterElement.getElementsByTagName("speed").item(0).getTextContent());
			minPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
			maxPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("maxPauseDuration").item(0).getTextContent());
			minMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
			maxMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("maxMobilityDuration").item(0).getTextContent());
			computingNode.getEnergyModel().setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			computingNode.getEnergyModel().setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batteryCapacity").item(0).getTextContent()));
			computingNode.getEnergyModel().setIntialBatteryPercentage(Double.parseDouble(
					datacenterElement.getElementsByTagName("initialBatteryLevel").item(0).getTextContent()));
			computingNode.getEnergyModel().setConnectivityType(
					datacenterElement.getElementsByTagName("connectivity").item(0).getTextContent());
			computingNode.enableTaskGeneration(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("generateTasks").item(0).getTextContent()));
			// Generate random location for edge devices
			datacenterLocation = new Location(random.nextInt(SimulationParameters.simulationMapWidth),
					random.nextInt(SimulationParameters.simulationMapLength));
			getSimulationManager().getSimulationLogger()
					.deepLog("DefaultComputingNodesGenerator- Edge device:" + mistOnlyList.size() + "    location: ( "
							+ datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
		}
		computingNode.setType(type);
		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
				datacenterLocation)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);

		computingNode.setMobilityModel(mobilityModel);

		return computingNode;
	}

}
