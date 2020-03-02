package com.mechalikh.pureedgesim.TasksGenerator;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;

public class Task extends CloudletSimple {
	private double offloadingTime;
	private double maxLatency;
	private DataCenter device;
	private long containerSize;
	private DataCenter orchestrator;
	private double receptionTime = -1; // the time when the task, or the corresponding container has been received by
										// the offloading destination
	private DataCenter registry;
	private int applicationID;
	private Status failureReason;

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

}
