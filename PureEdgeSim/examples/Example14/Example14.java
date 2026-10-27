package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Mist-only reinforcement placement: tasks may run only on nearby devices, and
 * the tabular Q-learning agent of Example 11 learns which device to use.
 *
 * @since PureEdgeSim 6.0
 */
public class Example14 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example14/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example14/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example14() {
		Simulation sim = new Simulation();
		sim.setCustomEdgeOrchestrator(Example11QLearningOrchestrator.class);
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
		new Example14();
	}
}
