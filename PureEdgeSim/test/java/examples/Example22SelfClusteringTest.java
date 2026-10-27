package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example22SelfClusteringTest extends AbstractTest {

	private static final double RANGE = 50;

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;
	private final Map<ComputingNode, double[]> positions = new IdentityHashMap<>();

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "MIST_ONLY" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "CLUSTER_LOCAL" };
		SimulationParameters.edgeDevicesRange = (int) RANGE;
		SimulationParameters.enableOrchestrators = false;

		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0,
				new Scenario(0, 0, 0));
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	@Test
	void capacityHasTheLargestInfluenceOnTheScore() {
		double powerful = Example22SelfClusteringDevice.clusterScore(4000, 4000, 1, 1, 0);
		double weak = Example22SelfClusteringDevice.clusterScore(1000, 4000, 1, 1, 0);

		assertTrue(powerful > weak);
	}

	@Test
	void aHalfEmptyBatteryScoresLowerThanAFullOne() {
		double full = Example22SelfClusteringDevice.clusterScore(4000, 4000, 1, 1, 5);
		double half = Example22SelfClusteringDevice.clusterScore(4000, 4000, 0.5, 1, 5);

		assertTrue(full > half);
	}

	@Test
	void theScoreReachesOneOnlyWhenEveryTermIsMaximal() {
		assertEquals(1.0, Example22SelfClusteringDevice.clusterScore(4000, 4000, 1, 1, 100), 1e-9);
		assertEquals(0.0, Example22SelfClusteringDevice.clusterScore(0, 4000, 0, 0, 0), 1e-9);
	}

	@Test
	void theDegreeTermSaturatesAtTenNeighbors() {
		double ten = Example22SelfClusteringDevice.clusterScore(4000, 4000, 1, 1, 10);
		double thousand = Example22SelfClusteringDevice.clusterScore(4000, 4000, 1, 1, 1000);

		assertEquals(ten, thousand, 1e-9);
	}

	@Test
	void aMovingDeviceScoresLowerThanAFixedOneWithTheSameCapacity() {
		ComputingNode fixed = scoredDevice(4000, false, 0, 100);
		ComputingNode moving = scoredDevice(4000, true, 5, 100);

		assertTrue(Example22SelfClusteringDevice.clusterScore(fixed, 5, 4000) > Example22SelfClusteringDevice
				.clusterScore(moving, 5, 4000));
	}

	@Test
	void aNodeWithoutBatteryCountsAsFullyCharged() {
		ComputingNode mains = scoredDevice(4000, false, 0, 0);
		ComputingNode battery = scoredDevice(4000, true, 0, 40);

		assertEquals(0.925, Example22SelfClusteringDevice.clusterScore(mains, 5, 4000), 1e-9,
				"mains power and a fixed position should score the full battery and stability weights");
		assertTrue(Example22SelfClusteringDevice.clusterScore(battery, 5, 4000) < 1.0);
	}

	@Test
	void theMostPowerfulNeighborBecomesTheHeadOfEveryDeviceInRange() {
		Example22SelfClusteringDevice weak = electionDevice(1, 0, 0, 1000);
		Example22SelfClusteringDevice strong = electionDevice(2, 10, 0, 4000);
		Example22SelfClusteringDevice medium = electionDevice(3, 20, 0, 2000);

		Map<ComputingNode, ComputingNode> heads = Example22SelfClusteringDevice
				.electClusters(List.of(weak, strong, medium), RANGE);

		assertEquals(strong, heads.get(weak));
		assertEquals(strong, heads.get(strong));
		assertEquals(strong, heads.get(medium));
	}

	@Test
	void devicesOutOfRangeKeepTheirOwnCluster() {
		Example22SelfClusteringDevice here = electionDevice(1, 0, 0, 4000);
		Example22SelfClusteringDevice far = electionDevice(2, 200, 0, 1000);

		Map<ComputingNode, ComputingNode> heads = Example22SelfClusteringDevice
				.electClusters(List.of(here, far), RANGE);

		assertEquals(here, heads.get(here));
		assertEquals(far, heads.get(far));
	}

	@Test
	void aDeviceFollowsTheChainToTheLocalMaximum() {
		Example22SelfClusteringDevice head = electionDevice(1, 0, 0, 4000);
		Example22SelfClusteringDevice middle = electionDevice(2, 20, 0, 2000);
		Example22SelfClusteringDevice tail = electionDevice(3, 40, 0, 1000);

		Map<ComputingNode, ComputingNode> heads = Example22SelfClusteringDevice
				.electClusters(List.of(head, middle, tail), 25);

		assertEquals(head, heads.get(middle));
		assertEquals(head, heads.get(tail), "the tail should join the head through the middle device");
	}

	@Test
	void deadDevicesAndSensorsAreLeftOut() {
		ComputingNode alive = electionDevice(1, 0, 0, 4000);
		ComputingNode dead = electionDevice(2, 10, 0, 2000);
		when(dead.isDead()).thenReturn(true);
		ComputingNode sensor = electionDevice(3, 20, 0, 0);
		when(sensor.isSensor()).thenReturn(true);

		Map<ComputingNode, ComputingNode> heads = Example22SelfClusteringDevice
				.electClusters(List.of(alive, dead, sensor), RANGE);

		assertEquals(1, heads.size());
		assertEquals(alive, heads.get(alive));
	}

	@Test
	void anEqualScoreGoesToTheLowerIdentifier() {
		Example22SelfClusteringDevice first = electionDevice(1, 0, 0, 4000);
		Example22SelfClusteringDevice second = electionDevice(2, 10, 0, 4000);

		Map<ComputingNode, ComputingNode> heads = Example22SelfClusteringDevice
				.electClusters(List.of(second, first), RANGE);

		assertEquals(first, heads.get(second));
		assertEquals(first, heads.get(first));
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanClusterLocal() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(List.of());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example22ClusterOrchestrator(manager));
		assertTrue(exception.getMessage().contains("CLUSTER_LOCAL"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutTheMistLayer() {
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(List.of());
		Example22ClusterOrchestrator orchestrator = new Example22ClusterOrchestrator(manager);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Cloud" }, task(1, null)));
		assertEquals(-1, orchestrator.findComputingNode(null, task(1, null)));
	}

	@Test
	void theClusterHeadWinsAtEqualCapacity() {
		Example22SelfClusteringDevice head = node(1, 1000);
		Example22SelfClusteringDevice member = node(2, 1000);
		Example22SelfClusteringDevice origin = node(3, 1000);
		when(origin.getClusterHead()).thenReturn(head);
		when(head.getClusterMembers()).thenReturn(List.of(head, member, origin));
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(head, member, origin));

		Example22ClusterOrchestrator orchestrator = new Example22ClusterOrchestrator(manager);
		Task task = task(7, origin);

		assertEquals(0, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void aMuchFasterMemberWinsDespiteTheHeadDiscount() {
		Example22SelfClusteringDevice head = node(1, 1000);
		Example22SelfClusteringDevice member = node(2, 4000);
		Example22SelfClusteringDevice origin = node(3, 1000);
		when(origin.getClusterHead()).thenReturn(head);
		when(head.getClusterMembers()).thenReturn(List.of(head, member, origin));
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(head, member, origin));

		Example22ClusterOrchestrator orchestrator = new Example22ClusterOrchestrator(manager);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Mist" }, task(7, origin)));
	}

	@Test
	void aDeviceWithoutAClusterCanUseAnyReachableNode() {
		Example22SelfClusteringDevice first = node(1, 1000);
		Example22SelfClusteringDevice second = node(2, 4000);
		Example22SelfClusteringDevice origin = node(3, 1000);
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(first, second, origin));

		Example22ClusterOrchestrator orchestrator = new Example22ClusterOrchestrator(manager);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Mist" }, task(7, origin)));
	}

	@Test
	void aPendingTaskRaisesTheCostUntilItsResultsReturn() {
		Example22SelfClusteringDevice head = node(1, 4000);
		Example22SelfClusteringDevice origin = node(2, 1000);
		when(origin.getClusterHead()).thenReturn(head);
		when(head.getClusterMembers()).thenReturn(List.of(head, origin));
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(head, origin));

		Example22ClusterOrchestrator orchestrator = new Example22ClusterOrchestrator(manager);
		Task task = task(7, origin);
		double before = orchestrator.placementCost(task, head);

		assertEquals(0, orchestrator.findComputingNode(new String[] { "Mist" }, task));
		assertTrue(orchestrator.placementCost(task, head) > before);

		orchestrator.resultsReturned(task);
		assertEquals(before, orchestrator.placementCost(task, head), 1e-9);
	}

	@Test
	void aDeadHeadIsNotChosen() {
		Example22SelfClusteringDevice head = node(1, 4000);
		when(head.isDead()).thenReturn(true);
		Example22SelfClusteringDevice origin = node(2, 1000);
		when(origin.getClusterHead()).thenReturn(head);
		when(head.getClusterMembers()).thenReturn(List.of(head, origin));
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(List.of(head, origin));

		Example22ClusterOrchestrator orchestrator = new Example22ClusterOrchestrator(manager);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Mist" }, task(7, origin)));
	}

	@Test
	@Tag("integration")
	void aRealSimulationFormsClustersAndPlacesTasksInsideThem() throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomComputingNode(Example22SelfClusteringDevice.class);
			simulation.setCustomEdgeOrchestrator(Example22ClusterOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("selfcluster.properties");
			simulation.launchSimulation();
		} finally {
			System.setOut(originalOut);
		}

		String output = console.toString(StandardCharsets.UTF_8);
		assertTrue(output.contains("Example22 - clustering:"), "the run should report the clusters it formed");
		assertTrue(output.contains("Example22 - cluster placement:"), "the run should report where tasks went");
		assertFalse(output.contains("clusters over 0 devices"), "the fleet should contain clusterable devices");

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
		assertTrue(results.contains("MIST_ONLY"), "the csv should name the architecture that ran");
		assertTrue(results.contains("CLUSTER_LOCAL"), "the csv should name the algorithm that ran");
	}

	private Example22SelfClusteringDevice node(int id, double capacity) {
		Example22SelfClusteringDevice node = mock(Example22SelfClusteringDevice.class);
		lenient().when(node.getId()).thenReturn(id);
		when(node.getType()).thenReturn(TYPES.EDGE_DEVICE);
		when(node.isDead()).thenReturn(false);
		when(node.isSensor()).thenReturn(false);
		when(node.getTotalMipsCapacity()).thenReturn(capacity);
		when(node.getMipsPerCore()).thenReturn(capacity);
		when(node.getNumberOfCPUCores()).thenReturn(1.0);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mobility.distanceTo(any(ComputingNode.class))).thenReturn(5.0);
		when(node.getMobilityModel()).thenReturn(mobility);
		return node;
	}

	private Example22SelfClusteringDevice electionDevice(int id, double x, double y, double capacity) {
		Example22SelfClusteringDevice device = mock(Example22SelfClusteringDevice.class);
		when(device.getId()).thenReturn(id);
		when(device.isDead()).thenReturn(false);
		when(device.isSensor()).thenReturn(false);
		when(device.getTotalMipsCapacity()).thenReturn(capacity);
		EnergyModelComputingNode energy = mock(EnergyModelComputingNode.class);
		when(energy.isBatteryPowered()).thenReturn(false);
		when(device.getEnergyModel()).thenReturn(energy);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mobility.isMobile()).thenReturn(false);
		when(device.getMobilityModel()).thenReturn(mobility);
		when(mobility.distanceTo(any(ComputingNode.class))).thenAnswer(invocation -> {
			double[] from = positions.get(device);
			double[] to = positions.get(invocation.getArgument(0));
			return Math.hypot(from[0] - to[0], from[1] - to[1]);
		});
		positions.put(device, new double[] { x, y });
		return device;
	}

	private ComputingNode scoredDevice(double capacity, boolean mobile, double speed, double batteryLevel) {
		ComputingNode device = mock(ComputingNode.class);
		when(device.getTotalMipsCapacity()).thenReturn(capacity);
		EnergyModelComputingNode energy = mock(EnergyModelComputingNode.class);
		when(energy.isBatteryPowered()).thenReturn(batteryLevel > 0);
		when(energy.getBatteryLevelPercentage()).thenReturn(batteryLevel);
		when(device.getEnergyModel()).thenReturn(energy);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mobility.isMobile()).thenReturn(mobile);
		when(mobility.getSpeed()).thenReturn(speed);
		when(device.getMobilityModel()).thenReturn(mobility);
		return device;
	}

	private Task task(int id, ComputingNode origin) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getLength()).thenReturn(1000.0);
		when(task.getEdgeDevice()).thenReturn(origin);
		return task;
	}
}
