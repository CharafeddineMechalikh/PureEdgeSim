package com.mechalikh.pureedgesim.taskgenerator;

public abstract class LatencySensitiveTask {

	private double maxLatency = 0;
	private double actualNetworkTime = 0;
	private double execFinishTime = 0;
	private double execStartTime = 0;
	private double arrivalTime = 0;
	private int id;

	public LatencySensitiveTask(int id) {
		this.id = id;
	}

	public double getMaxLatency() {
		return maxLatency;
	}

	public void setMaxLatency(double maxLatency) {
		this.maxLatency = maxLatency;
	}

	public double getActualNetworkTime() {
		return actualNetworkTime;
	}

	public void addActualNetworkTime(double actualNetworkTime) {
		this.actualNetworkTime += actualNetworkTime;
	}

	public double getActualCpuTime() {
		return this.execFinishTime - this.getExecStartTime();
	}

	public double getExecStartTime() {
		return execStartTime;
	}

	public double getWatingTime() {
		return this.execStartTime - this.arrivalTime;
	}

	public void setArrivalTime(double clock) {
		this.arrivalTime = clock;
	}

	public void setExecutionStartTime(double clock) {
		this.execStartTime = clock;
	}

	public void setExecutionFinishTime(double clock) {
		this.execFinishTime = clock;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
