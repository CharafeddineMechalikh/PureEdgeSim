package com.mechalikh.pureedgesim.scenariomanager;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

public class EdgeDevicesParser extends ComputingNodesParser {

	public EdgeDevicesParser(String file) {
		super(file, TYPES.EDGE_DEVICE);
	}

	@Override
	protected boolean typeSpecificChecking(Document xmlDoc) {
		NodeList devicesList = xmlDoc.getElementsByTagName("device");
		double percentage = 0;
		for (int i = 0; i < devicesList.getLength(); i++) {
			Node device = devicesList.item(i);
			Element deviceElement = (Element) device;
			checkEdgeDevice(deviceElement);
			percentage += Double.parseDouble(deviceElement.getElementsByTagName("percentage").item(0).getTextContent());

		}
		if (percentage != 100 && type == TYPES.EDGE_DEVICE) {
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - Check the file edge_devices.xml file!, the sum of percentages must be equal to 100%.");
		}
		return true;
	}

	protected void checkEdgeDevice(Element deviceElement) {
		for (String element : List.of("connectivity", "mobility", "battery", "percentage", "speed", "minPauseDuration",
				"maxPauseDuration", "minMobilityDuration", "maxMobilityDuration", "batteryCapacity", "generateTasks",
				"isOrchestrator", "idleConsumption", "maxConsumption", "cores", "mips", "ram", "storage"))
			isElementPresent(deviceElement, element);

		for (String element : List.of("speed", "minPauseDuration", "minMobilityDuration", "batteryCapacity",
				"idleConsumption", "cores", "mips", "ram", "storage"))
			assertDouble(deviceElement, element, value -> (value >= 0), ">= 0. Check the file " + file);

		assertDouble(deviceElement, "percentage", value -> (value > 0 && value <= 100),
				"> 0 and <= 100. Check the file " + file);

		boolean isBatteryPowered = Boolean
				.parseBoolean(deviceElement.getElementsByTagName("battery").item(0).getTextContent());
		if (isBatteryPowered)
			assertDouble(deviceElement, "initialBatteryLevel", value -> (value > 0 && value <= 100),
					"> 0 and <= 100 for battery-powered devices. Check the file " + file); 

		double idleConsumption = Double
				.parseDouble(deviceElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		assertDouble(deviceElement, "maxConsumption", value -> (value > idleConsumption),
				"> \"idleConsumption\". Check the file " + file);

		double minPauseDuration = Double
				.parseDouble(deviceElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
		assertDouble(deviceElement, "maxPauseDuration", value -> (value >= minPauseDuration),
				">= \"minPauseDuration\". Check the file " + file);

		double minMobilityDuration = Double
				.parseDouble(deviceElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
		assertDouble(deviceElement, "maxMobilityDuration", value -> (value >= minMobilityDuration),
				">= \"minMobilityDuration\". Check the file " + file);
	}

}
