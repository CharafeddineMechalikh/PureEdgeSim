package examples;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example19PriorityOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "PRIORITY_AWARE" };
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.enableOrchestrators = false;

		Scenario scenario = new Scenario(0, 0, 0);
		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0, scenario);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	private ComputingNode node(TYPES type, double capacity, double activePower) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		when(node.getMipsPerCore()).thenReturn(capacity);
		when(node.getNumberOfCPUCores()).thenReturn(1.0);
		EnergyModelComputingNode energy = mock(EnergyModelComputingNode.class);
		when(energy.getMaxActiveConsumption()).thenReturn(activePower);
		when(node.getEnergyModel()).thenReturn(energy);
		NetworkLink up = mock(NetworkLink.class);
		when(up.getLatency()).thenReturn(0.005);
		when(up.getDst()).thenReturn(type == TYPES.EDGE_DATACENTER ? node : ComputingNode.NULL);
		when(node.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);
		return node;
	}

	private ComputingNode device(ComputingNode edge) {
		ComputingNode device = mock(ComputingNode.class);
		NetworkLink up = mock(NetworkLink.class);
		when(up.getDst()).thenReturn(edge);
		when(device.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);
		return device;
	}

	private Task task(int id, String type, double length, double latency) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getType()).thenReturn(type);
		when(task.getLength()).thenReturn(length);
		when(task.getMaxLatency()).thenReturn(latency);
		when(task.getStatus()).thenReturn(Task.Status.SUCCESS);
		when(task.getTotalDelay()).thenReturn(0.1);
		return task;
	}

	private Example19PriorityOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example19PriorityOrchestrator(manager);
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanPriorityAware() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "ANT_COLONY" };
		ComputingNode cloud = node(TYPES.CLOUD, 100, 10);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(cloud));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example19PriorityOrchestrator(manager));
		assertTrue(exception.getMessage().contains("PRIORITY_AWARE"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(1, "Hard Real-time", 100, 0.05);

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoNodeMatchesTheLayers() {
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(1, "Hard Real-time", 100, 0.05);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void aHardTaskAvoidsTheWanWhenTheEdgeMeetsItsDeadline() {
		ComputingNode cloud = node(TYPES.CLOUD, 10000, 100);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 10000, 100);
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		Task task = task(1, "Hard Real-time", 100, 0.05);
		ComputingNode origin = device(edge);
		when(task.getEdgeDevice()).thenReturn(origin);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));
	}

	@Test
	void aHardTaskFallsBackToTheFastestNodeWhenEveryDeadlineIsMissed() {
		ComputingNode cloud = node(TYPES.CLOUD, 10000, 100);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 1, 100);
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		Task task = task(1, "Hard Real-time", 100, 0.05);
		ComputingNode origin = device(edge);
		when(task.getEdgeDevice()).thenReturn(origin);

		assertEquals(0, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));
	}

	@Test
	void aSoftTaskFollowsTheNormalizedDelay() {
		ComputingNode cloud = node(TYPES.CLOUD, 1_000_000, 100);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 10_000, 100);
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		Task task = task(1, "Soft Real-time", 1_000_000, 300);
		ComputingNode origin = device(edge);
		when(task.getEdgeDevice()).thenReturn(origin);

		assertEquals(0, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));
	}

	@Test
	void aBackgroundTaskFollowsTheEnergyEstimate() {
		ComputingNode cloud = node(TYPES.CLOUD, 10000, 5000);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 10000, 10);
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		Task task = task(1, "Non Real-time", 1000, 300);
		ComputingNode origin = device(edge);
		when(task.getEdgeDevice()).thenReturn(origin);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));
	}

	@Test
	void theEnergyEstimateMultipliesTheActivePowerByTheComputeTime() {
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 1, 10)));
		Task task = task(1, "Non Real-time", 3600, 300);

		assertEquals(10.0, orchestrator.estimatedEnergy(task, node(TYPES.CLOUD, 1, 10)), 1e-9);
	}

	@Test
	void theDelayEstimateGrowsWithTheQueueAndReturnsWhenTheTaskCompletes() {
		ComputingNode cloud = node(TYPES.CLOUD, 100, 10);
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(cloud));
		Task first = task(1, "Soft Real-time", 200, 300);
		Task second = task(2, "Soft Real-time", 200, 300);

		assertEquals(2.0 + SimulationParameters.wanLatency, orchestrator.estimatedDelay(first, cloud), 1e-9);
		orchestrator.findComputingNode(new String[] { "Cloud" }, first);
		assertEquals(4.0 + SimulationParameters.wanLatency, orchestrator.estimatedDelay(second, cloud), 1e-9);
		orchestrator.resultsReturned(first);
		assertEquals(2.0 + SimulationParameters.wanLatency, orchestrator.estimatedDelay(second, cloud), 1e-9);
	}

	@Test
	void resultsOfAnUnknownTaskAreIgnored() {
		Example19PriorityOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		assertDoesNotThrow(() -> orchestrator.resultsReturned(task(99, "Soft Real-time", 100, 300)));
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithThePriorityOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example19PriorityOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("priority.properties");
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		Path latest = Files.list(Path.of(OUTPUT_FOLDER))
				.filter(Files::isDirectory)
				.filter(path -> Character.isDigit(path.getFileName().toString().charAt(0)))
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder"));
		Path csv = Files.walk(latest)
				.filter(path -> path.toString().endsWith(".csv"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no results csv in " + latest));
		assertTrue(Files.readString(csv).contains("PRIORITY_AWARE"), "the csv should name the algorithm that ran");
	}
}
