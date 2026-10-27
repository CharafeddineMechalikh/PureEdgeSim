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
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example20MultiObjectiveOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		Randomness.setSeed(1);
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "SCALARIZED_Q" };
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

	private ComputingNode node(TYPES type, double capacity, double activePower) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		when(node.getMipsPerCore()).thenReturn(capacity);
		when(node.getNumberOfCPUCores()).thenReturn(1.0);
		EnergyModelComputingNode energy = mock(EnergyModelComputingNode.class);
		when(energy.getMaxActiveConsumption()).thenReturn(activePower);
		when(node.getEnergyModel()).thenReturn(energy);
		return node;
	}

	private Task task(int id, String type, double length, Task.Status status, double delay) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getType()).thenReturn(type);
		when(task.getLength()).thenReturn(length);
		when(task.getStatus()).thenReturn(status);
		when(task.getTotalDelay()).thenReturn(delay);
		when(task.getMaxLatency()).thenReturn(1.0);
		return task;
	}

	private Example20MultiObjectiveOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example20MultiObjectiveOrchestrator(manager);
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanScalarizedQ() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "Q_LEARNING" };
		ComputingNode cloud = node(TYPES.CLOUD, 100, 10);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(cloud));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example20MultiObjectiveOrchestrator(manager));
		assertTrue(exception.getMessage().contains("SCALARIZED_Q"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example20MultiObjectiveOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(1, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoNodeMatchesTheLayers() {
		Example20MultiObjectiveOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(1, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void theStateKeyBucketsTheTaskLength() {
		Example20MultiObjectiveOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));

		assertEquals("Soft Real-time|0", orchestrator.getStateKey(task(1, "Soft Real-time", 999, Task.Status.SUCCESS, 0)));
		assertEquals("Soft Real-time|1", orchestrator.getStateKey(task(2, "Soft Real-time", 1000, Task.Status.SUCCESS, 0)));
		assertEquals("Soft Real-time|2", orchestrator.getStateKey(task(3, "Soft Real-time", 5000, Task.Status.SUCCESS, 0)));
		assertEquals("Soft Real-time|3", orchestrator.getStateKey(task(4, "Soft Real-time", 20000, Task.Status.SUCCESS, 0)));
	}

	@Test
	void aSuccessfulTaskUpdatesTheDelayAndEnergyValuesSeparately() {
		Example20MultiObjectiveOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(1, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.4);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		String state = orchestrator.getStateKey(task);
		orchestrator.resultsReturned(task);

		assertEquals(-0.08, orchestrator.getDelayValue(state, 0), 1e-9);
		assertEquals(-0.5555555556, orchestrator.getEnergyValue(state, 0), 1e-6);
	}

	@Test
	void aFailedTaskAppliesThePenaltyToBothTables() {
		Example20MultiObjectiveOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(1, "Soft Real-time", 5000, Task.Status.FAILED, 0.4);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		String state = orchestrator.getStateKey(task);
		orchestrator.resultsReturned(task);

		assertEquals(-40.0, orchestrator.getDelayValue(state, 0), 1e-9);
		assertEquals(-40.0, orchestrator.getEnergyValue(state, 0), 1e-9);
	}

	@Test
	void theEnergyEstimateScalesWithTheActivePowerOfTheNode() {
		ComputingNode lowPower = node(TYPES.CLOUD, 100, 10);
		ComputingNode highPower = node(TYPES.CLOUD, 100, 100);
		Example20MultiObjectiveOrchestrator orchestrator = orchestrator(List.of(lowPower, highPower));
		Task task = task(1, "Soft Real-time", 3600, Task.Status.SUCCESS, 0.1);

		assertEquals(0.1, orchestrator.estimatedEnergy(task, 0), 1e-9);
		assertEquals(1.0, orchestrator.estimatedEnergy(task, 1), 1e-9);
	}

	@Test
	void resultsOfAnUnknownTaskAreIgnored() {
		Example20MultiObjectiveOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		assertDoesNotThrow(() -> orchestrator.resultsReturned(
				task(99, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.1)));
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheMultiObjectiveOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example20MultiObjectiveOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("scalarized.properties");
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
		assertTrue(Files.readString(csv).contains("SCALARIZED_Q"), "the csv should name the algorithm that ran");
	}
}
