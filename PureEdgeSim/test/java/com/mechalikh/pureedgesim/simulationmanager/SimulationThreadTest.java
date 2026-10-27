package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;

class SimulationThreadTest extends AbstractTest {

	private static class InvalidSimulationManager extends SimulationManager {
		InvalidSimulationManager(SimLog simLog, PureEdgeSim simulation) {
			super(simLog, simulation, 1, 1, new Scenario(20, 0, 0));
		}

		@Override
		public void startSimulation() {
			// Never reached: the thread expects the five argument constructor.
		}

		@Override
		public void processEvent(Event event) {
			// No events are processed by this invalid manager.
		}

		@Override
		public double getFailureRate() {
			return 0;
		}
	}

	private Simulation preparedSimulation(String outputFolder) {
		Simulation simulation = new Simulation();
		simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
		simulation.setCustomOutputFolder(outputFolder);
		useSettings(FAST_SETTINGS);
		assertTrue(simulation.checkFiles());
		simulation.loadScenarios();
		return simulation;
	}

	private static boolean containsCsv(String folder) throws Exception {
		Path root = Path.of(folder);
		return Files.exists(root) && Files.walk(root).anyMatch(path -> path.toString().endsWith(".csv"));
	}

	@Test
	void startSimulationRunsTheLoadedScenariosAndWritesResults() throws Exception {
		String output = OUTPUT_FOLDER + "thread-normal";
		Simulation simulation = preparedSimulation(output);

		SimLogCapture.mute();
		try {
			new SimulationThread(simulation, 0, 1).startSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(containsCsv(output));
	}

	@Test
	void firstIterationCleansTheOutputFolderWhenEnabled() throws Exception {
		String output = OUTPUT_FOLDER + "thread-clean";
		Simulation simulation = preparedSimulation(output);
		simulation.getScenarios().add(new Scenario(20, 0, 0));
		SimulationParameters.cleanOutputFolder = true;

		Path marker = Path.of(output, "marker.txt");
		Files.createDirectories(marker.getParent());
		Files.write(marker, new byte[] { 1 });

		SimLogCapture.mute();
		try {
			new SimulationThread(simulation, 0, 1).startSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		assertFalse(Files.exists(marker), "the marker should have been removed by the first iteration");
		assertTrue(containsCsv(output));
	}

	@Test
	void aThreadStartingAfterTheFirstIterationDoesNotCleanTheOutputFolder() throws Exception {
		String output = OUTPUT_FOLDER + "thread-clean-later";
		Simulation simulation = preparedSimulation(output);
		simulation.getScenarios().add(new Scenario(20, 0, 0));
		SimulationParameters.cleanOutputFolder = true;

		Path marker = Path.of(output, "marker.txt");
		Files.createDirectories(marker.getParent());
		Files.write(marker, new byte[] { 1 });

		SimLogCapture.mute();
		try {
			new SimulationThread(simulation, 1, 1).startSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(Files.exists(marker), "a thread that does not start at iteration zero must not clean the folder");
	}

	@Test
	void generateChartsOnlyRunsWhenAllChartConditionsAreMet() {
		SimulationThread thread = new SimulationThread(new Simulation(), 0, 1);
		SimulationParameters.saveCharts = true;
		SimulationParameters.parallelism_enabled = false;

		SimLog unreadableLog = mock(SimLog.class);
		when(unreadableLog.getFileName(".csv")).thenThrow(new IllegalStateException("no results file"));
		assertThrows(IllegalStateException.class, () -> thread.generateCharts(unreadableLog));

		SimulationParameters.parallelism_enabled = true;
		thread.generateCharts(mock(SimLog.class));

		SimulationParameters.parallelism_enabled = false;
		thread.generateCharts(null);
	}

	@Test
	void pausePrintsDotsForEachSecond() throws Exception {
		Simulation simulation = new Simulation();
		SimulationParameters.pauseLength = 1;
		SimLog simLog = mock(SimLog.class);

		SimLogCapture.mute();
		try {
			new SimulationThread(simulation, 0, 1).pause(simLog);
		} finally {
			SimLogCapture.unmute();
		}

		verify(simLog).printSameLine(".");
	}

	@Test
	void anInvalidSimulationManagerIsCaughtAndInterruptsTheThread() {
		Simulation simulation = preparedSimulation(OUTPUT_FOLDER + "thread-error");
		simulation.simulationManager = InvalidSimulationManager.class;

		SimLogCapture.mute();
		try {
			new SimulationThread(simulation, 0, 1).startSimulation();
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(Thread.interrupted(), "the thread should have been interrupted by the error handler");
	}

	@Test
	void aSimulationWithoutScenariosSimplyFinishes() {
		Simulation simulation = new Simulation();
		SimLogCapture.mute();
		try {
			new SimulationThread(simulation, 0, 1).startSimulation();
		} finally {
			SimLogCapture.unmute();
		}
		assertTrue(simulation.getScenarios().isEmpty());
	}
}
