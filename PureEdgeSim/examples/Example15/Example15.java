package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Multi-layer guided reinforcement learning: the Q-learning agent chooses the
 * layer and a guided heuristic picks the least loaded node inside it, with the
 * decision delegated to an edge orchestrator over four data centers.
 *
 * @since PureEdgeSim 6.0
 */
public class Example15 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example15/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example15/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example15() {
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
		new Example15();
	}
}
