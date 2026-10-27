package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Multi-layer fog for a smart home: sensors generate tasks, home gateways form
 * the mist layer, one edge data center covers the neighbourhood, and the cloud
 * is the last resort.
 *
 * @since PureEdgeSim 6.0
 */
public class Example13 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example13/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example13/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example13() {
		Simulation sim = new Simulation();
		sim.setCustomEdgeOrchestrator(Example13FogLayersOrchestrator.class);
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
		new Example13();
	}
}
