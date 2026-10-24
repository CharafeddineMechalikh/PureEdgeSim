package com.mechalikh.pureedgesim.taskgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.taskgenerator.Task.FailureReason;
import com.mechalikh.pureedgesim.taskgenerator.Task.Status;

class DefaultTaskTest {

	private DefaultTask task;

	@BeforeEach
	void setUp() {
		task = new DefaultTask(7);
	}

	@Test
	void constructorSetsTheId() {
		assertEquals(7, task.getId());
	}

	@Test
	void defaultValuesMatchTheNullObjectAndSuccessStatus() {
		assertEquals(0.0, task.getTime(), 0.0001);
		assertSame(ComputingNode.NULL, task.getEdgeDevice());
		assertSame(ComputingNode.NULL, task.getRegistry());
		assertSame(ComputingNode.NULL, task.getOffloadingDestination());
		assertSame(ComputingNode.NULL, task.getOrchestrator());
		assertEquals(Status.SUCCESS, task.getStatus());
		assertNull(task.getFailureReason());
		assertEquals(0, task.getApplicationID());
		assertEquals(0, task.getContainerSizeInBits());
		assertEquals(0.0, task.getContainerSizeInMBytes(), 0.0001);
		assertEquals(0.0, task.getFileSizeInBits(), 0.0001);
		assertEquals(0.0, task.getOutputSizeInBits(), 0.0001);
		assertNull(task.getType());
	}

	@Test
	void offloadingTimeIsExposedThroughTime() {
		task.setTime(12.5);
		assertEquals(12.5, task.getTime(), 0.0001);
	}

	@Test
	void edgeDeviceCanBeSetAndReturnedWithFluentApi() {
		ComputingNode device = mock(ComputingNode.class);
		assertSame(task, task.setEdgeDevice(device));
		assertSame(device, task.getEdgeDevice());
	}

	@Test
	void registryCanBeSetAndReturnedWithFluentApi() {
		ComputingNode registry = mock(ComputingNode.class);
		assertSame(task, task.setRegistry(registry));
		assertSame(registry, task.getRegistry());
	}

	@Test
	void applicationIdCanBeSetAndReturnedWithFluentApi() {
		assertSame(task, task.setApplicationID(3));
		assertEquals(3, task.getApplicationID());
	}

	@Test
	void failureReasonSetsTheFailedStatus() {
		task.setFailureReason(FailureReason.FAILED_DUE_TO_LATENCY);
		assertEquals(FailureReason.FAILED_DUE_TO_LATENCY, task.getFailureReason());
		assertEquals(Status.FAILED, task.getStatus());
	}

	@Test
	void statusCanBeOverridden() {
		task.setStatus(Status.FAILED);
		assertEquals(Status.FAILED, task.getStatus());
		task.setStatus(Status.SUCCESS);
		assertEquals(Status.SUCCESS, task.getStatus());
	}

	@Test
	void offloadingDestinationCanBeSet() {
		ComputingNode destination = mock(ComputingNode.class);
		task.setOffloadingDestination(destination);
		assertSame(destination, task.getOffloadingDestination());
	}

	@Test
	void fileAndOutputSizesCanBeSetWithFluentApi() {
		assertSame(task, task.setFileSizeInBits(1234L));
		assertSame(task, task.setOutputSizeInBits(5678L));
		assertEquals(1234.0, task.getFileSizeInBits(), 0.0001);
		assertEquals(5678.0, task.getOutputSizeInBits(), 0.0001);
	}

	@Test
	void containerSizeIsConvertedFromBitsToMegabytes() {
		assertSame(task, task.setContainerSizeInBits(8_000_000L));
		assertEquals(8_000_000L, task.getContainerSizeInBits());
		assertEquals(1.0, task.getContainerSizeInMBytes(), 0.0001);

		task.setContainerSizeInBits(2_000_000L);
		assertEquals(0.25, task.getContainerSizeInMBytes(), 0.0001);
	}

	@Test
	void typeCanBeSetWithFluentApi() {
		assertSame(task, task.setType("Hard Real-time"));
		assertEquals("Hard Real-time", task.getType());
	}

	@Test
	void orchestratorFallsBackToTheEdgeDeviceAndMarksItAsOrchestrator() {
		ComputingNode device = mock(ComputingNode.class);
		task.setEdgeDevice(device);

		assertSame(device, task.getOrchestrator());
		verify(device).setAsOrchestrator(true);
	}

	@Test
	void explicitOrchestratorIsReturnedAsIs() {
		ComputingNode device = mock(ComputingNode.class);
		ComputingNode orchestrator = mock(ComputingNode.class);
		task.setEdgeDevice(device);
		task.setOrchestrator(orchestrator);

		assertSame(orchestrator, task.getOrchestrator());
	}

	@Test
	void failureReasonAndStatusEnumsExposeAllConstants() {
		assertTrue(FailureReason.values().length >= 7);
		assertEquals(FailureReason.INSUFFICIENT_POWER, FailureReason.valueOf("INSUFFICIENT_POWER"));
		assertEquals(Status.FAILED, Status.valueOf("FAILED"));
		assertEquals(2, Status.values().length);
	}
}
