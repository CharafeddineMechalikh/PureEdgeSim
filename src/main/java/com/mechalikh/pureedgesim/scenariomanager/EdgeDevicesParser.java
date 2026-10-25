package com.mechalikh.pureedgesim.scenariomanager;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

/**
 * Parses the edge devices XML file. In addition to validating each device, it
 * enforces that the device percentages add up to 100.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class EdgeDevicesParser extends ComputingNodesParser {

	/**
	 * Creates a parser for the edge devices file.
	 *
	 * @param file path to the XML file
	 */
	public EdgeDevicesParser(String file) {
		super(file, TYPES.EDGE_DEVICE);
	}

	/**
	 * Validates every device and checks that the percentages sum to 100.
	 *
	 * @param xmlDoc the parsed XML document
	 * @return {@code true} when the file is valid
	 * @throws IllegalArgumentException if the percentages do not sum to 100
	 */
	@Override
	protected boolean typeSpecificChecking(Document xmlDoc) {
		NodeList devicesList = xmlDoc.getElementsByTagName("device");
		double percentage = 0;
		for (int i = 0; i < devicesList.getLength(); i++) {
			Node device = devicesList.item(i);
			Element deviceElement = (Element) device;
			checkEdgeDevice(deviceElement);
			percentage += getMean(deviceElement, "percentage");

		}
		if (percentage != 100 && type == TYPES.EDGE_DEVICE) {
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - Check the file edge_devices.xml file!, the sum of percentages must be equal to 100%.");
		}
		return true;
	}

	/**
	 * Ensures that a device element contains every required child element and
	 * that numeric values are not negative. Each numeric value may be a plain
	 * number or a distribution, in which case the mean is validated.
	 *
	 * @param deviceElement the device element to validate
	 * @throws IllegalArgumentException if a required element is missing or invalid
	 */
	protected void checkEdgeDevice(Element deviceElement) {
		for (String element : Arrays.asList(new String[]{"connectivity", "mobility", "battery", "percentage", "speed", "minPauseDuration",
				"maxPauseDuration", "minMobilityDuration", "maxMobilityDuration", "batteryCapacity", "generateTasks",
				"isOrchestrator", "idleConsumption", "maxConsumption", "cores", "mips", "ram", "storage"}))
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

		double idleConsumption = getMean(deviceElement, "idleConsumption");
		assertDouble(deviceElement, "maxConsumption", value -> (value > idleConsumption),
				"> \"idleConsumption\". Check the file " + file);

		double minPauseDuration = getMean(deviceElement, "minPauseDuration");
		assertDouble(deviceElement, "maxPauseDuration", value -> (value >= minPauseDuration),
				">= \"minPauseDuration\". Check the file " + file);

		double minMobilityDuration = getMean(deviceElement, "minMobilityDuration");
		assertDouble(deviceElement, "maxMobilityDuration", value -> (value >= minMobilityDuration),
				">= \"minMobilityDuration\". Check the file " + file);
	}

}
