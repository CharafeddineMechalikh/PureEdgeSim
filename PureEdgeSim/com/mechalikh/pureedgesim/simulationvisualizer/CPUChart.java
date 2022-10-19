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

public class CPUChart extends Chart {
	protected List<Double> cloudUsage = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	protected List<Double> mistUsage = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	protected List<Double> edgeUsage = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	protected List<Double> currentTime = new ArrayList<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));

	public CPUChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, 0.0, null);
	}

	public void update() {
		int i = (int) simulationManager.getSimulation().clock();
		currentTime.add((double) i);
		// Display edge devices CPU utilization
		edgeDevicesCpuUsage();
		// Display edge data centers CPU utilization
		edgeDataCentersCpuUsage();
		// Display cloud CPU utilization
		cloudCpuUsage();
	}

	protected void edgeDevicesCpuUsage() {
		// Only if Mist computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("MIST")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
			double msUsage = getAvgCpuUtilization(
					computingNodesGenerator.getMistOnlyListSensorsExcluded());
			mistUsage.add(msUsage / computingNodesGenerator.getMistOnlyListSensorsExcluded().size());
			updateSeries(getChart(), "Mist", toArray(currentTime), toArray(mistUsage), SeriesMarkers.NONE, Color.BLACK);
		}
	}

	protected void edgeDataCentersCpuUsage() {
		// Only if Edge computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
			double edUsage = getAvgCpuUtilization(computingNodesGenerator.getEdgeOnlyList());
			edgeUsage.add(edUsage / SimulationParameters.numberOfEdgeDataCenters);
			updateSeries(getChart(), "Edge", toArray(currentTime), toArray(edgeUsage), SeriesMarkers.NONE, Color.BLACK);
		}
	}

	protected void cloudCpuUsage() {
		double clUsage = getAvgCpuUtilization(computingNodesGenerator.getCloudOnlyList());
		cloudUsage.add(clUsage / SimulationParameters.numberOfCloudDataCenters);
		updateSeries(getChart(), "Cloud", toArray(currentTime), toArray(cloudUsage), SeriesMarkers.NONE, Color.BLACK);
	}

	protected double getAvgCpuUtilization(List<ComputingNode> nodeList) {
		double utilization = 0;
		for (ComputingNode node : nodeList) {
			utilization += node.getAvgCpuUtilization();
		}
		return utilization;
	}
}
