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
package com.mechalikh.pureedgesim.network;

import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class TransferProgress {
	public enum Type {
		TASK, CONTAINER, RESULTS_TO_DEV, RESULTS_TO_ORCH, REQUEST
	}

	protected Task task;
	protected double wanNetworkUsage = 0; // seconds
	protected double manNetworkUsage = 0; // seconds
	protected double lanNetworkUsage = 0; // seconds
	protected Type transferType; 
	protected double fileSize; // in bits
	protected double remainingFileSize; // in bits
	protected double currentBandwidth = 0; // bits/s
	protected double totalBandwidths = 0; // bits/s
	protected int bwAllocationTimes = 0;
	protected List<ComputingNode> vertexList;
	protected List<NetworkLink> edgeList; 

	public TransferProgress(Task task, double fileSize, Type type) {
		this.task = task; 
		this.remainingFileSize = fileSize;
		this.fileSize = fileSize;
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
		return bwAllocationTimes > 0 ? totalBandwidths / bwAllocationTimes : 0;
	}

	public List<ComputingNode> getVertexList() {
		return vertexList;
	}

	public TransferProgress setVertexList(List<ComputingNode> vertexList) {
		this.vertexList = vertexList;
		return this;
	}

	public List<NetworkLink> getEdgeList() {
		return edgeList;
	}

	public TransferProgress setEdgeList(List<NetworkLink> edgeList) {
		this.edgeList = edgeList;
		return this;
	}

	public double getManNetworkUsage() {
		return manNetworkUsage;
	}

	public void setManNetworkUsage(double manNetworkUsage) {
		this.manNetworkUsage = manNetworkUsage;
	}

}
