package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Minimal {@link TopologyCreator} implementation that records whether the
 * topology generation was requested.
 */
public class RecordingTopologyCreator extends TopologyCreator {

	public boolean generated = false;

	public RecordingTopologyCreator(SimulationManager simulationManager,
			ComputingNodesGenerator computingNodesGenerator) {
		super(simulationManager, computingNodesGenerator);
	}

	@Override
	public void generateTopologyGraph() {
		generated = true;
	}
}
