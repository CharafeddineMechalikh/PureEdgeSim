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

class Example16SoftmaxOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		Randomness.setSeed(1);
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "SOFTMAX" };
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
		return node;
	}

	private ComputingNode device(ComputingNode edge) {
		ComputingNode device = mock(ComputingNode.class);
		NetworkLink up = mock(NetworkLink.class);
		when(up.getDst()).thenReturn(edge == null ? ComputingNode.NULL : edge);
		when(device.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);
		return device;
	}

	private Task task(int id, double length, Task.Status status, double delay) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getLength()).thenReturn(length);
		when(task.getType()).thenReturn("Soft Real-time");
		when(task.getStatus()).thenReturn(status);
		when(task.getTotalDelay()).thenReturn(delay);
		when(task.getMaxLatency()).thenReturn(1.0);
		return task;
	}

	private Example16SoftmaxOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example16SoftmaxOrchestrator(manager);
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanSoftmax() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "Q_LEARNING" };
		ComputingNode cloud = node(TYPES.CLOUD);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(cloud));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example16SoftmaxOrchestrator(manager));
		assertTrue(exception.getMessage().contains("SOFTMAX"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example16SoftmaxOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoNodeMatchesTheLayers() {
		Example16SoftmaxOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void theSampledTierIsAlwaysOneThatCanRunTheTask() {
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		Example16SoftmaxOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		ComputingNode origin = device(edge);

		boolean exploredCloud = false;
		boolean exploredEdge = false;
		for (int id = 0; id < 40; id++) {
			Task next = task(id, 1000, Task.Status.SUCCESS, 0.1);
			when(next.getEdgeDevice()).thenReturn(origin);
			int index = orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, next);
			assertTrue(index == 0 || index == 1, "the chosen node must belong to an allowed tier");
			exploredCloud |= index == 0;
			exploredEdge |= index == 1;
		}

		assertTrue(exploredCloud && exploredEdge, "equal values should explore every available tier");
	}

	@Test
	void aSuccessfulTaskMovesTheValueTowardsTheNegatedNormalizedDelay() {
		Example16SoftmaxOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, 1000, Task.Status.SUCCESS, 0.4);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		String state = orchestrator.getStateKey(task, "Cloud");
		assertEquals(0.0, orchestrator.getQValue(state, 0));

		orchestrator.resultsReturned(task);
		assertEquals(-0.08, orchestrator.getQValue(state, 0), 1e-9);
	}

	@Test
	void aFailedTaskAppliesTheFailurePenalty() {
		Example16SoftmaxOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, 5000, Task.Status.FAILED, 0.4);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		String state = orchestrator.getStateKey(task, "Cloud");
		orchestrator.resultsReturned(task);

		assertEquals(-2.0, orchestrator.getQValue(state, 0), 1e-9);
	}

	@Test
	void theStateKeyBucketsTheTaskLengthAndTheTierLoad() {
		Example16SoftmaxOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, 999, Task.Status.SUCCESS, 0);

		assertEquals("Soft Real-time|0|0", orchestrator.getStateKey(task, "Cloud"));
		assertEquals("Soft Real-time|3|0", orchestrator.getStateKey(task(2, 20000, Task.Status.SUCCESS, 0), "Cloud"));

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		assertEquals("Soft Real-time|0|1", orchestrator.getStateKey(task, "Cloud"));
	}

	@Test
	void resultsOfAnUnknownTaskAreIgnored() {
		Example16SoftmaxOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		assertDoesNotThrow(() -> orchestrator.resultsReturned(task(99, 1000, Task.Status.SUCCESS, 0.1)));
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheSoftmaxOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example16SoftmaxOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("softmax.properties");
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		assertEquals("SOFTMAX", SimulationParameters.orchestrationAlgorithms[0]);
		Path latest = Files.list(Path.of(OUTPUT_FOLDER))
				.filter(Files::isDirectory)
				.filter(path -> Character.isDigit(path.getFileName().toString().charAt(0)))
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder"));
		Path csv = Files.walk(latest)
				.filter(path -> path.toString().endsWith(".csv"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no results csv in " + latest));
		assertTrue(Files.readString(csv).contains("SOFTMAX"), "the csv should name the algorithm that ran");
	}
}
