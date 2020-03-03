package com.mechalikh.pureedgesim.Network;

import com.mechalikh.pureedgesim.TasksGenerator.Task;

public class FileTransferProgress {
	public static enum Type {
		TASK, CONTAINER, RESULTS_TO_DEV, RESULTS_TO_ORCH, REQUEST
	}

	private Task task;
	private double remainingFileSize; // in kbits
	private double wanBandwidth;// kbits/s
	private double lanBandwidth;// kbits/s
	private double wanNetworkUsage = 0; // seconds
	private double lanNetworkUsage = 0; // seconds
	private Type transferType;
	private double fileSize; // in kbits
	private double currentBandwidth; // kbits/s
	private double totalBandwidths = 0; // kbits/s
	private int bwAllocationTimes = 0;

	public FileTransferProgress(Task task, double remainingFileSize, Type type) {
		this.task = task;
		this.remainingFileSize = remainingFileSize;
		this.fileSize = remainingFileSize;
		this.transferType = type;
	}

	public double getRemainingFileSize() {
		return remainingFileSize;
	}

	public void setRemainingFileSize(double remainingFileSize) {
		this.remainingFileSize = remainingFileSize;
	}

	public Task getTask() {
		return task;
	}

	public double getWanBandwidth() {
		return wanBandwidth;
	}

	public void setWanBandwidth(double wanBandwidth) {
		this.wanBandwidth = wanBandwidth;
	}

	public double getLanBandwidth() {
		return lanBandwidth;
	}

	public void setLanBandwidth(double lanBandwidth) {
		this.lanBandwidth = lanBandwidth;
	}

	public double getWanNetworkUsage() {
		return wanNetworkUsage;
	}

	public void setWanNetworkUsage(double wanNetworkUsage) {
		this.wanNetworkUsage = wanNetworkUsage;
	}

	public double getLanNetworkUsage() {
		return lanNetworkUsage;
	}

	public void setLanNetworkUsage(double lanNetworkUsage) {
		this.lanNetworkUsage = lanNetworkUsage;
	}

	public Type getTransferType() {
		return transferType;
	}

	public double getFileSize() {
		return fileSize;
	}

	public void setCurrentBandwidth(double bandwidth) {
		this.currentBandwidth = bandwidth;

		// these values used to get the average bandwidth
		totalBandwidths += bandwidth;
		bwAllocationTimes++;
	}

	public double getCurrentBandwidth() {
		return currentBandwidth;
	}

	public double getAverageBandwidth() {
		return totalBandwidths / bwAllocationTimes;
	}

}
