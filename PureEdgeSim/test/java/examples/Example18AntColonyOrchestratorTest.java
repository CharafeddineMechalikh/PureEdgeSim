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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example18AntColonyOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		Randomness.setSeed(1);
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "ANT_COLONY" };
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.enableOrchestrators = false;

		Scenario scenario = new Scenario(0, 0, 0);
		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0, scenario);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	@AfterEach
	void resetRandomness() {
		Randomness.reset();
	}

	private ComputingNode node(TYPES type) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		when(node.getMipsPerCore()).thenReturn(100.0);
		when(node.getNumberOfCPUCores()).thenReturn(1.0);
		return node;
	}

	private ComputingNode device(ComputingNode edge) {
		ComputingNode device = mock(ComputingNode.class);
		NetworkLink up = mock(NetworkLink.class);
		when(up.getDst()).thenReturn(edge == null ? ComputingNode.NULL : edge);
		when(device.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);
		return device;
	}

	private Task task(int id, Task.Status status, double delay) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getLength()).thenReturn(1000.0);
		when(task.getType()).thenReturn("Soft Real-time");
		when(task.getStatus()).thenReturn(status);
		when(task.getTotalDelay()).thenReturn(delay);
		when(task.getMaxLatency()).thenReturn(1.0);
		return task;
	}

	private Example18AntColonyOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example18AntColonyOrchestrator(manager);
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanAntColony() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "CONTEXT_AWARE" };
		ComputingNode cloud = node(TYPES.CLOUD);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(cloud));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example18AntColonyOrchestrator(manager));
		assertTrue(exception.getMessage().contains("ANT_COLONY"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example18AntColonyOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoNodeMatchesTheLayers() {
		Example18AntColonyOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void aSuccessfulTaskRaisesThePheromoneOfItsTier() {
		Example18AntColonyOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, Task.Status.SUCCESS, 0.1);

		assertEquals(0.5, orchestrator.getPheromone("Cloud"), 1e-9);
		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		orchestrator.resultsReturned(task);

		assertTrue(orchestrator.getPheromone("Cloud") > 0.5);
	}

	@Test
	void aFailedTaskLowersThePheromoneOfItsTier() {
		Example18AntColonyOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, Task.Status.FAILED, 0.1);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		orchestrator.resultsReturned(task);

		assertTrue(orchestrator.getPheromone("Cloud") < 0.5);
	}

	@Test
	void theEstimatedDelayOfTheCloudIncludesTheWanLatency() {
		Example18AntColonyOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		Task task = task(1, Task.Status.SUCCESS, 0.1);
		when(task.getLength()).thenReturn(200.0);

		assertEquals(2.0, orchestrator.estimatedDelay(task, edge), 1e-9);
		assertEquals(2.0 + SimulationParameters.wanLatency, orchestrator.estimatedDelay(task, cloud), 1e-9);
	}

	@Test
	void theTrailSteersLaterTasksAwayFromAFailingTier() {
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		Example18AntColonyOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		ComputingNode origin = device(edge);

		for (int id = 0; id < 40; id++) {
			Task task = task(id, Task.Status.SUCCESS, 0.1);
			when(task.getEdgeDevice()).thenReturn(origin);
			int chosen = orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task);
			when(task.getStatus()).thenReturn(chosen == 1 ? Task.Status.SUCCESS : Task.Status.FAILED);
			orchestrator.resultsReturned(task);
		}

		int edgeChoices = 0;
		int cloudChoices = 0;
		for (int id = 100; id < 300; id++) {
			Task task = task(id, Task.Status.SUCCESS, 0.1);
			when(task.getEdgeDevice()).thenReturn(origin);
			int chosen = orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task);
			if (chosen == 1)
				edgeChoices++;
			else
				cloudChoices++;
		}

		assertTrue(edgeChoices > cloudChoices,
				"the tier that keeps succeeding should attract most of the colony");
	}

	@Test
	void resultsOfAnUnknownTaskAreIgnored() {
		Example18AntColonyOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		assertDoesNotThrow(() -> orchestrator.resultsReturned(task(99, Task.Status.SUCCESS, 0.1)));
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheAntColonyOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example18AntColonyOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("antcolony.properties");
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
		assertTrue(Files.readString(csv).contains("ANT_COLONY"), "the csv should name the algorithm that ran");
	}
}
