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

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Link between two compute nodes in the infrastructure graph
 */
public class NetworkLink extends SimEntity {
	public static final int UPDATE_PROGRESS = 1;
	private double latency = 0;
	private double bandwidth = 0;
	private List<TransferProgress> transferProgressList = new ArrayList<>();
	private ComputingNode src = ComputingNode.NULL;
	private ComputingNode dst = ComputingNode.NULL;
	private SimulationManager simulationManager;
	private double usedBandwidth = 0;
	private double totalTrasferredData = 0;
	private EnergyModelNetworkLink energyModel = EnergyModelNetworkLink.NULL;

	public static enum NetworkLinkTypes {
		WAN, MAN, LAN, IGNORE
	};

	private NetworkLinkTypes type;

	public static NetworkLink NULL = new NetworkLinkNull();

	public NetworkLink(ComputingNode src, ComputingNode dst, SimulationManager simulationManager,
			NetworkLinkTypes type) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
		this.src = src;
		this.dst = dst;
		this.setType(type);
	}

	public NetworkLink() {
	}

	public double getLatency() {
		return latency;
	}

	public NetworkLink setLatency(double latency) {
		this.latency = latency;
		return this;
	}

	public NetworkLink setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
		return this;
	}

	@Override
	public void startInternal() {
		scheduleNow(this, UPDATE_PROGRESS);
	}

	public ComputingNode getSrc() {
		return src;
	}

	public void setSrc(ComputingNode src) {
		this.src = src;
	}

	public ComputingNode getDst() {
		return dst;
	}

	public void setDst(ComputingNode node) {
		this.dst = node;
	}

	@Override
	public void processEvent(Event evt) {
		switch (evt.getTag()) {
		case UPDATE_PROGRESS:
			// Update the progress of the current transfers and their allocated bandwidth
			updateTransfersProgress();
			schedule(this, SimulationParameters.NETWORK_UPDATE_INTERVAL, UPDATE_PROGRESS);
			break;
		default:
			break;
		}
	}

	protected void updateTransfersProgress() {
		usedBandwidth = 0;
		double bandwidth = getBandwidth(transferProgressList.size());
		for (int i = 0; i < transferProgressList.size(); i++) {
			// Allocate bandwidth
			usedBandwidth += transferProgressList.get(i).getRemainingFileSize();

			transferProgressList.get(i).setCurrentBandwidth(bandwidth);
			updateTransfer(transferProgressList.get(i));
		}
	}

	protected double getBandwidth(double remainingTasksCount) {
		return (bandwidth / (remainingTasksCount > 0 ? remainingTasksCount : 1));
	}

	protected void updateTransfer(TransferProgress transfer) {

		double oldRemainingSize = transfer.getRemainingFileSize();

		// Update progress (remaining file size)
		if (SimulationParameters.REALISTIC_NETWORK_MODEL)
			transfer.setRemainingFileSize(transfer.getRemainingFileSize()
					- (SimulationParameters.NETWORK_UPDATE_INTERVAL * transfer.getCurrentBandwidth()));
		else
			transfer.setRemainingFileSize(0);

		double transferDelay = (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth();

		// Set the task network delay to decide whether it has failed due to latency or
		// not.
		transfer.getTask().addActualNetworkTime(transferDelay);

		// Update network usage delay
		if (type == NetworkLinkTypes.LAN)
			transfer.setLanNetworkUsage(transfer.getLanNetworkUsage() + transferDelay);

		// Update MAN network usage delay
		else if (type == NetworkLinkTypes.MAN)
			transfer.setManNetworkUsage(transfer.getManNetworkUsage() + transferDelay);

		// Update WAN network usage delay
		else if (type == NetworkLinkTypes.WAN)
			transfer.setWanNetworkUsage(transfer.getWanNetworkUsage() + transferDelay);

		if (transfer.getRemainingFileSize() <= 0) { // Transfer finished
			transfer.setRemainingFileSize(0); // if < 0 set it to 0
			transferFinished(transfer);
		}
	}

	protected void transferFinished(TransferProgress transfer) {

		this.transferProgressList.remove(transfer);

		// Add the network link latency to the task network delay
		transfer.getTask().addActualNetworkTime(0);

		// Remove the previous hop (data has been transferred one hop)
		transfer.getVertexList().remove(0);
		transfer.getEdgeList().remove(0);

		// Data has reached the destination
		if (transfer.getVertexList().size() == 1) {
			// Update logger parameters
			simulationManager.getSimulationLogger().updateNetworkUsage(transfer);

			schedule(simulationManager.getNetworkModel(), latency, NetworkModel.TRANSFER_FINISHED, transfer);
		} else {
			// Still did not reach destination, send it to the next hop
			transfer.setRemainingFileSize(transfer.getFileSize());
			transfer.getEdgeList().get(0).addTransfer(transfer);
		}
	}

	public double getUsedBandwidth() {
		// Return bandwidth usage in bits per second
		return Math.min(bandwidth, usedBandwidth);
	}

	public NetworkLinkTypes getType() {
		return type;
	}

	public void setType(NetworkLinkTypes type) {
		this.type = type;
	}

	public void addTransfer(TransferProgress transfer) {
		// Used by the energy model to get the total energy consumed by this network
		// link
		totalTrasferredData += transfer.getFileSize();
		transferProgressList.add(transfer);

	}

	public EnergyModelNetworkLink getEnergyModel() {
		return energyModel;
	}

	protected void setEnergyModel(EnergyModelNetworkLink energyModel) {
		this.energyModel = energyModel;
	}

	public double getTotalTransferredData() {
		return totalTrasferredData;
	}

}
