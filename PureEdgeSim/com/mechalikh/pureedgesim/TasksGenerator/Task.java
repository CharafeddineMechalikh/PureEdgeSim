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
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.tasksgenerator;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;

public class Task extends CloudletSimple {
	private double offloadingTime;
	private double maxLatency;
	private DataCenter device;
	private long containerSize; // in KBytes
	private DataCenter orchestrator;
	private double receptionTime = -1; // the time when the task, or the corresponding container has been received by
										// the offloading destination
	private DataCenter registry;
	private int applicationID;
	private Status failureReason;
    private Object metaData;
    
	public static enum Status {
		FAILED_DUE_TO_LATENCY, FAILED_BECAUSE_DEVICE_DEAD, FAILED_DUE_TO_DEVICE_MOBILITY,
		NOT_GENERATED_BECAUSE_DEVICE_DEAD, FAILED_NO_RESSOURCES, NULL
	}

	public Task(int id, long cloudletLength, long pesNumber) {
		super(id, cloudletLength, pesNumber);
	}

	public void setTime(double time) {
		this.offloadingTime = time;
	}

	public double getTime() {
		return offloadingTime;
	}

	public double getMaxLatency() {
		return maxLatency;
	}

	public void setMaxLatency(double maxLatency) {
		this.maxLatency = maxLatency;
	}

	public DataCenter getEdgeDevice() {
		return device;
	}

	public void setEdgeDevice(DataCenter dev) {
		this.device = dev;
	}

	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
	}

	public long getContainerSize() {
		return containerSize;
	}

	public void setOrchestrator(DataCenter orch) {
		this.orchestrator = orch;
	}

	public DataCenter getOrchestrator() {
		return orchestrator;
	}

	public double getReceptionTime() {
		return receptionTime;
	}

	public void setReceptionTime(double time) {
		receptionTime = time;
	}

	public DataCenter getRegistry() {
		return registry;
	}

	public void setRegistry(DataCenter registry) {
		this.registry = registry;
	}

	public int getApplicationID() {
		return applicationID;
	}

	public void setApplicationID(int applicationID) {
		this.applicationID = applicationID;
	}

	public Status getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(Status status) {
		this.setStatus(Cloudlet.Status.FAILED);
		this.failureReason = status;
	}
	public Object getMetaData() {
		return metaData;
	}

	public void setMetaData(Object metaData) {
		this.metaData = metaData;
	}

}
