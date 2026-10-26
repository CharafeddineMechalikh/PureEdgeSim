/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.simulationmanager;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationStartListener;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationvisualizer.SimulationVisualizer;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * The {@code SimulationManager} class represents the default implementation of
 * the simulation manager. It schedules the offlaoding of tasks, links the
 * different modules and manages the simulation.
 * <p>
 * When the {@link SimulationThread#startSimulation()} method is called, it
 * creates an instance of the SimulationManager, and launches the different
 * modules. Afterwards, it tells the SimulationManager to start the simulation,
 * by calling its {@link #startSimulation()} method. Once called, the simulation
 * engine will start, announcing the beginning of the simulation.
 * 
 * @see #startSimulation()
 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
 * @see com.mechalikh.pureedgesim.simulationengine.PureEdgeSim#start()
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 4.2
 */
public class DefaultSimulationManager extends SimulationManager implements OnSimulationStartListener {

	/**
	 * Simulation progress parameters.
	 **/
	/** Number of tasks already reported in the progress display. */
	protected int lastWrittenNumber = 0;
	/** Last displayed progress percentage, or -1 before the first update. */
	protected int oldProgress = -1;

	/**
	 * The number of failed tasks.
	 **/
	protected int failedTasksCount = 0;

	/**
	 * The total number of executed tasks.
	 **/
	protected int tasksCount = 0;

	/**
	 * Used to show the tasks success rate in the live chart
	 **/
	protected int sentTasks = 0;

	/**
	 * Tracks one application request: the subtasks that still have to reach a
	 * terminal state, and whether one of them failed.
	 */
	protected static class RequestRecord {
		/** Time at which the request arrived. */
		final double startTime;
		/** Number of subtasks that have not reached a terminal state yet. */
		int remaining;
		/** Whether at least one required subtask failed. */
		boolean failed;

		RequestRecord(double startTime, int taskCount) {
			this.startTime = startTime;
			this.remaining = Math.max(1, taskCount);
		}
	}

	/** Requests of the scenario, indexed by request identifier. */
	protected final Map<Integer, RequestRecord> requests = new HashMap<>();

	/**
	 * Initializes the simulation manager.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @param simLog       The simulation logger
	 * @param pureEdgeSim  The CloudSim simulation engine.
	 * @param simulationId The simulation ID
	 * @param iteration    Which simulation run
	 * @param scenario     The scenario is composed of the algorithm and
	 *                     architecture that are being used, and the number of edge
	 *                     devices.
	 */
	public DefaultSimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
	}

	/**
	 * Starts PureEdgeSim simulation engine
	 * 
	 * @see com.mechalikh.pureedgesim.simulationengine.PureEdgeSim#start()
	 */
	@Override
	public void startSimulation() {
		// Show real-time results during the simulation.
		if (SimulationParameters.displayRealTimeCharts && !SimulationParameters.parallelism_enabled)
			simulationVisualizer = new SimulationVisualizer(this);

		simLog.print("%s -  %s", getClass().getSimpleName(), scenario.toString());
		simulation.start();
	}

	/**
	 * Defines the logic to be performed by the simulation manager when the
	 * simulation starts.
	 */
	@Override
	public void onSimulationStart() {
		// Initialize logger variables.
		simLog.setGeneratedTasks(countGeneratedTasks());
		simLog.setCurrentOrchPolicy(scenario.getStringOrchArchitecture());

		simLog.print("%s - Simulation: %d  , iteration: %d", getClass().getSimpleName(), getSimulationId(),
				getIteration());

		// Schedule the tasks offloading (first batch).
		for (int i = 0; i < Math.min(taskList.size(), SimulationParameters.batchSize); i++) {
			schedule(this, taskList.first().getTime() - simulation.clock(), SEND_TO_ORCH, taskList.first());
			taskList.remove(taskList.first());
		}

		// Schedule the offlaoding of next batch
		if (taskList.size() > 0)
			schedule(this, taskList.first().getTime() - simulation.clock(), NEXT_BATCH);

		// Scheduling the end of the simulation.
		schedule(this, SimulationParameters.simulationDuration, PRINT_LOG);

		// Schedule the update of real-time charts.
		if (SimulationParameters.displayRealTimeCharts && !SimulationParameters.parallelism_enabled)
			scheduleNow(this, UPDATE_REAL_TIME_CHARTS);

		// Show simulation progress.
		scheduleNow(this, SHOW_PROGRESS);

		simLog.printSameLine("Simulation progress : [", "red");
	}

	/**
	 * Counts every task of the scenario, including the subtasks that are not in the
	 * task list because they wait for their dependencies.
	 * <p>
	 * For applications without subtasks, each task of the list has no dependent and
	 * the result is the size of the list, as before.
	 *
	 * @return the number of tasks that the simulation will process
	 */
	protected int countGeneratedTasks() {
		Set<Integer> counted = new HashSet<>();
		return taskList.stream().mapToInt(task -> countTaskAndDependents(task, counted)).sum();
	}

	/**
	 * Counts one task and its transitive dependents, each of them once.
	 *
	 * @param task    the task to count
	 * @param counted the identifiers of the tasks already counted
	 * @return the number of newly counted tasks
	 */
	protected int countTaskAndDependents(Task task, Set<Integer> counted) {
		if (!counted.add(task.getId()))
			return 0;
		int total = 1;
		for (Task dependent : task.getDependents())
			total += countTaskAndDependents(dependent, counted);
		return total;
	}

	/**
	 * Processes events or services that are available for the simulation manager.
	 * This method is invoked by the {@link PureEdgeSim} class whenever there is an
	 * event in the deferred queue, which needs to be processed by the entity.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationengine.SimEntity#processEvent(Event
	 *      ev)
	 *
	 * @param ev information about the event just happened
	 */
	@Override
	public void processEvent(Event ev) {
		Task task = (Task) ev.getData();
		switch (ev.getTag()) {
		case NEXT_BATCH:
			// Schedule this batch.
			for (int i = 0; i < Math.min(taskList.size(), SimulationParameters.batchSize); i++) {
				schedule(this, taskList.first().getTime() - simulation.clock(), SEND_TO_ORCH, taskList.first());
				taskList.remove(taskList.first());
			}
			// Schedule the offloading of next batch
			if (taskList.size() > 0)
				schedule(this, taskList.first().getTime() - simulation.clock(), NEXT_BATCH);
			break;
		case SEND_TO_ORCH:
			// Send the offloading request to the closest orchestrator.
			sendTaskToOrchestrator(task);
			sentTasks++;
			break;

		case SEND_TASK_FROM_ORCH_TO_DESTINATION:
			// The offlaoding decision was made, send the request from the orchestrator to
			// the offloading destination.
			sendFromOrchToDestination(task);
			break;

		case EXECUTE_TASK:
			// Offlaoding request received by the destination, execute the task.
			if (taskFailed(task, 2))
				return;
			task.getOffloadingDestination().submitTask(task);
			break;

		case TRANSFER_RESULTS_TO_ORCH:
			// Task execution finished, transfer the results to the orchestrator.
			finishedTasks.add(task);
			sendResultsToOchestrator(task);
			break;

		case RESULT_RETURN_FINISHED:
			// Results returned to edge device.
			if (taskFailed(task, 3))
				return;

			edgeOrchestrator.resultsReturned(task);
			tasksCount++;
			releaseDependents(task);
			requestTaskFinished(task, false);
			break;

		case SHOW_PROGRESS:
			// Calculate the simulation progress.
			int progress = 100 * tasksCount / simLog.getGeneratedTasks();
			if (oldProgress != progress) {
				oldProgress = progress;
				if (progress % 10 == 0 || (progress % 10 < 5) && lastWrittenNumber + 10 < progress) {
					lastWrittenNumber = progress - progress % 10;
					if (lastWrittenNumber != 100)
						simLog.printSameLine(" " + lastWrittenNumber + " ", "red");
				} else
					simLog.printSameLine("#", "red");
			}
			schedule(this, SimulationParameters.simulationDuration / 100, SHOW_PROGRESS);
			break;

		case UPDATE_REAL_TIME_CHARTS:
			// Update simulation Map, network utilization, and the other real-time charts.
			simulationVisualizer.updateCharts();

			// Schedule the next update.
			schedule(this, SimulationParameters.chartsUpdateInterval, UPDATE_REAL_TIME_CHARTS);
			break;

		case PRINT_LOG:

			// Whether to wait or not, if some tasks have not been executed yet.
			if (SimulationParameters.waitForAllTasksToFinish && (tasksCount / simLog.getGeneratedTasks()) < 1) {
				// 1 = 100% , 0,9= 90%
				// Some tasks may take hours to be executed that's why we don't wait until
				// all of them get executed, but we only wait for 99% of tasks to be executed at
				// least, to end the simulation. that's why we set it to " < 0.99"
				// especially when 1% doesn't affect the simulation results that much, change
				// this value to lower ( 95% or 90%) in order to make simulation faster. however
				// this may affect the results.
				schedule(this, 10, PRINT_LOG);
				break;
			}

			simLog.printSameLine(" 100% ]", "red");

			if (SimulationParameters.displayRealTimeCharts && !SimulationParameters.parallelism_enabled) {

				// Close real time charts after the end of the simulation.
				if (SimulationParameters.autoCloseRealTimeCharts)
					simulationVisualizer.close();
				try {
					// Save those charts in bitmap and vector formats.
					if (SimulationParameters.saveCharts)
						simulationVisualizer.saveCharts();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Show results and stop the simulation.
			simLog.showIterationResults(finishedTasks);

			// Terminate the simulation.
			simulation.terminate();
			break;
		default:
			simLog.print("%s - Unknown event type", this.getClass().getSimpleName());
			break;
		}

	}

	/**
	 * Returns the task execution results to the orchestrator.
	 *
	 * @param task The task that has been executed.
	 */
	protected void sendResultsToOchestrator(Task task) {
		if (taskFailed(task, 2))
			return;
		// If the task was offloaded
		if (task.getEdgeDevice() != task.getOffloadingDestination())
			scheduleNow(getNetworkModel(), NetworkModel.SEND_RESULT_TO_ORCH, task);
		else // The task has been executed locally / no offloading
			scheduleNow(this, RESULT_RETURN_FINISHED, task);

		// Update tasks execution and waiting delays
		simLog.getTasksExecutionInfos(task);
	}

	/**
	 * Sends the task from the orchestrator to the offloading destination.
	 *
	 * @param task The task that has been offlaoded.
	 */
	protected void sendFromOrchToDestination(Task task) {
		if (taskFailed(task, 1))
			return;

		// If the application is not placed yet.
		if (!task.getEdgeDevice().isApplicationPlaced()) {
			// Find the best resource node for executing the task.
			edgeOrchestrator.orchestrate(task);

			// Stop if no resource is available for this task, the offloading is failed.
			if (task.getOffloadingDestination() == ComputingNode.NULL) {

				task.setFailureReason(Task.FailureReason.NO_OFFLOADING_DESTINATIONS);
				simLog.incrementTasksFailedLackOfRessources(task);
				tasksCount++;
				failDependents(task);
				requestTaskFinished(task, true);
				return;
			}

		} else
			// The application has already been placed, so send the task directly to that
			// computing node.
			task.setOffloadingDestination(task.getEdgeDevice().getApplicationPlacementLocation());

		simLog.taskSentFromOrchToDest(task);

		// Send the task from the orchestrator to the destination
		scheduleNow(getNetworkModel(), NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION, task);

	}

	/**
	 * Sends the task to the orchestrator in order to make the offloading decision.
	 *
	 * @param task The task that needs to be offloaded.
	 */
	protected void sendTaskToOrchestrator(Task task) {
		registerRequest(task);
		if (taskFailed(task, 0))
			return;

		if (SimulationParameters.enableOrchestrators)
			task.setOrchestrator(task.getEdgeDevice().getOrchestrator());
		simLog.incrementTasksSent();

		scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_ORCH, task);
	}

	/**
	 * Used to get the task failure rate.
	 * 
	 * @return The failure rate.
	 */
	public double getFailureRate() {
		double result = ((double) failedTasksCount * 100) / Math.max(1, sentTasks);
		failedTasksCount = 0;
		sentTasks = 0;
		return result;
	}

	/**
	 * Records the request of a task the first time one of its tasks is sent.
	 * <p>
	 * Tasks that carry no request identifier, which is the case for task classes
	 * that do not extend {@code TaskAbstract}, are ignored so that a custom model
	 * keeps working without request metrics.
	 *
	 * @param task one task of the request
	 */
	protected void registerRequest(Task task) {
		if (task.getRequestId() < 0 || requests.containsKey(task.getRequestId()))
			return;
		requests.put(task.getRequestId(),
				new RequestRecord(task.getRequestStartTime(), task.getRequestTaskCount()));
		simLog.incrementGeneratedRequests();
	}

	/**
	 * Records that one task of a request reached a terminal state. When it was the
	 * last one, the request is complete: its makespan is the time elapsed since it
	 * arrived and it succeeds only if no required subtask failed.
	 *
	 * @param task   the task that just succeeded, failed, or was skipped
	 * @param failed whether the task failed a required stage
	 */
	protected void requestTaskFinished(Task task, boolean failed) {
		if (task.getRequestId() < 0)
			return;
		registerRequest(task);
		RequestRecord request = requests.get(task.getRequestId());
		if (failed)
			request.failed = true;
		if (--request.remaining > 0)
			return;
		if (request.failed) {
			simLog.incrementFailedRequests();
			return;
		}
		double makespan = simulation.clock() - request.startTime;
		simLog.requestSucceeded(makespan);
	}

	/**
	 * Sets the task as failed and provides the reason of failure.
	 *
	 * @param task  The task that has been offloaded.
	 * @param phase At which phase the task has been failed.
	 * 
	 * @return task execution status.
	 */
	public boolean taskFailed(Task task, int phase) {
		if (task.getEdgeDevice().isDead()) {
			task.setFailureReason(Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD);
			// Died before the task was sent: it is never offloaded, so it counts as
			// not generated rather than failed.
			if (phase == 0) {
				task.getEdgeDevice().setApplicationPlaced(false);
				simLog.incrementNotGeneratedBeacuseDeviceDead();
				tasksCount++;
				requestTaskFinished(task, true);
				failDependents(task);
				return true;
			}
			simLog.incrementFailedBeacauseDeviceDead(task);
			return setFailed(task, phase);
		}
		// or if the orchestrator died
		if (phase == 1 && task.getOrchestrator() != ComputingNode.NULL && task.getOrchestrator().isDead()) {
			task.setFailureReason(Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD);
			simLog.incrementFailedBeacauseDeviceDead(task);
			return setFailed(task, phase);
		}
		// or the destination device is dead
		if (phase == 2 && ((ComputingNode) task.getOffloadingDestination()).isDead()) {
			task.setFailureReason(Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD);
			simLog.incrementFailedBeacauseDeviceDead(task);
			return setFailed(task, phase);
		}
		// If storage and ram are not sufficient to perform the task
		if (phase == 2 && (task.getOffloadingDestination().getAvailableStorage() < task.getContainerSizeInMBytes()
				|| task.getOffloadingDestination().getAvailableRam() < task.getContainerSizeInMBytes())) {
			task.setFailureReason(Task.FailureReason.INSUFFICIENT_RESOURCES);
			simLog.incrementTasksFailedLackOfRessources(task);
			return setFailed(task, phase);
		}
		// A simple representation of task failure due to
		// device mobility, if the offloading destination location doesn't match
		// the edge device location (that generated this task)
		if (phase == 1 && task.getOrchestrator() != ComputingNode.NULL
				&& task.getOrchestrator().getType() != SimulationParameters.TYPES.CLOUD
				&& !sameLocation(task.getEdgeDevice(), task.getOrchestrator())) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_DEVICE_MOBILITY);
			simLog.incrementTasksFailedMobility(task);
			return setFailed(task, phase);
		}
		if (phase == 2 && task.getOffloadingDestination() != ComputingNode.NULL
				&& task.getOffloadingDestination().getType() != SimulationParameters.TYPES.CLOUD
				&& (!sameLocation(task.getEdgeDevice(), task.getOrchestrator())
						|| !sameLocation(task.getOrchestrator(), task.getOffloadingDestination()))) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_DEVICE_MOBILITY);
			simLog.incrementTasksFailedMobility(task);
			return setFailed(task, phase);
		}
		// The task is failed due to long delay
		if (phase == 3 && task.getTotalDelay() >= task.getMaxLatency()) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_LATENCY);
			simLog.incrementTasksFailedLatency(task);
			return setFailed(task, phase);
		}
		// The request missed its end-to-end deadline, even if this subtask met its own
		// latency bound measured from its release.
		if (phase == 3 && task.getRequestDeadline() > 0 && simulation.clock() >= task.getRequestDeadline()) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_LATENCY);
			simLog.incrementTasksFailedLatency(task);
			return setFailed(task, phase);
		}
		return false;
	}

	/**
	 * Sets the task as failed.
	 *
	 * @param task  The task that has been offloaded.
	 * @param phase The phase at which the failure was detected: 1 before the task
	 *              reaches the orchestrator, 2 at the offloading destination, 3
	 *              when the results are returned.
	 *
	 * @return always {@code true}, so callers can return it directly.
	 */
	protected boolean setFailed(Task task, int phase) {

		// Keep record of the failed and returned tasks
		failedTasksCount++;
		tasksCount++;

		// Since the task has been failed, its application should be placed on another
		// location next time, to avoid the failure of future tasks
		// So, let's tell the orchestrator that the application is no more placed and it
		// should find another location for future tasks.
		task.getEdgeDevice().setApplicationPlaced(false);

		// Return the execution results, only if the task has been sent to the
		// orchestrator previously.
		if (phase > 1)
			edgeOrchestrator.resultsReturned(task);
		failDependents(task);
		requestTaskFinished(task, true);
		return true;
	}

	/**
	 * Offloads the subtasks whose last dependency has just returned its results,
	 * and charges their upload with the results they receive.
	 * <p>
	 * The request size of a subtask is the data it needs besides its inputs, so
	 * every parent that completes successfully adds its output to the size of the
	 * request that brings the child its input. A task without dependents, which is
	 * every task of an application without subtasks, makes this method do nothing.
	 *
	 * @param task the task whose results were just returned
	 */
	protected void releaseDependents(Task task) {
		releaseDependents(task, true);
	}

	/**
	 * Releases the dependents of a task, optionally adding the output of that task
	 * to the request they will upload.
	 *
	 * @param task      the task that just completed or was skipped
	 * @param withResults whether the results of the task travel with the request of
	 *                    its dependents
	 */
	protected void releaseDependents(Task task, boolean withResults) {
		for (Task dependent : task.getDependents()) {
			if (isTerminal(dependent))
				continue;
			if (withResults)
				dependent.setFileSizeInBits((long) (dependent.getFileSizeInBits() + task.getOutputSizeInBits()));
			if (dependent.dependencyFinished()) {
				dependent.setTime(simulation.clock());
				scheduleNow(this, SEND_TO_ORCH, dependent);
			}
		}
	}

	/**
	 * Returns whether a task already reached a final state, either by failing or
	 * by being skipped.
	 *
	 * @param task the task to check
	 * @return {@code true} when the task is failed or skipped
	 */
	protected boolean isTerminal(Task task) {
		return task.getStatus() == Task.Status.FAILED || task.getStatus() == Task.Status.SKIPPED;
	}

	/**
	 * Skips an optional subtask whose input can never be complete, and releases its
	 * own dependents without results.
	 * <p>
	 * A skipped subtask is not a failure: the request can still succeed with the
	 * results of the other branches.
	 *
	 * @param task the optional subtask to skip
	 */
	protected void skipTask(Task task) {
		task.setStatus(Task.Status.SKIPPED);
		simLog.incrementTasksSkipped();
		tasksCount++;
		requestTaskFinished(task, false);
		releaseDependents(task, false);
	}

	/**
	 * Marks as failed the subtasks that will never receive their input because a
	 * dependency failed, and does the same for their own dependents.
	 * <p>
	 * An optional subtask is skipped instead of being failed, and the request
	 * continues without it. These tasks never leave their device, so they are
	 * counted separately from the tasks that were sent and failed during their
	 * execution.
	 *
	 * @param task the task that failed
	 */
	protected void failDependents(Task task) {
		for (Task dependent : task.getDependents()) {
			if (isTerminal(dependent))
				continue;
			if (dependent.isOptional()) {
				skipTask(dependent);
				continue;
			}
			dependent.setFailureReason(Task.FailureReason.FAILED_BECAUSE_DEPENDENCY_FAILED);
			simLog.incrementTasksFailedDependency();
			tasksCount++;
			requestTaskFinished(dependent, true);
			failDependents(dependent);
		}
	}

	/**
	 * Checks if computing nodes can communicate directly (1 hop).
	 * 
	 * @param Dev1 the first computing node.
	 * @param Dev2 the second computing node.
	 * 
	 * @return true if these computing nodes can communicate through a single hop.
	 */
	protected boolean sameLocation(ComputingNode Dev1, ComputingNode Dev2) {
		if (Dev1.getType() == TYPES.CLOUD || Dev2.getType() == TYPES.CLOUD)
			return true;
		double distance = Dev1.getMobilityModel().distanceTo(Dev2);
		int RANGE = SimulationParameters.edgeDevicesRange;
		if (Dev1.getType() != Dev2.getType()) // One of them is an edge data center and the other is an edge device
			RANGE = SimulationParameters.edgeDataCentersRange;
		return (distance < RANGE);
	}

	

}