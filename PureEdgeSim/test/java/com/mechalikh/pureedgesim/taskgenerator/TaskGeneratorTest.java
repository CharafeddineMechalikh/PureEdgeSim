package com.mechalikh.pureedgesim.taskgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;

class TaskGeneratorTest extends AbstractTest {

	private DefaultSimulationManager manager;
	private ComputingNodesGenerator generator;
	private List<ComputingNode> mistDevices;

	@BeforeEach
	void setUp() {
		SimulationParameters.applicationList = List.of(new Application("A", 1, 100, 1, 1, 1, 1, 1));
		manager = TaskTestSupport.newManager();
		generator = mock(ComputingNodesGenerator.class);
		mistDevices = new ArrayList<>();
		mistDevices.add(TaskTestSupport.device(true));
		when(generator.getMistOnlyList()).thenReturn(mistDevices);
		TaskTestSupport.installGenerator(manager, generator);
	}

	private TaskGenerator generator() {
		return new TaskGenerator(manager) {
			@Override
			public FutureQueue<Task> generate() {
				return getTaskList();
			}
		};
	}

	@Test
	void constructorCopiesTheMistDevicesAndCreatesAnEmptyQueue() {
		TaskGenerator taskGenerator = generator();
		assertNotNull(taskGenerator.getTaskList());
		assertTrue(taskGenerator.getTaskList().isEmpty());
		assertEquals(1, taskGenerator.devicesList.size());
		assertSame(mistDevices.get(0), taskGenerator.devicesList.get(0));
		assertSame(manager, taskGenerator.getSimulationManager());
	}

	@Test
	void simulationManagerCanBeReplaced() {
		TaskGenerator taskGenerator = generator();
		DefaultSimulationManager other = TaskTestSupport.newManager();
		taskGenerator.setSimulationManager(other);
		assertSame(other, taskGenerator.getSimulationManager());
	}

	@Test
	void customTaskClassIsStored() throws Exception {
		TaskGenerator taskGenerator = generator();
		assertEquals(DefaultTask.class, taskClassField(taskGenerator));

		taskGenerator.setCustomTaskClass(DefaultTask.class);
		assertEquals(DefaultTask.class, taskClassField(taskGenerator));

		Field field = TaskGenerator.class.getDeclaredField("taskClass");
		field.setAccessible(true);
		assertSame(DefaultTask.class, field.get(taskGenerator));
	}

	private Class<?> taskClassField(TaskGenerator taskGenerator) throws Exception {
		Field field = TaskGenerator.class.getDeclaredField("taskClass");
		field.setAccessible(true);
		return (Class<?>) field.get(taskGenerator);
	}

	@Test
	void abstractGenerateIsImplementedBySubclasses() {
		TaskGenerator taskGenerator = generator();
		assertSame(taskGenerator.getTaskList(), taskGenerator.generate());
	}
}
