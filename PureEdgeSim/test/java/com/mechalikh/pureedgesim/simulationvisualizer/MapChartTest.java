package com.mechalikh.pureedgesim.simulationvisualizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knowm.xchart.XYSeries;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;

class MapChartTest extends AbstractTest {

	private static XYSeries series(MapChart chart, String name) {
		return chart.getChart().getSeriesMap().get(name);
	}

	@Test
	void allArchitecturePlotsDeadIdleAndActiveNodes() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 5, 0, 0, 0);
		ComputingNodesGenerator generator = VisualizerTestSupport.generator(manager);

		generator.getMistOnlyList().add(VisualizerTestSupport.newNode(10, 10).dead());
		generator.getMistOnlyList().add(VisualizerTestSupport.newNode(20, 20));
		VisualizerTestSupport.TestNode active = VisualizerTestSupport.newNode(30, 30);
		active.setIdle(false);
		generator.getMistOnlyList().add(active);

		generator.getEdgeOnlyList().add(VisualizerTestSupport.newNode(40, 40));
		VisualizerTestSupport.TestNode activeDataCenter = VisualizerTestSupport.newNode(50, 50);
		activeDataCenter.setIdle(false);
		generator.getEdgeOnlyList().add(activeDataCenter);

		MapChart chart = new MapChart("Map", "x", "y", manager);
		chart.update();

		assertArrayEquals(new double[] { 10 }, series(chart, "Dead devices").getXData(), 1e-9);
		assertArrayEquals(new double[] { 10 }, series(chart, "Dead devices").getYData(), 1e-9);
		assertArrayEquals(new double[] { 20 }, series(chart, "Idle devices").getXData(), 1e-9);
		assertArrayEquals(new double[] { 30 }, series(chart, "Active devices").getXData(), 1e-9);
		assertArrayEquals(new double[] { 40 }, series(chart, "Idle Edge data centers").getXData(), 1e-9);
		assertArrayEquals(new double[] { 50 }, series(chart, "Active Edge data centers").getXData(), 1e-9);

		// A second update goes through the existing-series branch.
		chart.update();
		assertArrayEquals(new double[] { 20 }, series(chart, "Idle devices").getXData(), 1e-9);
	}

	@Test
	void edgeArchitecturePlotsOnlyEdgeDataCentersAndEmptyDeviceSeries() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("EDGE", "TRADE_OFF", 5, 0, 0, 0);
		ComputingNodesGenerator generator = VisualizerTestSupport.generator(manager);

		generator.getEdgeOnlyList().add(VisualizerTestSupport.newNode(40, 40));
		VisualizerTestSupport.TestNode activeDataCenter = VisualizerTestSupport.newNode(50, 50);
		activeDataCenter.setIdle(false);
		generator.getEdgeOnlyList().add(activeDataCenter);

		MapChart chart = new MapChart("Map", "x", "y", manager);
		chart.update();

		// No device at all: the empty lists are replaced by the -100 sentinel value.
		assertArrayEquals(new double[] { -100 }, series(chart, "Idle devices").getXData(), 1e-9);
		assertArrayEquals(new double[] { -100 }, series(chart, "Active devices").getXData(), 1e-9);
		assertArrayEquals(new double[] { -100 }, series(chart, "Dead devices").getXData(), 1e-9);
		assertArrayEquals(new double[] { 40 }, series(chart, "Idle Edge data centers").getXData(), 1e-9);
		assertArrayEquals(new double[] { 50 }, series(chart, "Active Edge data centers").getXData(), 1e-9);
	}

	@Test
	void cloudArchitectureDoesNotPlotEdgeDataCenters() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("CLOUD", "TRADE_OFF", 5, 0, 0, 0);
		ComputingNodesGenerator generator = VisualizerTestSupport.generator(manager);

		generator.getMistOnlyList().add(VisualizerTestSupport.newNode(20, 20));
		generator.getEdgeOnlyList().add(VisualizerTestSupport.newNode(40, 40));

		MapChart chart = new MapChart("Map", "x", "y", manager);
		chart.update();

		assertArrayEquals(new double[] { 20 }, series(chart, "Idle devices").getXData(), 1e-9);
		assertFalse(chart.getChart().getSeriesMap().containsKey("Idle Edge data centers"));
		assertFalse(chart.getChart().getSeriesMap().containsKey("Active Edge data centers"));
		assertTrue(chart.getChart().getSeriesMap().containsKey("Dead devices"));
	}
}
