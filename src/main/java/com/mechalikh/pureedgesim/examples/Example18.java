package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Smart-city offloading with an ant colony over cloud, edge, and mist.
 *
 * @since PureEdgeSim 6.0
 */
public class Example18 {

	private static final String SETTINGS = "examples/Example18/settings/";
	private static final String OUTPUT = "output/Example18_output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example18() {
		Simulation sim = new Simulation();
		sim.setCustomEdgeOrchestrator(Example18AntColonyOrchestrator.class);
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
		new Example18();
	}
}
