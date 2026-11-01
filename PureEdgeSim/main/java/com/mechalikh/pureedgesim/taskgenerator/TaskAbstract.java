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
 *     @since PureEdgeSim 6.1
 **/
package com.mechalikh.pureedgesim.taskgenerator;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.simulationengine.AbstractQueueElement;

/**
 * 
 * This abstract class implements the {@link Task} interface and provides common
 * methods and fields for all types of tasks. The ordering of tasks inside a
 * queue, by offloading time and serial number, is inherited from
 * {@link AbstractQueueElement}, with {@link #getTime()} as the time key.
 * 
 * <p>
 * The fields include the maximum allowed latency, actual network time,
 * execution finish time, execution start time, arrival time, length, and ID.
 * <p>
 * The methods include getter and setter methods for all fields, as well as
 * methods to add actual network time, get actual CPU time, get waiting time,
 * set arrival time,get total delay, set execution start time, set execution
 * finish time, and get the hash code.
 * <p>
 * This class is abstract, meaning it cannot be instantiated directly but must
 * be extended by concrete task classes.
 * 
 * @author Charafeddine Mechalikh
 * 
 * @see Task
 * 
 * @see AbstractQueueElement
 */
public abstract class TaskAbstract extends AbstractQueueElement<Task> implements Task {

	/**
	 * 
	 * The maximum latency that this task can tolerate
	 */
	protected double maxLatency = 0;
	/**
	 * 
	 * The actual network time this task experiences
	 */
	protected double actualNetworkTime = 0;
	/**
	 * 
	 * The execution finish time of this task
	 */
	protected double execFinishTime = 0;
	/**
	 * 
	 * The execution start time of this task
	 */
	protected double execStartTime = 0;
	/**
	 * 
	 * The arrival time of this task
	 */
	protected double arrivalTime = 0;
	/**
	 * 
	 * The length of this task
	 */
	protected double length = 0;
	/**
	 * 
	 * The unique identifier of this task
	 */
	protected int id;

	/**
	 * The tasks that must complete before this one is offloaded; empty when the
	 * task is not part of a service graph.
	 */
	protected final List<Task> dependencies = new ArrayList<>();

	/**
	 * The tasks that become eligible for offloading when this one completes.
	 */
	protected final List<Task> dependents = new ArrayList<>();

	/**
	 * Number of dependencies that have not completed yet.
	 */
	protected int remainingDependencies = 0;

	/**
	 * Whether every dependency of this task has already completed.
	 */
	protected boolean dependenciesCompleted = false;

	/**
	 * Name of the subtask this task instantiates, or {@code null} when the task is
	 * not part of a service graph.
	 */
	protected String subtaskName;

	/** Identifier of the application request this task belongs to. */
	protected int requestId = -1;

	/** Number of tasks that make up the request of this task. */
	protected int requestTaskCount = 1;

	/** Time at which the request of this task arrived, in seconds. */
	protected double requestStartTime;

	/** Absolute end-to-end deadline of the request, or {@code 0} when there is none. */
	protected double requestDeadline;

	/** Whether a failed dependency skips this subtask instead of failing it. */
	protected boolean optional;

	/**
	 * 
	 * Constructs a new TaskAbstract instance with the given ID.
	 * 
	 * @param id the task ID to set
	 */
	protected TaskAbstract(int id) {
		this.setId(id);
	}

	/**
	 * 
	 * Gets the maximum allowed latency of the task.
	 * 
	 * @return the maximum allowed latency
	 */
	@Override
	public double getMaxLatency() {
		return maxLatency;
	}

	/**
	 * 
	 * Sets the maximum allowed latency of the task and returns the modified task.
	 * 
	 * @param maxLatency the maximum allowed latency to set
	 * @return the modified task
	 */
	@Override
	public Task setMaxLatency(double maxLatency) {
		this.maxLatency = maxLatency;
		return this;
	}

	/**
	 * 
	 * Gets the actual network time of the task.
	 * 
	 * @return the actual network time
	 */
	@Override
	public double getActualNetworkTime() {
		return actualNetworkTime;
	}

	/**
	 * 
	 * Adds the given actual network time to the existing actual network time of the
	 * task.
	 * 
	 * @param actualNetworkTime the actual network time to add
	 */
	@Override
	public void addActualNetworkTime(double actualNetworkTime) {
		this.actualNetworkTime += actualNetworkTime;
	}

	/**
	 * 
	 * Gets the actual CPU time of the task.
	 * 
	 * @return the actual CPU time
	 */
	@Override
	public double getActualCpuTime() {
		return this.execFinishTime - this.getExecStartTime();
	}

	/**
	 * 
	 * Gets the execution start time of the task.
	 * 
	 * @return the execution start time
	 */
	@Override
	public double getExecStartTime() {
		return execStartTime;
	}

	/**
	 * 
	 * Gets the waiting time of the task.
	 * 
	 * @return the waiting time
	 */
	@Override
	public double getWatingTime() {
		return this.execStartTime - this.arrivalTime;
	}

	/**
	 * 
	 * Sets the arrival time of the task to the given clock value.
	 * 
	 * @param clock the clock value to set
	 */
	@Override
	public void setArrivalTime(double clock) {
		this.arrivalTime = clock;
		this.execStartTime = clock;
	}

	/**
	 * 
	 * Returns the total delay of this task, which is the sum of the actual network
	 * time, waiting time, and actual CPU time.
	 * 
	 * @return the total delay of this task
	 */
	@Override
	public double getTotalDelay() {
		return this.getActualNetworkTime() + this.getWatingTime() + this.getActualCpuTime();
	}

	/**
	 * 
	 * Sets the start time of the execution of this task to the specified clock
	 * value.
	 * 
	 * @param clock the clock value to set as the execution start time
	 */
	@Override
	public void setExecutionStartTime(double clock) {
		this.execStartTime = clock;
		this.execFinishTime = clock;
	}

	/**
	 * 
	 * Sets the finish time of the execution of this task to the specified clock
	 * value.
	 * 
	 * @param clock the clock value to set as the execution finish time
	 */
	@Override
	public void setExecutionFinishTime(double clock) {
		this.execFinishTime = clock;
	}

	/**
	 * 
	 * Sets the ID of this task to the specified value.
	 * 
	 * @param id the ID to set for this task
	 */
	@Override
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 
	 * Returns the ID of this task.
	 * 
	 * @return the ID of this task
	 */
	@Override
	public int getId() {
		return id;
	}

	/**
	 * 
	 * Returns the length of this task.
	 * 
	 * @return the length of this task
	 */
	@Override
	public double getLength() {
		return length;
	}

	/**
	 * 
	 * Sets the length of this task to the specified value.
	 * 
	 * @param length the length to set for this task
	 * @return a reference to this task
	 */
	@Override
	public Task setLength(double length) {
		this.length = length;
		return this;
	}

	/**
	 * Returns the tasks that must complete before this one is offloaded.
	 *
	 * @return the dependencies of this task, empty for an independent task
	 */
	@Override
	public List<Task> getDependencies() {
		return dependencies;
	}

	/**
	 * Returns the tasks that become eligible for offloading when this one
	 * completes.
	 *
	 * @return the tasks waiting for this one, empty for an independent task
	 */
	@Override
	public List<Task> getDependents() {
		return dependents;
	}

	/**
	 * Registers a task that must complete before this one is offloaded.
	 *
	 * @param dependency the task that must complete first
	 */
	@Override
	public void addDependency(Task dependency) {
		dependencies.add(dependency);
		remainingDependencies++;
	}

	/**
	 * Registers a task that becomes eligible for offloading when this one
	 * completes.
	 *
	 * @param dependent the task waiting for this one
	 */
	@Override
	public void addDependent(Task dependent) {
		dependents.add(dependent);
	}

	/**
	 * Records that one of the dependencies of this task has completed.
	 * <p>
	 * The call is idempotent: once every dependency has completed, later calls
	 * return {@code false}, so a task is never released twice.
	 *
	 * @return {@code true} when all the dependencies have completed
	 */
	@Override
	public boolean dependencyFinished() {
		if (dependenciesCompleted)
			return false;
		remainingDependencies--;
		if (remainingDependencies > 0)
			return false;
		dependenciesCompleted = true;
		return true;
	}

	/**
	 * Returns the name of the subtask this task instantiates.
	 *
	 * @return the subtask name, or {@code null} for an independent task
	 */
	@Override
	public String getSubtaskName() {
		return subtaskName;
	}

	/**
	 * Sets the name of the subtask this task instantiates.
	 *
	 * @param name the subtask name declared in the applications file
	 */
	@Override
	public void setSubtaskName(String name) {
		this.subtaskName = name;
	}

	/**
	 * Returns the identifier of the request this task belongs to.
	 *
	 * @return the request identifier, or {@code -1} when the task carries none
	 */
	@Override
	public int getRequestId() {
		return requestId;
	}

	/**
	 * Returns the number of tasks that make up the request of this task.
	 *
	 * @return the number of tasks in the request
	 */
	@Override
	public int getRequestTaskCount() {
		return requestTaskCount;
	}

	/**
	 * Returns the time at which the request of this task arrived.
	 *
	 * @return the request start time in seconds
	 */
	@Override
	public double getRequestStartTime() {
		return requestStartTime;
	}

	/**
	 * Returns the absolute end-to-end deadline of the request.
	 *
	 * @return the deadline in seconds, or {@code 0} when there is none
	 */
	@Override
	public double getRequestDeadline() {
		return requestDeadline;
	}

	/**
	 * Sets the request this task belongs to.
	 *
	 * @param requestId        the identifier shared by the tasks of the request
	 * @param requestTaskCount the number of tasks in the request
	 * @param startTime        the time at which the request arrived
	 * @param deadline         the absolute end-to-end deadline, {@code 0} for none
	 */
	@Override
	public void setRequest(int requestId, int requestTaskCount, double startTime, double deadline) {
		this.requestId = requestId;
		this.requestTaskCount = requestTaskCount;
		this.requestStartTime = startTime;
		this.requestDeadline = deadline;
	}

	/**
	 * Returns whether this subtask may be skipped when a dependency fails.
	 *
	 * @return {@code true} when the subtask is optional
	 */
	@Override
	public boolean isOptional() {
		return optional;
	}

	/**
	 * Marks this task as an optional subtask.
	 *
	 * @param optional whether a failed dependency skips this task instead of failing it
	 */
	@Override
	public void setOptional(boolean optional) {
		this.optional = optional;
	}
}
