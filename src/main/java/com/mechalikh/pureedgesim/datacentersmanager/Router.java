package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * A network-only node used to represent the shared WAN router. It has no
 * computing capacity, so it is treated as a sensor and never executes tasks.
 */
public class Router extends DefaultComputingNode {

	/**
	 * Creates a router with zero CPU, storage, and RAM.
	 *
	 * @param simulationManager the simulation manager.
	 */
	public Router(SimulationManager simulationManager) {
		super(simulationManager, 0, 0, 0, 0);
	}

	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
		// Do nothing
	}
}
