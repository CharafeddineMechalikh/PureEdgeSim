package com.mechalikh.pureedgesim.network;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink; 

public class NetworkLinkNull extends NetworkLink {

	public double getLatency() {
		return 0;
	}

	public ComputingNode getSrc() {
		return ComputingNode.NULL;
	}

	public ComputingNode getDst() {
		return ComputingNode.NULL;
	}

	protected double getBandwidth(double remainingTasksCount) {
		return 0;
	}

	public double getUsedBandwidth() {
		return 0;
	}
	
	public EnergyModelNetworkLink getEnergyModel() {
		return EnergyModelNetworkLink.NULL;
	}

	public double getTotalTransferredData() {
		return 0;
	}
}
