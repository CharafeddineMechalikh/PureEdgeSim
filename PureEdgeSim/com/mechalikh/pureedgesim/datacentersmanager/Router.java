package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class Router extends DefaultComputingNode {

	public Router(SimulationManager simulationManager) {
		super(simulationManager, 0, 0, 0, 0);
	}

	@Override
	public void startInternal() {
		// Do nothing
	}

	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
		// Do nothing
	}
}
