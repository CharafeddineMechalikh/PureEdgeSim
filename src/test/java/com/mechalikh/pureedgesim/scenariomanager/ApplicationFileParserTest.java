package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;

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

	// ------------------------------------------------------------------
	// Distribution-valued parameters
	// ------------------------------------------------------------------

	/** Replaces the first occurrence of a tag by a raw element, attributes included. */
	private String withElement(String xml, String tag, String raw) {
		return xml.replaceFirst("(?s)<" + tag + "\\b[^>]*>.*?</" + tag + ">", Matcher.quoteReplacement(raw));
	}

	@Test
	void aDistributionParameterIsStoredAndSampled() throws Exception {
		String xml = withElement(applicationsXml, "request_size",
				"<request_size distribution=\"uniform\" min=\"10\" max=\"30\"/>");
		assertTrue(parse(xml));

		Application health = SimulationParameters.applicationList.get(0);
		// The reported value is the mean of the distribution, converted to bits.
		assertEquals(20.0 * 8000, health.getRequestSize(), 0.0001);
		assertEquals(20.0 * 8000, health.getRequestSizeDistribution().mean(), 0.0001);

		for (int i = 0; i < 300; i++) {
			long sample = health.sampleRequestSize();
			assertTrue(sample >= 10 * 8000 && sample <= 30 * 8000,
					"sampled request size " + sample + " left the configured range");
		}
	}

	@Test
	void everyParameterOfAnApplicationMayBeADistribution() throws Exception {
		String xml = applicationsXml;
		xml = withElement(xml, "rate", "<rate distribution=\"poisson\" mean=\"10\"/>");
		xml = withElement(xml, "latency", "<latency distribution=\"lognormal\" mean=\"1\" std=\"0.5\"/>");
		xml = withElement(xml, "container_size", "<container_size distribution=\"exponential\" mean=\"100\"/>");
		xml = withElement(xml, "results_size", "<results_size distribution=\"gamma\" shape=\"2\" scale=\"5\"/>");
		xml = withElement(xml, "task_length", "<task_length distribution=\"weibull\" shape=\"2\" scale=\"600\"/>");
		xml = withElement(xml, "usage_percentage", "<usage_percentage distribution=\"uniform\" min=\"10\" max=\"30\"/>");
		assertTrue(parse(xml));

		Application health = SimulationParameters.applicationList.get(0);
		assertEquals(10.0, health.getRateDistribution().mean(), 0.0001);
		assertEquals(1.0, health.getLatency(), 0.0001);
		assertEquals(100.0 * 8000, health.getContainerSizeInBits(), 0.0001);
		// The gamma distribution of shape 2 and scale 5 has a mean of 10 kilobytes.
		assertEquals(80_000, health.getResultsSize(), 0.5);
		assertTrue(health.getTaskLengthDistribution().mean() > 0, "the weibull mean must be positive");
		assertEquals(20.0, health.getUsagePercentage(), 0.0001);
		assertTrue(health.sampleTaskLength() > 0);
		assertTrue(health.sampleTasksPerMinute() >= 0, "a rate distribution must never give a negative count");
	}

	@Test
	void aPositiveDistributionIsTruncatedAtZero() throws Exception {
		String xml = withElement(applicationsXml, "task_length",
				"<task_length distribution=\"normal\" mean=\"100\" std=\"200\"/>");
		assertTrue(parse(xml));

		Application health = SimulationParameters.applicationList.get(0);
		assertEquals(100.0, health.getTaskLength(), 0.0001);
		boolean sawVariation = false;
		for (int i = 0; i < 500; i++) {
			long length = health.sampleTaskLength();
			assertTrue(length > 0, "a task length of " + length + " MI cannot be executed");
			sawVariation |= length != 100;
		}
		assertTrue(sawVariation, "a normal distribution must not sample the same value every time");
	}

	@Test
	void aDistributionWithZeroMeanIsRejected() throws Exception {
		assertFalse(parse(withElement(applicationsXml, "rate", "<rate distribution=\"constant\" value=\"0\"/>")));
	}

	@Test
	void anEmptyDistributionElementStillPassesThePresenceCheck() throws Exception {
		assertTrue(parse(withElement(applicationsXml, "latency",
				"<latency distribution=\"uniform\" min=\"0.1\" max=\"0.2\"/>")));
	}

	@Test
	void aBrokenDistributionMakesTheFileUnreadable() throws Exception {
		assertFalse(parse(withElement(applicationsXml, "latency", "<latency distribution=\"student\"/>")));
		assertFalse(parse(withElement(applicationsXml, "latency", "<latency distribution=\"uniform\" min=\"5\"/>")));
	}

	// ------------------------------------------------------------------
	// The optional arrival pattern
	// ------------------------------------------------------------------

	@Test
	void noArrivalElementMeansEvenlySpacedTasks() throws Exception {
		assertTrue(parse(applicationsXml));
		Application health = SimulationParameters.applicationList.get(0);
		assertFalse(health.hasRandomArrival());
		assertNull(health.getArrivalDistribution());
	}

	@Test
	void poissonArrivalUsesTheGenerationRateAsItsMeanGap() throws Exception {
		String xml = applicationsXml.replaceFirst("<rate>20</rate>", "<rate>20</rate><arrival>poisson</arrival>");
		assertTrue(parse(xml));

		Application health = SimulationParameters.applicationList.get(0);
		assertTrue(health.hasRandomArrival());
		assertEquals("exponential", health.getArrivalDistribution().getName());
		// 20 tasks per minute -> one task every 3 seconds on average.
		assertEquals(3.0, health.getArrivalDistribution().mean(), 0.0001);
	}

	@Test
	void theExponentialKeywordIsAcceptedAsASynonymOfPoisson() throws Exception {
		String xml = applicationsXml.replaceFirst("<rate>20</rate>", "<rate>20</rate><arrival>EXPONENTIAL</arrival>");
		assertTrue(parse(xml));
		assertTrue(SimulationParameters.applicationList.get(0).hasRandomArrival());
	}

	@Test
	void uniformArrivalCentersItsGapsOnTheSameMean() throws Exception {
		String xml = applicationsXml.replaceFirst("<rate>3</rate>", "<rate>3</rate><arrival>uniform</arrival>");
		assertTrue(parse(xml));

		Application heavy = SimulationParameters.applicationList.get(2);
		assertEquals("uniform", heavy.getArrivalDistribution().getName());
		assertEquals(20.0, heavy.getArrivalDistribution().mean(), 0.0001);
	}

	@Test
	void theConstantAndEvenKeywordsKeepTheHistoricalBehavior() throws Exception {
		for (String keyword : List.of("constant", "even", "")) {
			String xml = applicationsXml.replaceFirst("<rate>20</rate>",
					"<rate>20</rate><arrival>" + keyword + "</arrival>");
			assertTrue(parse(xml), "arrival=\"" + keyword + "\" should mean evenly spaced tasks");
			assertFalse(SimulationParameters.applicationList.get(0).hasRandomArrival());
		}
	}

	@Test
	void anArrivalDistributionReplacesTheRateBasedGap() throws Exception {
		String xml = applicationsXml.replaceFirst("<rate>20</rate>",
				"<rate>20</rate><arrival distribution=\"gamma\" shape=\"2\" scale=\"4\"/>");
		assertTrue(parse(xml));

		Application health = SimulationParameters.applicationList.get(0);
		assertTrue(health.hasRandomArrival());
		assertEquals("gamma", health.getArrivalDistribution().getName());
		assertEquals(8.0, health.getArrivalDistribution().mean(), 0.0001);
	}

	@Test
	void anUnknownArrivalKeywordIsRejected() throws Exception {
		String xml = applicationsXml.replaceFirst("<rate>20</rate>", "<rate>20</rate><arrival>markov</arrival>");
		assertFalse(parse(xml));
	}
}
