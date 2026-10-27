package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Storage-aware container caching at the edge.
 *
 * @since PureEdgeSim 6.0
 */
public class Example21 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example21/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example21/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example21() {
		Simulation sim = new Simulation();
		sim.setCustomComputingNode(Example21CacheDevice.class);
		sim.setCustomNetworkModel(Example21ContainerCacheNetworkModel.class);
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
		new Example21();
	}
}
