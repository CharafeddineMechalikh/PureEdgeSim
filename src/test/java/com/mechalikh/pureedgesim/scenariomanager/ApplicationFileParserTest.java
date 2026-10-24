package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.taskgenerator.Application;

class ApplicationFileParserTest extends AbstractTest {

	private String applicationsXml;

	@BeforeEach
	void loadBaseFile() throws Exception {
		TempSettingsFiles.resetDirectory();
		applicationsXml = TempSettingsFiles.resource("applications.xml");
		SimulationParameters.applicationList = null;
	}

	@AfterEach
	void cleanUp() throws Exception {
		TempSettingsFiles.resetDirectory();
	}

	private boolean parse(String xml) throws Exception {
		Path file = TempSettingsFiles.write("applications-" + System.nanoTime() + ".xml", xml);
		return new ApplicationFileParser(file.toString()).parse();
	}

	@Test
	void parsesTheReferenceApplicationFile() {
		ApplicationFileParser parser = new ApplicationFileParser(
				SETTINGS_FOLDER + "applications.xml");
		assertTrue(parser.parse());

		List<Application> applications = SimulationParameters.applicationList;
		assertEquals(3, applications.size());

		Application health = applications.get(0);
		assertEquals("Hard Real-time", health.getType());
		assertEquals(20, health.getRate());
		assertEquals(20.0, health.getUsagePercentage(), 0.0001);
		assertEquals(0.02, health.getLatency(), 0.0001);
		assertEquals(160_000, health.getContainerSizeInBits());
		assertEquals(160_000, health.getRequestSize());
		assertEquals(160_000, health.getResultsSize());
		assertEquals(500.0, health.getTaskLength(), 0.0001);
		assertEquals(0.02, health.getContainerSizeInMBytes(), 0.0001);

		Application heavy = applications.get(2);
		assertEquals("Non Real-time", heavy.getType());
		assertEquals(3, heavy.getRate());
		assertEquals(17_600_000, heavy.getContainerSizeInBits());
		assertEquals(20_000_000, heavy.getRequestSize());
		assertEquals(1_600_000, heavy.getResultsSize());
		assertEquals(30000.0, heavy.getTaskLength(), 0.0001);
	}

	@Test
	void emptyApplicationFileIsValidAndProducesAnEmptyList() throws Exception {
		assertTrue(parse("<?xml version=\"1.0\"?><applications></applications>"));
		assertNotNull(SimulationParameters.applicationList);
		assertTrue(SimulationParameters.applicationList.isEmpty());
	}

	@Test
	void returnsFalseWhenTheFileDoesNotExist() {
		ApplicationFileParser parser = new ApplicationFileParser(
				TempSettingsFiles.DIR.resolve("missing-applications.xml").toString());
		assertFalse(parser.parse());
	}

	@Test
	void returnsFalseForMalformedXml() throws Exception {
		assertFalse(parse("<applications><application name=\"broken\"></applications>"));
	}

	@Test
	void returnsFalseWhenDoctypeIsDeclared() throws Exception {
		String xml = "<?xml version=\"1.0\"?><!DOCTYPE applications [<!ENTITY x \"y\">]>"
				+ "<applications><application name=\"a\"><type>&x;</type></application></applications>";
		assertFalse(parse(xml));
	}

	@Test
	void rejectsAnApplicationWithoutNameAttribute() throws Exception {
		assertFalse(parse(applicationsXml.replaceFirst("<application name=\"Health\">", "<application>")));
	}

	@Test
	void rejectsAnApplicationWithAnEmptyNameAttribute() throws Exception {
		assertFalse(parse(applicationsXml.replaceFirst("<application name=\"Health\">", "<application name=\"\">")));
	}

	@Test
	void rejectsAMissingMandatoryElement() throws Exception {
		String[] tags = { "type", "latency", "usage_percentage", "container_size", "request_size", "results_size",
				"task_length", "rate" };
		for (String tag : tags) {
			String xml = TempSettingsFiles.removeFirstElement(applicationsXml, tag);
			assertFalse(parse(xml), "application file without <" + tag + "> should be rejected");
		}
	}

	@Test
	void rejectsNonPositiveNumericValues() throws Exception {
		String[] tags = { "latency", "container_size", "request_size", "results_size", "task_length", "rate",
				"usage_percentage" };
		for (String tag : tags) {
			String xml = TempSettingsFiles.replaceFirstElement(applicationsXml, tag, "0");
			assertFalse(parse(xml), "application file with " + tag + "=0 should be rejected");
		}
	}

	@Test
	void rejectsNonNumericValues() throws Exception {
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(applicationsXml, "latency", "not-a-number")));
		assertFalse(parse(TempSettingsFiles.replaceFirstElement(applicationsXml, "rate", "")));
	}

	@Test
	void rejectsNegativeValues() throws Exception {
		String[] tags = { "latency", "container_size", "request_size", "results_size", "task_length", "rate",
				"usage_percentage" };
		for (String tag : tags) {
			String xml = TempSettingsFiles.replaceFirstElement(applicationsXml, tag, "-2");
			assertFalse(parse(xml), "application file with " + tag + "=-2 should be rejected");
		}
	}
}
