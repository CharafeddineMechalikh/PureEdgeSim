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
 * Link between two compute nodes in the infrastructure graph. A link carries
 * concurrent transfers by splitting its bandwidth evenly among them, adds its
 * latency once per hop, and forwards each transfer to the next link on its path.
 */
public class NetworkLink extends SimEntity {
	/** Event tag used to refresh the progress of the active transfers. */
	public static final int UPDATE_PROGRESS = 1;
	/** Link latency, in seconds per hop. */
	protected double latency = 0;
	/** Link capacity, in bits per second. */
	protected double bandwidth = 0;
	/** Transfers currently using this link. */
	protected List<TransferProgress> transferProgressList = new ArrayList<>();
	/** Link source. */
	protected ComputingNode src = ComputingNode.NULL;
	/** Link destination. */
	protected ComputingNode dst = ComputingNode.NULL;
	/** Simulation manager that provides the clock and logger. */
	protected SimulationManager simulationManager;
	/** Usage estimate computed during the latest update. */
	protected double usedBandwidth = 0;
	/** Total data that traversed this link, in bits. */
	protected double totalTrasferredData = 0;
	/** Energy model attached to this link. */
	protected EnergyModelNetworkLink energyModel = EnergyModelNetworkLink.NULL;
	/** True while an update event is already scheduled. */
	protected boolean scheduled = false;

	/** Category of a link, used for metrics and energy accounting. */
	public enum NetworkLinkTypes {
		/** Wide area network link. */
		WAN,
		/** Metropolitan area network link. */
		MAN,
		/** Local area network link. */
		LAN,
		/** Link that is not used for routing or metrics. */
		IGNORE
	}

	/** Category of this link. */
	protected NetworkLinkTypes type;

	/** Null-object instance. */
	public static final NetworkLink NULL = new NetworkLinkNull();

	/**
	 * Creates a link and registers it in the simulation engine.
	 *
	 * @param src               the source node.
	 * @param dst               the destination node.
	 * @param simulationManager the simulation manager.
	 * @param type              the link category.
	 */
	public NetworkLink(ComputingNode src, ComputingNode dst, SimulationManager simulationManager,
			NetworkLinkTypes type) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
		this.src = src;
		this.dst = dst;
		this.setType(type);
	}

	/** Creates a detached link; used by the null-object instance. */
	public NetworkLink() {
	}

	/**
	 * Returns the link latency.
	 *
	 * @return the latency, in seconds per hop.
	 */
	public double getLatency() {
		return latency;
	}

	/**
	 * Sets the link latency.
	 *
	 * @param latency the latency, in seconds per hop.
	 * @return this link, for chaining.
	 */
	public NetworkLink setLatency(double latency) {
		this.latency = latency;
		return this;
	}

	/**
	 * Sets the link capacity.
	 *
	 * @param bandwidth the capacity, in bits per second.
	 * @return this link, for chaining.
	 */
	public NetworkLink setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
		return this;
	}

	/**
	 * Returns the source node.
	 *
	 * @return the source.
	 */
	public ComputingNode getSrc() {
		return src;
	}

	/**
	 * Sets the source node.
	 *
	 * @param src the source.
	 */
	public void setSrc(ComputingNode src) {
		this.src = src;
	}

	/**
	 * Returns the destination node.
	 *
	 * @return the destination.
	 */
	public ComputingNode getDst() {
		return dst;
	}

	/**
	 * Sets the destination node.
	 *
	 * @param node the destination.
	 */
	public void setDst(ComputingNode node) {
		this.dst = node;
	}

	/**
	 * Processes the periodic progress update while transfers are active.
	 *
	 * @param evt the event.
	 */
	@Override
	public void processEvent(Event evt) {
		if (evt.getTag() == UPDATE_PROGRESS) {
			// Update the progress of the current transfers and their allocated bandwidth
			updateTransfersProgress();
			if (this.transferProgressList.size() != 0)
				schedule(this, SimulationParameters.networkUpdateInterval, UPDATE_PROGRESS);
			else
				scheduled = false;
		}

	}

	/**
	 * Advances every active transfer by one network update interval, sharing the
	 * link capacity evenly among them.
	 */
	protected void updateTransfersProgress() {
		usedBandwidth = 0;
		double allocatedBandwidth = getBandwidth(transferProgressList.size());
		for (int i = 0; i < transferProgressList.size(); i++) {
			// Allocate bandwidth
			usedBandwidth += transferProgressList.get(i).getRemainingFileSize();

			transferProgressList.get(i).setCurrentBandwidth(allocatedBandwidth);
			updateTransfer(transferProgressList.get(i));
		}
	}

	/**
	 * Returns the bandwidth allocated to each active transfer.
	 *
	 * @param remainingTasksCount number of active transfers.
	 * @return the per-transfer bandwidth, in bits per second.
	 */
	protected double getBandwidth(double remainingTasksCount) {
		return (bandwidth / (remainingTasksCount > 0 ? remainingTasksCount : 1));
	}

	/**
	 * Advances one transfer. In realistic mode the remaining size decreases by
	 * {@code networkUpdateInterval * currentBandwidth}; in non-realistic mode it is
	 * set to zero in a single step while the delay is still computed from the
	 * allocated bandwidth. The resulting delay is added to the task and to the
	 * usage counter of the link type.
	 *
	 * @param transfer the transfer to advance.
	 */
	protected void updateTransfer(TransferProgress transfer) {

		double oldRemainingSize = transfer.getRemainingFileSize();

		// Update progress (remaining file size)
		if (SimulationParameters.realisticNetworkModel)
			transfer.setRemainingFileSize(transfer.getRemainingFileSize()
					- (SimulationParameters.networkUpdateInterval * transfer.getCurrentBandwidth()));
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

	/**
	 * Handles the end of one hop: adds the link latency, removes the hop from the
	 * path, and either schedules the next hop (with the full file size again) or
	 * notifies the network model that the transfer reached its destination.
	 *
	 * @param transfer the transfer that finished the current hop.
	 */
	protected void transferFinished(TransferProgress transfer) {

		this.transferProgressList.remove(transfer);

		// Add the network link latency to the task network delay
		transfer.getTask().addActualNetworkTime(latency);

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

	/**
	 * Returns the bandwidth currently used on this link.
	 *
	 * @return the used bandwidth, in bits per second, capped at the link capacity.
	 */
	public double getUsedBandwidth() {
		// Return bandwidth usage in bits per second
		return Math.min(bandwidth, usedBandwidth);
	}

	/**
	 * Returns the link category.
	 *
	 * @return the link type.
	 */
	public NetworkLinkTypes getType() {
		return type;
	}

	/**
	 * Sets the link category.
	 *
	 * @param type the link type.
	 */
	public void setType(NetworkLinkTypes type) {
		this.type = type;
	}

	/**
	 * Adds a transfer to this link and schedules the progress update if none is
	 * pending.
	 *
	 * @param transfer the transfer to send over this link.
	 */
	public void addTransfer(TransferProgress transfer) {
		// Used by the energy model to get the total energy consumed by this network
		// link
		totalTrasferredData += transfer.getFileSize();
		transferProgressList.add(transfer);

		if (!scheduled) {
			scheduleNow(this, UPDATE_PROGRESS);
			scheduled = true;
		}
	}

	/**
	 * Returns the energy model of this link.
	 *
	 * @return the energy model.
	 */
	public EnergyModelNetworkLink getEnergyModel() {
		return energyModel;
	}

	/**
	 * Sets the energy model of this link.
	 *
	 * @param energyModel the energy model.
	 */
	protected void setEnergyModel(EnergyModelNetworkLink energyModel) {
		this.energyModel = energyModel;
	}

	/**
	 * Returns the total data that traversed this link.
	 *
	 * @return the transferred data, in bits.
	 */
	public double getTotalTransferredData() {
		return totalTrasferredData;
	}

}
