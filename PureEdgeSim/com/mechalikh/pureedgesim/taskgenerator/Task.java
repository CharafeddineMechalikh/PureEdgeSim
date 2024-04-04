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
		FAILED_DUE_TO_LATENCY, FAILED_BECAUSE_DEVICE_DEAD, FAILED_DUE_TO_DEVICE_MOBILITY,
		NOT_GENERATED_BECAUSE_DEVICE_DEAD, NO_OFFLOADING_DESTINATIONS, INSUFFICIENT_RESOURCES, INSUFFICIENT_POWER
	}

	/**
	 * Enumeration for status of a Task.
	 */
	enum Status {
		SUCCESS, FAILED
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
}
