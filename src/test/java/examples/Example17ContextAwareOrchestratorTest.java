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
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example17ContextAwareOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		Randomness.setSeed(1);
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "CONTEXT_AWARE" };
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

	private ComputingNode device(boolean battery, double batteryLevel, String connectivity, boolean mobile) {
		ComputingNode device = mock(ComputingNode.class);
		EnergyModelComputingNode energy = mock(EnergyModelComputingNode.class);
		when(energy.isBatteryPowered()).thenReturn(battery);
		when(energy.getBatteryLevelPercentage()).thenReturn(batteryLevel);
		when(energy.getConnectivityType()).thenReturn(connectivity);
		when(device.getEnergyModel()).thenReturn(energy);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mobility.isMobile()).thenReturn(mobile);
		when(device.getMobilityModel()).thenReturn(mobility);
		return device;
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

	private Example17ContextAwareOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example17ContextAwareOrchestrator(manager);
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanContextAware() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "SOFTMAX" };
		ComputingNode cloud = node(TYPES.CLOUD);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(cloud));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example17ContextAwareOrchestrator(manager));
		assertTrue(exception.getMessage().contains("CONTEXT_AWARE"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example17ContextAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoNodeMatchesTheLayers() {
		Example17ContextAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void theContextVectorCarriesTheDeviceAndTaskState() {
		Example17ContextAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, "Hard Real-time", 50000, Task.Status.SUCCESS, 0.1);
		ComputingNode origin = device(true, 40, "cellular", true);
		when(task.getEdgeDevice()).thenReturn(origin);

		double[] features = orchestrator.getFeatures(task);

		assertEquals(Example17ContextAwareOrchestrator.FEATURE_COUNT, features.length);
		assertEquals(1.0, features[0], 1e-9, "Hard Real-time should be the first one-hot slot");
		assertEquals(0.5, features[3], 1e-9, "half of the length scale");
		assertEquals(0.4, features[Example17ContextAwareOrchestrator.BATTERY_FEATURE], 1e-9);
		assertEquals(1.0, features[Example17ContextAwareOrchestrator.CELLULAR_FEATURE], 1e-9);
		assertEquals(1.0, features[Example17ContextAwareOrchestrator.MOBILE_FEATURE], 1e-9);
	}

	@Test
	void aTaskWithoutBatteryOrMobilityStillProducesAContext() {
		Example17ContextAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, "Non Real-time", 1000, Task.Status.SUCCESS, 0.1);
		ComputingNode origin = device(false, 0, "wifi", false);
		when(task.getEdgeDevice()).thenReturn(origin);

		double[] features = orchestrator.getFeatures(task);

		assertEquals(1.0, features[2], 1e-9);
		assertEquals(1.0, features[Example17ContextAwareOrchestrator.BATTERY_FEATURE], 1e-9,
				"a device without battery is treated as full");
		assertEquals(0.0, features[Example17ContextAwareOrchestrator.CELLULAR_FEATURE], 1e-9);
	}

	@Test
	void onlyATierWithAnEligibleNodeCanBeChosen() {
		Example17ContextAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.1);

		assertEquals(0, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));
	}

	@Test
	void learningMovesTheValueOfAFailedDecisionDown() {
		Example17ContextAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, "Soft Real-time", 5000, Task.Status.FAILED, 0.4);
		ComputingNode origin = device(false, 0, "wifi", false);
		when(task.getEdgeDevice()).thenReturn(origin);

		double[] state = orchestrator.getFeatures(task);
		int cloudAction = 2;
		double before = orchestrator.getQValue(state, cloudAction);

		orchestrator.findComputingNode(new String[] { "Cloud" }, task);
		orchestrator.resultsReturned(task);

		assertEquals(1, orchestrator.getUpdates());
		assertTrue(orchestrator.getQValue(state, cloudAction) < before,
				"a failure should lower the value of the chosen layer");
	}

	@Test
	void resultsOfAnUnknownTaskAreIgnored() {
		Example17ContextAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		assertDoesNotThrow(() -> orchestrator.resultsReturned(
				task(99, "Soft Real-time", 1000, Task.Status.SUCCESS, 0.1)));
		assertEquals(0, orchestrator.getUpdates());
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheContextAwareOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example17ContextAwareOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("contextaware.properties");
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
		assertTrue(Files.readString(csv).contains("CONTEXT_AWARE"), "the csv should name the algorithm that ran");
	}
}
