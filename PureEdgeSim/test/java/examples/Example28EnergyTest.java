package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
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

/**
 * Tests the battery arithmetic and the two switches of Example 28: that a task is
 * charged against the whole machine, that the autonomy is what is left of the
 * battery once the task and its queue are paid for, that the ranking keeps work
 * off a device about to die, that the placement cache is cleared only when the
 * scenario asks, and that a real run loses the fleet when the ranking is turned
 * off.
 */
class Example28EnergyTest extends AbstractTest {

	private static final String EXAMPLE_SETTINGS = "PureEdgeSim/examples/Example28/settings/";
	private static final String TEST_OUTPUT = OUTPUT_FOLDER + "Example28/";

	private static final String BATTERY_AWARE = "example28.batteryAware";
	private static final String PER_TASK = "example28.perTask";

	/** Watts when the device computes at full load. */
	private static final double MAX_POWER = 10;
	/** Watts when the device has nothing to do. */
	private static final double IDLE_POWER = 4;

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator nodesGenerator;
	private MobilityModel samePlace;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "MIST_ONLY" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "BATTERY_AWARE" };
		SimulationParameters.edgeDevicesRange = 300;
		SimulationParameters.enableOrchestrators = false;

		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0,
				new Scenario(0, 0, 0));
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		nodesGenerator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(nodesGenerator);
		manager.setDataCentersManager(dataCentersManager);

		// Every device is within range of every other one, which is the wide
		// communication range of the scenario.
		samePlace = mock(MobilityModel.class);
		lenient().when(samePlace.distanceTo(any(ComputingNode.class))).thenReturn(0.0);
	}

	@AfterEach
	void clearSwitches() {
		System.clearProperty(BATTERY_AWARE);
		System.clearProperty(PER_TASK);
	}

	// -------------------------------------------------------- the arithmetic

	@Test
	void aTaskIsChargedAgainstTheWholeMachineNotAgainstOneCore() {
		DefaultComputingNode single = device(20000, 1, 2);
		DefaultComputingNode dual = device(20000, 2, 2);
		Task task = task(20000, 30);

		assertEquals(6.0 / 3600, Example28BatteryAwareOrchestrator.taskEnergy(single, task), 1e-12,
				"six watts above idle for two thousand million instructions on a twenty thousand machine");
		assertEquals(Example28BatteryAwareOrchestrator.taskEnergy(single, task) / 2,
				Example28BatteryAwareOrchestrator.taskEnergy(dual, task), 1e-12,
				"the same task on the same per core capacity but twice the machine costs half the energy");
	}

	@Test
	void theAutonomyIsTheIdleLifeLeftAfterPayingForTheTask() {
		DefaultComputingNode device = device(20000, 1, 1);
		Task task = task(20000, 30);

		assertEquals(898.5, Example28BatteryAwareOrchestrator.autonomy(device, task), 1e-9,
				"one watt-hour at four watts idle is nine hundred seconds, less the second and a half the task costs");
	}

	@Test
	void theWorkAlreadyQueuedIsPaidForBeforeTheNextTaskIsPlaced() {
		DefaultComputingNode device = device(20000, 1, 1);
		device.submitTask(task(20000, 30));
		device.submitTask(task(20000, 30));
		Task next = task(20000, 30);

		assertEquals(1, device.getTasksQueue().size(), "the first task runs, the second waits");
		assertEquals(895.5, Example28BatteryAwareOrchestrator.autonomy(device, next), 1e-9,
				"the running and the queued task are both owed their energy, so a device with a backlog is less able "
						+ "to afford this one");
	}

	@Test
	void aDeviceOffTheBatteryHasNothingToRunOutOf() {
		DefaultComputingNode device = device(20000, 1, 1);
		device.getEnergyModel().setBattery(false);

		assertEquals(Double.MAX_VALUE, Example28BatteryAwareOrchestrator.autonomy(device, task(20000, 30)),
				"a mains powered device can always pay");
	}

	@Test
	void theEstimatedServiceTimeGrowsWithTheQueueAndShrinksWithTheCores() {
		DefaultComputingNode single = device(10000, 1, 2);
		DefaultComputingNode dual = device(10000, 2, 2);

		assertEquals(2, Example28BatteryAwareOrchestrator.serviceTime(task(20000, 30), single), 1e-9,
				"twenty thousand million instructions at ten thousand per second");

		for (int i = 0; i < 3; i++)
			single.submitTask(task(20000, 30));
		for (int i = 0; i < 4; i++)
			dual.submitTask(task(20000, 30));

		assertEquals(2, single.getTasksQueue().size());
		assertEquals(2, dual.getTasksQueue().size(), "the two cores of the second device take two tasks off the queue");
		assertEquals(6, Example28BatteryAwareOrchestrator.serviceTime(task(20000, 30), single), 1e-9,
				"two tasks are ahead of this one on the only core");
		assertEquals(4, Example28BatteryAwareOrchestrator.serviceTime(task(20000, 30), dual), 1e-9,
				"the same backlog spread over two cores delays it by one execution instead of two");
	}

	// ------------------------------------------------------------ the ranking

	@Test
	void theRankingKeepsTheWorkOffTheDeviceThatIsAboutToDie() {
		DefaultComputingNode thirsty = device(40000, 1, 0.05);
		DefaultComputingNode charged = device(4000, 1, 2);
		Example28BatteryAwareOrchestrator orchestrator = orchestrator(thirsty, charged);

		assertEquals(1, orchestrator.findComputingNode(mist(), task(20000, 30)),
				"the slow device with the full battery can afford the task, the fast one with seconds to live cannot");
		assertEquals(1, orchestrator.getMovedPlacements(), "and the placement did not go to the fastest device");
		assertEquals(0, orchestrator.getLocalPlacements());
	}

	@Test
	void theFastestDeviceWinsWhenTheRankingIsByDelay() {
		DefaultComputingNode thirsty = device(40000, 1, 0.05);
		DefaultComputingNode charged = device(4000, 1, 2);
		System.setProperty(BATTERY_AWARE, "false");
		Example28BatteryAwareOrchestrator orchestrator = orchestrator(thirsty, charged);

		assertEquals(0, orchestrator.findComputingNode(mist(), task(20000, 30)),
				"without the battery in the ranking the fast device takes the task it cannot pay for");
		assertEquals(0, orchestrator.getMovedPlacements());
	}

	@Test
	void aCandidateThatWouldReturnTheTaskLateIsDroppedButTheDeviceOfTheTaskIsNot() {
		DefaultComputingNode local = device(1000, 1, 2);
		DefaultComputingNode other = device(1000, 1, 2);
		Example28BatteryAwareOrchestrator orchestrator = orchestrator(local, other);

		// One second of budget for twenty seconds of work on either device.
		assertEquals(0, orchestrator.findComputingNode(mist(), task(20000, 1, local)),
				"the device of the task keeps its own request even though it misses the deadline");
		assertEquals(1, orchestrator.getDeadlineRefusals(), "the other device is refused for the same reason");
		assertEquals(1, orchestrator.getLocalPlacements());
	}

	@Test
	void aTaskWithNoCandidateLeftIsReportedAsUnplaced() {
		DefaultComputingNode slow = device(1000, 1, 2);
		Example28BatteryAwareOrchestrator orchestrator = orchestrator(slow);

		assertEquals(-1, orchestrator.findComputingNode(mist(), task(20000, 1)),
				"the only candidate would have returned the result twenty times too late");
		assertEquals(1, orchestrator.getUnplacedTasks());
		assertEquals(1, orchestrator.getDeadlineRefusals());
	}

	@Test
	void thePlacementCacheIsClearedOnlyWhenTheScenarioAsksForATaskByTaskDecision() {
		ComputingNode origin = mock(ComputingNode.class);
		Example28BatteryAwareOrchestrator orchestrator = orchestrator(device(20000, 1, 2));

		orchestrator.resultsReturned(task(20000, 30, origin));
		verify(origin).setApplicationPlaced(false);

		System.setProperty(PER_TASK, "false");
		ComputingNode cached = mock(ComputingNode.class);
		orchestrator.resultsReturned(task(20000, 30, cached));
		verify(cached, never()).setApplicationPlaced(false);
	}

	@Test
	void theTwoSwitchesDefaultToOnAndReadTheSystemProperties() {
		assertTrue(Example28.batteryAware());
		assertTrue(Example28.perTask());

		System.setProperty(BATTERY_AWARE, "false");
		System.setProperty(PER_TASK, "false");
		assertFalse(Example28.batteryAware());
		assertFalse(Example28.perTask());
	}

	// ------------------------------------------------------------- settings

	@Test
	void theSettingsDeclareTheDrainingFleet() {
		assertTrue(new ParametersParser(EXAMPLE_SETTINGS + "simulation_parameters.properties").parse());
		assertTrue(new ApplicationFileParser(EXAMPLE_SETTINGS + "applications.xml").parse());

		assertEquals("MIST_ONLY", SimulationParameters.orchestrationArchitectures[0],
				"the fleet serves itself, there is nowhere else to offload");
		assertEquals("BATTERY_AWARE", SimulationParameters.orchestrationAlgorithms[0]);
		assertEquals(40, SimulationParameters.minNumberOfEdgeDevices);
		assertEquals(40, SimulationParameters.maxNumberOfEdgeDevices);
		assertEquals(900, SimulationParameters.simulationDuration, 1e-9, "fifteen simulated minutes");
		assertFalse(SimulationParameters.realisticNetworkModel,
				"the links are wide: the only thing that can kill a device here is its battery");

		assertEquals(20000, application("Sensor").getTaskLength(), 1e-9);
		assertEquals(80000, application("Photo").getTaskLength(), 1e-9);
		assertEquals(200000, application("Video").getTaskLength(), 1e-9);
		assertEquals(2, application("Sensor").getRate(), 1e-9, "two requests a minute, as the scenario states");
		assertEquals(50, application("Sensor").getUsagePercentage(), 1e-9);
		assertEquals(30, application("Photo").getUsagePercentage(), 1e-9);
		assertEquals(20, application("Video").getUsagePercentage(), 1e-9);
	}

	// ---------------------------------------------------------- one real run

	@Test
	@Tag("integration")
	void theFleetSurvivesWhenTheWorkIsSpreadOverTheDevicesThatCanAffordIt() throws Exception {
		String output = run();

		assertEquals(40, firstNumber(output, "BATTERY_AWARE - fleet: (\\d+) devices"), 1e-9);
		assertEquals(0, firstNumber(output, "BATTERY_AWARE - fleet: 40 devices, (\\d+) died"), 1e-9,
				"a placement that prices the battery keeps every device alive: "
						+ line(output, "BATTERY_AWARE - fleet:"));
		assertTrue(firstNumber(output, "BATTERY_AWARE - policy: (\\d+) decisions") > 500,
				"the placement is decided again for each task: " + line(output, "BATTERY_AWARE - policy:"));
	}

	@Test
	@Tag("integration")
	void placingBySpeedAloneKillsTheFastDevicesFirst() throws Exception {
		System.setProperty(BATTERY_AWARE, "false");
		String output = run();

		double died = firstNumber(output, "BATTERY_AWARE - fleet: 40 devices, (\\d+) died");
		assertTrue(died > 0, "the fast devices carry the fleet and run out before the end: "
				+ line(output, "BATTERY_AWARE - fleet:"));
		assertTrue(firstNumber(output, "BATTERY_AWARE - tasks lost: (\\d+) never generated") > 0,
				"and dead devices stop generating the requests they had left: "
						+ line(output, "BATTERY_AWARE - tasks lost:"));
	}

	@Test
	@Tag("integration")
	void reusingTheFirstPlacementTurnsOneDecisionIntoACommitment() throws Exception {
		System.setProperty(PER_TASK, "false");
		String output = run();

		double decisions = firstNumber(output, "BATTERY_AWARE - policy: (\\d+) decisions");
		assertTrue(decisions > 0 && decisions < 200,
				"the framework reuses the placement of each device instead of asking again: "
						+ line(output, "BATTERY_AWARE - policy:"));
		assertTrue(firstNumber(output, "BATTERY_AWARE - fleet: 40 devices, (\\d+) died") > 0,
				"and the work gathers on the devices that answered first, which is what costs the fleet: "
						+ line(output, "BATTERY_AWARE - fleet:"));
	}

	// -------------------------------------------------------------- helpers

	/**
	 * Runs the example scenario once and returns everything it printed.
	 *
	 * @return the console of the run
	 * @throws Exception when the output cannot be captured
	 */
	private String run() throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example28BatteryAwareOrchestrator.class);
			simulation.setCustomSimulationManager(Example28FleetReporter.class);
			simulation.setCustomSettingsFolder(EXAMPLE_SETTINGS);
			simulation.setCustomOutputFolder(TEST_OUTPUT);
			System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
			simulation.launchSimulation();
		} finally {
			System.setOut(originalOut);
		}
		return console.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Creates a battery powered device of the fleet with the power model of the
	 * scenario.
	 *
	 * @param mips      capacity of one core, in million instructions per second
	 * @param cores     number of cores
	 * @param batteryWh energy the battery holds, in watt-hours
	 * @return the device
	 */
	private DefaultComputingNode device(double mips, int cores, double batteryWh) {
		DefaultComputingNode device = new DefaultComputingNode(manager, mips, cores, 250, 2000);
		device.setType(TYPES.EDGE_DEVICE);
		device.setMobilityModel(samePlace);
		EnergyModelComputingNode energy = new EnergyModelComputingNode(MAX_POWER, IDLE_POWER);
		energy.setBattery(true);
		energy.setBatteryCapacity(batteryWh);
		energy.setIntialBatteryPercentage(100);
		device.setEnergyModel(energy);
		return device;
	}

	/**
	 * Builds an orchestrator over the given devices, the way the simulation manager
	 * does when it reads the architecture of the scenario.
	 *
	 * @param nodes the devices the fleet may place tasks on
	 * @return the battery aware orchestrator
	 */
	private Example28BatteryAwareOrchestrator orchestrator(ComputingNode... nodes) {
		when(nodesGenerator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(nodes));
		return new Example28BatteryAwareOrchestrator(manager);
	}

	/**
	 * A request of the scenario sent by a device outside the fleet.
	 *
	 * @param length  length of the task, in million instructions
	 * @param latency deadline of the task, in seconds
	 * @return the task
	 */
	private Task task(double length, double latency) {
		ComputingNode origin = mock(ComputingNode.class);
		lenient().when(origin.getMobilityModel()).thenReturn(samePlace);
		return task(length, latency, origin);
	}

	/**
	 * A request of the scenario sent by the given device.
	 *
	 * @param length  length of the task, in million instructions
	 * @param latency deadline of the task, in seconds
	 * @param origin  the device that generated the task
	 * @return the task
	 */
	private Task task(double length, double latency, ComputingNode origin) {
		Task task = mock(Task.class);
		lenient().when(task.getLength()).thenReturn(length);
		lenient().when(task.getMaxLatency()).thenReturn(latency);
		lenient().when(task.getContainerSizeInMBytes()).thenReturn(1.0);
		lenient().when(task.getEdgeDevice()).thenReturn(origin);
		lenient().when(task.getStatus()).thenReturn(Task.Status.SUCCESS);
		lenient().when(task.getTotalDelay()).thenReturn(0.0);
		return task;
	}

	/**
	 * @return the layers the scenario allows, which for this example is mist only
	 */
	private static String[] mist() {
		return new String[] { "Mist" };
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
	 * @param output  console of the run
	 * @param pattern expression with one group holding the number to read
	 * @return the number the run printed
	 */
	private static double firstNumber(String output, String pattern) {
		Matcher matcher = Pattern.compile(pattern).matcher(output);
		assertTrue(matcher.find(), "the run did not print " + pattern);
		return Double.parseDouble(matcher.group(1));
	}

	/**
	 * @param output console of the run
	 * @param start  the beginning of the line to read back
	 * @return the first line of the console starting with that text
	 */
	private static String line(String output, String start) {
		Matcher matcher = Pattern.compile(Pattern.quote(start) + ".*").matcher(output);
		return matcher.find() ? matcher.group() : "the run did not print " + start;
	}
}
