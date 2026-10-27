package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

class ChartsGeneratorTest extends AbstractTest {

	private static final String OUTPUT = AbstractTest.OUTPUT_FOLDER + "charts-generator/";

	/** A generator restricted to one chart per category to keep the test fast. */
	private static class SmallChartsGenerator extends ChartsGenerator {

		private int saveAttempts;

		SmallChartsGenerator(String fileName) {
			super(fileName);
			energyChartsList = Collections.singletonList("Energy consumption of computing nodes (Wh)");
			cpuChartsList = Collections.singletonList("Average CPU usage (%)");
			tasksChartsList = Collections.singletonList("Tasks successfully executed");
			delaysChartsList = Collections.singletonList("Average waiting time (s)");
			networkChartsList = Collections.singletonList("Wan usage (s)");
		}

		/**
		 * Counts every image the production code wants to save, but only writes the
		 * first two (each 300 DPI export is slow). This still exercises every chart
		 * generation path and validates two real end-to-end PNG exports.
		 */
		@Override
		protected void saveBitmap(XYChart chart, String folder, String name) {
			saveAttempts++;
			if (saveAttempts <= 2) {
				super.saveBitmap(chart, folder, name);
			}
		}

		/**
		 * Uses a small canvas so that the 300 DPI PNG export used by the production
		 * code stays fast in tests. The production implementation itself is covered
		 * by {@link #baseInitChart(String, String, String, String)}.
		 */
		@Override
		protected XYChart initChart(String xSeries, String ySeries, String ySeriesLabel, String title) {
			XYChart chart = new XYChartBuilder().height(60).width(100).xAxisTitle(xSeries)
					.yAxisTitle(ySeriesLabel).build();
			chart.setTitle(ySeries + " (" + title + ")");
			chart.getStyler().setLegendVisible(true);
			return chart;
		}

		XYChart baseInitChart(String xSeries, String ySeries, String ySeriesLabel, String title) {
			return super.initChart(xSeries, ySeries, ySeriesLabel, title);
		}
	}

	private static Path writeCsv(Path root) throws Exception {
		Path csv = root.resolve("results.csv");
		List<String> header = new SimLog("charts-test", true).getResultsList();
		String headerLine = header.get(0);
		int columns = headerLine.split(",", -1).length;

		List<String> lines = new ArrayList<>();
		lines.add(headerLine);
		lines.add(row("ARCH1", "ALG1", columns));
		// Rows that match only the architecture (second condition false), neither
		// field, and a blank line exercise every parsing branch.
		lines.add(row("ARCH1", "ALG2", columns));
		lines.add(row("OTHER", "OTHER", columns));
		lines.add("");
		Files.write(csv, lines, StandardCharsets.UTF_8);
		return csv;
	}

	private static String row(String architecture, String algorithm, int columns) {
		StringBuilder row = new StringBuilder(architecture).append(',').append(algorithm).append(",20");
		for (int i = 3; i < columns; i++) {
			row.append(',').append(i);
		}
		return row.toString();
	}

	private static List<Path> pngs(Path root) throws Exception {
		try (Stream<Path> files = Files.walk(root)) {
			return files.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
					.collect(Collectors.toList());
		}
	}

	@Test
	void generateProducesChartsForTasksNetworkCpuAndEnergy() throws Exception {
		Path root = Paths.get(OUTPUT);
		VisualizerTestSupportSupport.delete(root);
		Files.createDirectories(root);
		Path csv = writeCsv(root);

		SimulationParameters.orchestrationArchitectures = new String[] { "ARCH1" };
		SimulationParameters.orchestrationAlgorithms = new String[] { "ALG1" };

		SmallChartsGenerator generator = new SmallChartsGenerator(csv.toString());
		assertTrue(generator.records.size() >= 3);
		generator.generate();

		// One chart per architecture and one per algorithm for each of the five
		// selected series: ten save attempts are made.
		assertEquals(10, generator.saveAttempts);
		List<Path> images = pngs(root);
		assertEquals(2, images.size(), images.toString());

		Path finalResults = root.resolve("Final results");
		assertTrue(Files.isDirectory(finalResults.resolve("Architectures/Tasks")));
		assertTrue(Files.isDirectory(finalResults.resolve("Algorithms/Tasks")));
		assertTrue(Files.exists(finalResults.resolve("Architectures/Tasks/Tasks successfully executed__ALG1.png")));
		assertTrue(Files.exists(finalResults.resolve("Algorithms/Tasks/Tasks successfully executed__ARCH1.png")));
	}

	@Test
	void parsingHelpersHandleMatchesAndMissingValues() throws Exception {
		Path root = Paths.get(OUTPUT + "parsing/");
		VisualizerTestSupportSupport.delete(root);
		Files.createDirectories(root);
		Path csv = writeCsv(root);

		SmallChartsGenerator generator = new SmallChartsGenerator(csv.toString());
		assertEquals(0, generator.getColumnIndex("Orchestration architecture"));
		assertEquals(1, generator.getColumnIndex("Orchestration algorithm"));
		assertEquals(-1, generator.getColumnIndex("Does not exist"));

		int wanColumn = generator.getColumnIndex("Wan usage (s)");
		assertEquals(wanColumn, generator.getColumnIndex(" Wan usage (s) "));
		assertEquals(Collections.singletonList((double) wanColumn),
				generator.getColumn("Wan usage (s)", "ARCH1", "ALG1"));
		assertTrue(generator.getColumn("Wan usage (s)", "NO_ARCH", "NO_ALG").isEmpty());

		assertArrayEquals(new double[] { 1, 2 }, generator.toArray(Arrays.asList(1.0, 2.0)), 1e-9);
		assertEquals(0, generator.toArray(Collections.emptyList()).length);

		XYChart defaultChart = generator.baseInitChart("x", "y", "label", "title");
		assertEquals("y (title)", defaultChart.getTitle());
		assertEquals("x", defaultChart.getXAxisTitle());
		assertEquals("label", defaultChart.getYAxisTitle());
	}

	@Test
	void missingFileIsHandledGracefully() {
		Path missing = Paths.get(OUTPUT, "does-not-exist", "missing.csv");
		SmallChartsGenerator generator = new SmallChartsGenerator(missing.toString());
		assertTrue(generator.records.isEmpty());
	}

	@Test
	void saveBitmapFailureIsCaught() throws Exception {
		Path root = Paths.get(OUTPUT + "failure/");
		VisualizerTestSupportSupport.delete(root);
		Files.createDirectories(root);
		Path csv = writeCsv(root);
		SmallChartsGenerator generator = new SmallChartsGenerator(csv.toString());

		// Make the expected output directory impossible to create: its parent is a
		// regular file.
		Path blocker = Files.write(root.resolve("blocker"), new byte[] { 1 });
		generator.fileName = blocker.toString() + "/x.csv";

		XYChart chart = new XYChartBuilder().width(100).height(100).build();
		chart.addSeries("series", new double[] { 1 }, new double[] { 1 });

		SimLogCapture.mute();
		try {
			assertDoesNotThrow(() -> generator.saveBitmap(chart, "Tasks", "name"));
		} finally {
			SimLogCapture.unmute();
		}
		assertTrue(new File(blocker.toString()).isFile());
	}

	/** Local deletion helper (kept here to avoid depending on visualizer test code). */
	private static final class VisualizerTestSupportSupport {
		private static void delete(Path path) throws Exception {
			if (!Files.exists(path)) {
				return;
			}
			try (Stream<Path> files = Files.walk(path)) {
				for (Path file : files.sorted((a, b) -> b.getNameCount() - a.getNameCount())
						.collect(Collectors.toList())) {
					Files.deleteIfExists(file);
				}
			}
		}
	}
}
