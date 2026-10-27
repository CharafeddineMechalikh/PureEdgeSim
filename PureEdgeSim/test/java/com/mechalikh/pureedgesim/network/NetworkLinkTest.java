package com.mechalikh.pureedgesim.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class NetworkLinkTest extends AbstractTest {

	private PureEdgeSim simulation;
	private SimulationManager simulationManager;

	@BeforeEach
	void setUp() {
		simulation = NetworkTestSupport.newSimulation();
		simulationManager = NetworkTestSupport.newSimulationManager(simulation);
		SimulationParameters.networkUpdateInterval = 1;
	}

	private NetworkLink newLink() {
		return new NetworkLink(ComputingNode.NULL, ComputingNode.NULL, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN);
	}

	private TransferProgress newTransfer(double fileSize, ComputingNode... vertices) {
		Task task = mock(Task.class);
		TransferProgress transfer = new TransferProgress(task, fileSize, TransferProgress.Type.TASK);
		List<ComputingNode> vertexList = new ArrayList<>();
		for (ComputingNode vertex : vertices)
			vertexList.add(vertex);
		transfer.setVertexList(vertexList);
		List<NetworkLink> edgeList = new ArrayList<>();
		edgeList.add(mock(NetworkLink.class));
		transfer.setEdgeList(edgeList);
		return transfer;
	}

	@Test
	void defaultConstructorInitialisesNullObjectDefaults() {
		NetworkLink link = new NetworkLink();
		assertSame(ComputingNode.NULL, link.getSrc());
		assertSame(ComputingNode.NULL, link.getDst());
		assertEquals(0, link.getLatency());
		assertNull(link.getType());
		assertSame(EnergyModelNetworkLink.NULL, link.getEnergyModel());
		assertEquals(0, link.getTotalTransferredData());
		assertEquals(0, link.getUsedBandwidth());
		assertTrue(link.transferProgressList.isEmpty());
		assertFalse(link.scheduled);
	}

	@Test
	void constructorStoresNodesAndTypeAndRegistersEntity() {
		ComputingNode src = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode dst = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink link = new NetworkLink(src, dst, simulationManager, NetworkLink.NetworkLinkTypes.WAN);

		assertSame(src, link.getSrc());
		assertSame(dst, link.getDst());
		assertEquals(NetworkLink.NetworkLinkTypes.WAN, link.getType());
		assertSame(simulation, link.getSimulation());
		assertTrue(simulation.getEventsQueue().isEmpty());
	}

	@Test
	void settersUpdateStateAndChain() {
		NetworkLink link = new NetworkLink();
		assertSame(link, link.setLatency(1.25));
		assertSame(link, link.setBandwidth(500));
		assertEquals(1.25, link.getLatency());
		assertEquals(500, link.getBandwidth(1));
		assertEquals(250, link.getBandwidth(2));

		ComputingNode src = NetworkTestSupport.node(3, SimulationParameters.TYPES.CLOUD);
		ComputingNode dst = NetworkTestSupport.node(4, SimulationParameters.TYPES.CLOUD);
		link.setSrc(src);
		link.setDst(dst);
		assertSame(src, link.getSrc());
		assertSame(dst, link.getDst());

		link.setType(NetworkLink.NetworkLinkTypes.MAN);
		assertEquals(NetworkLink.NetworkLinkTypes.MAN, link.getType());
	}

	@Test
	void getBandwidthFallsBackToOneWhenNoRemainingTasks() {
		NetworkLink link = new NetworkLink();
		link.setBandwidth(900);
		assertEquals(900, link.getBandwidth(0));
		assertEquals(900, link.getBandwidth(-3));
		assertEquals(300, link.getBandwidth(3));
	}

	@Test
	void usedBandwidthIsCappedByLinkBandwidth() {
		NetworkLink link = new NetworkLink();
		link.setBandwidth(100);
		link.usedBandwidth = 40;
		assertEquals(40, link.getUsedBandwidth());
		link.usedBandwidth = 400;
		assertEquals(100, link.getUsedBandwidth());
	}

	@Test
	void addTransferAccumulatesDataAndSchedulesOnlyOnce() {
		NetworkLink link = newLink();
		TransferProgress first = newTransfer(100);
		TransferProgress second = newTransfer(250);

		link.addTransfer(first);
		link.addTransfer(second);

		assertEquals(350, link.getTotalTransferredData());
		assertEquals(2, link.transferProgressList.size());
		assertTrue(link.scheduled);
		assertEquals(1, simulation.getEventsQueue().size());
		assertEquals(NetworkLink.UPDATE_PROGRESS, simulation.getEventsQueue().first().getTag());
	}

	@Test
	void processEventIgnoresUnknownTags() {
		NetworkLink link = newLink();
		link.processEvent(new Event(link, 0.0, 12345));
		assertTrue(simulation.getEventsQueue().isEmpty());
	}

	@Test
	void processEventWithoutTransfersClearsScheduledFlag() {
		NetworkLink link = newLink();
		link.scheduled = true;
		link.processEvent(new Event(link, 0.0, NetworkLink.UPDATE_PROGRESS));
		assertFalse(link.scheduled);
	}

	@Test
	void processEventWithRemainingTransfersReschedulesItself() {
		SimulationParameters.realisticNetworkModel = true;
		NetworkLink link = newLink();
		link.setBandwidth(100);
		link.addTransfer(newTransfer(1000));

		link.processEvent(new Event(link, 0.0, NetworkLink.UPDATE_PROGRESS));

		assertEquals(1, link.transferProgressList.size());
		assertTrue(link.scheduled);
		assertEquals(2, simulation.getEventsQueue().size());
	}

	@Test
	void processEventFinishesLastTransferAndResetsScheduledFlag() {
		SimulationParameters.realisticNetworkModel = true;
		ComputingNode src = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode dst = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink link = new NetworkLink(src, dst, simulationManager, NetworkLink.NetworkLinkTypes.LAN);
		link.setBandwidth(1000);
		link.setLatency(0.5);

		SimLog logger = mock(SimLog.class);
		NetworkModel networkModel = mock(NetworkModel.class);
		when(simulationManager.getSimulationLogger()).thenReturn(logger);
		when(simulationManager.getNetworkModel()).thenReturn(networkModel);

		TransferProgress transfer = newTransfer(500, src, dst);
		link.addTransfer(transfer);
		link.processEvent(new Event(link, 0.0, NetworkLink.UPDATE_PROGRESS));

		assertTrue(link.transferProgressList.isEmpty());
		assertFalse(link.scheduled);
		assertEquals(0, transfer.getRemainingFileSize());
		verify(logger).updateNetworkUsage(transfer);
		assertEquals(2, simulation.getEventsQueue().size());
	}

	@Test
	void updateTransfersProgressSplitsBandwidthBetweenTransfers() {
		SimulationParameters.realisticNetworkModel = true;
		NetworkLink link = newLink();
		link.setBandwidth(1000);
		TransferProgress first = newTransfer(10000);
		TransferProgress second = newTransfer(10000);
		link.transferProgressList.add(first);
		link.transferProgressList.add(second);

		link.updateTransfersProgress();

		assertEquals(500, first.getCurrentBandwidth());
		assertEquals(500, second.getCurrentBandwidth());
		assertEquals(500, first.getAverageBandwidth());
		assertEquals(9500, first.getRemainingFileSize());
		assertEquals(9500, second.getRemainingFileSize());
		assertEquals(20000, link.usedBandwidth);
	}

	@Test
	void updateTransferAddsLanManAndWanUsageDependingOnType() {
		SimulationParameters.realisticNetworkModel = true;
		NetworkLink.NetworkLinkTypes[] types = { NetworkLink.NetworkLinkTypes.LAN,
				NetworkLink.NetworkLinkTypes.MAN, NetworkLink.NetworkLinkTypes.WAN,
				NetworkLink.NetworkLinkTypes.IGNORE };
		for (NetworkLink.NetworkLinkTypes type : types) {
			NetworkLink link = new NetworkLink();
			link.setType(type);
			link.setBandwidth(1000);
			TransferProgress transfer = newTransfer(5000);
			transfer.setCurrentBandwidth(1000);

			link.updateTransfer(transfer);

			assertEquals(4000, transfer.getRemainingFileSize(), 0.001);
			assertEquals(type == NetworkLink.NetworkLinkTypes.LAN ? 1 : 0, transfer.getLanNetworkUsage(), 0.001);
			assertEquals(type == NetworkLink.NetworkLinkTypes.MAN ? 1 : 0, transfer.getManNetworkUsage(), 0.001);
			assertEquals(type == NetworkLink.NetworkLinkTypes.WAN ? 1 : 0, transfer.getWanNetworkUsage(), 0.001);
		}
	}

	@Test
	void updateTransferNonRealisticModeCompletesTransferImmediately() {
		SimulationParameters.realisticNetworkModel = false;
		ComputingNode src = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode dst = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		NetworkLink link = new NetworkLink(src, dst, simulationManager, NetworkLink.NetworkLinkTypes.LAN);
		link.setLatency(0.5);
		when(simulationManager.getSimulationLogger()).thenReturn(mock(SimLog.class));
		when(simulationManager.getNetworkModel()).thenReturn(mock(NetworkModel.class));

		TransferProgress transfer = newTransfer(500, src, dst);
		transfer.setCurrentBandwidth(500);

		link.updateTransfer(transfer);

		assertEquals(0, transfer.getRemainingFileSize());
		assertTrue(link.transferProgressList.isEmpty() || !link.transferProgressList.contains(transfer));
		verify(transfer.getTask()).addActualNetworkTime(1.0);
		verify(transfer.getTask()).addActualNetworkTime(0.5);
	}

	@Test
	void transferFinishedIntermediateHopForwardsToNextLink() {
		NetworkLink link = newLink();
		link.setLatency(0.25);
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		ComputingNode c = NetworkTestSupport.node(3, SimulationParameters.TYPES.CLOUD);

		TransferProgress transfer = newTransfer(700, a, b, c);
		NetworkLink firstEdge = transfer.getEdgeList().get(0);
		NetworkLink nextEdge = mock(NetworkLink.class);
		transfer.getEdgeList().add(nextEdge);
		link.transferProgressList.add(transfer);

		link.transferFinished(transfer);

		assertFalse(link.transferProgressList.contains(transfer));
		assertEquals(700, transfer.getRemainingFileSize());
		assertEquals(List.of(b, c), transfer.getVertexList());
		assertEquals(List.of(nextEdge), transfer.getEdgeList());
		verify(nextEdge).addTransfer(transfer);
		verify(firstEdge, org.mockito.Mockito.never()).addTransfer(transfer);
		verify(transfer.getTask()).addActualNetworkTime(0.25);
	}

	@Test
	void transferFinishedLastHopNotifiesLoggerAndNetworkModel() {
		NetworkLink link = newLink();
		link.setLatency(0.75);
		ComputingNode a = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		ComputingNode b = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		SimLog logger = mock(SimLog.class);
		NetworkModel networkModel = mock(NetworkModel.class);
		when(simulationManager.getSimulationLogger()).thenReturn(logger);
		when(simulationManager.getNetworkModel()).thenReturn(networkModel);

		TransferProgress transfer = newTransfer(300, a, b);
		link.transferProgressList.add(transfer);

		link.transferFinished(transfer);

		assertTrue(link.transferProgressList.isEmpty());
		verify(logger).updateNetworkUsage(transfer);
		verify(transfer.getTask()).addActualNetworkTime(0.75);
		assertEquals(1, simulation.getEventsQueue().size());
		Event event = simulation.getEventsQueue().first();
		assertEquals(NetworkModel.TRANSFER_FINISHED, event.getTag());
		assertSame(networkModel, event.getSimEntity());
		assertSame(transfer, event.getData());
		assertEquals(0.75, event.getTime());
	}

	@Test
	void energyModelCanBeReplaced() {
		NetworkLink link = newLink();
		EnergyModelNetworkLink energyModel = new EnergyModelNetworkLink(0.5, link);
		link.setEnergyModel(energyModel);
		assertSame(energyModel, link.getEnergyModel());
		assertNotNull(link.getEnergyModel().getLink());
	}

	@Test
	void getTotalTransferredDataReflectsAllTransfers() {
		NetworkLink link = newLink();
		link.addTransfer(newTransfer(12.5));
		link.addTransfer(newTransfer(7.5));
		assertEquals(20, link.getTotalTransferredData(), 1e-9);
	}

	@Test
	void updateTransferUsesBandwidthWhenComputingDelay() {
		SimulationParameters.realisticNetworkModel = true;
		NetworkLink link = newLink();
		TransferProgress transfer = newTransfer(1000);
		transfer.setCurrentBandwidth(100);
		link.updateTransfer(transfer);
		assertEquals(900, transfer.getRemainingFileSize());
		verify(transfer.getTask()).addActualNetworkTime(1.0);
	}
}
