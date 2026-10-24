package com.mechalikh.pureedgesim;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

class MainApplicationTest extends AbstractTest {

	@Test
	void theApplicationClassCanBeInstantiated() {
		assertNotNull(new MainApplication());
	}

	@Test
	void mainLaunchesAFullSimulationUsingTheConfiguredSettings() throws Exception {
		String output = OUTPUT_FOLDER + "main-application";
		Simulation simulation = new Simulation();
		simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
		simulation.setCustomOutputFolder(output);
		useSettings(FAST_SETTINGS);

		SimLogCapture.mute();
		try {
			MainApplication.main(new String[0]);
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(Files.exists(Path.of(output)), "the simulation should have written its output");
		assertTrue(Files.walk(Path.of(output)).anyMatch(path -> path.toString().endsWith(".csv")),
				"a results csv should have been written");
	}
}
