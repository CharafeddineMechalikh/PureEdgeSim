package com.mechalikh.pureedgesim.simulationvisualizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

class SimulationVisualizerTest extends AbstractTest {

	private static void addSomeNodes(BootstrapSimulationManager manager) {
		ComputingNodesGenerator generator = VisualizerTestSupport.generator(manager);
		generator.getMistOnlyList().add(VisualizerTestSupport.newNode(10, 10).dead());
		generator.getMistOnlyList().add(VisualizerTestSupport.newNode(20, 20));
		VisualizerTestSupport.TestNode active = VisualizerTestSupport.newNode(30, 30);
		active.setIdle(false);
		generator.getMistOnlyList().add(active);
		generator.getMistOnlyListSensorsExcluded().add(VisualizerTestSupport.newNode(10));
		generator.getMistOnlyListSensorsExcluded().add(VisualizerTestSupport.newNode(20));
		generator.getEdgeOnlyList().add(VisualizerTestSupport.newNode(40, 40));
		generator.getCloudOnlyList().add(VisualizerTestSupport.newNode(30));
	}

	private static Set<String> savedChartNames(String outputFolder) throws Exception {
		Path base = Paths.get(outputFolder);
		assertTrue(Files.isDirectory(base), "charts output folder should exist");
		try (Stream<Path> files = Files.walk(base)) {
			return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
					.collect(Collectors.toSet());
		}
	}

	@Test
	void sharedWanVisualizerUpdatesFrameAndSavesFourCharts() throws Exception {
		String outputFolder = AbstractTest.OUTPUT_FOLDER + "visualizer-shared/";
		VisualizerTestSupport.deleteRecursively(new File(outputFolder));

		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 0, 25,
				2_000_000, 1_000_000);
		SimulationParameters.outputFolder = outputFolder;
		SimulationParameters.useOneSharedWanLink = true;
		addSomeNodes(manager);

		SimulationVisualizer visualizer = new SimulationVisualizer(manager);
		try {
			assertEquals(4, visualizer.charts.size());
			assertTrue(visualizer.firstTime);

			// The tasks chart expects updates to start at time zero.
			visualizer.updateCharts();
			assertFalse(visualizer.firstTime);
			assertNotNull(visualizer.simulationResultsFrame);

			VisualizerTestSupport.setClock(90);
			visualizer.updateCharts();
			String title = visualizer.simulationResultsFrame.getTitle();
			assertTrue(title.contains("1 min : 30 seconds"), title);
			assertTrue(title.contains("number of edge devices = 20"), title);
			assertTrue(title.contains("Architecture = ALL"), title);
			assertTrue(title.contains("Algorithm = TRADE_OFF"), title);

			// Second call takes the already-displayed branch and repaints all charts.
			visualizer.updateCharts();
			visualizer.saveCharts();
		} finally {
			if (visualizer.simulationResultsFrame != null) {
				visualizer.close();
			}
		}

		Set<String> saved = savedChartNames(outputFolder);
		assertTrue(saved.contains("map_chart.png"), saved.toString());
		assertTrue(saved.contains("cpu_usage.png"), saved.toString());
		assertTrue(saved.contains("tasks_success_rate.png"), saved.toString());
		assertTrue(saved.contains("network_usage.png"), saved.toString());
	}

	@Test
	void visualizerWithoutSharedWanSavesThreeCharts() throws Exception {
		String outputFolder = AbstractTest.OUTPUT_FOLDER + "visualizer-no-wan/";
		VisualizerTestSupport.deleteRecursively(new File(outputFolder));

		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 0, 0, 0, 0);
		SimulationParameters.outputFolder = outputFolder;
		SimulationParameters.useOneSharedWanLink = false;
		addSomeNodes(manager);

		SimulationVisualizer visualizer = new SimulationVisualizer(manager);
		try {
			assertEquals(3, visualizer.charts.size());
			visualizer.updateCharts();
			assertNotNull(visualizer.simulationResultsFrame);
			VisualizerTestSupport.setClock(61);
			visualizer.updateCharts();
			assertTrue(visualizer.simulationResultsFrame.getTitle().contains("1 min : 1 seconds"));
			visualizer.saveCharts();
		} finally {
			if (visualizer.simulationResultsFrame != null) {
				visualizer.close();
			}
		}

		Set<String> saved = savedChartNames(outputFolder);
		assertEquals(3, saved.size(), saved.toString());
		assertTrue(saved.contains("map_chart.png"), saved.toString());
		assertTrue(saved.contains("cpu_usage.png"), saved.toString());
		assertTrue(saved.contains("tasks_success_rate.png"), saved.toString());
		assertFalse(saved.contains("network_usage.png"), saved.toString());
	}
}
