package com.mechalikh.pureedgesim.taskorchestrator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class DefaultOrchestratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;
	private Scenario scenario;

	@BeforeEach
	void setUp() {
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.enableOrchestrators = false;

		scenario = new Scenario(0, 0, 0);
		manager = new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0, scenario);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	private ComputingNode cloudNode(double mips) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(TYPES.CLOUD);
		when(node.getMipsPerCore()).thenReturn(mips);
		return node;
	}

	private ComputingNode edgeDatacenter() {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(TYPES.EDGE_DATACENTER);
		when(node.getMipsPerCore()).thenReturn(100.0);
		return node;
	}

	private ComputingNode mistNode(MobilityModel mobility) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(TYPES.EDGE_DEVICE);
		when(node.getMipsPerCore()).thenReturn(100.0);
		when(node.getMobilityModel()).thenReturn(mobility);
		return node;
	}

	private Task task(ComputingNode device) {
		Task task = mock(Task.class);
		when(task.getLength()).thenReturn(1000.0);
		when(task.getEdgeDevice()).thenReturn(device);
		return task;
	}

	private DefaultOrchestrator orchestrator(List<ComputingNode> nodes) {
		when(generator.getAllNodesListSensorsExcluded()).thenReturn(nodes);
		return new DefaultOrchestrator(manager);
	}

	@Test
	void constructorInitializesTheHistoryMapForEveryNode() {
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100), cloudNode(200)));
		assertEquals(2, orchestrator.historyMap.size());
		assertEquals(0, orchestrator.historyMap.get(0));
		assertEquals(0, orchestrator.historyMap.get(1));
	}

	@Test
	void constructorHandlesAnEmptyNodeList() {
		DefaultOrchestrator orchestrator = orchestrator(List.of());
		assertTrue(orchestrator.historyMap.isEmpty());
	}

	@Test
	void tradeOffSelectsTheNodeWithTheLowestWeightAndUpdatesHistory() {
		ComputingNode slow = cloudNode(100);
		ComputingNode fast = cloudNode(200);
		ComputingNode verySlow = cloudNode(50);
		DefaultOrchestrator orchestrator = orchestrator(List.of(slow, fast, verySlow));
		Task task = task(mock(ComputingNode.class));
		String[] architecture = { "Cloud" };

		assertEquals(1, orchestrator.tradeOff(architecture, task));
		assertEquals(1, orchestrator.historyMap.get(1));

		assertEquals(0, orchestrator.tradeOff(architecture, task));
		assertEquals(1, orchestrator.historyMap.get(0));

		assertEquals(1, orchestrator.tradeOff(architecture, task));
		assertEquals(2, orchestrator.historyMap.get(1));
		assertEquals(0, orchestrator.historyMap.get(2));
	}

	@Test
	void tradeOffAccountsForCloudEdgeAndMistWeights() {
		ComputingNode edge = edgeDatacenter();
		ComputingNode device = mock(ComputingNode.class);
		NetworkLink upLink = mock(NetworkLink.class);
		when(upLink.getDst()).thenReturn(edge);
		when(device.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(upLink);

		MobilityModel mobility = mock(MobilityModel.class);
		ComputingNode mist = mistNode(mobility);
		when(mobility.distanceTo(device)).thenReturn(0.0);

		DefaultOrchestrator orchestrator = orchestrator(List.of(edge, mist));
		Task task = task(device);
		String[] architecture = { "Edge", "Mist" };

		assertEquals(0, orchestrator.tradeOff(architecture, task));
		assertEquals(1, orchestrator.tradeOff(architecture, task));
		assertEquals(1, orchestrator.historyMap.get(1));
		assertEquals(1, orchestrator.historyMap.get(0));
	}

	@Test
	void tradeOffReturnsMinusOneWhenNoNodeIsSuitable() {
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		ComputingNode device = mock(ComputingNode.class);
		Task task = task(device);

		assertEquals(-1, orchestrator.tradeOff(new String[] { "Mist" }, task));
		assertEquals(0, orchestrator.historyMap.get(0));
	}

	@Test
	void roundRobinCyclesBetweenTheAvailableNodes() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "ROUND_ROBIN" };
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100), cloudNode(200)));
		Task task = task(mock(ComputingNode.class));
		String[] architecture = { "Cloud" };

		assertEquals(0, orchestrator.roundRobin(architecture, task));
		assertEquals(1, orchestrator.roundRobin(architecture, task));
		assertEquals(0, orchestrator.roundRobin(architecture, task));
		assertEquals(1, orchestrator.roundRobin(architecture, task));
		assertEquals(2, orchestrator.historyMap.get(0));
		assertEquals(2, orchestrator.historyMap.get(1));
	}

	@Test
	void roundRobinReturnsMinusOneWithoutSuitableNodes() {
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(mock(ComputingNode.class));

		assertEquals(-1, orchestrator.roundRobin(new String[] { "Mist" }, task));
		assertEquals(0, orchestrator.historyMap.get(-1));
	}

	@Test
	void findComputingNodeDispatchesToTradeOff() {
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(mock(ComputingNode.class));
		assertEquals(0, orchestrator.findComputingNode(new String[] { "Cloud" }, task));
	}

	@Test
	void findComputingNodeDispatchesToRoundRobin() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "ROUND_ROBIN" };
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(mock(ComputingNode.class));
		assertEquals(0, orchestrator.findComputingNode(new String[] { "Cloud" }, task));
	}

	@Test
	void unknownAlgorithmRaisesAnException() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "UNKNOWN" };
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		Task task = task(mock(ComputingNode.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> orchestrator.findComputingNode(new String[] { "Cloud" }, task));
		assertTrue(exception.getMessage().contains("Unknown orchestration algorithm"));
		assertTrue(exception.getMessage().contains("UNKNOWN"));
	}

	@Test
	void resultsReturnedAndProcessEventAreNoOps() {
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100)));
		assertDoesNotThrow(() -> orchestrator.resultsReturned(mock(Task.class)));
		assertDoesNotThrow(() -> orchestrator.processEvent(null));
	}

	@Test
	void historyMapIsLinkedAndStartsAtZeroForAllNodes() {
		DefaultOrchestrator orchestrator = orchestrator(List.of(cloudNode(100), cloudNode(200), cloudNode(300)));
		assertTrue(orchestrator.historyMap instanceof LinkedHashMap);
		Map<Integer, Integer> expected = new LinkedHashMap<>();
		expected.put(0, 0);
		expected.put(1, 0);
		expected.put(2, 0);
		assertEquals(expected, orchestrator.historyMap);
		assertFalse(orchestrator.historyMap.containsValue(1));
	}
}
