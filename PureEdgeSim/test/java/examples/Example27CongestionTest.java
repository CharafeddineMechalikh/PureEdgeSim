package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.ApplicationFileParser;
import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Tests the congested uplink of Example 27: that the scenario really offers more
 * traffic than the link can carry, that the ablation changes one setting and
 * nothing else, that the auditor samples each task once and reports the tail of
 * the delay, and that a real run of both models disagrees the way the models are
 * supposed to disagree.
 */
class Example27CongestionTest extends AbstractTest {

	private static final String EXAMPLE_SETTINGS = "PureEdgeSim/examples/Example27/settings/";
	private static final String TEST_OUTPUT = OUTPUT_FOLDER + "Example27/";
	/** The one application of the scenario, declared by its type. */
	private static final String VIDEO_UPLOAD = "Normal";

	/** The nominal capacity of the shared link, in bits per second. */
	private static final double WAN_BITS_PER_SECOND = 3_000_000;
	/** How many devices send, and how often. */
	private static final int DEVICES = 20;
	private static final int REQUESTS_PER_MINUTE = 1;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "CLOUD_ONLY" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
	}

	// ------------------------------------------------------------- the scenario

	@Test
	void theScenarioOffersMoreThanTheLinkCanCarry() {
		assertTrue(new ParametersParser(EXAMPLE_SETTINGS + "simulation_parameters.properties").parse());
		assertTrue(new ApplicationFileParser(EXAMPLE_SETTINGS + "applications.xml").parse());

		assertTrue(SimulationParameters.realisticNetworkModel, "transfers advance over the link");
		assertTrue(SimulationParameters.useOneSharedWanLink, "all the cloud traffic shares one link");
		assertEquals(WAN_BITS_PER_SECOND, SimulationParameters.wanBandwidthBitsPerSecond, 1e-9,
				"three megabits per second, which is the bottleneck of the scenario");
		assertEquals(0.1, SimulationParameters.networkUpdateInterval, 1e-9,
				"the update step stays well under the transfer times it is meant to resolve");

		assertEquals(DEVICES, SimulationParameters.minNumberOfEdgeDevices);
		assertEquals(DEVICES, SimulationParameters.maxNumberOfEdgeDevices);
		assertEquals("CLOUD_ONLY", SimulationParameters.orchestrationArchitectures[0], "the congested iteration");
		assertEquals("EDGE_ONLY", SimulationParameters.orchestrationArchitectures[1], "the reference iteration");

		double offered = DEVICES * REQUESTS_PER_MINUTE * application(VIDEO_UPLOAD).getRequestSize() / 60.0;
		assertTrue(offered > WAN_BITS_PER_SECOND,
				"the devices offer " + offered + " bits per second to a " + WAN_BITS_PER_SECOND + " bits link");
	}

	@Test
	void theLinearVariantChangesOneSettingAndNothingElse() throws Exception {
		List<String> realistic = settingsLines("simulation_parameters.properties");
		List<String> linear = settingsLines("linear-network-model.properties");

		assertEquals(realistic.size(), linear.size(), "the two files declare the same settings");
		int differences = 0;
		for (int i = 0; i < realistic.size(); i++) {
			if (realistic.get(i).equals(linear.get(i)))
				continue;
			differences++;
			assertEquals("realistic_network_model=true", realistic.get(i),
					"the ablation is the model, not the scenario");
			assertEquals("realistic_network_model=false", linear.get(i));
		}
		assertEquals(1, differences, "one setting differs, so the two runs are comparable");
	}

	@Test
	void thePayloadIsAVideoNotAComputation() {
		assertTrue(new ApplicationFileParser(EXAMPLE_SETTINGS + "applications.xml").parse());

		Application video = application(VIDEO_UPLOAD);
		assertEquals(12_000_000, video.getRequestSize(), 1e-9, "twelve megabits up, which the link cannot carry");
		assertEquals(400_000, video.getResultsSize(), 1e-9, "the answer is small, so the return path is not the point");
		assertEquals(5000, video.getTaskLength(), 1e-9,
				"five thousand million instructions are a tenth of a second, so the deadline is about the link");
		assertEquals(25, video.getLatency(), 1e-9, "the budget is a promise about the network");
		assertTrue(video.hasRandomArrival(),
				"poisson arrivals, because a burst that starts together shares the link even in the one step model");
	}

	// -------------------------------------------------------------- the auditor

	@Test
	void theAuditorSamplesEachTaskOnceAndReportsTheTail() {
		Example27CongestionAuditor auditor = new Example27CongestionAuditor(new SimLog("test", false),
				new PureEdgeSim(), 0, 0, new Scenario(0, 0, 0));

		auditor.sample(task(1, 10, 1, 1), true);
		auditor.sample(task(2, 30, 1, 1), false);
		auditor.sample(task(1, 99, 1, 1), true);

		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
			auditor.onSimulationEnd();
		} finally {
			System.setOut(originalOut);
		}
		String output = console.toString(StandardCharsets.UTF_8);

		assertTrue(output.contains("2 tasks sampled, 1 succeeded, 1 failed"), output);
		assertTrue(output.contains("FAILED_DUE_TO_LATENCY 1"), "the reason of the failure is named: " + output);
		assertTrue(output.contains("mean 20.000 s"), "one tenth and one thirty give a mean of twenty: " + output);
		assertTrue(output.contains("p90 30.000 s"), "the tail is reported, not only the average: " + output);
		assertTrue(output.contains("the network accounts for 90.9 percent"),
				"forty seconds of network over forty four of delay: " + output);
	}

	@Test
	void aTaskThatNeverReachesItsEndIsNotCountedTwice() {
		Example27CongestionAuditor auditor = new Example27CongestionAuditor(new SimLog("test", false),
				new PureEdgeSim(), 0, 0, new Scenario(0, 0, 0));
		auditor.sample(task(7, 5, 0, 0), false);
		auditor.sample(task(7, 5, 0, 0), false);

		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
			auditor.onSimulationEnd();
		} finally {
			System.setOut(originalOut);
		}
		assertTrue(console.toString(StandardCharsets.UTF_8).contains("1 tasks sampled"),
				"the deduplication is by task, not by event");
	}

	// ----------------------------------------------------------- one real run

	@Test
	@Tag("integration")
	void theRealisticModelCollapsesTheSaturatedLink() throws Exception {
		String output = run(false);

		String cloud = line(output, "CLOUD_ONLY - .* tasks sampled");
		assertTrue(firstNumber(cloud, "(\\d+) succeeded") < firstNumber(cloud, "(\\d+) failed"),
				"a link over its capacity cannot carry the traffic: " + cloud);
		assertTrue(firstNumber(cloud, "FAILED_DUE_TO_LATENCY (\\d+)") > 0, "and it fails by latency: " + cloud);
		String slow = line(output, "CLOUD_ONLY - network time:.*");
		assertTrue(firstNumber(slow, "median ([\\d.]+) s") > 25,
				"the median request is over the twenty five seconds it was promised: " + slow);

		String edge = line(output, "EDGE_ONLY - .* tasks sampled");
		assertEquals(0, firstNumber(edge, "(\\d+) failed"), 1e-9,
				"the same traffic over the local link is fine: " + edge);
	}

	@Test
	@Tag("integration")
	void theOneStepModelDoesNotSeeTheSaturatedLink() throws Exception {
		String realistic = run(false);
		String linear = run(true);

		double realisticMedian = firstNumber(line(realistic, "CLOUD_ONLY - network time:.*"), "median ([\\d.]+) s");
		double linearMedian = firstNumber(line(linear, "CLOUD_ONLY - network time:.*"), "median ([\\d.]+) s");

		assertNotEquals(0, realisticMedian, 1e-9);
		assertTrue(linearMedian < realisticMedian / 5,
				"without progressive transfers the queue never builds, so the same link looks " + linearMedian
						+ " s late instead of " + realisticMedian + " s");
		assertEquals(0, firstNumber(line(linear, "CLOUD_ONLY - .* tasks sampled"), "(\\d+) failed"), 1e-9,
				"and the run that sees no congestion loses no request at all");
	}

	// --------------------------------------------------------------- utilities

	/**
	 * Reads one of the two parameter files of the example and keeps only the
	 * settings, so that a comment rewritten for the variant is not mistaken for a
	 * change of scenario.
	 *
	 * @param fileName the name of the file, in the settings folder of the example
	 * @return the non comment lines, trimmed
	 * @throws Exception when the file cannot be read
	 */
	private static List<String> settingsLines(String fileName) throws Exception {
		return Files.readAllLines(Path.of(EXAMPLE_SETTINGS + fileName)).stream().map(String::trim)
				.filter(line -> !line.isEmpty() && !line.startsWith("#")).collect(Collectors.toList());
	}

	/**
	 * Runs the example scenario once, with the given network model.
	 *
	 * @param linear whether the transfers complete in one step
	 * @return everything the run printed
	 * @throws Exception when the output cannot be captured
	 */
	private String run(boolean linear) throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomSimulationManager(Example27CongestionAuditor.class);
			simulation.setCustomSettingsFolder(EXAMPLE_SETTINGS);
			simulation.setCustomOutputFolder(TEST_OUTPUT);
			if (linear)
				simulation.setCustomFilePath(EXAMPLE_SETTINGS + "linear-network-model.properties",
						SimulationAbstract.Files.SIMULATION_PARAMETERS);
			System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
			simulation.launchSimulation();
		} finally {
			System.setOut(originalOut);
		}
		return console.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Returns the application of the given type declared by the settings folder.
	 *
	 * @param type the application type
	 * @return the application
	 */
	private Application application(String type) {
		return SimulationParameters.applicationList.stream()
				.filter(application -> application.getType().equals(type)).findFirst()
				.orElseThrow(() -> new AssertionError(type + " is not declared in the applications file"));
	}

	/**
	 * Builds a task whose delay breakdown is known, for the auditor.
	 *
	 * @param id          the task identifier
	 * @param networkTime the time its data spent crossing links, in seconds
	 * @param waitingTime the time it queued at its destination
	 * @param cpuTime     its execution time
	 * @return the task
	 */
	private Task task(int id, double networkTime, double waitingTime, double cpuTime) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getActualNetworkTime()).thenReturn(networkTime);
		when(task.getWatingTime()).thenReturn(waitingTime);
		when(task.getActualCpuTime()).thenReturn(cpuTime);
		when(task.getFailureReason()).thenReturn(Task.FailureReason.FAILED_DUE_TO_LATENCY);
		return task;
	}

	/**
	 * Returns the first line matching the given pattern.
	 *
	 * @param output  the captured console
	 * @param pattern the pattern to look for
	 * @return the line it ends with, or the pattern itself when nothing matched
	 */
	private static String line(String output, String pattern) {
		Matcher matcher = Pattern.compile(pattern).matcher(output);
		if (!matcher.find())
			return "<missing " + pattern + ">";
		int end = output.indexOf('\n', matcher.end());
		return output.substring(matcher.start(), end < 0 ? output.length() : end);
	}

	/**
	 * Returns the first number captured by the given pattern.
	 *
	 * @param text    the text to search
	 * @param pattern a pattern with one numeric group
	 * @return the number it captured
	 */
	private static double firstNumber(String text, String pattern) {
		Matcher matcher = Pattern.compile(pattern).matcher(text);
		assertTrue(matcher.find(), "no number matched by " + pattern + " in " + text);
		return Double.parseDouble(matcher.group(1));
	}
}
