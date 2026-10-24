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

import java.util.Objects;

/**
 * 
 * This abstract class implements the {@link Task} interface and provides common
 * methods and fields for all types of tasks. It also implements the Comparable
 * interface to provide an ordering of tasks based on their offloading time and
 * serial number.
 * 
 * <p>
 * The fields include the maximum allowed latency, actual network time,
 * execution finish time, execution start time, arrival time, length, ID, and
 * serial.
 * <p>
 * The methods include getter and setter methods for all fields, as well as
 * methods to add actual network time, get actual CPU time, get waiting time,
 * set arrival time,get total delay, set execution start time, set execution
 * finish time, compare tasks, check equality, and get the hash code.
 * <p>
 * This class is abstract, meaning it cannot be instantiated directly but must
 * be extended by concrete task classes.
 * 
 * @author Charafeddine Mechalikh
 * 
 * @see Task
 * 
 * @see Comparable
 */
public abstract class TaskAbstract implements Task, Comparable<Task> {

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
	 * 
	 * The serial number of this task
	 */
	protected long serial;

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
	 * 
	 * Sets the serial number of this task to the specified value.
	 * 
	 * @param l the serial number to set for this task
	 */
	public void setSerial(long l) {
		this.serial = l;
	}

	/**
	 * 
	 * Compares this task with the specified task for order. Returns a negative
	 * integer, zero, or a positive integer as this task is less than, equal to, or
	 * greater than the specified task.
	 * 
	 * @param that the task to be compared
	 * 
	 * @return a negative integer, zero, or a positive integer as this task is less
	 *         than, equal to, or greater than the specified task
	 */
	@Override
	public int compareTo(final Task that) {
		if (that.equals(null)) {
			return 1;
		}

		if (this.equals(that)) {
			return 0;
		}

		int res = Double.compare(this.getTime(), that.getTime());
		if (res != 0) {
			return res;
		}

		return Long.compare(serial, that.getSerial());
	}

	/**
	 * 
	 * Indicates whether some other object is "equal to" this one.
	 * 
	 * @param obj the object to compare to
	 * @return true if this object is the same as the obj argument; false otherwise
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final Task that = (Task) obj;
		return Double.compare(that.getTime(), getTime()) == 0 && getSerial() == that.getSerial();
	}

	/**
	 * 
	 * Returns a hash code value for the object. The hash code is generated based on
	 * the time and serial number of the task.
	 * 
	 * @return the hash code value for the object.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getTime(), getSerial());
	}

	/**
	 * 
	 * Returns the serial number of the task.
	 * 
	 * @return the serial number of the task.
	 */
	public long getSerial() {
		return this.serial;
	}
}
