package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

class DatacentersParserTest extends AbstractTest {

	private String cloudXml;
	private String edgeXml;

	@BeforeEach
	void setUp() throws Exception {
		TempSettingsFiles.resetDirectory();
		cloudXml = TempSettingsFiles.resource("cloud.xml");
		edgeXml = TempSettingsFiles.resource("edge_datacenters.xml");
		SimulationParameters.numberOfCloudDataCenters = 0;
		SimulationParameters.numberOfEdgeDataCenters = 0;
	}

	@AfterEach
	void cleanUp() throws Exception {
		TempSettingsFiles.resetDirectory();
	}

	private boolean parseCloud(String xml) throws Exception {
		Path file = TempSettingsFiles.write("cloud-" + System.nanoTime() + ".xml", xml);
		return new DatacentersParser(file.toString(), TYPES.CLOUD).parse();
	}

	private boolean parseEdge(String xml) throws Exception {
		Path file = TempSettingsFiles.write("edge-" + System.nanoTime() + ".xml", xml);
		return new DatacentersParser(file.toString(), TYPES.EDGE_DATACENTER).parse();
	}

	@Test
	void parsesTheReferenceCloudFile() {
		assertTrue(new DatacentersParser(SETTINGS_FOLDER + "cloud.xml", TYPES.CLOUD).parse());
		assertEquals(1, SimulationParameters.numberOfCloudDataCenters);
		assertEquals(0, SimulationParameters.numberOfEdgeDataCenters);
	}

	@Test
	void parsesTheReferenceEdgeDatacentersFile() {
		assertTrue(new DatacentersParser(SETTINGS_FOLDER + "edge_datacenters.xml", TYPES.EDGE_DATACENTER).parse());
		assertEquals(1, SimulationParameters.numberOfEdgeDataCenters);
		assertEquals(0, SimulationParameters.numberOfCloudDataCenters);
	}

	@Test
	void cloudDatacenterDoesNotNeedALocation() throws Exception {
		assertTrue(parseCloud(TempSettingsFiles.removeFirstElement(cloudXml, "location")));
		assertEquals(1, SimulationParameters.numberOfCloudDataCenters);
	}

	@Test
	void nonCloudTypeUsesTheEdgeDatacenterBranch() throws Exception {
		Path file = TempSettingsFiles.write("edge-as-device.xml", edgeXml);
		assertTrue(new DatacentersParser(file.toString(), TYPES.EDGE_DEVICE).parse());
		assertEquals(1, SimulationParameters.numberOfEdgeDataCenters);
	}

	@Test
	void returnsFalseWhenTheFileDoesNotExist() {
		assertFalse(new DatacentersParser(TempSettingsFiles.DIR.resolve("missing.xml").toString(), TYPES.CLOUD).parse());
		assertFalse(new DatacentersParser(TempSettingsFiles.DIR.resolve("missing.xml").toString(),
				TYPES.EDGE_DATACENTER).parse());
	}

	@Test
	void returnsFalseForMalformedXml() throws Exception {
		assertFalse(parseCloud("<cloud_data_centers><datacenter></cloud_data_centers>"));
		assertFalse(parseEdge("<edge_datacenters><datacenter></edge_datacenters>"));
	}

	@Test
	void returnsFalseWhenDoctypeIsDeclared() throws Exception {
		String xml = "<?xml version=\"1.0\"?><!DOCTYPE cloud_data_centers [<!ENTITY x \"y\">]>"
				+ "<cloud_data_centers><datacenter><idleConsumption>0</idleConsumption>"
				+ "<maxConsumption>10</maxConsumption><isOrchestrator>false</isOrchestrator>"
				+ "<cores>1</cores><mips>1</mips><ram>1</ram><storage>1</storage></datacenter></cloud_data_centers>";
		assertFalse(parseCloud(xml));
	}

	@Test
	void rejectsMissingMandatoryDatacenterElements() throws Exception {
		String[] tags = { "isOrchestrator", "idleConsumption", "maxConsumption", "cores", "mips", "ram", "storage" };
		for (String tag : tags) {
			assertFalse(parseCloud(TempSettingsFiles.removeFirstElement(cloudXml, tag)),
					"cloud file without <" + tag + "> should be rejected");
		}
	}

	@Test
	void rejectsNonPositiveResourceValues() throws Exception {
		String[] tags = { "cores", "mips", "ram", "storage" };
		for (String tag : tags) {
			assertFalse(parseCloud(TempSettingsFiles.replaceFirstElement(cloudXml, tag, "0")),
					"cloud file with " + tag + "=0 should be rejected");
			assertFalse(parseCloud(TempSettingsFiles.replaceFirstElement(cloudXml, tag, "-1")),
					"cloud file with " + tag + "=-1 should be rejected");
		}
	}

	@Test
	void rejectsNegativeIdleConsumption() throws Exception {
		assertFalse(parseCloud(TempSettingsFiles.replaceFirstElement(cloudXml, "idleConsumption", "-1")));
	}

	@Test
	void rejectsMaxConsumptionThatIsNotGreaterThanIdleConsumption() throws Exception {
		assertFalse(parseCloud(TempSettingsFiles.replaceFirstElement(cloudXml, "maxConsumption", "0")));
		assertFalse(parseCloud(TempSettingsFiles.replaceFirstElement(cloudXml, "maxConsumption", "-5")));
	}

	@Test
	void rejectsEdgeDatacenterWithoutLocation() throws Exception {
		assertFalse(parseEdge(TempSettingsFiles.removeFirstElement(edgeXml, "location")));
	}

	@Test
	void rejectsEdgeDatacenterWithIncompleteLocation() throws Exception {
		assertFalse(parseEdge(TempSettingsFiles.removeFirstElement(edgeXml, "x_pos")));
		assertFalse(parseEdge(TempSettingsFiles.removeFirstElement(edgeXml, "y_pos")));
	}

	@Test
	void rejectsInvalidEdgeDatacenterCoordinates() throws Exception {
		assertFalse(parseEdge(TempSettingsFiles.replaceFirstElement(edgeXml, "x_pos", "-1")));
		assertFalse(parseEdge(TempSettingsFiles.replaceFirstElement(edgeXml, "y_pos", "0")));
		assertFalse(parseEdge(TempSettingsFiles.replaceFirstElement(edgeXml, "y_pos", "-10")));
	}

	@Test
	void edgeDatacenterWithNetworkLinksElementIsAccepted() throws Exception {
		String withLinks = edgeXml.replace("<network_links>", "<network_links><link src=\"dc1\" dst=\"dc2\" />");
		assertTrue(parseEdge(withLinks));
	}

	@Test
	void rejectsEdgeDatacenterWithoutResourceElements() throws Exception {
		String[] tags = { "cores", "mips", "ram", "storage", "idleConsumption", "maxConsumption" };
		for (String tag : tags) {
			assertFalse(parseEdge(TempSettingsFiles.removeFirstElement(edgeXml, tag)),
					"edge file without <" + tag + "> should be rejected");
		}
	}
}
