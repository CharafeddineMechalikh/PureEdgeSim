package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

class SimulationTest extends AbstractTest {

	private static class ExposedSimulation extends Simulation {
		List<Scenario> scenarios() {
			return iterations;
		}

		void doLoadScenarios() {
			loadScenarios();
		}

		String duration(Date start, Date end) {
			return simulatioDuration(start, end);
		}

		boolean filesAreValid() {
			return checkFiles();
		}
	}

	private static class ParallelSimulation extends Simulation {
		@Override
		protected boolean checkFiles() {
			boolean valid = super.checkFiles();
			SimulationParameters.parallelism_enabled = true;
			return valid;
		}
	}

	@Test
	void loadScenariosCombinesAlgorithmsArchitecturesAndDeviceCounts() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "A1", "A2" };
		SimulationParameters.orchestrationArchitectures = new String[] { "X", "Y", "Z" };
		SimulationParameters.minNumberOfEdgeDevices = 10;
		SimulationParameters.maxNumberOfEdgeDevices = 12;
		SimulationParameters.edgeDevicesIncrementationStepSize = 1;

		ExposedSimulation simulation = new ExposedSimulation();
		simulation.doLoadScenarios();

		assertEquals(18, simulation.scenarios().size());
		Scenario first = simulation.scenarios().get(0);
		assertEquals(10, first.getDevicesCount());
		assertEquals(0, first.getOrchAlgorithm());
		assertEquals(0, first.getOrchArchitecture());
		Scenario last = simulation.scenarios().get(17);
		assertEquals(12, last.getDevicesCount());
		assertEquals(1, last.getOrchAlgorithm());
		assertEquals(2, last.getOrchArchitecture());
	}

	@Test
	void simulatioDurationFormatsDaysHoursMinutesAndSeconds() {
		ExposedSimulation simulation = new ExposedSimulation();

		assertEquals("0 minutes, 0 seconds.", simulation.duration(new Date(0), new Date(0)));

		long twoDaysThreeHours = ((2L * 24 + 3) * 60 * 60 + 4 * 60 + 5) * 1000;
		assertEquals("2 days, 3 hours, 4 minutes, 5 seconds.",
				simulation.duration(new Date(0), new Date(twoDaysThreeHours)));

		long oneHour = (60L * 60 + 2 * 60 + 3) * 1000;
		assertEquals("1 hours, 2 minutes, 3 seconds.", simulation.duration(new Date(0), new Date(oneHour)));

		long oneDay = 24L * 60 * 60 * 1000;
		assertEquals("1 days, 0 minutes, 0 seconds.",
				simulation.duration(new Date(0), new Date(oneDay)));
	}

	@Test
	void checkFilesAcceptsTheTestSettingsAndRejectsMissingFiles() {
		new Simulation().setCustomSettingsFolder(SETTINGS_FOLDER);
		useSettings(FAST_SETTINGS);
		assertTrue(new ExposedSimulation().filesAreValid());

		SimulationParameters.edgeDevicesFile = "target/does-not-exist-edge-devices.xml";
		assertFalse(new ExposedSimulation().filesAreValid());
	}

	@Test
	void checkFilesRejectsEachConfigurationFileIndividually() {
		Simulation simulation = new Simulation();
		simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
		useSettings(FAST_SETTINGS);

		SimulationParameters.edgeDataCentersFile = "target/missing-edge-datacenters.xml";
		assertFalse(new ExposedSimulation().filesAreValid());

		simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
		useSettings(FAST_SETTINGS);
		SimulationParameters.cloudDataCentersFile = "target/missing-cloud.xml";
		assertFalse(new ExposedSimulation().filesAreValid());

		simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
		useSettings(FAST_SETTINGS);
		SimulationParameters.simulationParametersFile = "target/missing-parameters.properties";
		assertFalse(new ExposedSimulation().filesAreValid());

		simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
		useSettings(FAST_SETTINGS);
		SimulationParameters.applicationFile = "target/missing-applications.xml";
		assertFalse(new ExposedSimulation().filesAreValid());
	}

	@Test
	void launchSimulationStopsWhenAConfigurationFileIsMissing() {
		SimulationParameters.edgeDevicesFile = "target/missing-edge-devices.xml";
		Simulation simulation = new Simulation();
		SimLogCapture.mute();
		try {
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}
		assertEquals(0, simulation.getScenarios().size());
	}

	@Test
	void parallelLaunchRunsTheScenariosThroughSimulationThreads() {
		ParallelSimulation simulation = new ParallelSimulation();
		simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
		simulation.setCustomOutputFolder(OUTPUT_FOLDER + "simulation-parallel");
		useSettings(FAST_SETTINGS);

		SimLogCapture.mute();
		try {
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(SimulationParameters.parallelism_enabled);
		assertEquals(1, simulation.getScenarios().size());
		assertTrue(new java.io.File(OUTPUT_FOLDER + "simulation-parallel").exists());
	}

	@Test
	void getScenariosReturnsTheMutableIterationList() {
		Simulation simulation = new Simulation();
		assertTrue(simulation.getScenarios().isEmpty());
		simulation.getScenarios().add(new Scenario(5, 0, 0));
		assertEquals(1, simulation.getScenarios().size());
	}
}
