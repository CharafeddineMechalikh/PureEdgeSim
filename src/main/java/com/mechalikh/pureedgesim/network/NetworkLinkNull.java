package com.mechalikh.pureedgesim.network;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink; 

/**
 * Null-object implementation of {@link NetworkLink}. It reports zero capacity,
 * zero usage, and the null computing node and energy model, so callers do not
 * need null checks when no link exists.
 */
public class NetworkLinkNull extends NetworkLink {

	@Override
	public double getLatency() {
		return 0;
	}

	@Override
	public ComputingNode getSrc() {
		return ComputingNode.NULL;
	}

	@Override
	public ComputingNode getDst() {
		return ComputingNode.NULL;
	}

	@Override
	protected double getBandwidth(double remainingTasksCount) {
		return 0;
	}

	@Override
	public double getUsedBandwidth() {
		return 0;
	}
	
	@Override
	public EnergyModelNetworkLink getEnergyModel() {
		return EnergyModelNetworkLink.NULL;
	}

	@Override
	public double getTotalTransferredData() {
		return 0;
	}
}
