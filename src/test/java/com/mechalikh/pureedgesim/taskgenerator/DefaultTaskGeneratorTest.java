package com.mechalikh.pureedgesim.taskgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;

class DefaultTaskGeneratorTest extends AbstractTest {

	/** A valid custom task class with the expected (int) constructor. */
	public static class CustomTask extends DefaultTask {
		public CustomTask(int id) {
			super(id);
		}
	}

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;
	private ComputingNode cloud;
	private List<ComputingNode> devices;

	@BeforeEach
	void setUp() {
		manager = TaskTestSupport.newManager();
		generator = mock(ComputingNodesGenerator.class);
		cloud = mock(ComputingNode.class);
		devices = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			devices.add(TaskTestSupport.device(true));
		}
		devices.add(TaskTestSupport.device(false));

		when(generator.getMistOnlyList()).thenReturn(devices);
		when(generator.getCloudOnlyList()).thenReturn(List.of(cloud));
		TaskTestSupport.installGenerator(manager, generator);

		SimulationParameters.applicationList = List.of(
				new Application("Hard Real-time", 20, 20, 0.02, 160_000, 160_000, 160_000, 500),
				new Application("Soft Real-time", 30, 30, 0.5, 12_000_000, 12_000_000, 400_000, 5000),
				new Application("Non Real-time", 3, 50, 300, 17_600_000, 20_000_000, 1_600_000, 30000));
		SimulationParameters.simulationDuration = 120;
		SimulationParameters.deepLoggingEnabled = false;
	}

	@Test
	void generateAssignsApplicationsAccordingToUsagePercentages() {
		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		List<Task> tasks = taskGenerator.generate().stream().collect(Collectors.toList());

		assertEquals(290, tasks.size());
		Map<Integer, Integer> countByApplication = new HashMap<>();
		for (Task task : tasks) {
			countByApplication.merge(task.getApplicationID(), 1, Integer::sum);
		}
		assertEquals(80, countByApplication.get(0));
		assertEquals(180, countByApplication.get(1));
		assertEquals(30, countByApplication.get(2));

		for (Task task : tasks) {
			Application application = SimulationParameters.applicationList.get(task.getApplicationID());
			assertEquals(application.getType(), task.getType());
			assertEquals(application.getRequestSize(), (long) task.getFileSizeInBits());
			assertEquals(application.getResultsSize(), (long) task.getOutputSizeInBits());
			assertEquals(application.getContainerSizeInBits(), task.getContainerSizeInBits());
			assertEquals(application.getLatency(), task.getMaxLatency(), 0.0001);
			assertEquals((long) application.getTaskLength(), (long) task.getLength());
			assertSame(cloud, task.getRegistry());
			assertNotNull(task.getEdgeDevice());
			assertTrue(task.getTime() > 0);
			assertTrue(task.getTime() <= 140);
			assertTrue(task.getId() > 0);
		}

		List<Integer> assignedApplications = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			for (org.mockito.invocation.Invocation invocation : org.mockito.Mockito.mockingDetails(devices.get(i))
					.getInvocations()) {
				if ("setApplicationType".equals(invocation.getMethod().getName())) {
					assignedApplications.add(invocation.getArgument(0));
				}
			}
		}
		assertEquals(5, assignedApplications.size());
		assertTrue(assignedApplications.contains(0));
		assertTrue(assignedApplications.contains(1));
		assertFalse(assignedApplications.contains(2));
	}

	@Test
	void devicesThatDoNotGenerateTasksAreRemoved() {
		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		taskGenerator.generate();
		// Two devices are assigned the first application, three the second one, and
		// the remaining five are kept for the last application.
		assertEquals(5, taskGenerator.devicesList.size());
		assertFalse(taskGenerator.devicesList.contains(devices.get(10)));
	}

	@Test
	void tasksGetSequentialIds() {
		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		List<Integer> ids = taskGenerator.generate().stream().map(Task::getId).sorted().collect(Collectors.toList());
		assertEquals(290, ids.size());
		for (int i = 0; i < ids.size(); i++) {
			assertEquals(i + 1, ids.get(i).intValue());
		}
	}

	@Test
	void noTasksAreGeneratedForVeryShortSimulations() {
		SimulationParameters.simulationDuration = 30;
		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		assertTrue(taskGenerator.generate().isEmpty());
	}

	@Test
	void singleApplicationUsesAllRemainingDevices() {
		SimulationParameters.applicationList = List.of(new Application("Only", 2, 100, 1, 8_000_000, 8_000_000,
				8_000_000, 10));
		SimulationParameters.simulationDuration = 60;

		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		List<Task> tasks = taskGenerator.generate().stream().collect(Collectors.toList());

		assertEquals(20, tasks.size());
		for (Task task : tasks) {
			assertEquals(0, task.getApplicationID());
			assertEquals("Only", task.getType());
		}
	}

	@Test
	void customTaskClassIsUsedForGeneratedTasks() {
		SimulationParameters.simulationDuration = 60;
		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		taskGenerator.setCustomTaskClass(CustomTask.class);

		List<Task> tasks = taskGenerator.generate().stream().collect(Collectors.toList());
		assertFalse(tasks.isEmpty());
		for (Task task : tasks) {
			assertInstanceOf(CustomTask.class, task);
		}
	}

	@Test
	void createTaskReturnsNullForAnInvalidTaskClass() {
		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		taskGenerator.setCustomTaskClass(Task.class);
		assertNull(taskGenerator.createTask(1));
	}

	@Test
	void createTaskUsesTheConfiguredClass() {
		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		Task task = taskGenerator.createTask(12);
		assertInstanceOf(DefaultTask.class, task);
		assertEquals(12, task.getId());
	}
}
