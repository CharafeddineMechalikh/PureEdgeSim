package examples;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.distribution.ConstantDistribution;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;
import com.mechalikh.pureedgesim.network.NetworkLinkMan;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example23ExtensibilityTest extends AbstractTest {

	@BeforeEach
	void prepareNetworks() {
		SimulationParameters.manLatency = 0.01;
		SimulationParameters.manBandwidthBitsPerSecond = 1_000_000_000;
		SimulationParameters.deepLoggingEnabled = false;
	}

	@Test
	void oneDeviceIsReservedForARareTypeThatRoundingDrops() {
		Example23TypeGuaranteeGenerator generator = generator(20);

		int[] allocation = generator.allocateInstances(List.of(39.0, 29.0, 30.0, 2.0));

		assertArrayEquals(new int[] { 7, 6, 6, 1 }, allocation,
				"the 2 percent wearable gets one device, taken from the type with the most");
		assertEquals(20, sum(allocation), "the fleet must keep the size requested by the scenario");
	}

	@Test
	void aTypeThatAlreadyHasDevicesKeepsItsAllocation() {
		Example23TypeGuaranteeGenerator generator = generator(20);

		assertArrayEquals(new int[] { 12, 8 }, generator.allocateInstances(List.of(60.0, 40.0)));
	}

	@Test
	void aTypeWithZeroShareIsNotReserved() {
		Example23TypeGuaranteeGenerator generator = generator(20);

		assertArrayEquals(new int[] { 20, 0 }, generator.allocateInstances(List.of(100.0, 0.0)));
	}

	@Test
	void aFleetTooSmallForEveryTypeKeepsItsSize() {
		Example23TypeGuaranteeGenerator generator = generator(2);

		int[] allocation = generator.allocateInstances(List.of(33.0, 33.0, 34.0));

		assertEquals(2, sum(allocation), "one device cannot be reserved for every type of a two device fleet");
	}

	@Test
	void anEmptyBackboneGetsALinkInBothDirectionsForEveryPair() {
		DefaultSimulationManager manager = manager(0);
		InfrastructureGraph topology = new InfrastructureGraph();
		List<ComputingNode> nodes = List.of(node(1), node(2), node(3), node(4));

		int added = Example23FullMeshTopologyCreator.connectAllPairs(topology, manager, nodes);

		assertEquals(12, added, "four nodes need six pairs, each with two directions");
		assertEquals(12, topology.getManLinks().size());
	}

	@Test
	void theLinksDeclaredInTheFileAreNotDuplicated() {
		DefaultSimulationManager manager = manager(0);
		InfrastructureGraph topology = new InfrastructureGraph();
		List<ComputingNode> nodes = List.of(node(1), node(2), node(3), node(4));
		ring(topology, manager, nodes);

		int added = Example23FullMeshTopologyCreator.connectAllPairs(topology, manager, nodes);

		assertEquals(4, added, "only the two missing diagonals are added, in both directions");
		assertEquals(12, topology.getManLinks().size());
	}

	@Test
	void theFullMeshShortensThePathBetweenOppositeEdgeDataCenters() {
		DefaultSimulationManager manager = manager(0);
		InfrastructureGraph topology = new InfrastructureGraph();
		List<ComputingNode> nodes = List.of(node(1), node(2), node(3), node(4));
		ring(topology, manager, nodes);
		double before = topology.getDelay(nodes.get(0), nodes.get(2));

		Example23FullMeshTopologyCreator.connectAllPairs(topology, manager, nodes);

		assertEquals(0.02, before, 1e-9, "the ring reaches dc3 in two MAN hops");
		assertEquals(0.01, topology.getDelay(nodes.get(0), nodes.get(2)), 1e-9,
				"the full mesh reaches dc3 in one MAN hop");
	}

	@Test
	void everyDeviceBurstsAtTheSameInstant() {
		SimulationParameters.simulationDuration = 120;
		SimulationParameters.applicationList = List.of(
				new Application("Reporting", 2, 100, 1, 8_000_000, 8_000_000, 8_000_000, 1000));
		ComputingNode first = device();
		ComputingNode second = device();
		Example23BurstTaskGenerator generator = burstGenerator(first, second);

		List<Task> tasks = generator.generate().stream().collect(Collectors.toList());

		assertEquals(List.of(30.0, 60.0, 90.0, 120.0), timesOf(tasks, first),
				"each minute starts on the minute mark, so the bursts are 30 seconds apart");
		assertEquals(timesOf(tasks, first), timesOf(tasks, second),
				"every device of the fleet bursts at the same instants");
	}

	@Test
	void anApplicationWithAnArrivalPatternKeepsTheDefaultBehaviour() {
		SimulationParameters.simulationDuration = 120;
		Application application = mock(Application.class);
		when(application.hasRandomArrival()).thenReturn(true);
		when(application.getArrivalDistribution()).thenReturn(new ConstantDistribution(1));
		when(application.getType()).thenReturn("Streaming");
		when(application.sampleRequestSize()).thenReturn(1000L);
		when(application.sampleResultsSize()).thenReturn(1000L);
		when(application.sampleContainerSize()).thenReturn(1000L);
		when(application.sampleLatency()).thenReturn(10.0);
		when(application.sampleTaskLength()).thenReturn(1000L);
		SimulationParameters.applicationList = List.of(application);
		Example23BurstTaskGenerator generator = burstGenerator(device(), device());

		List<Task> tasks = generator.generate().stream().collect(Collectors.toList());

		assertEquals(238, tasks.size(), "two devices stream one task per second for 119 seconds");
		assertEquals(1, tasks.get(0).getTime(), "the arrival distribution decides when tasks are generated");
	}

	@Test
	void theAuditCountsEachSuccessfulTaskOnce() {
		Example23AuditSimulationManager manager = auditManager();
		Task task = task(7);

		manager.recordSuccess(task);
		manager.recordSuccess(task);

		String audit = auditOf(manager);
		assertTrue(audit.contains("1 succeeded"), audit);
		assertTrue(audit.contains("0 failed"), audit);
	}

	@Test
	void theAuditCountsEachFailedTaskOnceByReason() {
		Example23AuditSimulationManager manager = auditManager();
		Task task = task(7);
		when(task.getFailureReason()).thenReturn(Task.FailureReason.FAILED_DUE_TO_LATENCY);

		manager.recordFailure(task);
		manager.recordFailure(task);

		String audit = auditOf(manager);
		assertTrue(audit.contains("0 succeeded, 1 failed"), audit);
		assertTrue(audit.contains("FAILED_DUE_TO_LATENCY 1"), audit);
	}

	@Test
	void aTaskLostWithItsDeviceIsAuditedApartFromTheFailures() {
		Example23AuditSimulationManager manager = auditManager();
		Task task = task(7);
		when(task.getFailureReason()).thenReturn(Task.FailureReason.NOT_GENERATED_BECAUSE_DEVICE_DEAD);

		manager.recordFailure(task);

		String audit = auditOf(manager);
		assertTrue(audit.contains("0 failed, 1 not generated"), audit);
	}

	@Test
	void theAuditNamesEveryFailureReason() {
		Example23AuditSimulationManager manager = auditManager();
		int id = 0;
		for (Task.FailureReason reason : List.of(Task.FailureReason.FAILED_DUE_TO_LATENCY,
				Task.FailureReason.FAILED_DUE_TO_DEVICE_MOBILITY, Task.FailureReason.INSUFFICIENT_RESOURCES,
				Task.FailureReason.INSUFFICIENT_POWER, Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD,
				Task.FailureReason.NO_OFFLOADING_DESTINATIONS)) {
			Task task = task(++id);
			when(task.getFailureReason()).thenReturn(reason);
			manager.recordFailure(task);
		}

		String failures = manager.describeFailures();
		assertTrue(failures.contains("FAILED_DUE_TO_LATENCY 1"), failures);
		assertTrue(failures.contains("FAILED_DUE_TO_DEVICE_MOBILITY 1"), failures);
		assertTrue(failures.contains("INSUFFICIENT_RESOURCES 1"), failures);
		assertTrue(failures.contains("INSUFFICIENT_POWER 1"), failures);
		assertTrue(failures.contains("FAILED_BECAUSE_DEVICE_DEAD 1"), failures);
		assertTrue(failures.contains("NO_OFFLOADING_DESTINATIONS 1"), failures);
	}

	@Test
	void theManagerReportsFailuresDetectedByTheFramework() {
		Example23AuditSimulationManager manager = auditManager();
		Task task = task(7);
		ComputingNode device = mock(ComputingNode.class);
		when(task.getEdgeDevice()).thenReturn(device);
		when(device.isDead()).thenReturn(true);
		when(task.getFailureReason()).thenReturn(Task.FailureReason.NOT_GENERATED_BECAUSE_DEVICE_DEAD);

		assertTrue(manager.taskFailed(task, 0), "a task of a dead device fails");

		assertTrue(auditOf(manager).contains("1 not generated"));
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheFourCustomModels() throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomComputingNodesGenerator(Example23TypeGuaranteeGenerator.class);
			simulation.setCustomTaskGenerator(Example23BurstTaskGenerator.class);
			simulation.setCustomTopologyCreator(Example23FullMeshTopologyCreator.class);
			simulation.setCustomSimulationManager(Example23AuditSimulationManager.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER + "example23/");
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			simulation.launchSimulation();
		} finally {
			System.setOut(originalOut);
		}

		String output = console.toString(StandardCharsets.UTF_8);
		assertTrue(output.contains("Device type 4 declares 2.0 % of the fleet"),
				"the generator should reserve the rare device type");
		assertTrue(output.contains("Added 4 directed MAN links"),
				"the topology creator should complete the ring into a full mesh");
		assertTrue(output.contains("Example23AuditSimulationManager - audit:"),
				"the custom manager should print the failure breakdown");
		assertTrue(output.contains("number of edge devices= 20"), "the fleet should keep every requested device");

		Path latest = Files.list(Path.of(OUTPUT_FOLDER))
				.filter(Files::isDirectory)
				.filter(path -> Character.isDigit(path.getFileName().toString().charAt(0)))
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder"));
		Path csv = Files.walk(latest)
				.filter(path -> path.toString().endsWith(".csv"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no results csv in " + latest));
		String results = Files.readString(csv);
		assertTrue(results.contains("ALL"), "the csv should name the architecture that ran");
		assertTrue(results.contains("TRADE_OFF"), "the csv should name the algorithm that ran");
		assertFalse(output.contains("0 succeeded, 0 failed"), "the scenario should produce tasks");
	}

	private Example23TypeGuaranteeGenerator generator(int devicesCount) {
		return new Example23TypeGuaranteeGenerator(manager(devicesCount), MobilityModel.class, ComputingNode.class);
	}

	private DefaultSimulationManager manager(int devicesCount) {
		return new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0,
				new Scenario(devicesCount, 0, 0));
	}

	private Example23AuditSimulationManager auditManager() {
		return new Example23AuditSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0,
				new Scenario(0, 0, 0));
	}

	private Example23BurstTaskGenerator burstGenerator(ComputingNode... devices) {
		DefaultSimulationManager manager = manager(devices.length);
		ComputingNodesGenerator generator = mock(ComputingNodesGenerator.class);
		ComputingNode cloud = mock(ComputingNode.class);
		when(generator.getMistOnlyList()).thenReturn(List.of(devices));
		when(generator.getCloudOnlyList()).thenReturn(List.of(cloud));
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
		return new Example23BurstTaskGenerator(manager);
	}

	private ComputingNode device() {
		ComputingNode device = mock(ComputingNode.class);
		lenient().when(device.isGeneratingTasks()).thenReturn(true);
		lenient().when(device.getId()).thenReturn(1);
		return device;
	}

	private ComputingNode node(int id) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getId()).thenReturn(id);
		return node;
	}

	private Task task(int id) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		lenient().when(task.getDependents()).thenReturn(List.of());
		return task;
	}

	private void ring(InfrastructureGraph topology, DefaultSimulationManager manager, List<ComputingNode> nodes) {
		for (int i = 0; i < nodes.size(); i++)
			link(topology, manager, nodes.get(i), nodes.get((i + 1) % nodes.size()));
	}

	private void link(InfrastructureGraph topology, DefaultSimulationManager manager, ComputingNode from,
			ComputingNode to) {
		topology.addLink(new NetworkLinkMan(from, to, manager, NetworkLinkTypes.MAN));
		topology.addLink(new NetworkLinkMan(to, from, manager, NetworkLinkTypes.MAN));
	}

	private List<Double> timesOf(List<Task> tasks, ComputingNode device) {
		List<Double> times = new ArrayList<>();
		for (Task task : tasks)
			if (task.getEdgeDevice() == device)
				times.add(task.getTime());
		return times;
	}

	private String auditOf(Example23AuditSimulationManager manager) {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
		try {
			manager.onSimulationEnd();
		} finally {
			System.setOut(originalOut);
		}
		return console.toString(StandardCharsets.UTF_8);
	}

	private int sum(int[] values) {
		int total = 0;
		for (int value : values)
			total += value;
		return total;
	}
}
