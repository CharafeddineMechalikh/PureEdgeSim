package examples;

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

class Example12AccessAwareOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "ACCESS_AWARE" };
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.enableOrchestrators = false;

		Scenario scenario = new Scenario(0, 0, 0);
		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0, scenario);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	private ComputingNode node(TYPES type, double mips, int cores) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		when(node.getMipsPerCore()).thenReturn(mips);
		when(node.getNumberOfCPUCores()).thenReturn((double) cores);
		return node;
	}

	private ComputingNode device(String connectivity, ComputingNode edge) {
		ComputingNode device = mock(ComputingNode.class);
		EnergyModelComputingNode energy = mock(EnergyModelComputingNode.class);
		when(energy.getConnectivityType()).thenReturn(connectivity);
		when(device.getEnergyModel()).thenReturn(energy);
		NetworkLink up = mock(NetworkLink.class);
		when(up.getDst()).thenReturn(edge == null ? ComputingNode.NULL : edge);
		when(device.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);
		return device;
	}

	private Task task(ComputingNode device, double length, double requestBits, double resultBits) {
		Task task = mock(Task.class);
		when(task.getEdgeDevice()).thenReturn(device);
		when(task.getLength()).thenReturn(length);
		when(task.getFileSizeInBits()).thenReturn(requestBits);
		when(task.getOutputSizeInBits()).thenReturn(resultBits);
		return task;
	}

	private Example12AccessAwareOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example12AccessAwareOrchestrator(manager);
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanAccessAware() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		ComputingNode cloud = node(TYPES.CLOUD, 100, 10);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(cloud));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example12AccessAwareOrchestrator(manager));
		assertTrue(exception.getMessage().contains("ACCESS_AWARE"));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example12AccessAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(device("wifi", null), 1000, 8_000_000, 400_000);

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoNodeMatchesTheLayers() {
		Example12AccessAwareOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD, 100, 10)));
		Task task = task(device("wifi", null), 1000, 8_000_000, 400_000);

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	void aCellularDevicePrefersTheEdgeOverTheSlowWan() {
		ComputingNode cloud = node(TYPES.CLOUD, 40_000, 200);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 40_000, 10);
		Example12AccessAwareOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		Task task = task(device("cellular", edge), 10_000, 80_000_000, 8_000_000);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));
	}

	@Test
	void aWifiDeviceFollowsTheWanRate() {
		ComputingNode cloud = node(TYPES.CLOUD, 40_000, 200);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 40_000, 10);
		Example12AccessAwareOrchestrator orchestrator = orchestrator(List.of(cloud, edge));
		Task task = task(device("wifi", edge), 10_000, 80_000_000, 8_000_000);

		assertEquals(1, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));

		SimulationParameters.wanLatency = 0.001;
		SimulationParameters.wanBandwidthBitsPerSecond = 10_000_000_000.0;
		assertEquals(0, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task));
	}

	@Test
	void theEstimatedDelayChargesTheAccessTechnologyOfTheDevice() {
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 40_000, 10);
		Example12AccessAwareOrchestrator orchestrator = orchestrator(List.of(edge));
		Task cellular = task(device("cellular", edge), 0, 80_000_000, 0);
		Task wifi = task(device("wifi", edge), 0, 80_000_000, 0);

		double cellularDelay = orchestrator.estimatedDelay(cellular, edge);
		double wifiDelay = orchestrator.estimatedDelay(wifi, edge);

		assertEquals(0.03 + 0.8, cellularDelay, 1e-9);
		assertEquals(0.005 + 80_000_000 / 1_300_000_000.0, wifiDelay, 1e-9);
		assertTrue(cellularDelay > wifiDelay);
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheAccessAwareOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example12AccessAwareOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("accessaware.properties");
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
		assertTrue(Files.readString(csv).contains("ACCESS_AWARE"), "the csv should name the algorithm that ran");
	}
}
