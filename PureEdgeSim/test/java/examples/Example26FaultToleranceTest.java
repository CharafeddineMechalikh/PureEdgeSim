package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.scenariomanager.ApplicationFileParser;
import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Task;

import examples.Example26FaultTolerantOrchestrator.Rule;

/**
 * Tests the three mechanisms of the FTR-AdWOrch scenario of Example 26: the
 * crash of the orchestrating device and the takeover by its standby, the devices
 * that defend the real-time traffic they carry, and the orchestrator that learns
 * which way of ranking the fleet brings the most tasks back.
 */
class Example26FaultToleranceTest extends AbstractTest {

	private static final String EXAMPLE_SETTINGS = "PureEdgeSim/examples/Example26/settings/";
	private static final String TEST_SETTINGS = SETTINGS_FOLDER + "example26/";
	private static final String TEST_OUTPUT = OUTPUT_FOLDER + "Example26/";

	private static final String[] MIST = { "Mist" };
	private static final String HARD_REAL_TIME = "Hard Real-time";
	private static final String SOFT_REAL_TIME = "Soft Real-time";
	private static final String NORMAL = "Normal";

	/** The latency budget of each class, in seconds, as the paper publishes it. */
	private static final Map<String, Double> BUDGETS = Map.of(HARD_REAL_TIME, 0.015, SOFT_REAL_TIME, 0.5, NORMAL,
			30.0);

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator nodesGenerator;
	private InfrastructureGraph topology;
	private MobilityModel samePlace;
	private final List<ComputingNode> orchestrators = new ArrayList<>();

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "MIST_ONLY" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "FTR_ADWORCH" };
		SimulationParameters.edgeDevicesRange = 300;
		SimulationParameters.enableOrchestrators = true;

		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0,
				new Scenario(0, 0, 0));
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		nodesGenerator = mock(ComputingNodesGenerator.class);
		topology = mock(InfrastructureGraph.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(nodesGenerator);
		when(dataCentersManager.getTopology()).thenReturn(topology);
		manager.setDataCentersManager(dataCentersManager);

		// Every device is within range of every other one, which is what the wide
		// communication range of the scenario means.
		samePlace = mock(MobilityModel.class);
		lenient().when(samePlace.distanceTo(any(ComputingNode.class))).thenReturn(0.0);
	}

	// ---------------------------------------------------------------- devices

	@Test
	void aDeviceRefusesTheWorkItCanNoLongerCarry() {
		Example26OrchestratedDevice device = device(110000, 100);
		assertTrue(device.accepts(task(1, NORMAL, 10000, 40)), "a free device takes any task");

		device.turnOff();
		assertFalse(device.accepts(task(2, HARD_REAL_TIME, 200, 30)), "a dead device takes nothing");
		device.turnOn();

		device.setAvailableStorage(20);
		assertFalse(device.accepts(task(3, HARD_REAL_TIME, 200, 30)), "the container has to fit in the storage");
		device.setAvailableStorage(250);

		device.setAvailableRam(10);
		assertFalse(device.accepts(task(4, HARD_REAL_TIME, 200, 30)), "and in the memory");
	}

	@Test
	void anAlmostFlatDeviceRefusesElasticWorkButKeepsTheRealTimeTraffic() {
		Example26OrchestratedDevice flat = device(110000, 8);
		Example26OrchestratedDevice charged = device(8000, 100);

		assertTrue(flat.isAlmostFlat(), "eight percent is below the ten at which a device stops taking work");
		assertFalse(charged.isAlmostFlat());
		assertTrue(flat.accepts(task(1, HARD_REAL_TIME, 200, 30)),
				"the traffic the fleet exists to carry is never refused for the sake of the battery");
		assertFalse(flat.accepts(task(2, NORMAL, 10000, 40)), "the elastic work moves to a device that can pay for it");
		assertTrue(charged.accepts(task(3, NORMAL, 10000, 40)));
	}

	@Test
	void aDevicePluggedToTheGridNeverRefusesWorkForEnergy() {
		Example26OrchestratedDevice device = device(110000, 100);
		device.getEnergyModel().setBattery(false);

		assertFalse(device.isAlmostFlat(), "a device that cannot run out of energy has nothing to spare");
		assertTrue(device.accepts(task(1, NORMAL, 10000, 40)));
	}

	@Test
	void aHardRealTimeTaskIsServedBeforeTheBackgroundWorkAlreadyWaiting() {
		Example26OrchestratedDevice device = device(110000, 100);
		Task background = task(1, NORMAL, 10000, 40);
		Task second = task(2, NORMAL, 10000, 40);
		Task realTime = task(3, HARD_REAL_TIME, 200, 30);

		device.submitTask(background);
		device.submitTask(second);
		assertEquals(0, device.getQueueJumps(), "the first task is executed at once, the second waits its turn");
		device.submitTask(realTime);

		assertEquals(1, device.getQueueJumps());
		assertEquals(List.of(realTime, second), device.getTasksQueue(),
				"the real-time task is served as soon as the core frees up, not behind the background work");
		assertEquals(140, device.getAvailableStorage(), 1e-9,
				"the task that jumped holds its container in the storage, as the one it passed");
	}

	@Test
	void theQueueOrderIsUntouchedWhenACoreIsFree() {
		Example26OrchestratedDevice device = device(110000, 100);
		Task realTime = task(1, HARD_REAL_TIME, 200, 30);

		device.submitTask(realTime);

		assertEquals(0, device.getQueueJumps(), "nothing jumps a queue that is empty");
		assertTrue(device.getTasksQueue().isEmpty(), "and is served at once");
	}

	@Test
	void aCrashedDeviceKeepsItsBattery() {
		Example26OrchestratedDevice device = device(110000, 100);

		device.turnOff();

		assertTrue(device.isDead(), "a crashed device stops serving");
		assertEquals(100, device.getEnergyModel().getBatteryLevelPercentage(), 1e-9,
				"and its battery is untouched: a crash is not an empty battery");
	}

	// ------------------------------------------------------ orchestration

	@Test
	void theFleetWorksForTheClosestAliveOrchestrator() {
		Example26OrchestratedDevice primary = orchestratingDevice(0.01);
		Example26OrchestratedDevice standby = orchestratingDevice(0.02);
		Example26OrchestratedDevice sensor = device(110000, 100);

		assertSame(primary, sensor.getOrchestrator());

		primary.turnOff();
		assertSame(standby, sensor.getOrchestrator(), "the fleet follows the standby the moment the primary is down");

		primary.turnOn();
		standby.turnOff();
		assertSame(primary, sensor.getOrchestrator(), "and follows it back, without waiting for anything");
	}

	@Test
	void anOutageWithNoStandbyLeavesTheFleetPointedAtTheDeadOrchestrator() {
		Example26OrchestratedDevice primary = orchestratingDevice(0.01);
		Example26OrchestratedDevice sensor = device(110000, 100);
		primary.turnOff();

		assertSame(primary, sensor.getOrchestrator(),
				"the requests of the fleet go to a dead device, which is the outage the scenario measures");
	}

	@Test
	void aFleetWithoutOrchestratingDeviceOrchestratesItself() {
		when(nodesGenerator.getOrchestratorsList()).thenReturn(List.of());
		Example26OrchestratedDevice sensor = device(110000, 100);

		assertSame(sensor, sensor.getOrchestrator());
	}

	@Test
	void withoutOrchestratorsInTheScenarioEveryNodeIsItsOwn() {
		SimulationParameters.enableOrchestrators = false;
		Example26OrchestratedDevice sensor = device(110000, 100);

		assertSame(sensor, sensor.getOrchestrator());
	}

	// ------------------------------------------------------------ orchestrator

	@Test
	void theOrchestratorOnlyAcceptsTheAlgorithmOfTheScenario() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		when(nodesGenerator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(device(110000, 100)));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example26FaultTolerantOrchestrator(manager));
		assertTrue(exception.getMessage().contains("FTR_ADWORCH"), exception.getMessage());
	}

	@Test
	void aTaskWithoutEligibleDeviceIsReportedAsUnplaced() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(server(110000));

		assertEquals(-1, orchestrator.findComputingNode(MIST, task(1, NORMAL, 10000, 40)),
				"an edge data center is not a mist destination");
		assertEquals(-1, orchestrator.findComputingNode(new String[0], task(2, NORMAL, 10000, 40)));
		assertEquals(1, orchestrator.getUnplacedTasks(), "the fleet reported one task it could not place");
	}

	@Test
	void aDeviceWithoutRoomIsSkippedAndTheMoveIsCounted() {
		Example26OrchestratedDevice noRoom = device(110000, 100);
		Example26OrchestratedDevice room = device(110000, 100);
		noRoom.setAvailableStorage(0);
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(noRoom, room);

		assertEquals(1, orchestrator.findComputingNode(MIST, task(1, NORMAL, 10000, 40)),
				"the ranking starts with the device that has no room, the task goes to the next one");
		assertEquals(1, orchestrator.getReallocatedTasks());
	}

	@Test
	void whenNobodyCanTakeTheTaskTheBestDeviceOfTheRankingKeepsIt() {
		Example26OrchestratedDevice fast = device(110000, 100);
		Example26OrchestratedDevice slow = device(8000, 100);
		fast.setAvailableStorage(0);
		slow.setAvailableStorage(0);
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(fast, slow);

		assertEquals(0, orchestrator.findComputingNode(MIST, task(1, NORMAL, 10000, 40)),
				"the task is placed anyway, so that the simulation reports why it failed");
		assertEquals(0, orchestrator.getReallocatedTasks(), "moving it would have changed nothing");
		assertEquals(0, orchestrator.getUnplacedTasks());
	}

	@Test
	void elasticWorkIsNeverLeftOnADeviceThatRefusesIt() {
		Example26OrchestratedDevice flat = device(110000, 8);
		Example26OrchestratedDevice charged = device(8000, 100);
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(flat, charged);

		assertNotEquals(0, orchestrator.findComputingNode(MIST, task(1, NORMAL, 10000, 40)),
				"whatever the rule the agent tried, the almost flat device gave the background task back");
		int moved = orchestrator.getReallocatedTasks();

		orchestrator.findComputingNode(MIST, task(2, HARD_REAL_TIME, 200, 30));

		assertEquals(moved, orchestrator.getReallocatedTasks(),
				"the real-time task was not moved for the battery: the flat device kept it, or the rule had "
						+ "already chosen the charged one");
	}

	@Test
	void aRuleTiedByItsOwnCriterionSplitsTheFleetInsteadOfHammeringOneDevice() {
		Example26OrchestratedDevice first = device(110000, 100);
		Example26OrchestratedDevice second = device(110000, 100);
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(first, second);

		int firstChoice = orchestrator.findComputingNode(MIST, task(1, NORMAL, 10000, 40));
		int secondChoice = orchestrator.findComputingNode(MIST, task(2, NORMAL, 10000, 40));

		assertNotEquals(firstChoice, secondChoice,
				"two devices alike for the rule are told apart by the work they already carry");
		assertEquals(0, orchestrator.getReallocatedTasks(), "both devices accept, nothing had to be moved");
	}

	@Test
	void theStateOfTheAgentIsTheClassTheLengthAndHowBusyTheFleetIs() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(device(110000, 100), device(8000, 100));

		assertEquals("Hard Real-time|0|0", orchestrator.getStateKey(task(1, HARD_REAL_TIME, 1999, 30), 5));
		assertEquals("Normal|1|0", orchestrator.getStateKey(task(2, NORMAL, 2000, 40), 5));
		assertEquals("Normal|2|0", orchestrator.getStateKey(task(3, NORMAL, 9999, 40), 5));
		assertEquals("Normal|3|0", orchestrator.getStateKey(task(4, NORMAL, 10000, 40), 5));
		assertEquals("Normal|4|0", orchestrator.getStateKey(task(5, NORMAL, 20000, 40), 5));
		assertEquals("Normal|5|0", orchestrator.getStateKey(task(6, NORMAL, 40000, 40), 5));

		// The third part of the state is how much of the fleet already carries work
		// given here, which only grows by placing tasks.
		assertEquals("Normal|0|0", orchestrator.getStateKey(task(7, NORMAL, 100, 40), 2),
				"an idle fleet is its own state, whatever the size of the crowd to choose from");
		orchestrator.findComputingNode(MIST, task(8, NORMAL, 100, 40));
		assertEquals("Normal|0|1", orchestrator.getStateKey(task(9, NORMAL, 100, 40), 2),
				"one task in hand for two devices: the fleet is not full yet");
		orchestrator.findComputingNode(MIST, task(10, NORMAL, 100, 40));
		assertEquals("Normal|0|1", orchestrator.getStateKey(task(11, NORMAL, 100, 40), 2),
				"one task per device is still the first bucket");
		orchestrator.findComputingNode(MIST, task(12, NORMAL, 100, 40));
		assertEquals("Normal|0|2", orchestrator.getStateKey(task(13, NORMAL, 100, 40), 2),
				"more work in hand than the fleet has devices is the last bucket");
	}

	@Test
	void aSingleSuccessIsWorthOneAndOnlyTheRuleThatWasTriedKnowsIt() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(device(110000, 100), device(8000, 100));
		Task task = task(1, HARD_REAL_TIME, 200, 30);
		String state = orchestrator.getStateKey(task, 2);

		orchestrator.findComputingNode(MIST, task);
		orchestrator.resultsReturned(task);

		Rule chosen = onlyRuleUsed(orchestrator);
		assertEquals(1, orchestrator.getQValue(state, chosen), 1e-9);
		for (Rule rule : Rule.values())
			if (rule != chosen)
				assertEquals(0, orchestrator.getQValue(state, rule), 1e-9, rule + " was never tried");
	}

	@Test
	void aHardRealTimeLateMissCostsThreeTimesHowFarItMissedItsBudget() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(device(110000, 100), device(8000, 100));
		Task task = task(1, HARD_REAL_TIME, 200, 30);
		String state = orchestrator.getStateKey(task, 2);
		orchestrator.findComputingNode(MIST, task);
		taskFailed(task, Task.FailureReason.FAILED_DUE_TO_LATENCY, 0.045);

		orchestrator.resultsReturned(task);

		assertEquals(-9, orchestrator.getQValue(state, onlyRuleUsed(orchestrator)), 1e-9,
				"three times the weight of the class, for results three times too late");
	}

	@Test
	void aSoftRealTimeLateMissCostsOneAndAHalfTimesItsOverrun() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(device(110000, 100), device(8000, 100));
		Task task = task(1, SOFT_REAL_TIME, 5000, 50);
		String state = orchestrator.getStateKey(task, 2);
		when(task.getMaxLatency()).thenReturn(0.5);
		orchestrator.findComputingNode(MIST, task);
		taskFailed(task, Task.FailureReason.FAILED_DUE_TO_LATENCY, 0.75);

		orchestrator.resultsReturned(task);

		assertEquals(-2.25, orchestrator.getQValue(state, onlyRuleUsed(orchestrator)), 1e-9,
				"results half again over the budget of the class, weighted by the 1.5 of soft real-time");
	}

	@Test
	void aContainerTooBigForTheFreeStorageIsChargedForWhatItOverflowed() {
		Example26OrchestratedDevice tight = device(110000, 100);
		tight.setAvailableStorage(50);
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(tight);
		Task task = task(1, NORMAL, 10000, 200);
		String state = orchestrator.getStateKey(task, 1);
		orchestrator.findComputingNode(MIST, task);
		taskFailed(task, Task.FailureReason.INSUFFICIENT_RESOURCES, 0);

		orchestrator.resultsReturned(task);

		assertEquals(-4, orchestrator.getQValue(state, onlyRuleUsed(orchestrator)), 1e-9,
				"the container was four times the storage that was free when the decision was taken");
	}

	@Test
	void aTaskLostToADeadDeviceCostsTheSuccessAndNothingMore() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(device(110000, 100));
		Task task = task(1, HARD_REAL_TIME, 200, 30);
		String state = orchestrator.getStateKey(task, 1);
		orchestrator.findComputingNode(MIST, task);
		taskFailed(task, Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD, 12);

		orchestrator.resultsReturned(task);

		Rule chosen = onlyRuleUsed(orchestrator);
		assertEquals(0, orchestrator.getQValue(state, chosen), 1e-9,
				"a loss of the fleet is not a penalty of the rule: it only misses the success");
		assertEquals(1, orchestrator.getRuleUsage(chosen)[2], "but it is still counted as a lost task");
	}

	@Test
	void aRuleThatKeptSeeingTheSameOutcomeSettlesOnIt() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(device(110000, 100), device(8000, 100));
		String state = orchestrator.getStateKey(task(0, NORMAL, 10000, 40), 2);
		for (int id = 1; id <= 6; id++) {
			Task task = task(id, NORMAL, 10000, 40);
			orchestrator.findComputingNode(MIST, task);
			orchestrator.resultsReturned(task);
		}

		int seen = 0;
		for (Rule rule : Rule.values()) {
			int[] usage = orchestrator.getRuleUsage(rule);
			seen += usage[0];
			if (usage[0] > 0)
				assertEquals(1, orchestrator.getQValue(state, rule), 1e-9,
						rule + " only saw successes, its value is the success itself");
		}
		assertEquals(6, seen, "every decision was credited to exactly one rule");
		assertTrue(orchestrator.getExplorations() > 0, "an agent that knows nothing has to try");
	}

	@Test
	void resultsOfATaskTheOrchestratorNeverPlacedAreIgnored() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator(device(110000, 100));

		orchestrator.resultsReturned(task(99, NORMAL, 10000, 40));

		assertEquals(0, orchestrator.getExplorations());
		for (Rule rule : Rule.values())
			assertEquals(0, orchestrator.getRuleUsage(rule)[0], rule + " was never chosen");
	}

	// --------------------------------------------------------------- settings

	@Test
	void theSettingsOfTheExampleDeclareTheScenarioOfThePaper() {
		assertTrue(new ParametersParser(EXAMPLE_SETTINGS + "simulation_parameters.properties").parse());

		assertEquals("MIST_ONLY", SimulationParameters.orchestrationArchitectures[0], "no server to fall back on");
		assertEquals("FTR_ADWORCH", SimulationParameters.orchestrationAlgorithms[0]);
		assertTrue(SimulationParameters.enableOrchestrators, "two devices carry the orchestration");
		assertEquals(100, SimulationParameters.minNumberOfEdgeDevices, "the fleet of the paper");
		assertEquals(100, SimulationParameters.maxNumberOfEdgeDevices);
		assertEquals(300, SimulationParameters.simulationDuration, 1e-9, "five simulated minutes");
	}

	@Test
	void theTestScenarioIsTheSameOneShrunk() {
		assertTrue(new ParametersParser(TEST_SETTINGS + "simulation_parameters.properties").parse());

		assertEquals("MIST_ONLY", SimulationParameters.orchestrationArchitectures[0]);
		assertEquals("FTR_ADWORCH", SimulationParameters.orchestrationAlgorithms[0]);
		assertTrue(SimulationParameters.enableOrchestrators);
		assertEquals(10, SimulationParameters.minNumberOfEdgeDevices);
		assertEquals(60, SimulationParameters.simulationDuration, 1e-9);
		assertEquals(0.001, SimulationParameters.wifiLatency, 1e-9, "the test radio is the fast radio of the "
				+ "example: an offloaded request crosses four hops, and at the five milliseconds of the framework "
				+ "default those hops alone would spend the whole budget of the health monitor");
	}

	@Test
	void theExampleDeclaresTheThreeClassesOfThePaper() {
		assertTrue(new ApplicationFileParser(EXAMPLE_SETTINGS + "applications.xml").parse());

		for (Map.Entry<String, Double> budget : BUDGETS.entrySet())
			assertEquals(budget.getValue(), application(budget.getKey()).getLatency(), 1e-9,
					budget.getKey() + " keeps the latency budget published in the paper, in seconds");

		assertEquals(200, application(HARD_REAL_TIME).getTaskLength(), 1e-9, "the health monitor is the short task");
		assertEquals(5000, application(SOFT_REAL_TIME).getTaskLength(), 1e-9);
		assertEquals(10000, application(NORMAL).getTaskLength(), 1e-9, "the background computation is the long one");

		assertEquals(25, application(HARD_REAL_TIME).getUsagePercentage(), 1e-9, "the mix of the paper is 25, 35, 40");
		assertEquals(35, application(SOFT_REAL_TIME).getUsagePercentage(), 1e-9);
		assertEquals(40, application(NORMAL).getUsagePercentage(), 1e-9);
	}

	@Test
	void theTestSettingsKeepTheBudgetsOfTheScenario() {
		assertTrue(new ApplicationFileParser(TEST_SETTINGS + "applications.xml").parse());

		for (Map.Entry<String, Double> budget : BUDGETS.entrySet())
			assertEquals(budget.getValue(), application(budget.getKey()).getLatency(), 1e-9,
					budget.getKey() + " is shrunk in fleet and in length, not in deadline");
	}

	// ----------------------------------------------------------- one real run

	@Test
	@Tag("integration")
	void aRealRunCrashesThePrimaryOrchestratorAndTheStandbyTakesOver() throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example26FaultTolerantOrchestrator.class);
			simulation.setCustomComputingNode(Example26OrchestratedDevice.class);
			simulation.setCustomSimulationManager(Example26FaultTolerantSimulationManager.class);
			simulation.setCustomSettingsFolder(TEST_SETTINGS);
			simulation.setCustomOutputFolder(TEST_OUTPUT);
			System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
			simulation.launchSimulation();
		} finally {
			System.setOut(originalOut);
		}
		String output = console.toString(StandardCharsets.UTF_8);

		double crash = firstNumber(output, "the primary orchestrator \\d+ crashed at (\\d+) s");
		double takeover = firstNumber(output, "standby orchestrator \\d+ took over at (\\d+) s");
		assertTrue(crash > 0, "the crash happens in the middle of the run, not before it starts: " + crash);
		assertTrue(takeover > crash, "the standby boots after the crash");
		assertTrue(takeover - crash <= 10, "and is serving again within its boot time");

		assertTrue(firstNumber(output, "(\\d+) requests lost with the primary") > 0,
				"the requests left in flight with the primary are what the crash costs: " + line(output, "fault tolerance:"));

		int jumps = (int) firstNumber(output, "robustness: (\\d+) hard real-time tasks jumped a queue");
		int moved = (int) firstNumber(output, "(\\d+) tasks were reallocated");
		assertTrue(jumps > 0, "a busy fleet makes real-time tasks jump queues: " + jumps);
		assertTrue(moved > 0, "and devices hand back the work they can no longer afford: " + moved);

		double hardRealTime = rate(output, HARD_REAL_TIME);
		double softRealTime = rate(output, SOFT_REAL_TIME);
		assertTrue(softRealTime > hardRealTime, "the fifteen milliseconds of the health monitor are harder to keep "
				+ "than the half second of augmented reality: " + hardRealTime + " against " + softRealTime);
		assertTrue(rate(output, NORMAL) > 0, "the background computation gets through as well");

		double hardDelay = meanDelay(output, HARD_REAL_TIME);
		double softDelay = meanDelay(output, SOFT_REAL_TIME);
		assertTrue(hardDelay < softDelay, "the health monitor is both the shorter task and the only one allowed to "
				+ "jump a queue, so it has to come back first: " + hardDelay + " against " + softDelay + " ms");
		assertTrue(softDelay < 1000 * BUDGETS.get(SOFT_REAL_TIME), "and the augmented reality traffic still fits its "
				+ "half second on average: " + softDelay + " ms");

		for (Rule rule : Rule.values())
			assertTrue(firstNumber(output, rule + " used (\\d+)") > 0, rule + " was tried, the agent explored");

		Path latest = Files.list(Path.of(TEST_OUTPUT)).filter(Files::isDirectory)
				.filter(path -> Character.isDigit(path.getFileName().toString().charAt(0)))
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder in " + TEST_OUTPUT));
		String results = Files.readString(latest.resolve("Sequential_simulation.csv"));
		assertTrue(results.contains("MIST_ONLY"), "the csv names the architecture that ran");
		assertTrue(results.contains("FTR_ADWORCH"), "and the algorithm that learned during it");
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * Creates a device of the fleet: one core, room for the containers of the
	 * scenario, the battery of the example, and a position within range of the
	 * whole fleet.
	 *
	 * @param mips              capacity of the core, in million instructions per
	 *                          second
	 * @param batteryPercentage charge the device starts with, in percent
	 * @return the device
	 */
	private Example26OrchestratedDevice device(double mips, double batteryPercentage) {
		Example26OrchestratedDevice device = new Example26OrchestratedDevice(manager, mips, 1, 250, 2000);
		device.setType(TYPES.EDGE_DEVICE);
		device.setMobilityModel(samePlace);
		EnergyModelComputingNode energy = new EnergyModelComputingNode(5, 0.2);
		energy.setBattery(true);
		energy.setBatteryCapacity(18.75);
		energy.setIntialBatteryPercentage(batteryPercentage);
		device.setEnergyModel(energy);
		return device;
	}

	/**
	 * Creates one of the two devices that carry the orchestration, at the delay the
	 * topology gives it from the device asking for an orchestrator. The list of
	 * orchestrating devices grows with the calls, in the order the scenario declares
	 * them: the primary first, then its standby.
	 *
	 * @param delay how far the device is from the one that asks
	 * @return the orchestrating device
	 */
	private Example26OrchestratedDevice orchestratingDevice(double delay) {
		Example26OrchestratedDevice device = device(110000, 100);
		when(topology.getDelay(any(ComputingNode.class), eq(device))).thenReturn(delay);
		orchestrators.add(device);
		when(nodesGenerator.getOrchestratorsList()).thenReturn(orchestrators);
		return device;
	}

	/**
	 * A node of another layer than the fleet: it cannot take mist traffic.
	 *
	 * @param mips capacity of the node
	 * @return the node
	 */
	private ComputingNode server(double mips) {
		ComputingNode node = mock(ComputingNode.class);
		lenient().when(node.getType()).thenReturn(TYPES.EDGE_DATACENTER);
		lenient().when(node.getMipsPerCore()).thenReturn(mips);
		return node;
	}

	/**
	 * Builds an orchestrator over the given devices, the way the simulation manager
	 * does when it reads the architecture of the scenario.
	 *
	 * @param nodes the devices the fleet may place tasks on
	 * @return the learning orchestrator
	 */
	private Example26FaultTolerantOrchestrator orchestrator(ComputingNode... nodes) {
		when(nodesGenerator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(nodes));
		return new Example26FaultTolerantOrchestrator(manager);
	}

	/**
	 * A request of one of the three classes of the scenario, sent by a device that
	 * is within range of the whole fleet.
	 *
	 * @param id                 identifier of the task
	 * @param type               class of the traffic
	 * @param length             length of the task, in million instructions
	 * @param containerMegabytes size of the container, in megabytes
	 * @return the task
	 */
	private Task task(int id, String type, double length, double containerMegabytes) {
		Task task = mock(Task.class);
		ComputingNode origin = mock(ComputingNode.class);
		lenient().when(origin.getMobilityModel()).thenReturn(samePlace);
		lenient().when(task.getId()).thenReturn(id);
		lenient().when(task.getType()).thenReturn(type);
		lenient().when(task.getLength()).thenReturn(length);
		lenient().when(task.getContainerSizeInMBytes()).thenReturn(containerMegabytes);
		lenient().when(task.getStatus()).thenReturn(Task.Status.SUCCESS);
		lenient().when(task.getMaxLatency()).thenReturn(0.015);
		lenient().when(task.getTotalDelay()).thenReturn(0.0);
		lenient().when(task.getEdgeDevice()).thenReturn(origin);
		return task;
	}

	/**
	 * Turns a task that came back into one that was lost, with the reason and the
	 * delay its reward is calculated from.
	 *
	 * @param task   the task to fail
	 * @param reason why it failed
	 * @param delay  total delay of its results, in seconds
	 */
	private void taskFailed(Task task, Task.FailureReason reason, double delay) {
		when(task.getStatus()).thenReturn(Task.Status.FAILED);
		when(task.getFailureReason()).thenReturn(reason);
		when(task.getTotalDelay()).thenReturn(delay);
	}

	/**
	 * @param orchestrator the agent of the run
	 * @return the only rule it was credited for using, which is how these tests read
	 *         the choice the agent made without fixing the random draw
	 */
	private Rule onlyRuleUsed(Example26FaultTolerantOrchestrator orchestrator) {
		Rule found = null;
		for (Rule rule : Rule.values())
			if (orchestrator.getRuleUsage(rule)[0] == 1)
				found = rule;
		if (found == null)
			throw new AssertionError("the agent was expected to have tried exactly one rule");
		return found;
	}

	/**
	 * @param output  console of the run
	 * @param pattern expression with one group holding the number to read
	 * @return the number the run printed
	 */
	private double firstNumber(String output, String pattern) {
		Matcher matcher = Pattern.compile(pattern).matcher(output);
		if (!matcher.find())
			throw new AssertionError("the run did not print " + pattern);
		return Double.parseDouble(matcher.group(1));
	}

	/**
	 * Reads one of the applications of the file that was parsed last.
	 *
	 * @param type class of the traffic, as the XML declares it
	 * @return the application of that class
	 */
	private Application application(String type) {
		return SimulationParameters.applicationList.stream().filter(application -> application.getType().equals(type))
				.findFirst().orElseThrow(() -> new AssertionError(type + " is not declared in the applications file"));
	}

	/**
	 * @param output console of the run
	 * @param type   class of the traffic
	 * @return its mean end to end delay, in milliseconds, as the run reports it
	 */
	private double meanDelay(String output, String type) {
		Matcher matcher = Pattern.compile(type + " (\\d+) ms").matcher(output);
		if (!matcher.find())
			throw new AssertionError("no mean delay for " + type + " in the console of the run");
		return Double.parseDouble(matcher.group(1));
	}

	/**
	 * @param output console of the run
	 * @param type   class of the traffic
	 * @return its success rate, in percent, as the run reports it
	 */
	private double rate(String output, String type) {
		Matcher matcher = Pattern.compile(type + " \\d+% \\((\\d+)/(\\d+)\\)").matcher(output);
		if (!matcher.find())
			throw new AssertionError("no success rate for " + type + " in the console of the run");
		int succeeded = Integer.parseInt(matcher.group(1));
		int total = Integer.parseInt(matcher.group(2));
		assertTrue(total > 0, type + " generated no traffic at all");
		return 100.0 * succeeded / total;
	}

	/**
	 * @param output console of the run
	 * @param start  the beginning of the line to read back
	 * @return the first line of the console starting with that text
	 */
	private String line(String output, String start) {
		Matcher matcher = Pattern.compile(Pattern.quote(start) + ".*").matcher(output);
		return matcher.find() ? matcher.group() : "the run did not print " + start;
	}
}
