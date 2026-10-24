package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTask;

/**
 * Tests the {@link Router} specialisation: a computing-node shaped network
 * component with no computing resources that never hosts applications.
 */
class RouterTest extends DatacentersTestSupport {

	private Router router;

	@BeforeEach
	void setUp() {
		createSimulationManager(1);
		router = new Router(simulationManager);
	}

	@Test
	void routerIsCreatedWithoutComputingResources() {
		assertEquals(0, router.getMipsPerCore(), 0.0001);
		assertEquals(0, router.getNumberOfCPUCores(), 0.0001);
		assertEquals(0, router.getTotalMipsCapacity(), 0.0001);
		assertEquals(0, router.getTotalStorage(), 0.0001);
		assertEquals(0, router.getAvailableStorage(), 0.0001);
		assertEquals(0, router.getRamCapacity(), 0.0001);
		assertEquals(0, router.getAvailableRam(), 0.0001);
		assertTrue(router.isSensor(), "a router has no computing capabilities");
		assertTrue(router.isIdle());
	}

	@Test
	void routerNeverHostsApplications() {
		DefaultComputingNode host = new DefaultComputingNode(simulationManager, 10, 1, 100, 100);
		host.setType(TYPES.EDGE_DEVICE);

		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		simulationManager.setDataCentersManager(dataCentersManager);

		router.setApplicationPlacementLocation(host);

		verifyNoInteractions(dataCentersManager);
		assertSame(ComputingNode.NULL, router.getApplicationPlacementLocation());
		assertFalse(router.isApplicationPlaced());
	}

	@Test
	void routerCanStillBeConnectedAndScheduledAsANode() {
		router.setName("router");
		router.setType(TYPES.EDGE_DEVICE);
		assertEquals("router", router.getName());
		assertEquals(TYPES.EDGE_DEVICE, router.getType());

		router.onSimulationStart();
		assertEquals(1, simulation.getEventsQueue().size());
		assertEquals(AbstractNode.UPDATE_STATUS, simulation.getEventsQueue().first().getTag());
	}

	@Test
	void routerInheritsTaskQueueingWithNoCpu() {
		DefaultTask task = new DefaultTask(1);
		task.setLength(100);
		task.setContainerSizeInBits(8_000_000);

		router.submitTask(task);

		assertEquals(1, router.getTasksQueue().size());
		assertSame(task, router.getTasksQueue().get(0));
	}

	@Test
	void routerHandlesExecutionFinishedEventsWithTaskData() {
		DefaultTask task = new DefaultTask(1);
		task.setLength(100);
		task.setContainerSizeInBits(8_000_000);
		task.setExecutionStartTime(0);

		setClock(10);
		router.processEvent(new Event(router, 10.0, DefaultComputingNode.EXECUTION_FINISHED, task));

		assertEquals(10, task.getActualCpuTime(), 0.0001);
		assertEquals(1, router.getAvailableRam(), 0.0001);
		assertEquals(1, router.getAvailableStorage(), 0.0001);
	}
}
