/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 *     @since PureEdgeSim 2.0
 **/
package com.mechalikh.pureedgesim.simulationvisualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Represents a chart showing CPU usage over time for different computing
 * resources.
 */
public class CPUChart extends Chart {
	// Lists to store CPU usage and time
	protected List<Double> cloudUsage = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	protected List<Double> mistUsage = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	protected List<Double> edgeUsage = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	protected List<Double> currentTime = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));

	/**
	 * Constructs a CPUChart object.
	 *
	 * @param title             the title of the chart
	 * @param xAxisTitle        the title of the x-axis
	 * @param yAxisTitle        the title of the y-axis
	 * @param simulationManager the simulation manager to get data from
	 */
	public CPUChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, 0.0, null);
	}

	/**
	 * Updates the chart with the latest CPU usage data.
	 */
	public void update() {
		int currentTimeValue = (int) simulationManager.getSimulation().clock();
		currentTime.add((double) currentTimeValue);
		edgeDevicesCpuUsage();
		edgeDataCentersCpuUsage();
		cloudCpuUsage();
	}

	/**
	 * Calculates and adds the average CPU usage for edge devices to the chart.
	 */
	protected void edgeDevicesCpuUsage() {
		// Only if Mist computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("MIST")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
			List<ComputingNode> mistOnlyList = computingNodesGenerator.getMistOnlyListSensorsExcluded();
			double mistUsageValue = mistOnlyList.stream().mapToDouble(ComputingNode::getAvgCpuUtilization).average()
					.orElse(0.0);
			mistUsage.add(mistUsageValue);
			updateSeries(getChart(), "Mist", toArray(currentTime), toArray(mistUsage), SeriesMarkers.NONE, Color.BLACK);
		}
	}

	/**
	 * Calculates and adds the average CPU usage for edge data centers to the chart.
	 */
	protected void edgeDataCentersCpuUsage() {
		// Only if Edge computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
			List<ComputingNode> edgeOnlyList = computingNodesGenerator.getEdgeOnlyList();
			double edgeUsageValue = edgeOnlyList.stream().mapToDouble(ComputingNode::getAvgCpuUtilization).average()
					.orElse(0.0);
			edgeUsage.add(edgeUsageValue);
			updateSeries(getChart(), "Edge", toArray(currentTime), toArray(edgeUsage), SeriesMarkers.NONE, Color.BLACK);
		}
	}

	/**
	 * Calculates and adds the average CPU usage for cloud data centers to the
	 * chart.
	 */
	protected void cloudCpuUsage() {
		List<ComputingNode> cloudOnlyList = computingNodesGenerator.getCloudOnlyList();
		double cloudUsageValue = cloudOnlyList.parallelStream().mapToDouble(ComputingNode::getAvgCpuUtilization).sum();
		cloudUsage.add(cloudUsageValue);
		updateSeries(getChart(), "Cloud", toArray(currentTime), toArray(cloudUsage), SeriesMarkers.NONE, Color.BLACK);
	}

}
