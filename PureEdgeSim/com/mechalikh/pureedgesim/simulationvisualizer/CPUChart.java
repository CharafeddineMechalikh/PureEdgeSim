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

	private List<Double> cloudUsage = new ArrayList<>((int) SimulationParameters.SIMULATION_TIME);
	private List<Double> mistUsage = new ArrayList<>((int) SimulationParameters.SIMULATION_TIME);
	private List<Double> edgeUsage = new ArrayList<>((int) SimulationParameters.SIMULATION_TIME);
	private List<Double> currentTime = new ArrayList<>((int) SimulationParameters.SIMULATION_TIME);

	public CPUChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, 0.0, null); 
	}

	public void update() {
		int i=(int) simulationManager.getSimulation().clock();
		currentTime.add((double) i);
		// Add edge devices to map and display their CPU utilization
		edgeDevicesCpuUsage();
		// Add edge data centers to the map and display their CPU utilization
		edgeDataCentersCpuUsage();
		// Display cloud CPU utilization
		cloudCpuUsage();
	}

	private void edgeDevicesCpuUsage() {
		double msUsage = 0;
		int sensors = 0;
		// Browse all devices and create the series
		// Skip the first items (cloud data centers + edge data centers)
		for (ComputingNode device : simulationManager.getDataCentersManager().getEdgeDevicesList()) {
			// If it is an edge device
			msUsage += device.getAvgCpuUtilization();
			if (device.getMipsCapacity() == 0) {
				sensors++;
			}
		}
		// Only if Mist computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("MIST")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
			mistUsage.add(msUsage / (simulationManager.getScenario().getDevicesCount() - sensors));
			updateSeries(getChart(), "Mist", toArray(currentTime), toArray(mistUsage), SeriesMarkers.NONE, Color.BLACK);
		}
	}

	private void edgeDataCentersCpuUsage() {

		double edUsage = 0;
		// Only if Edge computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {

			for (ComputingNode dc : simulationManager.getDataCentersManager().getEdgeDatacenterList()) {
				edUsage += dc.getAvgCpuUtilization();
			}

			edgeUsage.add(edUsage / SimulationParameters.NUM_OF_EDGE_DATACENTERS);
			updateSeries(getChart(), "Edge", toArray(currentTime), toArray(edgeUsage), SeriesMarkers.NONE, Color.BLACK);
		}
	}

	private void cloudCpuUsage() {
		double clUsage = 0;
		for (ComputingNode dc : simulationManager.getDataCentersManager().getCloudDatacentersList()) {
			clUsage = dc.getAvgCpuUtilization();
		}
		cloudUsage.add(clUsage / SimulationParameters.NUM_OF_CLOUD_DATACENTERS);
		updateSeries(getChart(), "Cloud", toArray(currentTime), toArray(cloudUsage), SeriesMarkers.NONE, Color.BLACK);
	}
}
