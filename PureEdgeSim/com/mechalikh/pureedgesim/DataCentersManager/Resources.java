package com.mechalikh.pureedgesim.DataCentersManager;

public class Resources {
	private long storageMemory;
	private long availableStorageMemory;
	private double totalCpuUtilization = 0;
	private int utilizationFrequency = 0;
	private long ramMemory;
	private double currentCpuUtilization = 0;
	private boolean isIdle = true;

	public Resources(long ram, long memory) {
		setStorageMemory(memory);
		setRamMemory(ram);
	}

	public long getStorageMemory() {
		return storageMemory;
	}

	public void setStorageMemory(long storage) {
		this.storageMemory = storage;
		setAvailableMemory(storage);
	}

	public long getRam() {
		return ramMemory;
	}

	public void setRamMemory(long ram) {
		this.ramMemory = ram;
	}

	public long getAvailableMemory() {
		return availableStorageMemory;
	}

	public void setAvailableMemory(long availableMemory) {
		this.availableStorageMemory = availableMemory;
	}

	public double getAvgCpuUtilization() {
		if (utilizationFrequency == 0)
			utilizationFrequency = 1;
		return totalCpuUtilization * 100 / utilizationFrequency;
	}
	public double getTotalCpuUtilization() { 
		return totalCpuUtilization;
	}
	public double getCurrentCpuUtilization() {
		return currentCpuUtilization * 100;
	}

	public void setCurrentCpuUtilization(double cpuUtilization) {
		currentCpuUtilization = cpuUtilization;
	}

	public boolean isIdle() {
		return isIdle;
	}

	public void setIdle(boolean isIdle) {
		this.isIdle = isIdle;
	}

	public void setTotalCpuUtilization(double totalCpuUsage) {
		totalCpuUtilization = totalCpuUsage;

	}

	public void incrementUtilizationFrequency() {
		utilizationFrequency++;

	}

}
