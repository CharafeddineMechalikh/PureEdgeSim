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
package com.mechalikh.pureedgesim.taskgenerator;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;

/**
 * 
 * This class represents a default task in the PureEdgeSim simulation. It
 * extends the TaskAbstract class and adds additional properties such as
 * offloading time, device, container size, application ID, failure reason,
 * status, file size, computing node, output size, type, and orchestrator. It
 * also implements several methods for setting and getting the values of these
 * properties.
 */
public class DefaultTask extends TaskAbstract {

	/**
	 * The time required for offloading the task in seconds.
	 */
	protected double offloadingTime;

	/**
	 * The edge device where the task will be offloaded.
	 */
	protected ComputingNode device = ComputingNode.NULL;

	/**
	 * The size of the container where the task is encapsulated in bits.
	 */
	protected long containerSize;

	/**
	 * The registry node where the task will be registered.
	 */
	protected ComputingNode registry = ComputingNode.NULL;

	/**
	 * The ID of the application that this task belongs to.
	 */
	protected int applicationID;

	/**
	 * The reason of failure for the task, if any.
	 */
	protected FailureReason failureReason;

	/**
	 * The status of the task.
	 */
	protected Status status = Status.SUCCESS;

	/**
	 * The size of the input file for the task in bits.
	 */
	protected long fileSize;

	/**
	 * The computing node where the task will be offloaded to.
	 */
	protected ComputingNode computingNode = ComputingNode.NULL;

	/**
	 * The size of the output file for the task in bits.
	 */
	protected double outputSize;

	/**
	 * The type of the task.
	 */
	protected String type;

	/**
	 * The orchestrator node that will manage the execution of this task.
	 */
	protected ComputingNode orchestrator = ComputingNode.NULL;

	/**
	 * Constructs a DefaultTask object with a specified task ID.
	 *
	 * @param id The ID of the task.
	 */
	public DefaultTask(int id) {
		super(id);
	}

	/**
	 * Sets the offloading time for the task.
	 *
	 * @param time The offloading time in seconds.
	 */
	@Override
	public void setTime(double time) {
		this.offloadingTime = time;
	}

	/**
	 * Returns the offloading time for the task.
	 *
	 * @return The offloading time in seconds.
	 */
	@Override
	public double getTime() {
		return offloadingTime;
	}

	/**
	 * Returns the edge device assigned to the task.
	 *
	 * @return The edge device assigned to the task.
	 */
	@Override
	public ComputingNode getEdgeDevice() {
		return device;
	}

	/**
	 * Assigns an edge device to the task.
	 *
	 * @param device The edge device to be assigned to the task.
	 */
	@Override
	public Task setEdgeDevice(ComputingNode device) {
		this.device = device;
		return this;
	}

	/**
	 * Sets the container size for the task in bits.
	 *
	 * @param containerSize The container size in bits.
	 */
	@Override
	public Task setContainerSizeInBits(long containerSize) {
		this.containerSize = containerSize;
		return this;
	}

	/**
	 * Returns the container size for the task in bits.
	 *
	 * @return The container size in bits.
	 */
	@Override
	public long getContainerSizeInBits() {
		return containerSize;
	}

	/**
	 * Returns the container size for the task in megabytes.
	 *
	 * @return The container size in megabytes.
	 */
	@Override
	public double getContainerSizeInMBytes() {
		return containerSize / 8000000.0;
	}

	/**
	 * Returns the orchestrator for the task. If no orchestrator has been set, the
	 * edge device is set as the orchestrator.
	 *
	 * @return The orchestrator for the task.
	 */
	@Override
	public ComputingNode getOrchestrator() {
		if (this.orchestrator == ComputingNode.NULL) {
			this.getEdgeDevice().setAsOrchestrator(true);
			return this.getEdgeDevice();
		}
		return this.orchestrator;
	}

	/**
	 * Assigns an orchestrator to the task.
	 *
	 * @param orchestrator The orchestrator to be assigned to the task.
	 */

	@Override
	public void setOrchestrator(ComputingNode orchestrator) {
		this.orchestrator = orchestrator;
	}

	/**
	 * 
	 * Returns the computing node registry.
	 * 
	 * @return the computing node registry.
	 */
	@Override
	public ComputingNode getRegistry() {
		return registry;
	}

	/**
	 * 
	 * Sets the computing node registry.
	 * 
	 * @param registry the computing node registry.
	 */
	@Override
	public Task setRegistry(ComputingNode registry) {
		this.registry = registry;
		return this;
	}

	/**
	 * 
	 * Returns the ID of the application.
	 * 
	 * @return the ID of the application.
	 */
	@Override
	public int getApplicationID() {
		return applicationID;
	}

	/**
	 * 
	 * Sets the ID of the application.
	 * 
	 * @param applicationID the ID of the application.
	 */
	@Override
	public Task setApplicationID(int applicationID) {
		this.applicationID = applicationID;
		return this;
	}

	/**
	 * 
	 * Returns the failure reason of the task.
	 * 
	 * @return the failure reason of the task.
	 */
	@Override
	public FailureReason getFailureReason() {
		return failureReason;
	}

	/**
	 * 
	 * Sets the failure reason of the task and updates its status to "FAILED".
	 * 
	 * @param reason the failure reason of the task.
	 */
	@Override
	public void setFailureReason(FailureReason reason) {
		this.setStatus(Task.Status.FAILED);
		this.failureReason = reason;
	}

	/**
	 * 
	 * Returns the offloading destination computing node.
	 * 
	 * @return the offloading destination computing node.
	 */
	@Override
	public ComputingNode getOffloadingDestination() {
		return computingNode;
	}

	/**
	 * 
	 * Sets the offloading destination computing node.
	 * 
	 * @param applicationPlacementLocation the offloading destination computing
	 *                                     node.
	 */
	@Override
	public void setOffloadingDestination(ComputingNode applicationPlacementLocation) {
		this.computingNode = applicationPlacementLocation;
	}

	/**
	 * 
	 * Sets the size of the file for the task and returns this task.
	 * 
	 * @param requestSize the size of the file for the task.
	 * @return this task.
	 */
	@Override
	public DefaultTask setFileSizeInBits(long requestSize) {
		this.fileSize = requestSize;
		return this;
	}

	/**
	 * 
	 * Sets the output size of the task and returns this task.
	 * 
	 * @param outputSize the output size of the task.
	 * @return this task.
	 */
	@Override
	public DefaultTask setOutputSizeInBits(long outputSize) {
		this.outputSize = outputSize;
		return this;
	}

	/**
	 * 
	 * Returns the size of the file for the task.
	 * 
	 * @return the size of the file for the task.
	 */
	@Override
	public double getFileSizeInBits() {
		return fileSize;
	}

	/**
	 * 
	 * Returns the output size of the task.
	 * 
	 * @return the output size of the task.
	 */
	@Override
	public double getOutputSizeInBits() {
		return this.outputSize;
	}

	/**
	 * 
	 * Sets the status of the task.
	 * 
	 * @param status the status of the task.
	 */
	@Override
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * 
	 * Returns the status of the task.
	 * 
	 * @return the status of the task.
	 */
	@Override
	public Status getStatus() {
		return status;
	}

	/**
	 * 
	 * Returns the type of the task.
	 * 
	 * @return the type of the task.
	 */
	@Override
	public String getType() {
		return type;
	}

	/**
	 * 
	 * Sets the type of the task.
	 * 
	 * @param type the type of the task.
	 */
	@Override
	public Task setType(String type) {
		this.type = type;
		return this;
	}

}
