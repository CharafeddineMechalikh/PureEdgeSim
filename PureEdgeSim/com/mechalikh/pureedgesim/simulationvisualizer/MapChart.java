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

public class MapChart extends Chart {

	public MapChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
		getChart().getStyler().setMarkerSize(4);
		updateSize(0.0, (double) SimulationParameters.simulationMapWidth, 0.0,
				(double) SimulationParameters.simulationMapLength);

	}

	protected void edgeDevices() {
		// Initialize the X and Y series that will be used to draw the map
		// Dead devices series
		List<Double> x_deadEdgeDevicesList = new ArrayList<>();
		List<Double> y_deadEdgeDevicesList = new ArrayList<>();
		// Idle devices series
		List<Double> x_idleEdgeDevicesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		List<Double> y_idleEdgeDevicesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		// Active devices series
		List<Double> x_activeEdgeDevicesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		List<Double> y_activeEdgeDevicesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		// Browse all devices and create the series
		for (ComputingNode node :computingNodesGenerator.getMistOnlyList()) {
			ComputingNode device = node;
			double Xpos = device.getMobilityModel().getCurrentLocation().getXPos();
			double Ypos = device.getMobilityModel().getCurrentLocation().getYPos();
			if (device.isDead()) {
				x_deadEdgeDevicesList.add(Xpos);
				y_deadEdgeDevicesList.add(Ypos);
			} else if (device.isIdle()) {
				x_idleEdgeDevicesList.add(Xpos);
				y_idleEdgeDevicesList.add(Ypos);
			} else { // If the device is busy
				x_activeEdgeDevicesList.add(Xpos);
				y_activeEdgeDevicesList.add(Ypos);
			}
		}
		updateSeries(getChart(), "Idle devices", toArray(x_idleEdgeDevicesList), toArray(y_idleEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.blue);

		updateSeries(getChart(), "Active devices", toArray(x_activeEdgeDevicesList), toArray(y_activeEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.red);

		updateSeries(getChart(), "Dead devices", toArray(x_deadEdgeDevicesList), toArray(y_deadEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.LIGHT_GRAY);
	}

	public void update() {
		// Add edge devices to map and display their CPU utilization
		edgeDevices();
		// Add edge data centers to the map and display their CPU utilization
		edgeDataCenters();
		// Display cloud CPU utilization
	}

	protected void edgeDataCenters() {
		// Only if Edge computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
			// List of idle servers
			List<Double> x_idleEdgeDataCentersList = new ArrayList<>();
			List<Double> y_idleEdgeDataCentersList = new ArrayList<>();
			// List of active servers
			List<Double> x_activeEdgeDataCentersList = new ArrayList<>();
			List<Double> y_activeEdgeDataCentersList = new ArrayList<>();

			for (ComputingNode node : computingNodesGenerator.getEdgeOnlyList()) {
				ComputingNode edgeDataCenter = node;
				double Xpos = edgeDataCenter.getMobilityModel().getCurrentLocation().getXPos();
				double Ypos = edgeDataCenter.getMobilityModel().getCurrentLocation().getYPos();
				if (edgeDataCenter.isIdle()) {
					x_idleEdgeDataCentersList.add(Xpos);
					y_idleEdgeDataCentersList.add(Ypos);
				} else {
					x_activeEdgeDataCentersList.add(Xpos);
					y_activeEdgeDataCentersList.add(Ypos);

				}
			}

			updateSeries(getChart(), "Idle Edge data centers", toArray(x_idleEdgeDataCentersList),
					toArray(y_idleEdgeDataCentersList), SeriesMarkers.CROSS, Color.BLACK);

			updateSeries(getChart(), "Active Edge data centers", toArray(x_activeEdgeDataCentersList),
					toArray(y_activeEdgeDataCentersList), SeriesMarkers.CROSS, Color.red);

		}
	}
}
