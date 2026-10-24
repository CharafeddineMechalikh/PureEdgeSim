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

/**
 * Tracks one file transfer along a path: what remains to send, how much
 * bandwidth it currently receives, and how much delay it accumulated on each
 * network type. A single instance follows the data from hop to hop until it
 * reaches its destination.
 */
public class TransferProgress {

	/** The kind of data being transferred. */
	public enum Type {
		/** A task request sent to its offloading destination. */
		TASK,
		/** An application container downloaded from the registry. */
		CONTAINER,
		/** Execution results sent back to the originating device. */
		RESULTS_TO_DEV,
		/** Execution results sent to the orchestrator. */
		RESULTS_TO_ORCH,
		/** An offloading request sent from a device to its orchestrator. */
		REQUEST
	}

	/** Task concerned by the transfer. */
	protected Task task;
	/** Accumulated WAN delay, in seconds. */
	protected double wanNetworkUsage = 0; // seconds
	/** Accumulated MAN delay, in seconds. */
	protected double manNetworkUsage = 0; // seconds
	/** Accumulated LAN delay, in seconds. */
	protected double lanNetworkUsage = 0; // seconds
	/** The kind of data being transferred. */
	protected Type transferType; 
	/** Total file size, in bits. */
	protected double fileSize; // in bits
	/** Data that has not reached the destination yet, in bits. */
	protected double remainingFileSize; // in bits
	/** Bandwidth allocated during the latest update, in bits per second. */
	protected double currentBandwidth = 0; // bits/s
	/** Sum of the bandwidth allocations, used to compute the average. */
	protected double totalBandwidths = 0; // bits/s
	/** Number of bandwidth allocations recorded. */
	protected int bwAllocationTimes = 0;
	/** Path vertices, starting with the current node; mutated at each hop. */
	protected List<ComputingNode> vertexList;
	/** Path links, starting with the next hop; mutated at each hop. */
	protected List<NetworkLink> edgeList; 

	/**
	 * Creates a transfer.
	 *
	 * @param task     the task concerned by the transfer.
	 * @param fileSize the total file size, in bits.
	 * @param type     the kind of data being transferred.
	 */
	public TransferProgress(Task task, double fileSize, Type type) {
		this.task = task; 
		this.remainingFileSize = fileSize;
		this.fileSize = fileSize;
		this.transferType = type;
	}

	/**
	 * Returns the data that has not reached the destination yet.
	 *
	 * @return the remaining size, in bits.
	 */
	public double getRemainingFileSize() {
		return remainingFileSize;
	}

	/**
	 * Sets the data that has not reached the destination yet.
	 *
	 * @param remainingFileSize the remaining size, in bits.
	 */
	public void setRemainingFileSize(double remainingFileSize) {
		this.remainingFileSize = remainingFileSize; 
	}

	/**
	 * Returns the concerned task.
	 *
	 * @return the task.
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * Returns the accumulated WAN delay.
	 *
	 * @return the WAN usage, in seconds.
	 */
	public double getWanNetworkUsage() {
		return wanNetworkUsage;
	}

	/**
	 * Sets the accumulated WAN delay.
	 *
	 * @param wanNetworkUsage the WAN usage, in seconds.
	 */
	public void setWanNetworkUsage(double wanNetworkUsage) {
		this.wanNetworkUsage = wanNetworkUsage;
	}

	/**
	 * Returns the accumulated LAN delay.
	 *
	 * @return the LAN usage, in seconds.
	 */
	public double getLanNetworkUsage() {
		return lanNetworkUsage;
	}

	/**
	 * Sets the accumulated LAN delay.
	 *
	 * @param lanNetworkUsage the LAN usage, in seconds.
	 */
	public void setLanNetworkUsage(double lanNetworkUsage) {
		this.lanNetworkUsage = lanNetworkUsage;
	}

	/**
	 * Returns the kind of data being transferred.
	 *
	 * @return the transfer type.
	 */
	public Type getTransferType() {
		return transferType;
	}

	/**
	 * Returns the total file size.
	 *
	 * @return the file size, in bits.
	 */
	public double getFileSize() {
		return fileSize;
	}

	/**
	 * Records the bandwidth allocated during the current update and adds it to the
	 * values used for the average.
	 *
	 * @param bandwidth the allocated bandwidth, in bits per second.
	 */
	public void setCurrentBandwidth(double bandwidth) {
		this.currentBandwidth = bandwidth;

		// these values used to get the average bandwidth
		totalBandwidths += bandwidth;
		bwAllocationTimes++;
	}

	/**
	 * Returns the bandwidth allocated during the latest update.
	 *
	 * @return the current bandwidth, in bits per second.
	 */
	public double getCurrentBandwidth() {
		return currentBandwidth;
	}

	/**
	 * Returns the mean bandwidth allocated during the transfer.
	 *
	 * @return the average bandwidth, in bits per second, or 0 if no allocation was
	 *         recorded.
	 */
	public double getAverageBandwidth() {
		return bwAllocationTimes > 0 ? totalBandwidths / bwAllocationTimes : 0;
	}

	/**
	 * Returns the remaining path vertices.
	 *
	 * @return the vertices, starting with the current node.
	 */
	public List<ComputingNode> getVertexList() {
		return vertexList;
	}

	/**
	 * Sets the path vertices.
	 *
	 * @param vertexList the vertices, starting with the current node.
	 * @return this transfer, for chaining.
	 */
	public TransferProgress setVertexList(List<ComputingNode> vertexList) {
		this.vertexList = vertexList;
		return this;
	}

	/**
	 * Returns the remaining path links.
	 *
	 * @return the links, starting with the next hop.
	 */
	public List<NetworkLink> getEdgeList() {
		return edgeList;
	}

	/**
	 * Sets the path links.
	 *
	 * @param edgeList the links, starting with the next hop.
	 * @return this transfer, for chaining.
	 */
	public TransferProgress setEdgeList(List<NetworkLink> edgeList) {
		this.edgeList = edgeList;
		return this;
	}

	/**
	 * Returns the accumulated MAN delay.
	 *
	 * @return the MAN usage, in seconds.
	 */
	public double getManNetworkUsage() {
		return manNetworkUsage;
	}

	/**
	 * Sets the accumulated MAN delay.
	 *
	 * @param manNetworkUsage the MAN usage, in seconds.
	 */
	public void setManNetworkUsage(double manNetworkUsage) {
		this.manNetworkUsage = manNetworkUsage;
	}

}
