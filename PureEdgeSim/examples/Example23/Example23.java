package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * The four model hooks that the other examples do not use: a custom computing
 * nodes generator, task generator, topology creator, and simulation manager.
 * <p>
 * The scenario is a 20 device fleet with a rare device type that the default
 * allocation would drop, synchronized minute bursts of requests, a full mesh of
 * edge data centers, and an audit of the task failures by reason.
 *
 * @since PureEdgeSim 6.0
 */
public class Example23 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example23/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example23/output/";

	/**
	 * Creates and runs the simulation.
	 */
	public Example23() {
		Simulation sim = new Simulation();
		sim.setCustomComputingNodesGenerator(Example23TypeGuaranteeGenerator.class);
		sim.setCustomTaskGenerator(Example23BurstTaskGenerator.class);
		sim.setCustomTopologyCreator(Example23FullMeshTopologyCreator.class);
		sim.setCustomSimulationManager(Example23AuditSimulationManager.class);
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
		new Example23();
	}
}
