package com.mechalikh.pureedgesim.taskorchestrator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class OrchestratorTest extends AbstractTest {

	static class TestOrchestrator extends Orchestrator {
		int selectedIndex = -1;

		TestOrchestrator(SimulationManager simulationManager) {
			super(simulationManager);
		}

		@Override
		protected int findComputingNode(String[] architectureLayers, Task task) {
			return selectedIndex;
		}

		@Override
		public void resultsReturned(Task task) {
			// Nothing to do in the test implementation.
		}

		@Override
		protected void processEvent(Event e) {
			// Nothing to do in the test implementation.
		}
	}

	private DefaultSimulationManager manager;
	private DataCentersManager dataCentersManager;
	private ComputingNodesGenerator generator;
	private SimLog simLog;
	private Scenario scenario;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.enableOrchestrators = false;

		scenario = new Scenario(0, 0, 0);
		simLog = new SimLog("test", false);
		manager = new DefaultSimulationManager(simLog, new PureEdgeSim(), 0, 0, scenario);
		dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	private TestOrchestrator orchestrator() {
		return new TestOrchestrator(manager);
	}

	private ComputingNode node(TYPES type) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		return node;
	}

	private Task taskWith(ComputingNode device, ComputingNode orchestrator) {
		Task task = mock(Task.class);
		when(task.getEdgeDevice()).thenReturn(device);
		when(task.getOrchestrator()).thenReturn(orchestrator);
		when(task.getId()).thenReturn(1);
		when(task.getLength()).thenReturn(1000.0);
		return task;
	}

	private void linkUp(ComputingNode node, ComputingNode destination) {
		NetworkLink link = mock(NetworkLink.class);
		when(link.getDst()).thenReturn(destination);
		when(node.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(link);
	}

	@Test
	void constructorCopiesScenarioAndRegistersItself() {
		ComputingNode cloud = node(TYPES.CLOUD);
		List<ComputingNode> nodes = List.of(cloud);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);

		SimulationManager mockedManager = mock(SimulationManager.class);
		when(mockedManager.getSimulation()).thenReturn(new PureEdgeSim());
		when(mockedManager.getSimulationLogger()).thenReturn(simLog);
		when(mockedManager.getScenario()).thenReturn(scenario);
		when(mockedManager.getDataCentersManager()).thenReturn(dataCentersManager);

		TestOrchestrator created = new TestOrchestrator(mockedManager);

		assertEquals("TRADE_OFF", created.algorithmName);
		assertEquals("ALL", created.architectureName);
		assertArrayEquals(new String[] { "Cloud", "Edge", "Mist" }, created.architectureLayers);
		assertSame(nodes, created.nodeList);
		verify(mockedManager).setOrchestrator(created);
	}

	@Test
	void everyArchitectureSelectsTheExpectedNodeList() {
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		ComputingNode mist = node(TYPES.EDGE_DEVICE);
		List<ComputingNode> cloudList = List.of(cloud);
		List<ComputingNode> mistList = List.of(mist);
		List<ComputingNode> edgeCloudList = List.of(edge, cloud);
		List<ComputingNode> mistCloudList = List.of(mist, cloud);
		List<ComputingNode> edgeList = List.of(edge);
		List<ComputingNode> mistEdgeList = List.of(mist, edge);
		List<ComputingNode> allList = List.of(cloud, edge, mist);

		when(generator.getCloudOnlyList()).thenReturn(cloudList);
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(mistList);
		when(generator.getEdgeAndCloudList()).thenReturn(edgeCloudList);
		when(generator.getMistAndCloudListSensorsExcluded()).thenReturn(mistCloudList);
		when(generator.getEdgeOnlyList()).thenReturn(edgeList);
		when(generator.getMistAndEdgeListSensorsExcluded()).thenReturn(mistEdgeList);
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(allList);

		String[][] cases = { { "CLOUD_ONLY", "Cloud", "cloud" }, { "MIST_ONLY", "Mist", "mist" },
				{ "EDGE_AND_CLOUD", "EdgeAndCloud", "edgeCloud" }, { "ALL", "All", "all" },
				{ "EDGE_ONLY", "Edge", "edge" }, { "MIST_AND_CLOUD", "MistAndCloud", "mistCloud" },
				{ "MIST_AND_EDGE", "MistAndEdge", "mistEdge" } };
		String[][] expectedLayers = { { "Cloud" }, { "Mist" }, { "Cloud", "Edge" }, { "Cloud", "Edge", "Mist" },
				{ "Edge" }, { "Cloud", "Mist" }, { "Mist", "Edge" } };
		List<?>[] expectedLists = { cloudList, mistList, edgeCloudList, allList, edgeList, mistCloudList,
				mistEdgeList };

		for (int i = 0; i < cases.length; i++) {
			SimulationParameters.orchestrationArchitectures = new String[] { cases[i][0] };
			TestOrchestrator created = orchestrator();
			assertArrayEquals(expectedLayers[i], created.architectureLayers,
					"architecture layers for " + cases[i][0]);
			assertSame(expectedLists[i], created.nodeList, "node list for " + cases[i][0]);
		}
	}

	@Test
	void unknownArchitectureLeavesTheOrchestratorUninitialized() {
		SimulationParameters.orchestrationArchitectures = new String[] { "SOMETHING_ELSE" };
		TestOrchestrator created = orchestrator();
		assertNull(created.architectureLayers);
		assertNull(created.nodeList);
	}

	@Test
	void cloudOffloadingIsOnlyPossibleForCloudNodes() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		ComputingNode mist = node(TYPES.EDGE_DEVICE);
		Task task = taskWith(mock(ComputingNode.class), mock(ComputingNode.class));

		assertTrue(orchestrator.offloadingIsPossible(task, cloud, new String[] { "Cloud" }));
		assertFalse(orchestrator.offloadingIsPossible(task, edge, new String[] { "Cloud" }));
		assertFalse(orchestrator.offloadingIsPossible(task, mist, new String[] { "Cloud" }));
		assertFalse(orchestrator.offloadingIsPossible(task, node(TYPES.NULL), new String[] { "Cloud" }));
	}

	@Test
	void edgeOffloadingUsesTheDeviceUpLink() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		ComputingNode device = mock(ComputingNode.class);
		linkUp(device, edge);
		Task task = taskWith(device, mock(ComputingNode.class));

		assertTrue(orchestrator.offloadingIsPossible(task, edge, new String[] { "Edge" }));
	}

	@Test
	void edgeOffloadingCanUseTheOrchestratorUpLink() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		ComputingNode device = mock(ComputingNode.class);
		ComputingNode orchestratorNode = mock(ComputingNode.class);
		linkUp(device, mock(ComputingNode.class));
		linkUp(orchestratorNode, edge);
		Task task = taskWith(device, orchestratorNode);

		assertTrue(orchestrator.offloadingIsPossible(task, edge, new String[] { "Edge" }));
	}

	@Test
	void edgeOffloadingFailsWhenNeitherLinkPointsToTheEdgeDatacenter() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		ComputingNode device = mock(ComputingNode.class);
		ComputingNode orchestratorNode = mock(ComputingNode.class);
		linkUp(device, mock(ComputingNode.class));
		linkUp(orchestratorNode, mock(ComputingNode.class));
		Task task = taskWith(device, orchestratorNode);

		assertFalse(orchestrator.offloadingIsPossible(task, edge, new String[] { "Edge" }));
		assertFalse(orchestrator.offloadingIsPossible(task, edge, new String[] { "Cloud", "Mist" }));
	}

	@Test
	void mistOffloadingRequiresALiveNonSensorDeviceInRange() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode mist = node(TYPES.EDGE_DEVICE);
		ComputingNode device = mock(ComputingNode.class);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mist.getMobilityModel()).thenReturn(mobility);
		when(mobility.distanceTo(device)).thenReturn(10.0);
		Task task = taskWith(device, mock(ComputingNode.class));

		assertTrue(orchestrator.offloadingIsPossible(task, mist, new String[] { "Mist" }));

		when(mist.isDead()).thenReturn(true);
		assertFalse(orchestrator.offloadingIsPossible(task, mist, new String[] { "Mist" }));

		when(mist.isDead()).thenReturn(false);
		when(mist.isSensor()).thenReturn(true);
		assertFalse(orchestrator.offloadingIsPossible(task, mist, new String[] { "Mist" }));
	}

	@Test
	void mistOffloadingFailsWhenTheDeviceIsTooFarAway() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode mist = node(TYPES.EDGE_DEVICE);
		ComputingNode device = mock(ComputingNode.class);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mist.getMobilityModel()).thenReturn(mobility);
		when(mobility.distanceTo(device)).thenReturn(100.0);
		Task task = taskWith(device, mock(ComputingNode.class));

		assertFalse(orchestrator.offloadingIsPossible(task, mist, new String[] { "Mist" }));
	}

	@Test
	void mistOffloadingCanUseTheOrchestratorLocation() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode mist = node(TYPES.EDGE_DEVICE);
		ComputingNode device = mock(ComputingNode.class);
		ComputingNode orchestratorNode = mock(ComputingNode.class);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mist.getMobilityModel()).thenReturn(mobility);
		when(mobility.distanceTo(device)).thenReturn(100.0);
		when(mobility.distanceTo(orchestratorNode)).thenReturn(5.0);
		Task task = taskWith(device, orchestratorNode);

		SimulationParameters.enableOrchestrators = true;
		assertTrue(orchestrator.offloadingIsPossible(task, mist, new String[] { "Mist" }));

		when(mobility.distanceTo(orchestratorNode)).thenReturn(100.0);
		assertFalse(orchestrator.offloadingIsPossible(task, mist, new String[] { "Mist" }));
	}

	@Test
	void sameLocationAlwaysAcceptsCloudDestinations() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode source = mock(ComputingNode.class);
		ComputingNode cloud = node(TYPES.CLOUD);

		assertTrue(orchestrator.sameLocation(source, cloud, 10));
	}

	@Test
	void sameLocationComparesDistancesForOtherDestinations() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode source = mock(ComputingNode.class);
		ComputingNode destination = mock(ComputingNode.class);
		MobilityModel mobility = mock(MobilityModel.class);
		when(source.getMobilityModel()).thenReturn(mobility);
		when(mobility.distanceTo(destination)).thenReturn(10.0);

		assertTrue(orchestrator.sameLocation(source, destination, 10));
		assertFalse(orchestrator.sameLocation(source, destination, 9));
	}

	@Test
	void allArchitectureAcceptsEverySupportedNodeType() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER);
		ComputingNode mist = node(TYPES.EDGE_DEVICE);
		ComputingNode device = mock(ComputingNode.class);
		MobilityModel mobility = mock(MobilityModel.class);
		when(mist.getMobilityModel()).thenReturn(mobility);
		when(mobility.distanceTo(device)).thenReturn(1.0);
		linkUp(device, edge);
		Task task = taskWith(device, mock(ComputingNode.class));
		String[] all = { "Cloud", "Edge", "Mist" };

		assertTrue(orchestrator.offloadingIsPossible(task, cloud, all));
		assertTrue(orchestrator.offloadingIsPossible(task, edge, all));
		assertTrue(orchestrator.offloadingIsPossible(task, mist, all));
	}

	@Test
	void unknownArchitectureRejectsEveryNode() {
		TestOrchestrator orchestrator = orchestrator();
		Task task = taskWith(mock(ComputingNode.class), mock(ComputingNode.class));
		String[] unknown = { "Unknown" };

		assertFalse(orchestrator.offloadingIsPossible(task, node(TYPES.CLOUD), unknown));
		assertFalse(orchestrator.offloadingIsPossible(task, node(TYPES.EDGE_DATACENTER), unknown));
		assertFalse(orchestrator.offloadingIsPossible(task, node(TYPES.EDGE_DEVICE), unknown));
	}

	@Test
	void arrayContainsChecksEveryLayer() {
		TestOrchestrator orchestrator = orchestrator();
		String[] layers = { "Cloud", "Edge" };
		assertTrue(orchestrator.arrayContains(layers, "Cloud"));
		assertTrue(orchestrator.arrayContains(layers, "Edge"));
		assertFalse(orchestrator.arrayContains(layers, "Mist"));
		assertFalse(orchestrator.arrayContains(new String[0], "Cloud"));
	}

	@Test
	void checkComputingNodeRejectsSensors() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode sensor = mock(ComputingNode.class);
		when(sensor.isSensor()).thenReturn(true);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> orchestrator.checkComputingNode(sensor));
		assertTrue(exception.getMessage().contains("sensor"));

		assertDoesNotThrow(() -> orchestrator.checkComputingNode(mock(ComputingNode.class)));
	}

	@Test
	void assignTaskToComputingNodeUpdatesTheTaskAndTheApplicationPlacement() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode node = mock(ComputingNode.class);
		orchestrator.nodeList = List.of(node);
		orchestrator.selectedIndex = 0;
		Task task = mock(Task.class);
		ComputingNode device = mock(ComputingNode.class);
		when(task.getEdgeDevice()).thenReturn(device);

		orchestrator.assignTaskToComputingNode(task, new String[] { "Cloud" });

		verify(task).setOffloadingDestination(node);
		verify(device).setApplicationPlacementLocation(node);
	}

	@Test
	void assignTaskToComputingNodeDoesNothingWhenNoNodeIsSelected() {
		TestOrchestrator orchestrator = orchestrator();
		orchestrator.nodeList = List.of(mock(ComputingNode.class));
		orchestrator.selectedIndex = -1;
		Task task = mock(Task.class);

		orchestrator.assignTaskToComputingNode(task, new String[] { "Cloud" });

		verify(task, never()).setOffloadingDestination(any(ComputingNode.class));
	}

	@Test
	void assignTaskToComputingNodeStillAssignsAfterARejectedSensor() {
		TestOrchestrator orchestrator = orchestrator();
		ComputingNode sensor = mock(ComputingNode.class);
		when(sensor.isSensor()).thenReturn(true);
		orchestrator.nodeList = List.of(sensor);
		orchestrator.selectedIndex = 0;
		Task task = mock(Task.class);
		ComputingNode device = mock(ComputingNode.class);
		when(task.getEdgeDevice()).thenReturn(device);

		assertDoesNotThrow(() -> orchestrator.assignTaskToComputingNode(task, new String[] { "Mist" }));
		verify(task).setOffloadingDestination(sensor);
		verify(device).setApplicationPlacementLocation(sensor);
	}

	@Test
	void orchestrateUsesTheInitializedArchitectureLayers() {
		SimulationParameters.orchestrationArchitectures = new String[] { "CLOUD_ONLY" };
		ComputingNode cloud = node(TYPES.CLOUD);
		when(generator.getCloudOnlyList()).thenReturn(List.of(cloud));
		TestOrchestrator orchestrator = orchestrator();
		orchestrator.selectedIndex = 0;
		Task task = mock(Task.class);
		ComputingNode device = mock(ComputingNode.class);
		when(task.getEdgeDevice()).thenReturn(device);

		orchestrator.orchestrate(task);

		verify(task).setOffloadingDestination(cloud);
	}

	@Test
	void resultsReturnedWithoutOrchestratorHooksIsANoOp() {
		TestOrchestrator orchestrator = orchestrator();
		assertDoesNotThrow(() -> orchestrator.resultsReturned(mock(Task.class)));
	}
}
