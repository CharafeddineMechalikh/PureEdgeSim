package com.mechalikh.pureedgesim.simulationvisualizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;

class CPUChartTest extends AbstractTest {

	@Test
	void allArchitectureComputesMistEdgeAndCloudSeries() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 10, 0, 0, 0);
		ComputingNodesGenerator generator = VisualizerTestSupport.generator(manager);
		generator.getMistOnlyListSensorsExcluded().add(VisualizerTestSupport.newNode(10).cpu(10));
		generator.getEdgeOnlyList().add(VisualizerTestSupport.newNode(20).cpu(20));
		generator.getCloudOnlyList().add(VisualizerTestSupport.newNode(30).cpu(30));

		CPUChart chart = new CPUChart("CPU", "time", "usage", manager);
		chart.update();

		assertEquals(1, chart.currentTime.size());
		assertEquals(10.0, chart.currentTime.get(0), 1e-9);
		assertEquals(10.0, chart.mistUsage.get(0), 1e-9);
		assertEquals(20.0, chart.edgeUsage.get(0), 1e-9);
		assertEquals(30.0, chart.cloudUsage.get(0), 1e-9);
		assertTrue(chart.getChart().getSeriesMap().containsKey("Mist"));
		assertTrue(chart.getChart().getSeriesMap().containsKey("Edge"));
		assertTrue(chart.getChart().getSeriesMap().containsKey("Cloud"));

		// Second update exercises the existing-series branch of Chart.updateSeries.
		VisualizerTestSupport.setClock(20);
		chart.update();
		assertEquals(2, chart.currentTime.size());
		assertEquals(20.0, chart.currentTime.get(1), 1e-9);
		assertEquals(2, chart.getChart().getSeriesMap().get("Cloud").getYData().length);
	}

	@Test
	void mistArchitectureDoesNotComputeEdgeUsage() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("MIST", "TRADE_OFF", 5, 0, 0, 0);
		ComputingNodesGenerator generator = VisualizerTestSupport.generator(manager);
		generator.getMistOnlyListSensorsExcluded().add(VisualizerTestSupport.newNode(0).cpu(40));
		generator.getEdgeOnlyList().add(VisualizerTestSupport.newNode(0).cpu(50));

		CPUChart chart = new CPUChart("CPU", "time", "usage", manager);
		chart.update();

		assertEquals(1, chart.mistUsage.size());
		assertEquals(40.0, chart.mistUsage.get(0), 1e-9);
		assertEquals(0, chart.edgeUsage.size());
		assertTrue(chart.getChart().getSeriesMap().containsKey("Mist"));
		assertFalse(chart.getChart().getSeriesMap().containsKey("Edge"));
	}

	@Test
	void edgeArchitectureDoesNotComputeMistUsage() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("EDGE", "TRADE_OFF", 5, 0, 0, 0);
		ComputingNodesGenerator generator = VisualizerTestSupport.generator(manager);
		generator.getMistOnlyListSensorsExcluded().add(VisualizerTestSupport.newNode(0).cpu(40));
		generator.getEdgeOnlyList().add(VisualizerTestSupport.newNode(0).cpu(50));

		CPUChart chart = new CPUChart("CPU", "time", "usage", manager);
		chart.update();

		assertEquals(0, chart.mistUsage.size());
		assertEquals(1, chart.edgeUsage.size());
		assertEquals(50.0, chart.edgeUsage.get(0), 1e-9);
		assertFalse(chart.getChart().getSeriesMap().containsKey("Mist"));
		assertTrue(chart.getChart().getSeriesMap().containsKey("Edge"));
	}

	@Test
	void cloudArchitectureWithEmptyNodeListsUsesZeroAverages() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("CLOUD", "TRADE_OFF", 5, 0, 0, 0);

		CPUChart chart = new CPUChart("CPU", "time", "usage", manager);
		chart.update();

		assertEquals(0, chart.mistUsage.size());
		assertEquals(0, chart.edgeUsage.size());
		assertEquals(1, chart.cloudUsage.size());
		assertEquals(0.0, chart.cloudUsage.get(0), 1e-9);
		assertEquals(1, chart.getChart().getSeriesMap().size());
		assertTrue(chart.getChart().getSeriesMap().containsKey("Cloud"));
	}

	@Test
	void allArchitectureWithEmptyListsUsesZeroAverages() {
		BootstrapSimulationManager manager = VisualizerTestSupport.configure("ALL", "TRADE_OFF", 5, 0, 0, 0);

		CPUChart chart = new CPUChart("CPU", "time", "usage", manager);
		chart.update();

		assertEquals(0.0, chart.mistUsage.get(0), 1e-9);
		assertEquals(0.0, chart.edgeUsage.get(0), 1e-9);
		assertEquals(0.0, chart.cloudUsage.get(0), 1e-9);
	}
}
