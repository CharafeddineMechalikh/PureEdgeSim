package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Multi-access offloading with a context-aware deep reinforcement agent.
 *
 * @since PureEdgeSim 6.0
 */
public class Example17 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example17/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example17/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example17() {
		Simulation sim = new Simulation();
		sim.setCustomEdgeOrchestrator(Example17ContextAwareOrchestrator.class);
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
		new Example17();
	}
}
