package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Self-organizing mist clusters: devices elect their own heads and the
 * orchestrator places every task inside the cluster the origin joined.
 *
 * @since PureEdgeSim 6.0
 */
public class Example22 {

	private static final String SETTINGS = "examples/Example22/settings/";
	private static final String OUTPUT = "output/Example22_output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example22() {
		Simulation sim = new Simulation();
		sim.setCustomComputingNode(Example22SelfClusteringDevice.class);
		sim.setCustomEdgeOrchestrator(Example22ClusterOrchestrator.class);
		sim.setCustomSettingsFolder(SETTINGS);
		sim.setCustomOutputFolder(OUTPUT);
		sim.launchSimulation();
	}

	/**
	 * Starts the example.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		new Example22();
	}
}
