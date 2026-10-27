package com.mechalikh.pureedgesim.simulationvisualizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.knowm.xchart.XYSeries;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

class WanChartTest extends AbstractTest {

	@Test
	void updateKeepsOnlyTheLastDataPointsAndComputesTimeAxis() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 100, 0,
				2_000_000, 1_000_000);
		SimulationParameters.chartsUpdateInterval = 60;

		WanChart chart = new WanChart("Network", "time", "usage", manager);
		for (int i = 0; i < 7; i++) {
			VisualizerTestSupport.setClock(100 + i);
			chart.update();
		}

		// 300 / 60 = 5 data points are retained.
		assertEquals(5, chart.wanUpUsage.size());
		assertEquals(5, chart.wanDownUsage.size());
		assertEquals(2.0, chart.wanUpUsage.get(4), 1e-9);
		assertEquals(1.0, chart.wanDownUsage.get(4), 1e-9);

		XYSeries up = chart.getChart().getSeriesMap().get("WanUp");
		XYSeries down = chart.getChart().getSeriesMap().get("WanDown");
		assertArrayEquals(new double[] { -194, -134, -74, -14, 46 }, up.getXData(), 1e-9);
		assertArrayEquals(new double[] { 2, 2, 2, 2, 2 }, up.getYData(), 1e-9);
		assertArrayEquals(new double[] { 1, 1, 1, 1, 1 }, down.getYData(), 1e-9);
	}

	@Test
	void bandwidthIsConvertedFromBitsPerSecondToMegabits() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 0, 0,
				20_000_000, 5_000_000);
		SimulationParameters.chartsUpdateInterval = 1;

		WanChart chart = new WanChart("Network", "time", "usage", manager);
		chart.update();

		assertEquals(20.0, chart.wanUpUsage.get(0), 1e-9);
		assertEquals(5.0, chart.wanDownUsage.get(0), 1e-9);
	}
}
