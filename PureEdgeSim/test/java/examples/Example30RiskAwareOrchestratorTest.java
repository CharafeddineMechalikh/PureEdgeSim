package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Subtask;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example30RiskAwareOrchestratorTest extends AbstractTest {

	private static final String[] ALL = { "Cloud", "Edge", "Mist" };

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "RELIABLE_INNER" };
		SimulationParameters.edgeDevicesRange = 100;
		SimulationParameters.enableOrchestrators = false;
		SimulationParameters.applicationList = videoApplication();

		Scenario scenario = new Scenario(0, 0, 0);
		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0, scenario);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	/** The capture, detect, alert chain: capture and detect have dependents, alert does not. */
	private static List<Application> videoApplication() {
		Application application = mock(Application.class);
		when(application.hasSubtasks()).thenReturn(true);
		List<Subtask> subtasks = Arrays.asList(subtask("capture"), subtask("detect", "capture"),
				subtask("alert", "detect"));
		when(application.getSubtasks()).thenReturn(subtasks);
		return new ArrayList<>(Collections.singletonList(application));
	}

	private static Subtask subtask(String name, String... dependencies) {
		Subtask subtask = mock(Subtask.class);
		when(subtask.getName()).thenReturn(name);
		when(subtask.getDependencies()).thenReturn(Arrays.asList(dependencies));
		return subtask;
	}

	private ComputingNode node(TYPES type, double mips, boolean battery, double level, boolean mobile) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		when(node.getMipsPerCore()).thenReturn(mips);
		when(node.getNumberOfCPUCores()).thenReturn(1.0);
		EnergyModelComputingNode energy = mock(EnergyModelComputingNode.class);
		when(energy.isBatteryPowered()).thenReturn(battery);
		when(energy.getBatteryLevelPercentage()).thenReturn(level);
		when(node.getEnergyModel()).thenReturn(energy);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mobility.isMobile()).thenReturn(mobile);
		when(mobility.distanceTo(org.mockito.ArgumentMatchers.any())).thenReturn(1.0);
		when(node.getMobilityModel()).thenReturn(mobility);
		return node;
	}

	private Task task(int id, String subtask, double length, double latency, ComputingNode edge) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getApplicationID()).thenReturn(0);
		when(task.getSubtaskName()).thenReturn(subtask);
		when(task.getLength()).thenReturn(length);
		when(task.getMaxLatency()).thenReturn(latency);
		ComputingNode origin = node(TYPES.EDGE_DEVICE, 1, false, 0, false);
		NetworkLink up = mock(NetworkLink.class);
		when(up.getDst()).thenReturn(edge);
		when(up.getLatency()).thenReturn(0.005);
		when(origin.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);
		when(task.getEdgeDevice()).thenReturn(origin);
		return task;
	}

	private Example30RiskAwareOrchestrator orchestrator(String algorithm, List<ComputingNode> nodes) {
		SimulationParameters.orchestrationAlgorithms = new String[] { algorithm };
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example30RiskAwareOrchestrator(manager);
	}

	@Test
	void riskIsZeroForInfrastructureAndGrowsWithBatteryAndMobility() {
		assertEquals(0, Example30RiskAwareOrchestrator.risk(node(TYPES.CLOUD, 1, false, 0, false)), 1e-12);
		assertEquals(0, Example30RiskAwareOrchestrator.risk(node(TYPES.EDGE_DATACENTER, 1, false, 0, false)), 1e-12);
		assertEquals(0.25, Example30RiskAwareOrchestrator.risk(node(TYPES.EDGE_DEVICE, 1, false, 0, false)), 1e-12);
		assertEquals(0.5, Example30RiskAwareOrchestrator.risk(node(TYPES.EDGE_DEVICE, 1, true, 100, false)), 1e-12);
		assertEquals(0.75, Example30RiskAwareOrchestrator.risk(node(TYPES.EDGE_DEVICE, 1, true, 0, false)), 1e-12);
		assertEquals(0.75, Example30RiskAwareOrchestrator.risk(node(TYPES.EDGE_DEVICE, 1, false, 0, true)), 1e-12);
		assertEquals(1, Example30RiskAwareOrchestrator.risk(node(TYPES.EDGE_DEVICE, 1, true, 50, true)), 1e-12);
	}

	@Test
	void theConstructorRejectsAnUnknownAlgorithm() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> orchestrator("ANT_COLONY", Arrays.asList(node(TYPES.CLOUD, 1, false, 0, false))));
		assertTrue(exception.getMessage().contains("RELIABLE_INNER"));
	}

	@Test
	void onlySubtasksThatOthersWaitForHaveDependents() {
		Example30RiskAwareOrchestrator orchestrator = orchestrator("RELIABLE_INNER",
				Arrays.asList(node(TYPES.CLOUD, 1, false, 0, false)));
		assertTrue(orchestrator.hasDependents(0, "capture"));
		assertTrue(orchestrator.hasDependents(0, "detect"));
		assertFalse(orchestrator.hasDependents(0, "alert"));
		assertFalse(orchestrator.hasDependents(1, "capture"));
	}

	/**
	 * A gateway on a battery is twice as fast as the edge data center. A subtask
	 * that others wait for goes to the data center under RELIABLE_INNER and to
	 * the gateway under LATENCY_GREEDY.
	 */
	@Test
	void aSubtaskOthersWaitForAvoidsARiskyFastDevice() {
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 10_000, false, 0, false);
		ComputingNode gateway = node(TYPES.EDGE_DEVICE, 20_000, true, 40, false);
		List<ComputingNode> nodes = Arrays.asList(edge, gateway);

		assertEquals(1, orchestrator("LATENCY_GREEDY", nodes).findComputingNode(ALL, task(1, "capture", 5_000, 1, edge)));
		assertEquals(0, orchestrator("RELIABLE_INNER", nodes).findComputingNode(ALL, task(1, "capture", 5_000, 1, edge)));
	}

	@Test
	void aLeafStillUsesTheFastDevice() {
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 10_000, false, 0, false);
		ComputingNode gateway = node(TYPES.EDGE_DEVICE, 20_000, true, 40, false);
		List<ComputingNode> nodes = Arrays.asList(edge, gateway);

		assertEquals(1, orchestrator("RELIABLE_INNER", nodes).findComputingNode(ALL, task(1, "alert", 5_000, 1, edge)));
	}

	@Test
	void withoutASafeNodeTheFastestDeviceIsUsed() {
		ComputingNode gateway = node(TYPES.EDGE_DEVICE, 20_000, true, 40, false);
		ComputingNode phone = node(TYPES.EDGE_DEVICE, 5_000, true, 90, false);

		assertEquals(0, orchestrator("RELIABLE_INNER", Arrays.asList(gateway, phone)).findComputingNode(ALL,
				task(1, "capture", 5_000, 1, ComputingNode.NULL)));
	}

	@Test
	void theDelayEstimateForgetsAFailedTask() {
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 10_000, false, 0, false);
		Example30RiskAwareOrchestrator orchestrator = orchestrator("LATENCY_GREEDY", Arrays.asList(edge));
		Task first = task(1, "capture", 10_000, 10, edge);
		Task second = task(2, "capture", 10_000, 10, edge);
		orchestrator.findComputingNode(ALL, first);
		assertEquals(1, orchestrator.pending(0));
		orchestrator.taskEnded(first);
		assertEquals(0, orchestrator.pending(0));
		orchestrator.findComputingNode(ALL, second);
		orchestrator.resultsReturned(second);
		assertEquals(0, orchestrator.pending(0));
	}

	@Test
	@Tag("integration")
	void aShortCrashRunWritesOneLineOfMetrics() throws Exception {
		Path out = Files.createTempFile("example30-", ".jsonl");
		Path output = Files.createTempDirectory("example30-output");
		System.setProperty("example30.scenario", "crash");
		System.setProperty("example30.devices", "20");
		System.setProperty("example30.seed", "3");
		System.setProperty("example30.algorithms", "RELIABLE_INNER");
		System.setProperty("example30.out", out.toString());
		System.setProperty("example30.output", output.toString() + "/");
		SimLogCapture.mute();
		try {
			new Example30();
		} finally {
			SimLogCapture.unmute();
			for (String key : Arrays.asList("example30.scenario", "example30.devices", "example30.seed",
					"example30.algorithms", "example30.out", "example30.output"))
				System.clearProperty(key);
		}
		List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
		assertEquals(1, lines.size());
		String json = lines.get(0);
		assertTrue(json.contains("\"algorithm\":\"RELIABLE_INNER\""), json);
		assertTrue(json.contains("\"devices\":20"), json);
		assertTrue(json.contains("\"requests_generated\":"), json);
	}
}
