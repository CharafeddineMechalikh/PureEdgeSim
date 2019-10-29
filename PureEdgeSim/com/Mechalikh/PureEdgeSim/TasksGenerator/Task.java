package com.mechalikh.pureedgesim.TasksGenerator;

import org.cloudbus.cloudsim.cloudlets.CloudletSimple;

import com.mechalikh.pureedgesim.DataCentersManager.EdgeDataCenter;

public class Task extends CloudletSimple {
	private Status failureReason = Status.NULL;

	public static enum Status {
		FAILED_DUE_TO_LATENCY, FAILED_BECAUSE_DEVICE_DEAD, FAILED_DUE_TO_DEVICE_MOBILITY,
		NOT_GENERATED_BECAUSE_DEVICE_DEAD, FAILED_NO_RESOURCES, NULL
	}

	private double offloadingTime;
	private double maxLatency; 
	private EdgeDataCenter device;
	private long containerSize;
	private EdgeDataCenter orchestrator;

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

	public Status getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(Status status) {
		this.failureReason = status;
	}
 

	public EdgeDataCenter getEdgeDevice() {
		return device;
	}

	public void setEdgeDevice(EdgeDataCenter dev) {
		this.device = dev;
	}

	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
	}

	public long getContainerSize() {
		return containerSize;
	}

	public void setOrchestrator(EdgeDataCenter orch) {
		this.orchestrator = orch;

	}

	public EdgeDataCenter getOrchestrator() {
		return orchestrator;

	}

}
