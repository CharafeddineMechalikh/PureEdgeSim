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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.distribution.ConstantDistribution;
import com.mechalikh.pureedgesim.distribution.ExponentialDistribution;
import com.mechalikh.pureedgesim.distribution.UniformDistribution;
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

	// ------------------------------------------------------------------
	// Distribution-valued tasks
	// ------------------------------------------------------------------

	@Test
	void eachTaskDrawsItsOwnSizeAndLengthFromTheApplication() {
		Random random = new Random(7);
		SimulationParameters.applicationList = List.of(new Application("Varied", 2, 100, 1.0, 8_000_000, 12_000, 8_000,
				150, new UniformDistribution(random, 8_000, 16_000), null, null,
				new UniformDistribution(random, 100, 200), null, null, null));
		SimulationParameters.simulationDuration = 60;

		List<Task> tasks = new DefaultTaskGenerator(manager).generate().stream().collect(Collectors.toList());

		assertFalse(tasks.isEmpty());
		Set<Long> requestSizes = new HashSet<>();
		Set<Double> lengths = new HashSet<>();
		for (Task task : tasks) {
			long request = (long) task.getFileSizeInBits();
			double length = task.getLength();
			assertTrue(request >= 8_000 && request <= 16_000, "request size out of the support: " + request);
			assertTrue(length >= 100 && length <= 200, "task length out of the support: " + length);
			// Parameters without a distribution keep their fixed value.
			assertEquals(8_000, (long) task.getOutputSizeInBits());
			requestSizes.add(request);
			lengths.add(length);
		}
		assertTrue(requestSizes.size() > 1, "every task got the same request size: " + requestSizes);
		assertTrue(lengths.size() > 1, "every task got the same length: " + lengths);
	}

	@Test
	void theRateDistributionSetsHowManyTasksARateMinuteProduces() {
		SimulationParameters.applicationList = List.of(new Application("Bursty", 2, 100, 1.0, 8_000_000, 8_000_000,
				8_000_000, 10, null, null, null, null, null, new ConstantDistribution(4), null));
		SimulationParameters.simulationDuration = 60;

		List<Task> tasks = new DefaultTaskGenerator(manager).generate().stream().collect(Collectors.toList());

		assertEquals(40, tasks.size());
		Map<ComputingNode, List<Double>> timesByDevice = new HashMap<>();
		for (Task task : tasks)
			timesByDevice.computeIfAbsent(task.getEdgeDevice(), device -> new ArrayList<>()).add(task.getTime());
		assertEquals(10, timesByDevice.size());
		for (List<Double> times : timesByDevice.values()) {
			assertEquals(4, times.size());
			Collections.sort(times);
			for (int i = 1; i < times.size(); i++)
				assertEquals(15, times.get(i) - times.get(i - 1), "tasks must be evenly spaced inside the minute");
		}
	}

	@Test
	void aRateDistributionThatDrawsZeroSkipsTheMinute() {
		SimulationParameters.applicationList = List.of(new Application("Silent", 5, 100, 1.0, 8_000_000, 8_000_000,
				8_000_000, 10, null, null, null, null, null, new ConstantDistribution(0), null));
		SimulationParameters.simulationDuration = 60;

		assertTrue(new DefaultTaskGenerator(manager).generate().isEmpty(),
				"a device with no task this minute must not generate anything");
	}

	@Test
	void anArrivalDistributionReplacesTheEvenlySpacedPattern() {
		SimulationParameters.applicationList = List.of(new Application("Poisson", 2, 100, 1.0, 8_000_000, 8_000_000,
				8_000_000, 10, null, null, null, null, null, null,
				new ExponentialDistribution(new Random(3), 10)));
		SimulationParameters.simulationDuration = 60;

		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		List<Task> tasks = taskGenerator.generate().stream().collect(Collectors.toList());

		assertFalse(tasks.isEmpty());
		Map<ComputingNode, List<Double>> timesByDevice = new HashMap<>();
		for (Task task : tasks) {
			assertTrue(task.getTime() >= 0 && task.getTime() < 60,
					"the arrival pattern went past the end of the simulation: " + task.getTime());
			timesByDevice.computeIfAbsent(task.getEdgeDevice(), device -> new ArrayList<>()).add(task.getTime());
		}
		assertFalse(timesByDevice.isEmpty(), "no device generated a task");
		// Two tasks per minute per device would give 20 tasks; the 10 second average
		// gap of the Poisson process gives around 60.
		assertTrue(tasks.size() > 30, "the arrival pattern did not drive the number of tasks: " + tasks.size());
		for (List<Double> times : timesByDevice.values()) {
			Collections.sort(times);
			assertFalse(times.isEmpty());
			for (int i = 1; i < times.size(); i++)
				assertTrue(times.get(i) - times.get(i - 1) >= 1, "two tasks cannot arrive in the same second");
		}
	}

	@Test
	void anArrivalGapBelowOneSecondDoesNotPutTheFirstTaskAtZero() {
		SimulationParameters.applicationList = List.of(new Application("Dense", 5, 100, 1.0, 8_000_000, 8_000_000,
				8_000_000, 10, null, null, null, null, null, null, new ConstantDistribution(0.2)));
		SimulationParameters.simulationDuration = 60;

		List<Task> tasks = new DefaultTaskGenerator(manager).generate().stream().collect(Collectors.toList());

		assertFalse(tasks.isEmpty());
		assertTrue(tasks.stream().allMatch(task -> task.getTime() >= 1),
				"the first task must not land at t=0");
	}

	@Test
	void theArrivalPatternNeverRunsPastTheEndOfTheSimulation() {
		SimulationParameters.applicationList = List.of(new Application("Fast", 60, 100, 1.0, 8_000_000, 8_000_000,
				8_000_000, 10, null, null, null, null, null, null,
				new ConstantDistribution(1)));
		SimulationParameters.simulationDuration = 300;

		DefaultTaskGenerator taskGenerator = new DefaultTaskGenerator(manager);
		List<Task> tasks = taskGenerator.generate().stream().collect(Collectors.toList());

		// The first task lands on the second after the initial gap, so a 300 second
		// run holds the tasks of seconds 1 to 299 on each device.
		assertEquals(10 * 299, tasks.size(), "one task per second after the initial gap on each of the ten generating devices");
		for (Task task : tasks)
			assertTrue(task.getTime() < 300, "task generated at " + task.getTime() + " s, after the end");
	}
}
