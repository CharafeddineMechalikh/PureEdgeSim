package com.mechalikh.pureedgesim.TasksGenerator;

public class Application {
	private int id;
	private int rate;
	private double latency;
	private long containerSize;
	private long requestSize;
	private long resultsSize;
	private double taskLength;
	private int numberOfCores;

	public Application(int id, int rate, double latency, long containerSize, long requestSize, long resultsSize,
			double taskLength, int numberOfCores) {
		this.setId(id);
		this.rate = rate;
		this.latency = latency;
		this.containerSize = containerSize;
		this.requestSize = requestSize;
		this.resultsSize = resultsSize;
		this.taskLength = taskLength;
		this.numberOfCores = numberOfCores;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

	public double getLatency() {
		return latency;
	}

	public void setLatency(double latency) {
		this.latency = latency;
	}

	public long getContainerSize() {
		return containerSize;
	}

	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
	}

	public long getRequestSize() {
		return requestSize;
	}

	public void setRequestSize(long requestSize) {
		this.requestSize = requestSize;
	}

	public double getTaskLength() {
		return taskLength;
	}

	public void setTaskLength(double taskLength) {
		this.taskLength = taskLength;
	}

	public long getResultsSize() {
		return resultsSize;
	}

	public void setResultsSize(long resultsSize) {
		this.resultsSize = resultsSize;
	}

	public int getNumberOfCores() {
		return numberOfCores;
	}

	public void setNumberOfCores(int numberOfCores) {
		this.numberOfCores = numberOfCores;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
