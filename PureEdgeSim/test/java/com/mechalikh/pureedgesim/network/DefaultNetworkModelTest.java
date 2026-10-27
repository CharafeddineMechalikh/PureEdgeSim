package com.mechalikh.pureedgesim.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class DefaultNetworkModelTest extends AbstractTest {

	private PureEdgeSim simulation;
	private SimulationManager simulationManager;
	private DataCentersManager dataCentersManager;
	private InfrastructureGraph topology;
	private DefaultNetworkModel model;

	@BeforeEach
	void setUp() {
		simulation = NetworkTestSupport.newSimulation();
		simulationManager = NetworkTestSupport.newSimulationManager(simulation);
		dataCentersManager = mock(DataCentersManager.class);
		topology = new InfrastructureGraph();
		when(simulationManager.getDataCentersManager()).thenReturn(dataCentersManager);
		when(dataCentersManager.getTopology()).thenReturn(topology);
		model = new DefaultNetworkModel(simulationManager);
	}

	private ComputingNode node(int id, TYPES type) {
		return NetworkTestSupport.node(id, type);
	}

	private Task task(ComputingNode edgeDevice, ComputingNode orchestrator, ComputingNode destination) {
		Task task = mock(Task.class);
		when(task.getEdgeDevice()).thenReturn(edgeDevice);
		when(task.getOrchestrator()).thenReturn(orchestrator);
		when(task.getOffloadingDestination()).thenReturn(destination);
		when(task.getFileSizeInBits()).thenReturn(100.0);
		when(task.getOutputSizeInBits()).thenReturn(50.0);
		when(task.getContainerSizeInBits()).thenReturn(25L);
		when(task.getRegistry()).thenReturn(orchestrator);
		return task;
	}

	private TransferProgress transfer(Task task, TransferProgress.Type type, ComputingNode... vertices) {
		TransferProgress transfer = new TransferProgress(task, 10.0, type);
		List<ComputingNode> vertexList = new ArrayList<>();
		for (ComputingNode vertex : vertices)
			vertexList.add(vertex);
		transfer.setVertexList(vertexList);
		transfer.setEdgeList(new ArrayList<>());
		return transfer;
	}

	private NetworkLink addGraphEdge(ComputingNode from, ComputingNode to) {
		NetworkLink edge = mock(NetworkLink.class);
		when(edge.getSrc()).thenReturn(from);
		when(edge.getDst()).thenReturn(to);
		when(edge.getLatency()).thenReturn(1.0);
		topology.addLink(edge);
		return edge;
	}

	private DefaultNetworkModel spiedModel() {
		DefaultNetworkModel spied = spy(model);
		doNothing().when(spied).send(any(), any(), any(), anyDouble(), any());
		return spied;
	}

	@SuppressWarnings("unchecked")
	private GraphPath<ComputingNode, NetworkLink> mockedPath(List<ComputingNode> vertices, List<NetworkLink> edges) {
		GraphPath<ComputingNode, NetworkLink> path = mock(GraphPath.class);
		when(path.getVertexList()).thenReturn(vertices);
		when(path.getEdgeList()).thenReturn(edges);
		return path;
	}

	private Event firstEvent() {
		return simulation.getEventsQueue().first();
	}

	@Test
	void sendBetweenTwoEdgeDevicesUsesDirectDeviceToDeviceLink() {
		ComputingNode from = node(1, TYPES.EDGE_DEVICE);
		ComputingNode to = node(2, TYPES.EDGE_DEVICE);
		NetworkLink d2d = mock(NetworkLink.class);
		when(from.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE)).thenReturn(d2d);
		Task task = task(from, to, to);

		model.send(from, to, task, 123, TransferProgress.Type.TASK);

		verify(d2d).setDst(to);
		ArgumentCaptor<TransferProgress> captor = ArgumentCaptor.forClass(TransferProgress.class);
		verify(d2d).addTransfer(captor.capture());
		TransferProgress transfer = captor.getValue();
		assertEquals(List.of(from, to), transfer.getVertexList());
		assertEquals(List.of(d2d), transfer.getEdgeList());
		assertEquals(123, transfer.getFileSize());
		assertEquals(TransferProgress.Type.TASK, transfer.getTransferType());
	}

	@Test
	void sendFromEdgeDeviceToDataCenterPrependsUpLink() {
		ComputingNode device = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dc = node(2, TYPES.EDGE_DATACENTER);
		ComputingNode router = node(3, TYPES.EDGE_DATACENTER);
		NetworkLink up = mock(NetworkLink.class);
		NetworkLink pathEdge = mock(NetworkLink.class);
		when(up.getDst()).thenReturn(router);
		when(device.getCurrentLink(LinkOrientation.UP_LINK)).thenReturn(up);
		topology.getPathsMap().put(topology.getUniqueId(router.getId(), dc.getId()),
				mockedPath(List.of(router, dc), List.of(pathEdge)));
		Task task = task(device, device, dc);

		model.send(device, dc, task, 10, TransferProgress.Type.REQUEST);

		ArgumentCaptor<TransferProgress> captor = ArgumentCaptor.forClass(TransferProgress.class);
		verify(up).addTransfer(captor.capture());
		assertEquals(List.of(device, router, dc), captor.getValue().getVertexList());
		assertEquals(List.of(up, pathEdge), captor.getValue().getEdgeList());
	}

	@Test
	void sendFromDataCenterToEdgeDeviceAppendsDownLink() {
		ComputingNode dc = node(2, TYPES.EDGE_DATACENTER);
		ComputingNode device = node(1, TYPES.EDGE_DEVICE);
		ComputingNode router = node(3, TYPES.EDGE_DATACENTER);
		NetworkLink down = mock(NetworkLink.class);
		NetworkLink pathEdge = mock(NetworkLink.class);
		when(down.getSrc()).thenReturn(router);
		when(device.getCurrentLink(LinkOrientation.DOWN_LINK)).thenReturn(down);
		topology.getPathsMap().put(topology.getUniqueId(dc.getId(), router.getId()),
				mockedPath(List.of(router), List.of(pathEdge)));
		Task task = task(device, dc, dc);

		model.send(dc, device, task, 10, TransferProgress.Type.RESULTS_TO_DEV);

		ArgumentCaptor<TransferProgress> captor = ArgumentCaptor.forClass(TransferProgress.class);
		verify(pathEdge).addTransfer(captor.capture());
		assertEquals(List.of(router, dc), captor.getValue().getVertexList());
		assertEquals(List.of(pathEdge, down), captor.getValue().getEdgeList());
	}

	@Test
	void sendGenericPathIsComputedOnceAndThenCached() {
		ComputingNode from = node(10, TYPES.CLOUD);
		ComputingNode to = node(20, TYPES.EDGE_DATACENTER);
		NetworkLink edge = addGraphEdge(from, to);
		Task task = task(from, from, to);

		model.send(from, to, task, 10, TransferProgress.Type.RESULTS_TO_DEV);
		model.send(from, to, task, 10, TransferProgress.Type.RESULTS_TO_DEV);

		verify(edge, times(2)).addTransfer(any(TransferProgress.class));
		long id = topology.getUniqueId(from.getId(), to.getId());
		assertEquals(1, topology.getPathsMap().size());
		assertTrue(topology.getPathsMap().containsKey(id));
	}

	@Test
	void sendFromEdgeDeviceToCloudUsesGenericPathAndCachesIt() {
		ComputingNode device = node(1, TYPES.EDGE_DEVICE);
		ComputingNode cloud = node(30, TYPES.CLOUD);
		NetworkLink edge = addGraphEdge(device, cloud);
		Task task = task(device, device, cloud);

		model.send(device, cloud, task, 10, TransferProgress.Type.RESULTS_TO_DEV);

		ArgumentCaptor<TransferProgress> captor = ArgumentCaptor.forClass(TransferProgress.class);
		verify(edge).addTransfer(captor.capture());
		assertEquals(List.of(device, cloud), captor.getValue().getVertexList());
		assertEquals(List.of(edge), captor.getValue().getEdgeList());
	}

	@Test
	void sendFromDataCenterToCloudUsesGenericPath() {
		ComputingNode dc = node(2, TYPES.EDGE_DATACENTER);
		ComputingNode cloud = node(30, TYPES.CLOUD);
		NetworkLink edge = addGraphEdge(dc, cloud);
		Task task = task(dc, dc, cloud);

		model.send(dc, cloud, task, 10, TransferProgress.Type.REQUEST);

		verify(edge).addTransfer(any(TransferProgress.class));
	}

	@Test
	void sendRequestFromOrchToDestinationForwardsTaskWhenNeeded() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		ComputingNode dest = node(3, TYPES.CLOUD);
		Task task = task(edge, orch, dest);

		spied.sendRequestFromOrchToDest(task);

		verify(spied).send(orch, dest, task, 100.0, TransferProgress.Type.TASK);
	}

	@Test
	void sendRequestFromOrchToDestinationExecutesLocallyWhenDestinationIsEdgeDevice() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, edge);

		spied.sendRequestFromOrchToDest(task);

		verify(spied, never()).send(any(), any(), any(), anyDouble(), any());
		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
		assertSame(simulationManager, firstEvent().getSimEntity());
	}

	@Test
	void sendRequestFromOrchToDestinationExecutesLocallyWhenOrchestratorIsDestination() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, orch);

		spied.sendRequestFromOrchToDest(task);

		verify(spied, never()).send(any(), any(), any(), anyDouble(), any());
		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void sendResultFromOrchToDeviceForwardsWhenOrchestratorDiffers() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, orch);

		spied.sendResultFromOrchToDev(task);

		verify(spied).send(orch, edge, task, 50.0, TransferProgress.Type.RESULTS_TO_DEV);
	}

	@Test
	void sendResultFromOrchToDeviceFinishesLocallyWhenOrchestratorIsDevice() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		Task task = task(edge, edge, edge);

		spied.sendResultFromOrchToDev(task);

		verify(spied, never()).send(any(), any(), any(), anyDouble(), any());
		assertEquals(DefaultSimulationManager.RESULT_RETURN_FINISHED, firstEvent().getTag());
		assertSame(task, firstEvent().getData());
		assertSame(simulationManager, firstEvent().getSimEntity());
	}

	@Test
	void sendResultFromDeviceToOrchForwardsWhenDestinationDiffers() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		ComputingNode dest = node(3, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, dest);

		spied.sendResultFromDevToOrch(task);

		verify(spied).send(dest, orch, task, 50.0, TransferProgress.Type.RESULTS_TO_ORCH);
	}

	@Test
	void sendResultFromDeviceToOrchSkipsOrchestratorWhenDestinationIsOrchestrator() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, orch);

		spied.sendResultFromDevToOrch(task);

		verify(spied, never()).send(any(), any(), any(), anyDouble(), any());
		assertEquals(NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, firstEvent().getTag());
		assertSame(spied, firstEvent().getSimEntity());
	}

	@Test
	void addContainerDownloadsFromRegistryWhenDifferent() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(2, TYPES.EDGE_DATACENTER);
		ComputingNode registry = node(3, TYPES.CLOUD);
		Task task = task(edge, edge, dest);
		when(task.getRegistry()).thenReturn(registry);

		spied.addContainer(task);

		verify(spied).send(registry, dest, task, 25.0, TransferProgress.Type.CONTAINER);
	}

	@Test
	void addContainerExecutesDirectlyWhenRegistryIsDestination() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, edge, dest);
		when(task.getRegistry()).thenReturn(dest);

		spied.addContainer(task);

		verify(spied, never()).send(any(), any(), any(), anyDouble(), any());
		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void sendRequestFromDeviceToOrchSendsOnlyWhenOrchestrationIsRemote() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, orch);

		SimulationParameters.enableOrchestrators = true;
		spied.sendRequestFromDeviceToOrch(task);

		verify(spied).send(edge, orch, task, 100.0, TransferProgress.Type.REQUEST);
	}

	@Test
	void sendRequestFromDeviceToOrchSchedulesDestinationWhenOrchestrationDisabled() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, orch);

		SimulationParameters.enableOrchestrators = false;
		spied.sendRequestFromDeviceToOrch(task);

		verify(spied, never()).send(any(), any(), any(), anyDouble(), any());
		assertEquals(DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, firstEvent().getTag());
	}

	@Test
	void sendRequestFromDeviceToOrchSchedulesDestinationWhenDeviceIsOrchestrator() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		Task task = task(edge, edge, edge);

		SimulationParameters.enableOrchestrators = true;
		spied.sendRequestFromDeviceToOrch(task);

		verify(spied, never()).send(any(), any(), any(), anyDouble(), any());
		assertEquals(DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, firstEvent().getTag());
	}

	@Test
	void executeTaskOrDownloadContainerExecutesWhenRegistryDisabled() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, edge, dest);
		SimulationParameters.enableRegistry = false;

		model.executeTaskOrDownloadContainer(transfer(task, TransferProgress.Type.TASK, dest));

		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void executeTaskOrDownloadContainerExecutesWhenRegistryModeIsNotCloud() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, edge, dest);
		SimulationParameters.enableRegistry = true;
		SimulationParameters.registryMode = "CACHE";

		model.executeTaskOrDownloadContainer(transfer(task, TransferProgress.Type.TASK, dest));

		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void executeTaskOrDownloadContainerExecutesWhenDestinationIsCloud() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode cloud = node(2, TYPES.CLOUD);
		Task task = task(edge, edge, cloud);
		SimulationParameters.enableRegistry = true;
		SimulationParameters.registryMode = "CLOUD";

		model.executeTaskOrDownloadContainer(transfer(task, TransferProgress.Type.TASK, cloud));

		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void executeTaskOrDownloadContainerDownloadsForNonCloudDestination() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(2, TYPES.EDGE_DATACENTER);
		Task task = task(edge, edge, dest);
		SimulationParameters.enableRegistry = true;
		SimulationParameters.registryMode = "CLOUD";

		model.executeTaskOrDownloadContainer(transfer(task, TransferProgress.Type.TASK, dest));

		assertEquals(DefaultNetworkModel.DOWNLOAD_CONTAINER, firstEvent().getTag());
		assertSame(model, firstEvent().getSimEntity());
	}

	@Test
	void processEventDispatchesEverySupportedTag() {
		DefaultNetworkModel spied = spiedModel();
		ComputingNode edge = node(1, TYPES.CLOUD);
		ComputingNode orch = node(2, TYPES.CLOUD);
		ComputingNode dest = node(3, TYPES.CLOUD);
		Task task = task(edge, orch, dest);

		int[] tags = { NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_ORCH,
				NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION, NetworkModel.DOWNLOAD_CONTAINER,
				NetworkModel.SEND_RESULT_TO_ORCH, NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV };
		for (int tag : tags)
			assertDoesNotThrow(() -> spied.processEvent(new Event(spied, 0.0, tag, task)));

		Task localTask = task(edge, edge, edge);
		TransferProgress transfer = transfer(localTask, TransferProgress.Type.RESULTS_TO_DEV, orch);
		assertDoesNotThrow(() -> spied.processEvent(
				new Event(spied, 0.0, NetworkModel.TRANSFER_FINISHED, transfer)));

		assertDoesNotThrow(() -> spied.processEvent(new Event(spied, 0.0, 99, task)));
	}

	@Test
	void transferFinishedRequestUpdatesEnergyAndNotifiesOrchestrator() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DEVICE);
		EnergyModelComputingNode edgeEnergy = mock(EnergyModelComputingNode.class);
		EnergyModelComputingNode orchEnergy = mock(EnergyModelComputingNode.class);
		when(edge.getEnergyModel()).thenReturn(edgeEnergy);
		when(orch.getEnergyModel()).thenReturn(orchEnergy);
		Task task = task(edge, orch, orch);

		model.transferFinished(transfer(task, TransferProgress.Type.REQUEST, orch));

		verify(edgeEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.TRANSMISSION);
		verify(orchEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.RECEPTION);
		assertEquals(DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, firstEvent().getTag());
		assertSame(task, firstEvent().getData());
	}

	@Test
	void transferFinishedRequestSkipsEnergyWhenNotReceivedByOrchestrator() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DEVICE);
		ComputingNode other = node(3, TYPES.EDGE_DEVICE);
		EnergyModelComputingNode edgeEnergy = mock(EnergyModelComputingNode.class);
		EnergyModelComputingNode orchEnergy = mock(EnergyModelComputingNode.class);
		when(edge.getEnergyModel()).thenReturn(edgeEnergy);
		when(orch.getEnergyModel()).thenReturn(orchEnergy);
		Task task = task(edge, orch, orch);

		model.transferFinished(transfer(task, TransferProgress.Type.REQUEST, other));

		verifyNoInteractions(edgeEnergy, orchEnergy);
		assertEquals(DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, firstEvent().getTag());
	}

	@Test
	void transferFinishedTaskUpdatesEnergyOnlyWhenItIsTheDestination() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.CLOUD);
		ComputingNode dest = node(3, TYPES.EDGE_DATACENTER);
		EnergyModelComputingNode edgeEnergy = mock(EnergyModelComputingNode.class);
		when(edge.getEnergyModel()).thenReturn(edgeEnergy);
		Task task = task(edge, orch, dest);

		model.transferFinished(transfer(task, TransferProgress.Type.TASK, dest));

		verify(edgeEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.TRANSMISSION);
		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void transferFinishedTaskSkipsEnergyWhenNotTheDestination() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.CLOUD);
		ComputingNode dest = node(3, TYPES.EDGE_DATACENTER);
		EnergyModelComputingNode edgeEnergy = mock(EnergyModelComputingNode.class);
		when(edge.getEnergyModel()).thenReturn(edgeEnergy);
		Task task = task(edge, orch, dest);

		model.transferFinished(transfer(task, TransferProgress.Type.TASK, orch));

		verifyNoInteractions(edgeEnergy);
		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void transferFinishedContainerExecutesTaskAndUpdatesEnergy() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(2, TYPES.EDGE_DATACENTER);
		EnergyModelComputingNode edgeEnergy = mock(EnergyModelComputingNode.class);
		when(edge.getEnergyModel()).thenReturn(edgeEnergy);
		Task task = task(edge, edge, dest);
		when(task.getRegistry()).thenReturn(ComputingNode.NULL);

		model.transferFinished(transfer(task, TransferProgress.Type.CONTAINER, dest));

		verify(edgeEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.RECEPTION);
		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());
	}

	@Test
	void transferFinishedContainerUpdatesBothEndsWhenRegistryIsAnEdgeDevice() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(2, TYPES.EDGE_DATACENTER);
		EnergyModelComputingNode edgeEnergy = mock(EnergyModelComputingNode.class);
		when(edge.getEnergyModel()).thenReturn(edgeEnergy);
		Task task = task(edge, edge, dest);
		when(task.getRegistry()).thenReturn(edge);

		model.transferFinished(transfer(task, TransferProgress.Type.CONTAINER, dest));

		verify(edgeEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.TRANSMISSION);
		verify(edgeEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.RECEPTION);
	}

	@Test
	void transferFinishedResultsToOrchReturnsResultsToTheDevice() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(3, TYPES.EDGE_DEVICE);
		EnergyModelComputingNode destEnergy = mock(EnergyModelComputingNode.class);
		EnergyModelComputingNode orchEnergy = mock(EnergyModelComputingNode.class);
		when(dest.getEnergyModel()).thenReturn(destEnergy);
		when(orch.getEnergyModel()).thenReturn(orchEnergy);
		Task task = task(edge, orch, dest);

		model.transferFinished(transfer(task, TransferProgress.Type.RESULTS_TO_ORCH, dest));

		verify(destEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.TRANSMISSION);
		verify(orchEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.RECEPTION);
		assertEquals(NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, firstEvent().getTag());
	}

	@Test
	void transferFinishedResultsToDeviceFinishesTheTask() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.EDGE_DEVICE);
		ComputingNode dest = node(3, TYPES.EDGE_DEVICE);
		EnergyModelComputingNode orchEnergy = mock(EnergyModelComputingNode.class);
		EnergyModelComputingNode edgeEnergy = mock(EnergyModelComputingNode.class);
		when(orch.getEnergyModel()).thenReturn(orchEnergy);
		when(edge.getEnergyModel()).thenReturn(edgeEnergy);
		Task task = task(edge, orch, dest);

		model.transferFinished(transfer(task, TransferProgress.Type.RESULTS_TO_DEV, orch));

		verify(orchEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.TRANSMISSION);
		verify(edgeEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.RECEPTION);
		assertEquals(DefaultSimulationManager.RESULT_RETURN_FINISHED, firstEvent().getTag());
		assertSame(task, firstEvent().getData());
	}

	@Test
	void updateEdgeDevicesRemainingEnergyHandlesAllTypeCombinations() {
		ComputingNode edgeOrigin = node(1, TYPES.EDGE_DEVICE);
		ComputingNode edgeDestination = node(2, TYPES.EDGE_DEVICE);
		ComputingNode cloudOrigin = node(3, TYPES.CLOUD);
		ComputingNode cloudDestination = node(4, TYPES.CLOUD);
		EnergyModelComputingNode originEnergy = mock(EnergyModelComputingNode.class);
		EnergyModelComputingNode destinationEnergy = mock(EnergyModelComputingNode.class);
		when(edgeOrigin.getEnergyModel()).thenReturn(originEnergy);
		when(edgeDestination.getEnergyModel()).thenReturn(destinationEnergy);
		Task task = task(edgeOrigin, cloudOrigin, cloudDestination);
		TransferProgress transfer = transfer(task, TransferProgress.Type.TASK, cloudDestination);

		model.updateEdgeDevicesRemainingEnergy(transfer, ComputingNode.NULL, cloudDestination);
		verifyNoInteractions(originEnergy, destinationEnergy);

		model.updateEdgeDevicesRemainingEnergy(transfer, cloudOrigin, edgeDestination);
		verify(destinationEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.RECEPTION);

		model.updateEdgeDevicesRemainingEnergy(transfer, edgeOrigin, cloudDestination);
		verify(originEnergy).updatewirelessEnergyConsumption(10.0, EnergyModelComputingNode.TRANSMISSION);

		model.updateEdgeDevicesRemainingEnergy(transfer, edgeOrigin, edgeDestination);
		verify(originEnergy, times(2)).updatewirelessEnergyConsumption(10.0,
				EnergyModelComputingNode.TRANSMISSION);
		verify(destinationEnergy, times(2)).updatewirelessEnergyConsumption(10.0,
				EnergyModelComputingNode.RECEPTION);
	}

	@Test
	void helperMethodsScheduleTheirCorrespondingEvents() {
		ComputingNode edge = node(1, TYPES.EDGE_DEVICE);
		ComputingNode orch = node(2, TYPES.CLOUD);
		ComputingNode dest = node(3, TYPES.EDGE_DATACENTER);
		Task task = task(edge, orch, dest);
		TransferProgress transfer = transfer(task, TransferProgress.Type.TASK, dest);

		model.containerDownloadFinished(transfer);
		assertEquals(DefaultSimulationManager.EXECUTE_TASK, firstEvent().getTag());

		simulation.getEventsQueue().remove(firstEvent());
		model.resultsReturnedToDevice(transfer);
		assertEquals(DefaultSimulationManager.RESULT_RETURN_FINISHED, firstEvent().getTag());

		simulation.getEventsQueue().remove(firstEvent());
		model.returnResultsToDevice(transfer);
		assertEquals(NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, firstEvent().getTag());

		simulation.getEventsQueue().remove(firstEvent());
		model.offloadingRequestRecievedByOrchestrator(transfer);
		assertEquals(DefaultSimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, firstEvent().getTag());
	}
}
