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
package com.mechalikh.pureedgesim.network;

import com.mechalikh.pureedgesim.tasksgenerator.Task;

public class FileTransferProgress {
	public static enum Type {
		TASK, CONTAINER, RESULTS_TO_DEV, RESULTS_TO_ORCH, REQUEST
	}

	private Task task;
	private double wanBandwidth;// kbits/s
	private double lanBandwidth;// kbits/s 
	private double wanNetworkUsage = 0; // seconds
	private double lanNetworkUsage = 0; // seconds
	private Type transferType;
	private double fileSize; // in kbits
	private double remainingFileSize; // in kbits
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
