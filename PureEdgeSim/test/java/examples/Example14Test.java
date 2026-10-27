package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;

class Example14Test extends AbstractTest {

	@Test
	void theExampleSettingsSelectTheMistOnlyArchitecture() {
		assertTrue(new ParametersParser("PureEdgeSim/examples/Example14/settings/simulation_parameters.properties").parse());
		assertEquals("MIST_ONLY", SimulationParameters.orchestrationArchitectures[0]);
		assertEquals("Q_LEARNING", SimulationParameters.orchestrationAlgorithms[0]);
		assertEquals(200, SimulationParameters.minNumberOfEdgeDevices);
	}

	@Test
	@Tag("integration")
	void aRealSimulationRunsWithTheMistOnlyArchitecture() throws Exception {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomEdgeOrchestrator(Example11QLearningOrchestrator.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("mistq.properties");
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		Path latest = Files.list(Path.of(OUTPUT_FOLDER))
				.filter(Files::isDirectory)
				.filter(path -> Character.isDigit(path.getFileName().toString().charAt(0)))
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder"));
		Path csv = Files.walk(latest)
				.filter(path -> path.toString().endsWith(".csv"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no results csv in " + latest));
		String content = Files.readString(csv);
		assertTrue(content.contains("MIST_ONLY"), "the csv should name the architecture");
		assertTrue(content.contains("Q_LEARNING"), "the csv should name the algorithm");
	}
}
