package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTask;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Behavioural tests for {@link DefaultComputingNode}: resource accounting, CPU
 * utilisation, task admission/queueing, execution completion and application
 * placement.
 */
class DefaultComputingNodeTest extends DatacentersTestSupport {

	private DefaultComputingNode node;

	@BeforeEach
	void setUp() {
		createSimulationManager(1);
		node = new DefaultComputingNode(simulationManager, 10, 1, 100, 10);
	}

	private static DefaultTask task(int id, double length, double containerMegabytes) {
		DefaultTask task = new DefaultTask(id);
		task.setLength(length);
		task.setContainerSizeInBits((long) (containerMegabytes * 8_000_000));
		return task;
	}

	@Test
	void nodesWithoutComputingResourcesAreSensors() {
		assertTrue(new DefaultComputingNode(simulationManager, 0, 4, 100, 100).isSensor());
		assertTrue(new DefaultComputingNode(simulationManager, 10, 0, 100, 100).isSensor());
		assertTrue(new DefaultComputingNode(simulationManager, 10, 4, 0, 100).isSensor());
		assertFalse(new DefaultComputingNode(simulationManager, 10, 4, 100, 100).isSensor());
	}

	@Test
	void cpuUtilizationIsZeroWithoutMipsCapacity() {
		DefaultComputingNode noCpu = new DefaultComputingNode(simulationManager, 0, 4, 100, 100);
		noCpu.addCpuUtilization(task(1, 100, 1));
		assertEquals(0, noCpu.getCurrentCpuUtilization());
		assertEquals(0, noCpu.getAvgCpuUtilization());
	}

	@Test
	void currentAndAverageCpuUtilizationAreComputed() {
		setClock(1);
		assertTrue(node.isIdle());
		node.addCpuUtilization(task(1, 5, 1));
		assertEquals(50, node.getCurrentCpuUtilization(), 0.0001);
		// average over the simulation clock: (5 * 100) / (10 * 1) = 50 %
		assertEquals(50, node.getAvgCpuUtilization(), 0.0001);

		node.addCpuUtilization(task(2, 10, 1));
		assertEquals(100, node.getCurrentCpuUtilization(), 0.0001, "utilisation is capped at 100%");
		setClock(0.5);
		assertEquals(100, node.getAvgCpuUtilization(), 0.0001, "average utilisation is capped at 100%");
	}

	@Test
	void removalFreesTheCpuAndMarksTheNodeIdle() {
		Task task = task(1, 50, 1);
		node.addCpuUtilization(task);
		assertFalse(node.isIdle());

		node.removeCpuUtilization(task);
		assertTrue(node.isIdle());
		assertEquals(0, node.getCurrentCpuUtilization(), 0.0001);
	}

	@Test
	void removingOneTaskKeepsTheNodeBusyWhileOtherTasksRemain() {
		Task first = task(1, 5, 1);
		Task second = task(2, 5, 1);
		node.addCpuUtilization(first);
		node.addCpuUtilization(second);

		node.removeCpuUtilization(first);

		assertFalse(node.isIdle());
		assertEquals(50, node.getCurrentCpuUtilization(), 0.0001);
	}

	@Test
	void submitTaskStartsExecutionWhenResourcesAreAvailable() {
		Task task = task(1, 100, 1);

		node.submitTask(task);

		assertEquals(99, node.getAvailableStorage(), 0.0001);
		assertEquals(9, node.getAvailableRam(), 0.0001);
		assertTrue(node.getTasksQueue().isEmpty());
		assertFalse(node.isIdle());
		assertEquals(0, task.getExecStartTime(), 0.0001);

		assertEquals(1, simulation.getEventsQueue().size());
		Event event = simulation.getEventsQueue().first();
		assertEquals(10.0, event.getTime(), 0.0001);
		assertEquals(DefaultComputingNode.EXECUTION_FINISHED, event.getTag());
		assertSame(task, event.getData());
	}

	@Test
	void submitTaskQueuesWhenNoCpuCoreIsFree() {
		Task first = task(1, 100, 1);
		Task second = task(2, 100, 1);

		node.submitTask(first);
		node.submitTask(second);

		assertEquals(1, node.getTasksQueue().size());
		assertSame(second, node.getTasksQueue().get(0));
		// The queued task consumes storage immediately but not RAM or a core.
		assertEquals(98, node.getAvailableStorage(), 0.0001);
		assertEquals(9, node.getAvailableRam(), 0.0001);
	}

	@Test
	void submitTaskQueuesWhenRamIsInsufficient() {
		DefaultComputingNode smallRam = new DefaultComputingNode(simulationManager, 10, 2, 100, 1);
		Task task = task(1, 100, 1);

		smallRam.submitTask(task);

		assertEquals(1, smallRam.getTasksQueue().size());
		assertEquals(1, smallRam.getAvailableRam(), 0.0001);
	}

	@Test
	void executionFinishedReleasesResourcesAndReturnsResults() {
		Task task = task(1, 100, 1);
		node.submitTask(task);
		Event executionEvent = simulation.getEventsQueue().first();
		simulation.getEventsQueue().remove(executionEvent);

		setClock(10);
		node.processEvent(executionEvent);

		assertEquals(100, node.getAvailableStorage(), 0.0001);
		assertEquals(10, node.getAvailableRam(), 0.0001);
		assertEquals(1, node.availableCores);
		assertTrue(node.isIdle());
		assertEquals(10, task.getActualCpuTime(), 0.0001);

		assertEquals(1, simulation.getEventsQueue().size());
		Event resultEvent = simulation.getEventsQueue().first();
		assertEquals(SimulationManager.TRANSFER_RESULTS_TO_ORCH, resultEvent.getTag());
		assertSame(task, resultEvent.getData());
		assertSame(simulationManager, resultEvent.getSimEntity());
	}

	@Test
	void executionFinishedStartsTheNextQueuedTask() {
		Task first = task(1, 100, 1);
		Task second = task(2, 100, 1);
		node.submitTask(first);
		node.submitTask(second);
		Event executionEvent = simulation.getEventsQueue().first();
		simulation.getEventsQueue().remove(executionEvent);
		assertEquals(1, node.getTasksQueue().size());

		setClock(10);
		node.processEvent(executionEvent);

		assertTrue(node.getTasksQueue().isEmpty());
		assertEquals(9, node.getAvailableRam(), 0.0001);
		// The finished task releases its MB, but the queued task's container was
		// already accounted for when it was submitted.
		assertEquals(99, node.getAvailableStorage(), 0.0001);
		assertEquals(10, second.getExecStartTime(), 0.0001);
		assertFalse(node.isIdle());
	}

	@Test
	void processEventDelegatesExecutionFinishedToTheHandler() {
		Task task = task(1, 100, 1);
		node.submitTask(task);
		Event executionEvent = simulation.getEventsQueue().first();
		simulation.getEventsQueue().remove(executionEvent);

		setClock(10);
		node.processEvent(new Event(node, 10.0, DefaultComputingNode.EXECUTION_FINISHED, task));

		assertEquals(10, task.getActualCpuTime(), 0.0001);
		assertEquals(100, node.getAvailableStorage(), 0.0001);
	}

	@Test
	void applicationPlacementMovesTheDeviceToDeviceLinkToTheNewHost() {
		node.setType(TYPES.EDGE_DEVICE);
		DefaultComputingNode host = new DefaultComputingNode(simulationManager, 10, 1, 100, 100);
		host.setType(TYPES.EDGE_DEVICE);

		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		InfrastructureGraph topology = mock(InfrastructureGraph.class);
		when(dataCentersManager.getTopology()).thenReturn(topology);
		simulationManager.setDataCentersManager(dataCentersManager);

		NetworkLink ownLink = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.LAN);
		node.setCurrentLink(ownLink, LinkOrientation.DEVICE_TO_DEVICE);

		node.setApplicationPlacementLocation(host);

		verify(topology).removeLink(ownLink);
		assertSame(host, ownLink.getDst());
		verify(topology).addLink(ownLink);
		assertSame(host, node.getApplicationPlacementLocation());
		assertTrue(node.isApplicationPlaced());
	}

	@Test
	void applicationPlacementOnANonEdgeDeviceKeepsTheTopologyUntouched() {
		node.setType(TYPES.EDGE_DEVICE);
		DefaultComputingNode cloud = new DefaultComputingNode(simulationManager, 10, 1, 100, 100);
		cloud.setType(TYPES.CLOUD);

		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		simulationManager.setDataCentersManager(dataCentersManager);

		node.setApplicationPlacementLocation(cloud);

		verifyNoInteractions(dataCentersManager);
		assertSame(cloud, node.getApplicationPlacementLocation());
		assertTrue(node.isApplicationPlaced());
	}

	@Test
	void applicationPlacementOnItselfDoesNotChangeLinks() {
		node.setType(TYPES.EDGE_DEVICE);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		simulationManager.setDataCentersManager(dataCentersManager);
		NetworkLink ownLink = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.LAN);
		node.setCurrentLink(ownLink, LinkOrientation.DEVICE_TO_DEVICE);

		node.setApplicationPlacementLocation(node);

		verify(dataCentersManager, never()).getTopology();
		assertSame(node, ownLink.getDst());
		assertSame(node, node.getApplicationPlacementLocation());
		assertTrue(node.isApplicationPlaced());
	}

	@Test
	void executionFinishedOnlyProcessesQueuedTasksWhenTheQueueIsNotEmpty() {
		// A node whose queue is empty must not schedule any execution after
		// releasing a finished task.
		Task task = task(1, 100, 1);
		node.submitTask(task);
		Event executionEvent = simulation.getEventsQueue().first();
		simulation.getEventsQueue().remove(executionEvent);

		assertEquals(0, node.getTasksQueue().size());

		setClock(10);
		node.processEvent(executionEvent);

		assertTrue(node.getTasksQueue().isEmpty());
	}
}
