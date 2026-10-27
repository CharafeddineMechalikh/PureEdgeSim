package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Smart-city offloading with a softmax policy over cloud, edge, and mist.
 *
 * @since PureEdgeSim 6.0
 */
public class Example16 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example16/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example16/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example16() {
		Simulation sim = new Simulation();
		sim.setCustomEdgeOrchestrator(Example16SoftmaxOrchestrator.class);
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
		new Example16();
	}
}
