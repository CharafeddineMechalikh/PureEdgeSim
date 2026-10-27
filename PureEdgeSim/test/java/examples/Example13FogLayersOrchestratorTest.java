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
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example13FogLayersOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "FOG_LAYERS" };
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.enableOrchestrators = false;

		Scenario scenario = new Scenario(0, 0, 0);
		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0, scenario);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	private ComputingNode node(TYPES type) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		when(node.getMipsPerCore()).thenReturn(100.0);
		return node;
	}

	private ComputingNode mistNode(MobilityModel mobility) {
		ComputingNode node = node(TYPES.EDGE_DEVICE);
		when(node.getMobilityModel()).thenReturn(mobility);
		return node;
	}

	private Task task(int id, ComputingNode device) {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(id);
		when(task.getLength()).thenReturn(1000.0);
		when(task.getEdgeDevice()).thenReturn(device);
		return task;
	}

	private Example13FogLayersOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new Example13FogLayersOrchestrator(manager);
	}

	@Test
	void constructorRejectsAnAlgorithmNameOtherThanFogLayers() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		ComputingNode cloud = node(TYPES.CLOUD);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(List.of(cloud));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new Example13FogLayersOrchestrator(manager));
		assertTrue(exception.getMessage().contains("FOG_LAYERS"));
	}

	@Test
	void theMistLayerIsPreferredOverEdgeAndCloud() {
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		MobilityModel mobility = mock(MobilityModel.class);
		ComputingNode mist = mistNode(mobility);
		ComputingNode device = mock(ComputingNode.class);
		when(mobility.distanceTo(device)).thenReturn(0.0);

		Example13FogLayersOrchestrator orchestrator = orchestrator(List.of(cloud, edge, mist));
		assertEquals(2, orchestrator.findComputingNode(new String[] { "Cloud", "Edge", "Mist" }, task(1, device)));
	}

	@Test
	void aLayerMissingFromTheArchitectureIsSkipped() {
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		MobilityModel mobility = mock(MobilityModel.class);
		ComputingNode mist = mistNode(mobility);
		ComputingNode device = mock(ComputingNode.class);
		when(mobility.distanceTo(device)).thenReturn(0.0);
		NetworkLink up = mock(NetworkLink.class);
		when(up.getDst()).thenReturn(edge);
		when(device.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);

		Example13FogLayersOrchestrator orchestrator = orchestrator(List.of(cloud, edge, mist));
		assertEquals(1, orchestrator.findComputingNode(new String[] { "Cloud", "Edge" }, task(1, device)));
	}

	@Test
	void workSpreadsOverTheLayerBeforeItSpillsOver() {
		MobilityModel mobility = mock(MobilityModel.class);
		ComputingNode first = mistNode(mobility);
		ComputingNode second = mistNode(mobility);
		ComputingNode device = mock(ComputingNode.class);
		when(mobility.distanceTo(device)).thenReturn(0.0);

		Example13FogLayersOrchestrator orchestrator = orchestrator(List.of(first, second));
		String[] layers = { "Mist" };
		assertEquals(0, orchestrator.findComputingNode(layers, task(1, device)));
		assertEquals(1, orchestrator.findComputingNode(layers, task(2, device)));
		assertEquals(0, orchestrator.findComputingNode(layers, task(3, device)));
		assertEquals(2, orchestrator.pendingOn(0));
		assertEquals(1, orchestrator.pendingOn(1));
	}

	@Test
	void aReturnedTaskReleasesItsNode() {
		MobilityModel mobility = mock(MobilityModel.class);
		ComputingNode mist = mistNode(mobility);
		ComputingNode device = mock(ComputingNode.class);
		when(mobility.distanceTo(device)).thenReturn(0.0);

		Example13FogLayersOrchestrator orchestrator = orchestrator(List.of(mist));
		Task task = task(1, device);
		orchestrator.findComputingNode(new String[] { "Mist" }, task);
		assertEquals(1, orchestrator.pendingOn(0));

		orchestrator.resultsReturned(task);
		assertEquals(0, orchestrator.pendingOn(0));
	}

	@Test
	void findComputingNodeReturnsMinusOneWithoutArchitectureLayers() {
		Example13FogLayersOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, mock(ComputingNode.class));

		assertEquals(-1, orchestrator.findComputingNode(null, task));
		assertEquals(-1, orchestrator.findComputingNode(new String[] {}, task));
	}

	@Test
	void findComputingNodeReturnsMinusOneWhenNoLayerHasANode() {
		Example13FogLayersOrchestrator orchestrator = orchestrator(List.of(node(TYPES.CLOUD)));
		Task task = task(1, mock(ComputingNode.class));

		assertEquals(-1, orchestrator.findComputingNode(new String[] { "Mist" }, task));
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheFogLayersOrchestrator() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example13FogLayersOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("fog.properties");
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
		assertTrue(Files.readString(csv).contains("FOG_LAYERS"), "the csv should name the algorithm that ran");
	}
}
