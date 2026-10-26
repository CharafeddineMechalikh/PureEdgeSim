package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationvisualizer.SimulationVisualizer;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTask;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskgenerator.Task.FailureReason;
import com.mechalikh.pureedgesim.taskgenerator.TaskAbstract;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

class DefaultSimulationManagerTest extends AbstractTest {

	private PureEdgeSim engine;
	private SimLog simLog;
	private Scenario scenario;
	private DefaultSimulationManager manager;
	private NetworkModel networkModel;
	private Orchestrator orchestrator;

	@BeforeEach
	void setUp() {
		engine = new PureEdgeSim();
		simLog = mock(SimLog.class);
		scenario = new Scenario(10, 0, 0);
		manager = new DefaultSimulationManager(simLog, engine, 1, 1, scenario);
		networkModel = mock(NetworkModel.class);
		orchestrator = mock(Orchestrator.class);
		manager.setNetworkModel(networkModel);
		manager.setOrchestrator(orchestrator);
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.edgeDataCentersRange = 200;
	}

	private static ComputingNode node(TYPES type) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		return node;
	}

	private static MobilityModel mobility(double distance) {
		MobilityModel mobility = mock(MobilityModel.class);
		when(mobility.distanceTo(any())).thenReturn(distance);
		return mobility;
	}

	private static Task taskFor(ComputingNode edgeDevice) {
		Task task = mock(Task.class);
		when(task.getEdgeDevice()).thenReturn(edgeDevice);
		when(task.getTime()).thenReturn(0.0);
		when(task.getOrchestrator()).thenReturn(ComputingNode.NULL);
		return task;
	}

	private static Event event(int tag, Object data) {
		return new Event(null, 0.0, tag, data);
	}

	private List<Integer> tags() {
		return engine.getEventsQueue().stream().map(Event::getTag).collect(Collectors.toList());
	}

	private long countTag(int tag) {
		return engine.getEventsQueue().stream().filter(event -> event.getTag() == tag).count();
	}

	private boolean isEngineRunning() {
		try {
			java.lang.reflect.Field field = PureEdgeSim.class.getDeclaredField("isRunning");
			field.setAccessible(true);
			return field.getBoolean(engine);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	// ---------------------------------------------------------------- SEND_TO_ORCH

	@Test
	void sendToOrchSendsTheRequestToTheNetwork() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		Task task = taskFor(edge);

		manager.processEvent(event(SimulationManager.SEND_TO_ORCH, task));

		verify(simLog).incrementTasksSent();
		assertEquals(List.of(NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_ORCH), tags());
		assertSame(networkModel, engine.getEventsQueue().first().getSimEntity());
	}

	@Test
	void sendToOrchAssignsTheOrchestratorWhenOrchestratorsAreEnabled() {
		SimulationParameters.enableOrchestrators = true;
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode closestOrchestrator = node(TYPES.EDGE_DATACENTER);
		when(edge.getOrchestrator()).thenReturn(closestOrchestrator);
		Task task = taskFor(edge);

		manager.processEvent(event(SimulationManager.SEND_TO_ORCH, task));

		verify(task).setOrchestrator(closestOrchestrator);
	}

	@Test
	void aTaskOfADeadEdgeDeviceIsCountedAsNotGenerated() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		when(edge.isDead()).thenReturn(true);
		Task task = taskFor(edge);

		manager.processEvent(event(SimulationManager.SEND_TO_ORCH, task));

		verify(simLog).incrementNotGeneratedBeacuseDeviceDead();
		verify(simLog, never()).incrementFailedBeacauseDeviceDead(task);
		verify(task).setFailureReason(FailureReason.FAILED_BECAUSE_DEVICE_DEAD);
		verify(edge).setApplicationPlaced(false);
		assertTrue(engine.getEventsQueue().isEmpty());
		assertEquals(0.0, manager.getFailureRate(), 1e-9);
	}

	// ------------------------------------------- SEND_TASK_FROM_ORCH_TO_DESTINATION

	@Test
	void theOrchestratorSelectsTheDestinationAndTheTaskIsForwarded() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode destination = node(TYPES.CLOUD);
		Task task = taskFor(edge);
		when(edge.isApplicationPlaced()).thenReturn(false);
		when(task.getOffloadingDestination()).thenReturn(destination);

		manager.processEvent(event(SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task));

		verify(orchestrator).orchestrate(task);
		verify(simLog).taskSentFromOrchToDest(task);
		assertEquals(List.of(NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION), tags());
	}

	@Test
	void noOffloadingDestinationFailsTheTaskWithoutSendingIt() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		Task task = taskFor(edge);
		when(edge.isApplicationPlaced()).thenReturn(false);
		when(task.getOffloadingDestination()).thenReturn(ComputingNode.NULL);

		manager.processEvent(event(SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task));

		verify(task).setFailureReason(FailureReason.NO_OFFLOADING_DESTINATIONS);
		verify(simLog).incrementTasksFailedLackOfRessources(task);
		assertTrue(engine.getEventsQueue().isEmpty());
	}

	@Test
	void anAlreadyPlacedApplicationUsesItsPlacementLocationWithoutOrchestrating() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode placement = node(TYPES.EDGE_DEVICE);
		Task task = taskFor(edge);
		when(edge.isApplicationPlaced()).thenReturn(true);
		when(edge.getApplicationPlacementLocation()).thenReturn(placement);

		manager.processEvent(event(SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task));

		verify(task).setOffloadingDestination(placement);
		verify(orchestrator, never()).orchestrate(task);
		assertEquals(List.of(NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION), tags());
	}

	@Test
	void aDeadOrchestratorFailsTheTaskInPhaseOne() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode deadOrchestrator = node(TYPES.EDGE_DATACENTER);
		when(deadOrchestrator.isDead()).thenReturn(true);
		Task task = taskFor(edge);
		when(task.getOrchestrator()).thenReturn(deadOrchestrator);

		manager.processEvent(event(SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task));

		verify(simLog).incrementFailedBeacauseDeviceDead(task);
		verify(edge).setApplicationPlaced(false);
		verify(orchestrator, never()).resultsReturned(task);
		assertTrue(engine.getEventsQueue().isEmpty());
	}

	@Test
	void deviceMobilityFailsTheTaskWhenTheOrchestratorIsOutOfRangeInPhaseOne() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		MobilityModel edgeMobility = mobility(500);
		when(edge.getMobilityModel()).thenReturn(edgeMobility);
		ComputingNode remoteOrchestrator = node(TYPES.EDGE_DATACENTER);
		Task task = taskFor(edge);
		when(task.getOrchestrator()).thenReturn(remoteOrchestrator);

		manager.processEvent(event(SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task));

		verify(simLog).incrementTasksFailedMobility(task);
		verify(task).setFailureReason(FailureReason.FAILED_DUE_TO_DEVICE_MOBILITY);
		assertTrue(engine.getEventsQueue().isEmpty());
	}

	@Test
	void phaseOneMobilityIsIgnoredForCloudOrchestrators() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode cloud = node(TYPES.CLOUD);
		Task task = taskFor(edge);
		when(task.getOrchestrator()).thenReturn(cloud);

		manager.processEvent(event(SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task));

		verify(simLog, never()).incrementTasksFailedMobility(task);
	}

	// -------------------------------------------------------------- EXECUTE_TASK

	@Test
	void executeTaskSubmitsTheTaskToTheDestination() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode destination = node(TYPES.CLOUD);
		when(destination.getAvailableStorage()).thenReturn(1000.0);
		when(destination.getAvailableRam()).thenReturn(1000.0);
		Task task = taskFor(edge);
		when(task.getOffloadingDestination()).thenReturn(destination);
		when(task.getContainerSizeInMBytes()).thenReturn(10.0);

		manager.processEvent(event(SimulationManager.EXECUTE_TASK, task));

		verify(destination).submitTask(task);
	}

	@Test
	void executeTaskFailsWhenTheDestinationIsDead() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode destination = node(TYPES.EDGE_DEVICE);
		when(destination.isDead()).thenReturn(true);
		Task task = taskFor(edge);
		when(task.getOffloadingDestination()).thenReturn(destination);

		manager.processEvent(event(SimulationManager.EXECUTE_TASK, task));

		verify(simLog).incrementFailedBeacauseDeviceDead(task);
		verify(destination, never()).submitTask(task);
	}

	@Test
	void executeTaskFailsWhenStorageIsInsufficient() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode destination = node(TYPES.EDGE_DEVICE);
		when(destination.getAvailableStorage()).thenReturn(5.0);
		when(destination.getAvailableRam()).thenReturn(1000.0);
		Task task = taskFor(edge);
		when(task.getOffloadingDestination()).thenReturn(destination);
		when(task.getContainerSizeInMBytes()).thenReturn(10.0);

		manager.processEvent(event(SimulationManager.EXECUTE_TASK, task));

		verify(simLog).incrementTasksFailedLackOfRessources(task);
		verify(destination, never()).submitTask(task);
	}

	@Test
	void executeTaskFailsWhenRamIsInsufficient() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode destination = node(TYPES.EDGE_DEVICE);
		when(destination.getAvailableStorage()).thenReturn(1000.0);
		when(destination.getAvailableRam()).thenReturn(5.0);
		Task task = taskFor(edge);
		when(task.getOffloadingDestination()).thenReturn(destination);
		when(task.getContainerSizeInMBytes()).thenReturn(10.0);

		manager.processEvent(event(SimulationManager.EXECUTE_TASK, task));

		verify(simLog).incrementTasksFailedLackOfRessources(task);
		verify(destination, never()).submitTask(task);
	}

	@Test
	void executeTaskWithoutAnOffloadingDestinationSkipsTheMobilityCheck() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		Task task = taskFor(edge);
		when(task.getOffloadingDestination()).thenReturn(ComputingNode.NULL);

		manager.processEvent(event(SimulationManager.EXECUTE_TASK, task));

		verify(simLog, never()).incrementTasksFailedMobility(task);
	}

	@Test
	void executeTaskFailsOnMobilityWhenTheEdgeDeviceMovedAway() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		MobilityModel edgeMobility = mobility(500);
		when(edge.getMobilityModel()).thenReturn(edgeMobility);
		ComputingNode orch = node(TYPES.EDGE_DATACENTER);
		ComputingNode destination = node(TYPES.EDGE_DATACENTER);
		when(destination.getAvailableStorage()).thenReturn(1000.0);
		when(destination.getAvailableRam()).thenReturn(1000.0);
		Task task = taskFor(edge);
		when(task.getOrchestrator()).thenReturn(orch);
		when(task.getOffloadingDestination()).thenReturn(destination);
		when(task.getContainerSizeInMBytes()).thenReturn(10.0);

		manager.processEvent(event(SimulationManager.EXECUTE_TASK, task));

		verify(simLog).incrementTasksFailedMobility(task);
		verify(destination, never()).submitTask(task);
	}

	@Test
	void executeTaskFailsOnMobilityWhenTheOrchestratorMovedAwayFromTheDestination() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		MobilityModel edgeMobility = mobility(10);
		when(edge.getMobilityModel()).thenReturn(edgeMobility);
		ComputingNode orch = node(TYPES.EDGE_DATACENTER);
		MobilityModel orchMobility = mobility(500);
		when(orch.getMobilityModel()).thenReturn(orchMobility);
		ComputingNode destination = node(TYPES.EDGE_DATACENTER);
		when(destination.getAvailableStorage()).thenReturn(1000.0);
		when(destination.getAvailableRam()).thenReturn(1000.0);
		Task task = taskFor(edge);
		when(task.getOrchestrator()).thenReturn(orch);
		when(task.getOffloadingDestination()).thenReturn(destination);
		when(task.getContainerSizeInMBytes()).thenReturn(10.0);

		manager.processEvent(event(SimulationManager.EXECUTE_TASK, task));

		verify(simLog).incrementTasksFailedMobility(task);
		verify(destination, never()).submitTask(task);
	}

	// ------------------------------------------------ TRANSFER_RESULTS_TO_ORCH

	@Test
	void finishedLocalTaskReturnsItsResultDirectly() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		MobilityModel edgeMobility = mobility(0);
		when(edge.getMobilityModel()).thenReturn(edgeMobility);
		Task task = taskFor(edge);
		when(task.getOrchestrator()).thenReturn(edge);
		when(task.getOffloadingDestination()).thenReturn(edge);

		manager.processEvent(event(SimulationManager.TRANSFER_RESULTS_TO_ORCH, task));

		assertTrue(manager.getFinishedTaskList().contains(task));
		verify(simLog).getTasksExecutionInfos(task);
		assertEquals(List.of(SimulationManager.RESULT_RETURN_FINISHED), tags());
	}

	@Test
	void offloadedTaskSendsItsResultThroughTheNetwork() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode destination = node(TYPES.CLOUD);
		Task task = taskFor(edge);
		when(task.getOffloadingDestination()).thenReturn(destination);

		manager.processEvent(event(SimulationManager.TRANSFER_RESULTS_TO_ORCH, task));

		verify(simLog).getTasksExecutionInfos(task);
		assertEquals(List.of(NetworkModel.SEND_RESULT_TO_ORCH), tags());
	}

	@Test
	void aTransferFromADeadDestinationDoesNotScheduleAResult() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		ComputingNode destination = node(TYPES.EDGE_DEVICE);
		when(destination.isDead()).thenReturn(true);
		Task task = taskFor(edge);
		when(task.getOffloadingDestination()).thenReturn(destination);

		manager.processEvent(event(SimulationManager.TRANSFER_RESULTS_TO_ORCH, task));

		assertTrue(manager.getFinishedTaskList().contains(task));
		verify(simLog, never()).getTasksExecutionInfos(task);
		assertTrue(engine.getEventsQueue().isEmpty());
	}

	// -------------------------------------------------- RESULT_RETURN_FINISHED

	@Test
	void successfulResultIsReturnedToTheOrchestrator() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		Task task = taskFor(edge);
		when(task.getTotalDelay()).thenReturn(1.0);
		when(task.getMaxLatency()).thenReturn(5.0);

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, task));

		verify(orchestrator).resultsReturned(task);
		assertEquals(1, manager.tasksCount);
		assertEquals(0, manager.failedTasksCount);
	}

	@Test
	void aLateResultFailsTheTaskDueToLatency() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		Task task = taskFor(edge);
		when(task.getTotalDelay()).thenReturn(10.0);
		when(task.getMaxLatency()).thenReturn(5.0);

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, task));

		verify(simLog).incrementTasksFailedLatency(task);
		verify(orchestrator).resultsReturned(task);
		verify(edge).setApplicationPlaced(false);
		assertEquals(1, manager.failedTasksCount);
		assertEquals(100.0, manager.getFailureRate(), 1e-9);
	}

	@Test
	void aResultForADeadDeviceFailsTheTask() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		when(edge.isDead()).thenReturn(true);
		Task task = taskFor(edge);

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, task));

		verify(simLog).incrementFailedBeacauseDeviceDead(task);
		verify(orchestrator).resultsReturned(task);
	}

	// ---------------------------------------------------------------- NEXT_BATCH

	@Test
	void nextBatchSchedulesTheFollowingBatch() {
		SimulationParameters.batchSize = 2;
		FutureQueue<Task> tasks = new FutureQueue<>();
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		for (int i = 0; i < 3; i++) {
			DefaultTask task = new DefaultTask(i);
			task.setEdgeDevice(edge);
			task.setTime(i);
			tasks.add(task);
		}
		manager.setTaskList(tasks);

		manager.processEvent(event(SimulationManager.NEXT_BATCH, null));

		assertEquals(2, countTag(SimulationManager.SEND_TO_ORCH));
		assertEquals(1, countTag(SimulationManager.NEXT_BATCH));
		assertEquals(1, tasks.size());
	}

	@Test
	void theLastBatchDoesNotScheduleAnotherOne() {
		SimulationParameters.batchSize = 3;
		FutureQueue<Task> tasks = new FutureQueue<>();
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		DefaultTask task = new DefaultTask(0);
		task.setEdgeDevice(edge);
		tasks.add(task);
		manager.setTaskList(tasks);

		manager.processEvent(event(SimulationManager.NEXT_BATCH, null));

		assertEquals(1, countTag(SimulationManager.SEND_TO_ORCH));
		assertEquals(0, countTag(SimulationManager.NEXT_BATCH));
		assertTrue(tasks.isEmpty());
	}

	// ---------------------------------------------------------------- PRINT_LOG

	@Test
	void printLogWaitsWhenTasksAreStillRunning() {
		SimulationParameters.waitForAllTasksToFinish = true;
		when(simLog.getGeneratedTasks()).thenReturn(10);
		manager.tasksCount = 0;

		manager.processEvent(event(SimulationManager.PRINT_LOG, null));

		assertTrue(isEngineRunning());
		assertEquals(List.of(SimulationManager.PRINT_LOG), tags());
		assertEquals(10.0, engine.getEventsQueue().first().getTime());
	}

	@Test
	void printLogShowsResultsAndTerminatesTheSimulation() {
		SimulationParameters.waitForAllTasksToFinish = true;
		when(simLog.getGeneratedTasks()).thenReturn(10);
		manager.tasksCount = 10;

		manager.processEvent(event(SimulationManager.PRINT_LOG, null));

		verify(simLog).showIterationResults(anyList());
		assertFalse(isEngineRunning());
	}

	@Test
	void printLogClosesAndSavesChartsWhenChartsAreEnabled() throws Exception {
		SimulationParameters.displayRealTimeCharts = true;
		SimulationParameters.autoCloseRealTimeCharts = true;
		SimulationParameters.saveCharts = true;
		SimulationVisualizer visualizer = mock(SimulationVisualizer.class);
		doThrow(new IOException("cannot save")).when(visualizer).saveCharts();
		manager.simulationVisualizer = visualizer;

		SimLogCapture.mute();
		try {
			manager.processEvent(event(SimulationManager.PRINT_LOG, null));
		} finally {
			SimLogCapture.unmute();
		}

		verify(visualizer).close();
		verify(visualizer).saveCharts();
		assertFalse(isEngineRunning());
	}

	@Test
	void printLogKeepsChartsOpenWhenAutoCloseIsDisabled() throws Exception {
		SimulationParameters.displayRealTimeCharts = true;
		SimulationParameters.autoCloseRealTimeCharts = false;
		SimulationParameters.saveCharts = false;
		SimulationVisualizer visualizer = mock(SimulationVisualizer.class);
		manager.simulationVisualizer = visualizer;

		manager.processEvent(event(SimulationManager.PRINT_LOG, null));

		verify(visualizer, never()).close();
		verify(visualizer, never()).saveCharts();
		assertFalse(isEngineRunning());
	}

	// ------------------------------------------------------------ SHOW_PROGRESS

	@Test
	void showProgressPrintsTensAndReschedulesItself() {
		when(simLog.getGeneratedTasks()).thenReturn(100);
		manager.tasksCount = 0;
		manager.lastWrittenNumber = 0;
		manager.oldProgress = -1;

		manager.processEvent(event(SimulationManager.SHOW_PROGRESS, null));

		verify(simLog).printSameLine(" 0 ", "red");
		assertEquals(List.of(SimulationManager.SHOW_PROGRESS), tags());
		assertEquals(SimulationParameters.simulationDuration / 100,
				engine.getEventsQueue().first().getTime());
	}

	@Test
	void showProgressDoesNothingWhenTheProgressDidNotChange() {
		when(simLog.getGeneratedTasks()).thenReturn(100);
		manager.tasksCount = 0;
		manager.lastWrittenNumber = 0;
		manager.oldProgress = 0;

		manager.processEvent(event(SimulationManager.SHOW_PROGRESS, null));

		verify(simLog, never()).printSameLine(any(), any());
	}

	@Test
	void showProgressPrintsAHashForIrregularSteps() {
		when(simLog.getGeneratedTasks()).thenReturn(100);
		manager.tasksCount = 1;
		manager.lastWrittenNumber = 0;
		manager.oldProgress = -1;

		manager.processEvent(event(SimulationManager.SHOW_PROGRESS, null));

		verify(simLog).printSameLine("#", "red");
	}

	@Test
	void showProgressCatchesUpAfterSkippedSteps() {
		when(simLog.getGeneratedTasks()).thenReturn(100);
		manager.tasksCount = 14;
		manager.lastWrittenNumber = 0;
		manager.oldProgress = 1;

		manager.processEvent(event(SimulationManager.SHOW_PROGRESS, null));

		verify(simLog).printSameLine(" 10 ", "red");
		assertEquals(10, manager.lastWrittenNumber);
	}

	@Test
	void showProgressDoesNotPrintHundredAsAnIntermediateStep() {
		when(simLog.getGeneratedTasks()).thenReturn(100);
		manager.tasksCount = 100;
		manager.lastWrittenNumber = 0;
		manager.oldProgress = 1;

		manager.processEvent(event(SimulationManager.SHOW_PROGRESS, null));

		verify(simLog, never()).printSameLine(any(), any());
		assertEquals(100, manager.lastWrittenNumber);
	}

	// -------------------------------------------------- UPDATE_REAL_TIME_CHARTS

	@Test
	void updateRealTimeChartsUpdatesAndReschedules() {
		SimulationParameters.chartsUpdateInterval = 30;
		SimulationVisualizer visualizer = mock(SimulationVisualizer.class);
		manager.simulationVisualizer = visualizer;

		manager.processEvent(event(SimulationManager.UPDATE_REAL_TIME_CHARTS, null));

		verify(visualizer).updateCharts();
		assertEquals(List.of(SimulationManager.UPDATE_REAL_TIME_CHARTS), tags());
		assertEquals(30.0, engine.getEventsQueue().first().getTime());
	}

	@Test
	void unknownEventsAreLogged() {
		manager.processEvent(event(999, null));
		verify(simLog).print("%s - Unknown event type", "DefaultSimulationManager");
	}

	// ----------------------------------------------------------- onSimulationStart

	@Test
	void onSimulationStartSchedulesTheFirstBatchTheEndAndTheProgress() {
		SimulationParameters.batchSize = 2;
		FutureQueue<Task> tasks = new FutureQueue<>();
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		for (int i = 0; i < 3; i++) {
			DefaultTask task = new DefaultTask(i);
			task.setEdgeDevice(edge);
			tasks.add(task);
		}
		manager.setTaskList(tasks);

		manager.onSimulationStart();

		verify(simLog).setGeneratedTasks(3);
		verify(simLog).setCurrentOrchPolicy("ALL");
		assertEquals(2, countTag(SimulationManager.SEND_TO_ORCH));
		assertEquals(1, countTag(SimulationManager.NEXT_BATCH));
		assertEquals(1, countTag(SimulationManager.PRINT_LOG));
		assertEquals(1, countTag(SimulationManager.SHOW_PROGRESS));
		assertEquals(0, countTag(SimulationManager.UPDATE_REAL_TIME_CHARTS));
	}

	@Test
	void onSimulationStartOmitsTheNextBatchWhenEverythingFits() {
		SimulationParameters.batchSize = 100;
		FutureQueue<Task> tasks = new FutureQueue<>();
		DefaultTask task = new DefaultTask(0);
		task.setEdgeDevice(node(TYPES.EDGE_DEVICE));
		tasks.add(task);
		manager.setTaskList(tasks);

		manager.onSimulationStart();

		assertEquals(1, countTag(SimulationManager.SEND_TO_ORCH));
		assertEquals(0, countTag(SimulationManager.NEXT_BATCH));
	}

	@Test
	void onSimulationStartSchedulesChartUpdatesWhenChartsAreDisplayed() {
		SimulationParameters.displayRealTimeCharts = true;
		SimulationParameters.parallelism_enabled = false;
		manager.setTaskList(new FutureQueue<>());

		manager.onSimulationStart();

		assertEquals(1, countTag(SimulationManager.UPDATE_REAL_TIME_CHARTS));
	}

	@Test
	void onSimulationStartDoesNotDisplayChartsDuringParallelSimulations() {
		SimulationParameters.displayRealTimeCharts = true;
		SimulationParameters.parallelism_enabled = true;
		manager.setTaskList(new FutureQueue<>());

		manager.onSimulationStart();

		assertEquals(0, countTag(SimulationManager.UPDATE_REAL_TIME_CHARTS));
	}

	@Test
	void startSimulationRunsTheEngineUntilTheLogEventTerminatesIt() {
		SimulationParameters.simulationDuration = 1;
		SimulationParameters.displayRealTimeCharts = false;
		SimulationParameters.waitForAllTasksToFinish = false;
		manager.setTaskList(new FutureQueue<>());
		when(simLog.getGeneratedTasks()).thenReturn(1);

		manager.startSimulation();

		verify(simLog).showIterationResults(anyList());
		assertEquals(1.0, engine.clock(), 1e-9);
		assertFalse(isEngineRunning());
	}

	@Test
	void startSimulationDoesNotCreateChartsDuringParallelSimulations() {
		SimulationParameters.simulationDuration = 1;
		SimulationParameters.displayRealTimeCharts = true;
		SimulationParameters.parallelism_enabled = true;
		SimulationParameters.waitForAllTasksToFinish = false;
		manager.setTaskList(new FutureQueue<>());
		when(simLog.getGeneratedTasks()).thenReturn(1);

		manager.startSimulation();

		verify(simLog).showIterationResults(anyList());
		assertNull(manager.simulationVisualizer);
	}

	// ------------------------------------------------------------- sameLocation

	@Test
	void sameLocationTreatsCloudAndNearbyNodesAsReachable() {
		ComputingNode cloud = node(TYPES.CLOUD);
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		assertTrue(manager.sameLocation(cloud, edge));
		assertTrue(manager.sameLocation(edge, cloud));

		ComputingNode sameTypeNearby = node(TYPES.EDGE_DEVICE);
		MobilityModel nearbyMobility = mobility(10);
		when(edge.getMobilityModel()).thenReturn(nearbyMobility);
		assertTrue(manager.sameLocation(edge, sameTypeNearby));

		MobilityModel farMobility = mobility(100);
		when(edge.getMobilityModel()).thenReturn(farMobility);
		assertFalse(manager.sameLocation(edge, sameTypeNearby));

		ComputingNode edgeDatacenter = node(TYPES.EDGE_DATACENTER);
		MobilityModel edgeDatacenterMobility = mobility(100);
		when(edge.getMobilityModel()).thenReturn(edgeDatacenterMobility);
		assertTrue(manager.sameLocation(edge, edgeDatacenter));

		MobilityModel veryFarMobility = mobility(300);
		when(edge.getMobilityModel()).thenReturn(veryFarMobility);
		assertFalse(manager.sameLocation(edge, edgeDatacenter));
	}

	// ------------------------------------------------------------ service graphs

	/**
	 * Creates a real task, so that the dependency bookkeeping of {@link TaskAbstract}
	 * is exercised rather than mocked.
	 */
	private static DefaultTask graphTask(int id, ComputingNode edge, double maxLatency) {
		DefaultTask task = new DefaultTask(id);
		task.setEdgeDevice(edge);
		task.setMaxLatency(maxLatency);
		return task;
	}

	private static void dependOn(DefaultTask dependent, DefaultTask... parents) {
		for (DefaultTask parent : parents) {
			parent.addDependent(dependent);
			dependent.addDependency(parent);
		}
	}

	private boolean isQueuedForOrchestration(Task task) {
		return engine.getEventsQueue().stream()
				.anyMatch(event -> event.getTag() == SimulationManager.SEND_TO_ORCH && event.getData() == task);
	}

	@Test
	void theGeneratedCountIncludesTheSubtasksWaitingForTheirDependencies() {
		SimulationParameters.batchSize = 10;
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		DefaultTask root = graphTask(1, edge, 5);
		DefaultTask left = graphTask(2, edge, 5);
		DefaultTask right = graphTask(3, edge, 5);
		DefaultTask join = graphTask(4, edge, 5);
		dependOn(left, root);
		dependOn(right, root);
		dependOn(join, left, right);
		FutureQueue<Task> tasks = new FutureQueue<>();
		tasks.add(root);
		manager.setTaskList(tasks);

		manager.onSimulationStart();

		verify(simLog).setGeneratedTasks(4);
		// Only the root enters the task list; the others wait in the graph.
		assertEquals(1, countTag(SimulationManager.SEND_TO_ORCH));
	}

	@Test
	void aSuccessfulParentReleasesItsDependent() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		DefaultTask parent = graphTask(1, edge, 5);
		DefaultTask child = graphTask(2, edge, 5);
		dependOn(child, parent);

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, parent));

		assertEquals(1, manager.tasksCount);
		assertTrue(isQueuedForOrchestration(child), "the child must be offloaded once its parent finished");
		assertEquals(0, manager.failedTasksCount);
	}

	@Test
	void aJoinIsReleasedOnlyWhenEveryParentReturnedItsResults() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		DefaultTask firstParent = graphTask(1, edge, 5);
		DefaultTask secondParent = graphTask(2, edge, 5);
		DefaultTask join = graphTask(3, edge, 5);
		dependOn(join, firstParent, secondParent);

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, firstParent));

		assertFalse(isQueuedForOrchestration(join), "the join still waits for its second parent");

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, secondParent));

		assertTrue(isQueuedForOrchestration(join));
	}

	@Test
	void aFailedParentCascadesToItsWholeSubtree() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		// A maximum latency of zero makes the parent fail when its results return.
		DefaultTask parent = graphTask(1, edge, 0);
		DefaultTask child = graphTask(2, edge, 5);
		DefaultTask grandChild = graphTask(3, edge, 5);
		dependOn(child, parent);
		dependOn(grandChild, child);

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, parent));

		verify(simLog).incrementTasksFailedLatency(parent);
		verify(simLog, times(2)).incrementTasksFailedDependency();
		assertEquals(3, manager.tasksCount);
		// Only the parent was sent, so only it counts in the sent-task failure rate.
		assertEquals(1, manager.failedTasksCount);
		assertEquals(Task.FailureReason.FAILED_BECAUSE_DEPENDENCY_FAILED, child.getFailureReason());
		assertEquals(Task.FailureReason.FAILED_BECAUSE_DEPENDENCY_FAILED, grandChild.getFailureReason());
		assertFalse(isQueuedForOrchestration(child));
		assertFalse(isQueuedForOrchestration(grandChild));
	}

	@Test
	void aDependentIsNotCountedTwiceWhenItsSecondParentAlsoFails() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		DefaultTask firstParent = graphTask(1, edge, 0);
		DefaultTask secondParent = graphTask(2, edge, 0);
		DefaultTask join = graphTask(3, edge, 5);
		dependOn(join, firstParent, secondParent);

		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, firstParent));
		manager.processEvent(event(SimulationManager.RESULT_RETURN_FINISHED, secondParent));

		verify(simLog, times(1)).incrementTasksFailedDependency();
		assertEquals(3, manager.tasksCount);
	}

	@Test
	void aDeadDeviceAlsoFailsTheDependentsOfItsTask() {
		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		when(edge.isDead()).thenReturn(true);
		DefaultTask parent = graphTask(1, edge, 5);
		DefaultTask child = graphTask(2, edge, 5);
		dependOn(child, parent);

		manager.processEvent(event(SimulationManager.SEND_TO_ORCH, parent));

		verify(simLog).incrementNotGeneratedBeacuseDeviceDead();
		verify(simLog).incrementTasksFailedDependency();
		assertEquals(2, manager.tasksCount);
		assertEquals(Task.FailureReason.FAILED_BECAUSE_DEPENDENCY_FAILED, child.getFailureReason());
		assertTrue(engine.getEventsQueue().isEmpty());
	}

	// ------------------------------------------------------------------- getters

	@Test
	void accessorsAndSettersExposeTheWiredCollaborators() {
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		manager.setDataCentersManager(dataCentersManager);

		assertSame(simLog, manager.getSimulationLogger());
		assertSame(scenario, manager.getScenario());
		assertSame(networkModel, manager.getNetworkModel());
		assertSame(dataCentersManager, manager.getDataCentersManager());
		assertEquals(1, manager.getSimulationId());
		assertEquals(1, manager.getIteration());
		assertTrue(manager.getFinishedTaskList().isEmpty());
	}

	@Test
	void failureRateIsZeroWithoutSentTasksAndResets() {
		assertEquals(0.0, manager.getFailureRate(), 1e-9);
		assertEquals(0.0, manager.getFailureRate(), 1e-9);

		ComputingNode edge = node(TYPES.EDGE_DEVICE);
		manager.processEvent(event(SimulationManager.SEND_TO_ORCH, taskFor(edge)));
		manager.processEvent(event(SimulationManager.SEND_TO_ORCH, taskFor(edge)));

		ComputingNode deadOrchestrator = node(TYPES.EDGE_DATACENTER);
		when(deadOrchestrator.isDead()).thenReturn(true);
		Task failed = taskFor(edge);
		when(failed.getOrchestrator()).thenReturn(deadOrchestrator);
		manager.processEvent(event(SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, failed));

		assertEquals(50.0, manager.getFailureRate(), 1e-9);
	}
}
