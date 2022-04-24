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

public class Task extends LatencySensitiveTask {
	private double offloadingTime;
	private ComputingNode device = ComputingNode.NULL;
	private long containerSize; // in KBytes
	private ComputingNode registry = ComputingNode.NULL;
	private int applicationID;
	private FailureReason failureReason;
	private Status status = Status.SUCCESS; 
	private long fileSize;
	private ComputingNode computingNode = ComputingNode.NULL;
	private double outputSize;
	private double length;

	public static enum FailureReason {
		FAILED_DUE_TO_LATENCY, FAILED_BECAUSE_DEVICE_DEAD, FAILED_DUE_TO_DEVICE_MOBILITY,
		NOT_GENERATED_BECAUSE_DEVICE_DEAD, FAILED_NO_RESSOURCES
	}

	public static enum Status {
		SUCCESS, FAILED
	}

	public Task(int id, long length) {
		super(id);
		this.length = length;
	}

	public void setTime(double time) {
		this.offloadingTime = time;
	}

	public double getTime() {
		return offloadingTime;
	}

	public ComputingNode getEdgeDevice() {
		return device;
	}

	public void setEdgeDevice(ComputingNode device) {
		this.device = device;
	}

	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
	}

	public long getContainerSize() {
		return containerSize;
	}

	public ComputingNode getOrchestrator() {
		return device.getOrchestrator();
	}

	public ComputingNode getRegistry() {
		return registry;
	}

	public void setRegistry(ComputingNode registry) {
		this.registry = registry;
	}

	public int getApplicationID() {
		return applicationID;
	}

	public void setApplicationID(int applicationID) {
		this.applicationID = applicationID;
	}

	public FailureReason getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(FailureReason reason) {
		this.setStatus(Task.Status.FAILED);
		this.failureReason = reason;
	}

	public ComputingNode getOffloadingDestination() {
		return computingNode;
	}

	public void setComputingNode(ComputingNode applicationPlacementLocation) {
		this.computingNode = applicationPlacementLocation;
	}

	public Task setFileSize(long requestSize) {
		this.fileSize = requestSize;
		return this;
	}

	public Task setOutputSize(long outputSize) {
		this.outputSize = outputSize;
		return this;
	}

	public double getLength() {
		return this.length;
	}

	public double getFileSize() {
		return fileSize;
	}

	public double getOutputSize() {
		return this.outputSize;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Status getStatus() {
		return status;
	}

}
