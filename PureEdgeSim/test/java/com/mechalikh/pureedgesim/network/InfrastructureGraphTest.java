package com.mechalikh.pureedgesim.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jgrapht.GraphPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class InfrastructureGraphTest extends AbstractTest {

	private SimulationManager simulationManager;
	private InfrastructureGraph graph;

	@BeforeEach
	void setUp() {
		simulationManager = NetworkTestSupport.newSimulationManager();
		graph = new InfrastructureGraph();
	}

	private NetworkLink addLink(ComputingNode src, ComputingNode dst, double latency,
			NetworkLink.NetworkLinkTypes type) {
		NetworkLink link = new NetworkLink(src, dst, simulationManager, type);
		link.setLatency(latency);
		graph.addLink(link);
		return link;
	}

	private NetworkLink mockLink(ComputingNode src, ComputingNode dst, double latency) {
		NetworkLink link = mock(NetworkLink.class);
		when(link.getSrc()).thenReturn(src);
		when(link.getDst()).thenReturn(dst);
		when(link.getLatency()).thenReturn(latency);
		return link;
	}

	@Test
	void addLinkAddsVerticesEdgeAndWeight() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink link = addLink(a, b, 1.5, NetworkLink.NetworkLinkTypes.LAN);

		assertTrue(graph.getGraph().containsVertex(a));
		assertTrue(graph.getGraph().containsVertex(b));
		assertTrue(graph.getGraph().containsEdge(link));
		assertEquals(1.5, graph.getGraph().getEdgeWeight(link));
	}

	@Test
	void getDelayReturnsPathWeightAndInfinityWhenUnreachable() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		addLink(a, b, 1.25, NetworkLink.NetworkLinkTypes.LAN);

		assertEquals(1.25, graph.getDelay(a, b));
		assertEquals(Double.POSITIVE_INFINITY, graph.getDelay(b, a));
	}

	@Test
	void getDelayReturnsInfinityForUnknownNode() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		ComputingNode unknown = NetworkTestSupport.node(3, SimulationParameters.TYPES.CLOUD);
		addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);
		assertEquals(Double.POSITIVE_INFINITY, graph.getDelay(a, unknown));
	}

	@Test
	void removeLinkByNodesRemovesTheEdge() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink link = addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);

		graph.removeLink(a, b);

		assertFalse(graph.getGraph().containsEdge(link));
	}

	@Test
	void removeLinkByReferenceRemovesTheEdge() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink link = addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);

		graph.removeLink(link);

		assertFalse(graph.getGraph().containsEdge(link));
	}

	@Test
	void getPathReturnsShortestPath() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink link = addLink(a, b, 2.0, NetworkLink.NetworkLinkTypes.LAN);

		GraphPath<ComputingNode, NetworkLink> path = graph.getPath(a, b);

		assertNotNull(path);
		assertEquals(List.of(a, b), path.getVertexList());
		assertEquals(List.of(link), path.getEdgeList());
		assertEquals(2.0, path.getWeight());
	}

	@Test
	void getPathThrowsDetailedExceptionWhenNoPathExists() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> graph.getPath(b, a));

		assertTrue(exception.getMessage().contains("Cannot get a path from node"));
		assertTrue(exception.getMessage().contains("Possible solutions"));
	}

	@Test
	void getPathWrapsMissingVertexException() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		ComputingNode unknown = NetworkTestSupport.node(3, SimulationParameters.TYPES.CLOUD);
		addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> graph.getPath(a, unknown));

		assertTrue(exception.getMessage().contains("Cannot get a path from node"));
	}

	@Test
	void assertNotNullRejectsNullPathAndReturnsValidPath() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		assertThrows(IllegalArgumentException.class, () -> graph.assertNotNull(null));

		addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);
		GraphPath<ComputingNode, NetworkLink> path = graph.getPath(a, b);
		assertSame(path, graph.assertNotNull(path));
	}

	@Test
	void typedGettersFilterEdgesByClass() {
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		ComputingNode c = NetworkTestSupport.node(3, SimulationParameters.TYPES.EDGE_DATACENTER);
		ComputingNode d = NetworkTestSupport.node(4, SimulationParameters.TYPES.CLOUD);
		ComputingNode e = NetworkTestSupport.node(5, SimulationParameters.TYPES.EDGE_DATACENTER);
		ComputingNode f = NetworkTestSupport.node(6, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode g = NetworkTestSupport.node(7, SimulationParameters.TYPES.EDGE_DEVICE);

		graph.addLink(new NetworkLinkWifiUp(a, b, simulationManager, NetworkLink.NetworkLinkTypes.LAN));
		graph.addLink(new NetworkLinkWifiDown(b, a, simulationManager, NetworkLink.NetworkLinkTypes.LAN));
		graph.addLink(new NetworkLinkWifiDeviceToDevice(c, a, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN));
		graph.addLink(new NetworkLinkEthernet(d, e, simulationManager, NetworkLink.NetworkLinkTypes.LAN));
		graph.addLink(new NetworkLinkCellularUp(f, g, simulationManager, NetworkLink.NetworkLinkTypes.LAN));
		graph.addLink(new NetworkLinkCellularDown(g, f, simulationManager, NetworkLink.NetworkLinkTypes.LAN));
		graph.addLink(new NetworkLinkWanUp(e, d, simulationManager, NetworkLink.NetworkLinkTypes.WAN));
		graph.addLink(new NetworkLinkWanDown(d, e, simulationManager, NetworkLink.NetworkLinkTypes.WAN));
		graph.addLink(new NetworkLinkMan(b, c, simulationManager, NetworkLink.NetworkLinkTypes.MAN));

		assertEquals(3, graph.getWifiLinks().size());
		assertEquals(1, graph.getEthernetLinks().size());
		assertEquals(2, graph.getCellularLinks().size());
		assertEquals(1, graph.getWanUpLinks().size());
		assertEquals(1, graph.getWanDownLinks().size());
		assertEquals(2, graph.getWanLinks().size());
		assertEquals(1, graph.getManLinks().size());
		assertEquals(6, graph.getLanLinks().size());
	}

	@Test
	void getEdgeByClassReturnsEmptyListWhenNoMatch() {
		assertTrue(graph.getWifiLinks().isEmpty());
		assertTrue(graph.getEthernetLinks().isEmpty());
		assertTrue(graph.getCellularLinks().isEmpty());
		assertTrue(graph.getWanLinks().isEmpty());
		assertTrue(graph.getManLinks().isEmpty());
		assertTrue(graph.getLanLinks().isEmpty());
	}

	@Test
	void getEdgeByTypeFiltersByNetworkLinkType() {
		ComputingNode a = NetworkTestSupport.node(10, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(20, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink lan = addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);
		NetworkLink man = addLink(b, a, 2.0, NetworkLink.NetworkLinkTypes.MAN);
		NetworkLink wan = addLink(a, b, 3.0, NetworkLink.NetworkLinkTypes.WAN);

		assertEquals(List.of(lan), graph.getLanLinks());
		assertEquals(List.of(man), graph.getManLinks());
		assertEquals(List.of(wan), graph.getWanLinks());
	}

	@Test
	void savePathsToMapStoresAllPairs() {
		ComputingNode a = NetworkTestSupport.node(10, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(20, SimulationParameters.TYPES.EDGE_DATACENTER);
		addLink(a, b, 1.0, NetworkLink.NetworkLinkTypes.LAN);
		addLink(b, a, 1.0, NetworkLink.NetworkLinkTypes.LAN);

		graph.savePathsToMap(List.of(a, b));

		assertEquals(4, graph.getPathsMap().size());
		assertNotNull(graph.getPathsMap().get(graph.getUniqueId(10, 20)));
		assertNotNull(graph.getPathsMap().get(graph.getUniqueId(20, 10)));
		assertNotNull(graph.getPathsMap().get(graph.getUniqueId(10, 10)));
		assertNotNull(graph.getPathsMap().get(graph.getUniqueId(20, 20)));
	}

	@Test
	void getUniqueIdIsSymmetricForTheSameOperands() {
		assertEquals(0, graph.getUniqueId(0, 0));
		assertEquals(graph.getUniqueId(3, 5), graph.getUniqueId(3, 5));
		assertEquals(graph.getUniqueId(7, 11), graph.getUniqueId(7, 11));
		assertTrue(graph.getUniqueId(7, 11) > 0);
	}

	@Test
	void nullInfrastructureGraphStillProvidesItsOwnStructures() {
		InfrastructureGraph nullGraph = InfrastructureGraph.NULL;
		assertNotNull(nullGraph);
		assertNotNull(nullGraph.getGraph());
		assertNotNull(nullGraph.getPathsMap());
		assertTrue(nullGraph.getPathsMap().isEmpty());
	}
}
