/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General  License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General  License for more details.
 *
 *     You should have received a copy of the GNU General  License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 *     @since PureEdgeSim 5.0
 **/
package com.mechalikh.pureedgesim.taskgenerator;

import java.util.Collections;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationengine.QueueElement;

/**
 * The Task interface represents a unit of work that can be executed in the
 * system. It defines the methods to get and set various properties of a task
 * such as its status, execution time, resource requirements, and failure
 * reasons. Task interface extends the QueueElement interface, which defines the
 * methods for enqueuing and dequeuing a task in a queue.
 * 
 * @author Charafeddine Mechalikh
 */
public interface Task extends QueueElement {

	/**
	 * Enumeration for failure reasons of a Task.
	 */
	enum FailureReason {
		/** The task could not finish within its maximum latency. */
		FAILED_DUE_TO_LATENCY,
		/** The source or destination device ran out of battery during the task. */
		FAILED_BECAUSE_DEVICE_DEAD,
		/** The source device moved out of range while the task was being processed. */
		FAILED_DUE_TO_DEVICE_MOBILITY,
		/** The task was never generated because its device was already dead. */
		NOT_GENERATED_BECAUSE_DEVICE_DEAD,
		/** The orchestrator found no destination allowed by the architecture. */
		NO_OFFLOADING_DESTINATIONS,
		/** The destination lacked the CPU, memory, or storage required by the task. */
		INSUFFICIENT_RESOURCES,
		/** The destination did not have enough battery energy for the task. */
		INSUFFICIENT_POWER,
		/** A subtask this one depends on failed, so it could never be offloaded. */
		FAILED_BECAUSE_DEPENDENCY_FAILED
	}

	/**
	 * Enumeration for status of a Task.
	 */
	enum Status {
		/** The task completed within its latency requirement. */
		SUCCESS,
		/** The task failed; see {@link #getFailureReason()}. */
		FAILED,
		/**
		 * The task was never offloaded because a dependency failed and it is declared
		 * optional, so its absence does not fail the request.
		 */
		SKIPPED
	}

	/**
	 * Returns the maximum latency of the Task.
	 * 
	 * @return the maximum latency of the Task
	 */
	double getMaxLatency();

	/**
	 * Sets the maximum latency of the Task.
	 * 
	 * @param maxLatency the maximum latency of the Task
	 * @return the updated Task
	 */
	Task setMaxLatency(double maxLatency);

	/**
	 * Returns the actual network time of the Task.
	 * 
	 * @return the actual network time of the Task
	 */
	double getActualNetworkTime();

	/**
	 * Adds the actual network time of the Task.
	 * 
	 * @param actualNetworkTime the actual network time of the Task
	 */
	void addActualNetworkTime(double actualNetworkTime);

	/**
	 * Returns the actual CPU time of the Task.
	 * 
	 * @return the actual CPU time of the Task
	 */
	double getActualCpuTime();

	/**
	 * Returns the execution start time of the Task.
	 * 
	 * @return the execution start time of the Task
	 */
	double getExecStartTime();

	/**
	 * Returns the waiting time of the Task.
	 * 
	 * @return the waiting time of the Task
	 */
	double getWatingTime();

	/**
	 * Sets the arrival time of the Task.
	 * 
	 * @param clock the arrival time of the Task
	 */
	void setArrivalTime(double clock);

	/**
	 * Sets the execution start time of the Task.
	 * 
	 * @param clock the execution start time of the Task
	 */
	void setExecutionStartTime(double clock);

	/**
	 * Sets the execution finish time of the Task.
	 * 
	 * @param clock the execution finish time of the Task
	 */
	void setExecutionFinishTime(double clock);

	/**
	 * Sets the ID of the Task.
	 * 
	 * @param id the ID of the Task
	 */
	void setId(int id);

	/**
	 * Returns the ID of the Task.
	 * 
	 * @return the ID of the Task
	 */
	int getId();

	/**
	 * 
	 * Sets the time of the task.
	 * 
	 * @param time the time to set
	 */
	void setTime(double time);

	/**
	 * 
	 * Gets the time of the task.
	 * 
	 * @return the time of the task
	 */
	double getTime();

	/**
	 * 
	 * Gets the edge device associated with the task.
	 * 
	 * @return the edge device associated with the task
	 */
	ComputingNode getEdgeDevice();

	/**
	 * 
	 * Sets the edge device associated with the task.
	 * 
	 * @param device the edge device to set
	 * @return the updated Task object
	 */
	Task setEdgeDevice(ComputingNode device);

	/**
	 * 
	 * Sets the container size of the task in bits.
	 * 
	 * @param containerSize the container size to set in bits
	 * @return the updated Task object
	 */
	Task setContainerSizeInBits(long containerSize);

	/**
	 * 
	 * Gets the container size of the task in bits.
	 * 
	 * @return the container size of the task in bits
	 */
	long getContainerSizeInBits();

	/**
	 * 
	 * Gets the container size of the task in megabytes.
	 * 
	 * @return the container size of the task in megabytes
	 */
	double getContainerSizeInMBytes();

	/**
	 * 
	 * Gets the orchestrator associated with the task.
	 * 
	 * @return the orchestrator associated with the task
	 */
	ComputingNode getOrchestrator();

	/**
	 * 
	 * Gets the registry associated with the task.
	 * 
	 * @return the registry associated with the task
	 */
	ComputingNode getRegistry();

	/**
	 * 
	 * Sets the registry associated with the task.
	 * 
	 * @param registry the registry to set
	 * @return the updated Task object
	 */
	Task setRegistry(ComputingNode registry);

	/**
	 * 
	 * Gets the ID of the application associated with the task.
	 * 
	 * @return the ID of the application associated with the task
	 */
	int getApplicationID();

	/**
	 * 
	 * Sets the ID of the application associated with the task.
	 * 
	 * @param applicationID the ID of the application to set
	 * @return the updated Task object
	 */
	Task setApplicationID(int applicationID);

	/**
	 * 
	 * Gets the reason for task failure.
	 * 
	 * @return the reason for task failure
	 */
	FailureReason getFailureReason();

	/**
	 * 
	 * Sets the reason for task failure.
	 * 
	 * @param reason the reason for task failure to set
	 */
	void setFailureReason(FailureReason reason);

	/**
	 * 
	 * Gets the offloading destination associated with the task.
	 * 
	 * @return the offloading destination associated with the task
	 */
	ComputingNode getOffloadingDestination();

	/**
	 * 
	 * Sets the offloading destination associated with the task.
	 * 
	 * @param applicationPlacementLocation the offloading destination to set
	 */
	void setOffloadingDestination(ComputingNode applicationPlacementLocation);

	/**
	 * 
	 * Sets the file size of the task request in bits.
	 * 
	 * @param requestSize the file size of the task request to set in bits
	 * @return the updated Task object
	 */
	Task setFileSizeInBits(long requestSize);

	/**
	 * 
	 * Sets the output size of the task in bits.
	 * 
	 * @param outputSize the output size of the task to set in bits
	 * @return the updated Task object
	 */
	Task setOutputSizeInBits(long outputSize);

	/**
	 * 
	 * Gets the length of the task.
	 * 
	 * @return the length of the task
	 */
	double getLength();

	/**
	 * 
	 * Gets the file size of the task request in bits.
	 * 
	 * @return the file size of the task request in bits
	 */
	double getFileSizeInBits();

	/**
	 * 
	 * Gets the output size of the task in bits.
	 * 
	 * @return the output size of the task in bits
	 */
	double getOutputSizeInBits();

	/**
	 * 
	 * Sets the status of the task.
	 * 
	 * @param status the status of the task
	 */
	void setStatus(Status status);

	/**
	 * 
	 * Gets the status of the task.
	 * 
	 * @return the status of the task
	 */
	Status getStatus();

	/**
	 * 
	 * Gets the type of the task.
	 * 
	 * @return the type of the task
	 */
	String getType();

	/**
	 * 
	 * Sets the type of the task.
	 * 
	 * @param type the type of the task
	 * @return the task with the updated type
	 */
	Task setType(String type);

	/**
	 * 
	 * Sets the length of the task.
	 * 
	 * @param length the length of the task
	 * @return the task with the updated length
	 */
	Task setLength(double length);

	/**
	 * 
	 * Sets the orchestrator node of the task.
	 * 
	 * @param orchestrator the orchestrator node of the task
	 */
	void setOrchestrator(ComputingNode orchestrator);

	/**
	 * 
	 * Gets the total delay of the task.
	 * 
	 * @return the total delay of the task
	 */
	double getTotalDelay();

	/**
	 * 
	 * Sets the serial number of the task.
	 * 
	 * @param l the serial number of the task
	 */
	void setSerial(long l);

	/**
	 * 
	 * Gets the serial number of the task.
	 * 
	 * @return the serial number of the task
	 */
	long getSerial();

	/**
	 * Returns the tasks that must complete before this one can be offloaded.
	 * <p>
	 * A task that is not part of a service graph has no dependency, so the default
	 * implementation returns an empty list and the simulation manager offloads it
	 * as soon as its arrival time is reached.
	 *
	 * @return the tasks this one depends on, empty for an independent task
	 */
	default List<Task> getDependencies() {
		return Collections.emptyList();
	}

	/**
	 * Returns the tasks that become eligible for offloading when this one
	 * completes successfully.
	 * <p>
	 * A task that is not part of a service graph has no dependent, so the default
	 * implementation returns an empty list.
	 *
	 * @return the tasks waiting for this one, empty for an independent task
	 */
	default List<Task> getDependents() {
		return Collections.emptyList();
	}

	/**
	 * Records that one of the dependencies of this task has completed and tells
	 * whether the task can now be offloaded.
	 * <p>
	 * The default implementation returns {@code true} because an independent task
	 * is always ready. Implementations that support service graphs decrement their
	 * remaining dependency count and return {@code true} only once it reaches zero.
	 *
	 * @return {@code true} when all the dependencies have completed
	 */
	default boolean dependencyFinished() {
		return true;
	}

	/**
	 * Registers a task that must complete before this one can be offloaded.
	 * <p>
	 * The default implementation rejects the call: a task class that does not
	 * extend {@link TaskAbstract} cannot be used with a service graph, while it
	 * works unchanged when applications declare no subtask.
	 *
	 * @param dependency the task that must complete first
	 */
	default void addDependency(Task dependency) {
		throw new UnsupportedOperationException("The task class " + getClass().getName()
				+ " does not support service graphs; extend TaskAbstract or implement addDependency()");
	}

	/**
	 * Registers a task that becomes eligible for offloading when this one completes
	 * successfully.
	 * <p>
	 * The default implementation rejects the call, for the same reason as
	 * {@link #addDependency(Task)}.
	 *
	 * @param dependent the task waiting for this one
	 */
	default void addDependent(Task dependent) {
		throw new UnsupportedOperationException("The task class " + getClass().getName()
				+ " does not support service graphs; extend TaskAbstract or implement addDependent()");
	}

	/**
	 * Returns the name of the subtask this task instantiates, or {@code null} when
	 * the task is not part of a service graph.
	 *
	 * @return the subtask name declared in the applications file, or {@code null}
	 */
	default String getSubtaskName() {
		return null;
	}

	/**
	 * Sets the name of the subtask this task instantiates.
	 * <p>
	 * The default implementation rejects the call: a task class that does not
	 * extend {@link TaskAbstract} cannot be used with a service graph, while it
	 * works unchanged when applications declare no subtask.
	 *
	 * @param name the subtask name declared in the applications file
	 */
	default void setSubtaskName(String name) {
		throw new UnsupportedOperationException("The task class " + getClass().getName()
				+ " does not support service graphs; extend TaskAbstract or implement setSubtaskName()");
	}

	/**
	 * Returns the identifier of the application request this task belongs to.
	 * <p>
	 * A request is one invocation of an application: a single task when the
	 * application is monolithic, and the whole subtask graph when the application
	 * declares subtasks. All the tasks of one request share the identifier.
	 *
	 * @return the request identifier, or {@code -1} when the task carries none
	 */
	default int getRequestId() {
		return -1;
	}

	/**
	 * Returns the number of tasks that make up the request of this task.
	 *
	 * @return the size of the request, {@code 1} for an independent task
	 */
	default int getRequestTaskCount() {
		return 1;
	}

	/**
	 * Returns the time at which the request of this task arrived.
	 *
	 * @return the request start time in seconds, or {@code 0} when unknown
	 */
	default double getRequestStartTime() {
		return 0;
	}

	/**
	 * Returns the end-to-end deadline of the request of this task, as an absolute
	 * simulation time.
	 * <p>
	 * The request deadline is sampled once per request from the latency
	 * distribution of the application and is shared by every subtask, unlike
	 * {@link #getMaxLatency()} which bounds each subtask from its own release time.
	 * A value of {@code 0} disables the end-to-end check.
	 *
	 * @return the absolute deadline in seconds, or {@code 0} when there is none
	 */
	default double getRequestDeadline() {
		return 0;
	}

	/**
	 * Sets the request this task belongs to.
	 * <p>
	 * The default implementation rejects the call, for the same reason as
	 * {@link #addDependency(Task)}.
	 *
	 * @param requestId      the identifier shared by the tasks of the request
	 * @param requestTaskCount the number of tasks in the request
	 * @param startTime      the time at which the request arrived
	 * @param deadline       the absolute end-to-end deadline, {@code 0} for none
	 */
	default void setRequest(int requestId, int requestTaskCount, double startTime, double deadline) {
		throw new UnsupportedOperationException("The task class " + getClass().getName()
				+ " does not support service graphs; extend TaskAbstract or implement setRequest()");
	}

	/**
	 * Returns whether this subtask may be skipped when one of its dependencies
	 * fails, instead of failing.
	 *
	 * @return {@code true} when the subtask is optional
	 */
	default boolean isOptional() {
		return false;
	}

	/**
	 * Marks this task as an optional subtask.
	 *
	 * @param optional whether a failed dependency skips this task instead of failing it
	 */
	default void setOptional(boolean optional) {
	}
}
