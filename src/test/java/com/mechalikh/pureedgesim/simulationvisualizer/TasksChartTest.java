package com.mechalikh.pureedgesim.simulationvisualizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.knowm.xchart.XYSeries;

import com.mechalikh.pureedgesim.AbstractTest;

class TasksChartTest extends AbstractTest {

	@Test
	void updateAddsOnePointPerMinuteAndSkipsUnchangedClock() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 30, 25, 0, 0);

		TasksChart chart = new TasksChart("Tasks", "time", "success", manager);
		chart.update();

		assertEquals(1, chart.tasksFailedList.size());
		assertEquals(75.0, chart.tasksFailedList.get(0), 1e-9);
		assertArrayEquals(new double[] { 0 }, chart.getChart().getSeriesMap().get("Failed tasks").getXData(), 1e-9);

		VisualizerTestSupport.setClock(90);
		chart.update();

		assertEquals(2, chart.tasksFailedList.size());
		assertEquals(75.0, chart.tasksFailedList.get(1), 1e-9);
		XYSeries series = chart.getChart().getSeriesMap().get("Failed tasks");
		assertArrayEquals(new double[] { 0, 1 }, series.getXData(), 1e-9);
		assertArrayEquals(new double[] { 75, 75 }, series.getYData(), 1e-9);

		// Same minute: the chart must not change.
		chart.update();
		assertEquals(2, chart.tasksFailedList.size());
	}

	@Test
	void failureRateIsConvertedToSuccessRate() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 0, 100, 0, 0);

		TasksChart chart = new TasksChart("Tasks", "time", "success", manager);
		chart.update();

		assertEquals(0.0, chart.tasksFailedList.get(0), 1e-9);
		assertTrue(chart.getChart().getSeriesMap().containsKey("Failed tasks"));
	}
}
