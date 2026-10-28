package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Tests the replay of the recorded workload of Example 25, from the trace file
 * up to a full simulation run.
 */
class Example25TraceReplayTest extends AbstractTest {

	private static final String SETTINGS = "PureEdgeSim/examples/Example25/settings/";
	private static final String TEST_OUTPUT = "target/test-output/Example25/";

	@Test
	void everyInvocationOfTheRecordedTraceBecomesOneRequest() {
		fullDuration();
		Example25TraceTaskGenerator generator = traceGenerator(device(1), device(2), device(3));

		List<Task> tasks = tasks(generator);

		assertEquals(8819, tasks.size(), "the committed trace holds 8819 recorded invocations");
		assertEquals(1, tasks.get(0).getTime(), "the first invocation is replayed after the initialization");
		assertEquals(3437, tasks.get(tasks.size() - 1).getTime(),
				"the last one arrives 3436 seconds after the first");
		assertTrue(tasks.stream().allMatch(task -> task.getRequestTaskCount() == 1),
				"every recorded invocation is a request of its own");
	}

	@Test
	void theRecordedTokensSetTheSizeTheLengthAndTheDeadlineOfTheRequest() {
		fullDuration();
		Example25TraceTaskGenerator generator = traceGenerator(device(1));

		Task first = tasks(generator).get(0);

		// The first line of the trace is one request with 4808 prompt tokens and a
		// completion of 10 tokens, mapped with the default values of the example.
		assertEquals(4808 * 4 * 8, first.getFileSizeInBits(), 1e-9, "4 bytes per prompt token, sent up");
		assertEquals(10 * 4 * 8, first.getOutputSizeInBits(), 1e-9, "4 bytes per completion token, sent back");
		assertEquals(4808 * 12 + 10 * 2000, first.getLength(), 1e-9,
				"the prompt is read once, the completion is produced token by token");
		assertEquals(2 + 10 * 0.1, first.getMaxLatency(), 1e-9, "the budget grows with the completion length");
		assertEquals(1 + 2 + 10 * 0.1, first.getRequestDeadline(), 1e-9, "and is counted from the arrival");
	}

	@Test
	void theRequestsAreHandedToTheDevicesInTurn() {
		fullDuration();
		ComputingNode first = device(1);
		ComputingNode second = device(2);
		ComputingNode third = device(3);
		Example25TraceTaskGenerator generator = traceGenerator(first, second, third);

		List<Task> tasks = tasks(generator);
		Map<ComputingNode, Long> perDevice = tasks.stream()
				.collect(Collectors.groupingBy(Task::getEdgeDevice, Collectors.counting()));

		assertEquals(List.of(first, second, third),
				tasks.stream().map(Task::getEdgeDevice).limit(3).collect(Collectors.toList()),
				"the recorded requests go to one device after the other");
		assertEquals(List.of(2940L, 2940L, 2939L),
				List.of(perDevice.get(first), perDevice.get(second), perDevice.get(third)),
				"the fleet shares the recorded hour as evenly as 8819 requests allow");
	}

	@Test
	void aShorterRunStopsTheReplayInsteadOfFailingIt() {
		SimulationParameters.simulationDuration = 600;
		Example25TraceTaskGenerator generator = traceGenerator(device(1), device(2));

		List<Task> tasks = tasks(generator);

		assertEquals(1482, tasks.size(), "the trace holds 1482 invocations during its first ten minutes");
		assertEquals(587, tasks.get(tasks.size() - 1).getTime(),
				"the last request replayed is the last one that arrives before the end of the run");
	}

	@Test
	void theReplayDoesNotDrawAnyRandomValue() {
		fullDuration();
		Randomness.setSeed(1);
		List<Double> withSeedOne = times(tasks(traceGenerator(device(1), device(2))));
		Randomness.setSeed(2);
		List<Double> withSeedTwo = times(tasks(traceGenerator(device(1), device(2))));

		assertEquals(withSeedOne, withSeedTwo,
				"the same trace and the same fleet give the same task list whatever the seed is");
	}

	@Test
	void theExampleSettingsReplayTheTraceOnASingleEdgeServer() {
		assertTrue(new ParametersParser(SETTINGS + "simulation_parameters.properties").parse());
		assertEquals("EDGE_ONLY", SimulationParameters.orchestrationArchitectures[0]);
		assertEquals(58 * 60, SimulationParameters.simulationDuration, 1e-9,
				"the simulated period has to cover the 3436 s of the recorded hour");
		assertEquals(50, SimulationParameters.minNumberOfEdgeDevices,
				"fifty devices replay the recorded hour, one request per device every 20 seconds on average");
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheReplayedRequests() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomTaskGenerator(Example25TraceTaskGenerator.class);
			simulation.setCustomSettingsFolder(SETTINGS);
			simulation.setCustomOutputFolder(TEST_OUTPUT);
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		Path run = Files.list(Path.of(TEST_OUTPUT)).filter(Files::isDirectory)
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder in " + TEST_OUTPUT));
		List<String> lines = Files.readAllLines(run.resolve("Sequential_simulation.csv"));
		String[] result = lines.get(1).split(",");

		assertTrue(lines.get(0).contains("Orchestration architecture"), run.toString());
		assertEquals("EDGE_ONLY", result[0], "every request is served by the edge data center");
		assertEquals(8819, Integer.parseInt(result[7]), "the whole recorded hour reaches the simulator");
		assertEquals(8819, Integer.parseInt(result[17]), "and none of it is executed on another tier");
		assertTrue(Integer.parseInt(result[8]) < 8819,
				"the bursts of the recorded traffic make requests miss their deadline, unlike random ones");
	}

	/**
	 * Declares the application of the scenario and a period long enough for the
	 * whole recorded hour.
	 */
	private void fullDuration() {
		SimulationParameters.simulationDuration = 3600;
		SimulationParameters.applicationList = List.of(new Application("Soft Real-time", 3, 100, 4.8,
				2000 * 8192L, 8 * 8192L, 102L, 80960));
	}

	/**
	 * Creates a generator for the given devices, the way the simulation manager
	 * does when it launches the task generator module.
	 *
	 * @param devices the edge devices that send the requests
	 * @return a generator reading the committed trace
	 */
	private Example25TraceTaskGenerator traceGenerator(ComputingNode... devices) {
		DefaultSimulationManager manager = new DefaultSimulationManager(new SimLog("test", false),
				new PureEdgeSim(), 0, 0, new Scenario(devices.length, 0, 0));
		ComputingNodesGenerator nodesGenerator = mock(ComputingNodesGenerator.class);
		when(nodesGenerator.getMistOnlyList()).thenReturn(List.of(devices));
		when(nodesGenerator.getCloudOnlyList()).thenReturn(List.of(mock(ComputingNode.class)));
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(nodesGenerator);
		manager.setDataCentersManager(dataCentersManager);
		return new Example25TraceTaskGenerator(manager);
	}

	/**
	 * @param generator the generator to run
	 * @return its tasks, in the order the trace recorded them
	 */
	private List<Task> tasks(Example25TraceTaskGenerator generator) {
		return generator.generate().stream().sorted(Comparator.comparingInt(Task::getRequestId))
				.collect(Collectors.toList());
	}

	private List<Double> times(List<Task> tasks) {
		return tasks.stream().map(Task::getTime).collect(Collectors.toList());
	}

	private ComputingNode device(int id) {
		ComputingNode device = mock(ComputingNode.class);
		lenient().when(device.isGeneratingTasks()).thenReturn(true);
		lenient().when(device.getId()).thenReturn(id);
		return device;
	}
}
