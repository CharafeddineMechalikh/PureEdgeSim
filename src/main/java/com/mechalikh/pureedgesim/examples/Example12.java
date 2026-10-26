package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Multi-access MEC: Wi-Fi and cellular devices reach four edge data centers,
 * and an access-aware orchestrator compares the transfer delay of the access
 * link with the compute delay of each tier.
 *
 * @since PureEdgeSim 6.0
 */
public class Example12 {

	private static final String SETTINGS = "examples/Example12/settings/";
	private static final String OUTPUT = "output/Example12_output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example12() {
		Simulation sim = new Simulation();
		sim.setCustomEdgeOrchestrator(Example12AccessAwareOrchestrator.class);
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
		new Example12();
	}
}
