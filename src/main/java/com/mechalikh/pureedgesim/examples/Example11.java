package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * A tabular Q-learning orchestrator that learns which tier should run each task.
 * <p>
 * The agent observes the application type, a bucket of the task length, and the
 * pending load of a tier, and chooses between the cloud, edge, and mist tiers.
 * A reward is the negative delay of the completed task, so the policy learns to
 * prefer the tier that returns results faster for the workloads it sees. The
 * scenario keeps the three application types of the default settings (hard,
 * soft, and non real-time) so that the state of the agent is meaningful.
 * <p>
 * The policy is inspired by the reinforcement-learning orchestration work of
 * Safavifar, Ghanadbashi, and Golpayegani (ICTAI 2021) and by the multi-objective
 * extension of Safavifar et al. (IEEE Access 2024). This example implements a
 * compact tabular agent, not the neural models of those papers, and its
 * parameters are chosen to keep the run short.
 *
 * @since PureEdgeSim 6.0
 *
 * @see Example11QLearningOrchestrator
 */
public class Example11 {

	private static final String SETTINGS = "examples/Example11/settings/";
	private static final String OUTPUT = "output/Example11_output/";

	/**
	 * Runs the Q-learning scenario.
	 */
	public Example11() {
		Simulation sim = new Simulation();

		sim.setCustomEdgeOrchestrator(Example11QLearningOrchestrator.class);

		sim.setCustomSettingsFolder(SETTINGS);
		sim.setCustomOutputFolder(OUTPUT);

		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example11();
	}
}
