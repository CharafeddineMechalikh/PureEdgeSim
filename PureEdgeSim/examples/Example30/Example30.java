package examples;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract;

/**
 * Example 30: placing the subtasks of service graphs with and without regard to
 * the risk that the chosen node fails, when edge devices and an edge data center
 * fail.
 * <p>
 * The run is chosen with system properties, so one settings folder serves a
 * whole sweep:
 * <ul>
 * <li>{@code example30.scenario}: {@code crash} (default), {@code battery} or
 * {@code mobility}, which selects {@code edge_devices_<scenario>.xml};</li>
 * <li>{@code example30.devices}: the number of edge devices (default 100);</li>
 * <li>{@code example30.seed}: the random seed (default 1);</li>
 * <li>{@code example30.algorithms}: a comma-separated list of algorithms of
 * {@link Example30RiskAwareOrchestrator} (default {@code RELIABLE_INNER});</li>
 * <li>{@code example30.architecture}: the orchestration architecture (default
 * {@code ALL}); {@code EDGE_AND_CLOUD} keeps every subtask off the devices;</li>
 * <li>{@code example30.edgeOutage}: {@code false} keeps every edge data center
 * up in the crash scenario (default {@code true});</li>
 * <li>{@code example30.out}: a file to which each run appends one JSON line of
 * metrics (optional).</li>
 * </ul>
 */
public class Example30 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example30/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example30/output/";

	/**
	 * Runs the scenario described by the system properties.
	 *
	 * @throws IOException if the settings cannot be read or written
	 */
	public Example30() throws IOException {
		String settings = System.getProperty("example30.settings", SETTINGS);
		String scenario = System.getProperty("example30.scenario", "crash");
		System.setProperty("example30.scenario", scenario);
		Path parameters = withOverrides(Paths.get(settings, "simulation_parameters.properties"),
				Integer.getInteger("example30.devices", 100), Long.getLong("example30.seed", 1L),
				System.getProperty("example30.algorithms", "RELIABLE_INNER"),
				System.getProperty("example30.architecture", "ALL"));

		Simulation sim = new Simulation();
		sim.setCustomSettingsFolder(settings);
		sim.setCustomFilePath(parameters.toString(), SimulationAbstract.Files.SIMULATION_PARAMETERS);
		sim.setCustomFilePath(settings + "edge_devices_" + scenario + ".xml",
				SimulationAbstract.Files.EDGE_DEVICES_FILE);
		sim.setCustomOutputFolder(System.getProperty("example30.output", OUTPUT));
		sim.setCustomEdgeOrchestrator(Example30RiskAwareOrchestrator.class);
		sim.setCustomSimulationManager(Example30FailureSimulationManager.class);
		sim.setCustomComputingNode(Example30Device.class);
		sim.launchSimulation();
	}

	/**
	 * Writes a copy of the parameters file with the device count, the seed and the
	 * algorithms of this run.
	 */
	static Path withOverrides(Path source, int devices, long seed, String algorithms, String architecture)
			throws IOException {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		properties.setProperty("min_number_of_edge_devices", Integer.toString(devices));
		properties.setProperty("max_number_of_edge_devices", Integer.toString(devices));
		properties.setProperty("edge_device_counter_size", Integer.toString(devices));
		properties.setProperty("random_seed", Long.toString(seed));
		properties.setProperty("orchestration_algorithms", algorithms);
		properties.setProperty("orchestration_architectures", architecture);
		Path copy = Files.createTempFile("example30-", ".properties");
		copy.toFile().deleteOnExit();
		try (Writer writer = Files.newBufferedWriter(copy, StandardCharsets.UTF_8)) {
			properties.store(writer, "Example 30 run");
		}
		return copy;
	}

	public static void main(String[] args) throws IOException {
		new Example30();
	}
}
