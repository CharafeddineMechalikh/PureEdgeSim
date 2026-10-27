package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Multi-objective tabular reinforcement learning over delay and energy.
 *
 * @since PureEdgeSim 6.0
 */
public class Example20 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example20/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example20/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example20() {
		Simulation sim = new Simulation();
		sim.setCustomEdgeOrchestrator(Example20MultiObjectiveOrchestrator.class);
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
		new Example20();
	}
}
