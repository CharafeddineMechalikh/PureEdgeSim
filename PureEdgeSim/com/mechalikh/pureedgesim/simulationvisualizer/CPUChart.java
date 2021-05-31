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
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.simulationvisualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;

public class CPUChart extends Chart {

	private List<Double> cloudUsage = new ArrayList<>();
	private List<Double> mistUsage = new ArrayList<>();
	private List<Double> edgeUsage = new ArrayList<>();
	private List<Double> currentTime = new ArrayList<>();

	public CPUChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(SimulationParameters.INITIALIZATION_TIME, null, 0.0, null);
	}

	public void update() {
		currentTime.add(simulationManager.getSimulation().clock());
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
		DataCenter device;
		// Browse all devices and create the series
		// Skip the first items (cloud data centers + edge data centers)
		for (int i = SimulationParameters.NUM_OF_EDGE_DATACENTERS
				+ SimulationParameters.NUM_OF_CLOUD_DATACENTERS; i < simulationManager.getDataCentersManager()
						.getDatacenterList().size(); i++) {
			// If it is an edge device
			device = simulationManager.getDataCentersManager().getDatacenterList().get(i);
			msUsage += device.getResources().getAvgCpuUtilization();
			if (device.getResources().getTotalMips() == 0) {
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

			for (int j = SimulationParameters.NUM_OF_CLOUD_DATACENTERS; j < SimulationParameters.NUM_OF_EDGE_DATACENTERS
					+ SimulationParameters.NUM_OF_CLOUD_DATACENTERS; j++) {

				edUsage += simulationManager.getDataCentersManager().getDatacenterList().get(j).getResources()
						.getAvgCpuUtilization();
			}

			edgeUsage.add(edUsage / SimulationParameters.NUM_OF_EDGE_DATACENTERS);
			updateSeries(getChart(), "Edge", toArray(currentTime), toArray(edgeUsage), SeriesMarkers.NONE, Color.BLACK);
		}
	}

	private void cloudCpuUsage() {
		double clUsage = 0;
		for (DataCenter dc : simulationManager.getDataCentersManager().getDatacenterList()) {
			if (dc.getType() == TYPES.CLOUD) {
				clUsage = dc.getResources().getAvgCpuUtilization();

			}
		}
		cloudUsage.add(clUsage / SimulationParameters.NUM_OF_CLOUD_DATACENTERS);
		updateSeries(getChart(), "Cloud", toArray(currentTime), toArray(cloudUsage), SeriesMarkers.NONE, Color.BLACK);
	}
}
