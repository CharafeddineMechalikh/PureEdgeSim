package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

class EdgeDevicesParserTest extends AbstractTest {

	private String devicesXml;

	private static class NonEdgeDeviceParser extends EdgeDevicesParser {
		NonEdgeDeviceParser(String file) {
			super(file);
			type = TYPES.EDGE_DATACENTER;
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		TempSettingsFiles.resetDirectory();
		devicesXml = TempSettingsFiles.resource("edge_devices.xml");
	}

	@AfterEach
	void cleanUp() throws Exception {
		TempSettingsFiles.resetDirectory();
	}

	private boolean parse(String xml) throws Exception {
		Path file = TempSettingsFiles.write("devices-" + System.nanoTime() + ".xml", xml);
		return new EdgeDevicesParser(file.toString()).parse();
	}

	@Test
	void parsesTheReferenceEdgeDevicesFile() {
		assertTrue(new EdgeDevicesParser(SETTINGS_FOLDER + "edge_devices.xml").parse());
	}

	@Test
	void returnsFalseWhenTheFileDoesNotExist() {
		assertFalse(new EdgeDevicesParser(TempSettingsFiles.DIR.resolve("missing-devices.xml").toString()).parse());
	}

	@Test
	void returnsFalseForMalformedXml() throws Exception {
		assertFalse(parse("<edge_devices><device></edge_devices>"));
	}

	@Test
	void returnsFalseWhenDoctypeIsDeclared() throws Exception {
		String xml = "<?xml version=\"1.0\"?><!DOCTYPE edge_devices [<!ENTITY x \"y\">]><edge_devices></edge_devices>";
		assertFalse(parse(xml));
	}

	@Test
	void rejectsMissingMandatoryDeviceElements() throws Exception {
		String[] tags = { "connectivity", "mobility", "battery", "percentage", "speed", "minPauseDuration",
				"maxPauseDuration", "minMobilityDuration", "maxMobilityDuration", "batteryCapacity", "generateTasks",
				"isOrchestrator", "idleConsumption", "maxConsumption", "cores", "mips", "ram", "storage" };
		for (String tag : tags) {
			assertFalse(parse(TempSettingsFiles.removeFirstElement(devicesXml, tag)),
					"device file without <" + tag + "> should be rejected");
		}
	}

	@Test
	void rejectsMissingInitialBatteryLevelForBatteryPoweredDevice() throws Exception {
		assertFalse(parse(TempSettingsFiles.removeFirstElement(devicesXml, "initialBatteryLevel")));
	}

	@Test
	void rejectsNegativeNonPercentageValues() throws Exception {
		String[] tags = { "speed", "minPauseDuration", "minMobilityDuration", "batteryCapacity", "idleConsumption",
				"cores", "mips", "ram", "storage" };
		for (String tag : tags) {
			assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, tag, "-1")),
					"device file with " + tag + "=-1 should be rejected");
		}
	}

	@Test
	void rejectsInvalidPercentageBounds() throws Exception {
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "percentage", "0")));
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "percentage", "101")));
	}

	@Test
	void rejectsDevicePercentagesThatDoNotSumTo100() throws Exception {
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "percentage", "17")));
	}

	@Test
	void skipsThePercentageSumCheckForNonEdgeDeviceParsers() throws Exception {
		Path file = TempSettingsFiles.write("devices-other-type.xml",
				TempSettingsFiles.replaceFirstElement(devicesXml, "percentage", "17"));
		assertTrue(new NonEdgeDeviceParser(file.toString()).parse());
	}

	@Test
	void rejectsInvalidInitialBatteryLevels() throws Exception {
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "initialBatteryLevel", "0")));
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "initialBatteryLevel", "101")));
	}

	@Test
	void rejectsMaxConsumptionNotGreaterThanIdleConsumption() throws Exception {
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "maxConsumption", "0.2")));
	}

	@Test
	void rejectsMaxPauseDurationLowerThanMinPauseDuration() throws Exception {
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "maxPauseDuration", "99")));
	}

	@Test
	void rejectsMaxMobilityDurationLowerThanMinMobilityDuration() throws Exception {
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(devicesXml, "maxMobilityDuration", "59")));
	}

	@Test
	void acceptsEqualMinimumAndMaximumDurations() throws Exception {
		String xml = TempSettingsFiles.replaceFirstElement(devicesXml, "maxPauseDuration", "100");
		xml = TempSettingsFiles.replaceFirstElement(xml, "maxMobilityDuration", "60");
		assertTrue(parse(xml));
	}

	@Test
	void acceptsBatteryPoweredDevicesWithFullInitialLevel() throws Exception {
		assertTrue(parse(devicesXml));
	}

	@Test
	void acceptsNonBatteryPoweredDevicesWithZeroBatteryLevel() throws Exception {
		String xml = TempSettingsFiles.replaceFirstElement(devicesXml, "battery", "false");
		assertTrue(parse(xml));
	}
}
