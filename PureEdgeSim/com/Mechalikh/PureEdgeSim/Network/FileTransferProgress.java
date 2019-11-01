package com.mechalikh.pureedgesim.Network;

import com.mechalikh.pureedgesim.TasksGenerator.Task;

public class FileTransferProgress {
	public static final int Base = 3000;
	public static final int TASK = Base + 1;
	public static final int CONTAINER = Base + 2;
	public static final int RESULTS_TO_DEV = Base + 3;
	public static final int RESULTS_TO_ORCH = Base + 4;
	public static final int REQUEST = Base + 5;

	private Task task;
	private double remainingFileSize; // in kbits
	private double wanBandwidth;// kbits/s
	private double lanBandwidth;// kbits/s
	private double wanNetworkUsage = 0; // seconds
	private double lanNetworkUsage = 0; // seconds
	private int transferType;
	private double fileSize; // in kbits
	private double currentBandwidth; // kbits/s

	public FileTransferProgress(Task task, double remainingFileSize, int transferType) {
		this.task = task;
		this.remainingFileSize = remainingFileSize;
		this.fileSize= remainingFileSize;
		this.transferType = transferType;
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

	public int getTransferType() {
		return transferType;
	}
 
	public double getFileSize() {
		return fileSize;
	}
 
	public void setCurrentBandwidth(double bandwidth) {
		this.currentBandwidth = bandwidth; 
	}

	public double getCurrentBandwidth() {
		return this.currentBandwidth; 
	}

}
