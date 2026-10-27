package com.mechalikh.pureedgesim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;

@Tag("integration")
class SimulationIntegrationTest extends AbstractTest {

	private void run(String parametersFile) {
		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings(parametersFile);
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}
	}

	@Test
	void fastSimulationRunsToCompletion() throws Exception {
		run(FAST_SETTINGS);
		assertTrue(SimulationParameters.simulationDuration > 0);
		assertTrue(new File(OUTPUT_FOLDER).exists(), "output folder should be created");
		assertTrue(Files.walk(Path.of(OUTPUT_FOLDER)).anyMatch(p -> p.toString().endsWith(".csv")),
				"a results csv should be written");
	}

	@Test
	void simulationWithOrchestratorsAndRegistryRunsToCompletion() {
		run(ORCHESTRATOR_SETTINGS);
		assertTrue(SimulationParameters.enableOrchestrators);
		assertTrue(SimulationParameters.enableRegistry);
		assertEquals("MIST", SimulationParameters.deployOrchestrators);
	}

	@Test
	void allArchitecturesAndAlgorithmsRun() {
		run(FULL_SETTINGS);
		assertTrue(SimulationParameters.orchestrationArchitectures.length > 1);
		assertTrue(SimulationParameters.orchestrationAlgorithms.length > 1);
	}
}
