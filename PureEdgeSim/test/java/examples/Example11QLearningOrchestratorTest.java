package examples;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
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
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example11QLearningOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		Randomness.setSeed(1);
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "Q_LEARNING" };
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

	private ComputingNode cloudNode(double mips) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(TYPES.CLOUD);
		when(node.getMipsPerCore()).thenReturn(mips);
		return node;
	}

	private Example11QLearningOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example11QLearningOrchestrator(manager);
	}

	private Task task(int id, double length, Task.Status status, double delay) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getLength()).thenReturn(length);
		when(task.getType()).thenReturn("Soft Real-time");
		when(task.getStatus()).thenReturn(status);
		when(task.getTotalDelay()).thenReturn(delay);
		return task;
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanQLearning() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		ComputingNode node = cloudNode(100);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(node));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example11QLearningOrchestrator(manager));
		assertTrue(exception.getMessage().contains("Q_LEARNING"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(1, 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoNodeMatchesTheLayers() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(1, 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void theLeastLoadedNodeOfTheChosenTierIsUsed() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100), cloudNode(200)));
		Task first = task(1, 1000, Task.Status.SUCCESS, 0.1);
		Task second = task(2, 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Cloud" }, first));
		assertEquals(0, orchestrator.findComputingNode(new String[] { "Cloud" }, second));
	}

	@Test
	void theStateKeyBucketsTheTaskLength() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		assertEquals("Soft Real-time|0|0", orchestrator.getStateKey(task(1, 999, Task.Status.SUCCESS, 0), "Cloud"));
		assertEquals("Soft Real-time|1|0", orchestrator.getStateKey(task(2, 1000, Task.Status.SUCCESS, 0), "Cloud"));
		assertEquals("Soft Real-time|1|0", orchestrator.getStateKey(task(3, 4999, Task.Status.SUCCESS, 0), "Cloud"));
		assertEquals("Soft Real-time|2|0", orchestrator.getStateKey(task(4, 5000, Task.Status.SUCCESS, 0), "Cloud"));
		assertEquals("Soft Real-time|3|0", orchestrator.getStateKey(task(5, 20000, Task.Status.SUCCESS, 0), "Cloud"));
	}

	@Test
	void theStateKeyCountsThePendingTasksOfTheTier() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task first = task(1, 1000, Task.Status.SUCCESS, 0.1);

		orchestrator.findComputingNode(new String[] { "Cloud" }, first);
		assertEquals("Soft Real-time|1|1", orchestrator.getStateKey(first, "Cloud"));
	}

	@Test
	void aSuccessfulTaskMovesTheValueTowardsTheNegatedDelay() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(1, 1000, Task.Status.SUCCESS, 0.4);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		String state = orchestrator.getStateKey(task, "Cloud");
		assertEquals(0.0, orchestrator.getQValue(state, 0));

		orchestrator.resultsReturned(task);
		assertEquals(-0.08, orchestrator.getQValue(state, 0), 1e-9);
	}

	@Test
	void aFailedTaskAppliesTheFailurePenalty() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(1, 5000, Task.Status.FAILED, 0.4);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		String state = orchestrator.getStateKey(task, "Cloud");
		orchestrator.resultsReturned(task);

		assertEquals(-200.0, orchestrator.getQValue(state, 0), 1e-9);
	}

	@Test
	void resultsOfAnUnknownTaskAreIgnored() {
		Example11QLearningOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		assertDoesNotThrow(() -> orchestrator.resultsReturned(task(99, 1000, Task.Status.SUCCESS, 0.1)));
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheQLearningOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example11QLearningOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("qlearning.properties");
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		assertEquals("Q_LEARNING", SimulationParameters.orchestrationAlgorithms[0]);
		Path latest = Files.list(Path.of(OUTPUT_FOLDER))
				.filter(Files::isDirectory)
				.filter(path -> Character.isDigit(path.getFileName().toString().charAt(0)))
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder"));
		Path csv = Files.walk(latest)
				.filter(path -> path.toString().endsWith(".csv"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no results csv in " + latest));
		assertTrue(new File(csv.toString()).length() > 0);
		assertTrue(Files.readString(csv).contains("Q_LEARNING"), "the csv should name the algorithm that ran");
	}
}
