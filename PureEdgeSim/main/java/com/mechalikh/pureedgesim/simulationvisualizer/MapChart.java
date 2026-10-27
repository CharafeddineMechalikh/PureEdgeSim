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

/**
 * 
 * This class represents a Map Chart that displays the current state of the
 * simulation in a scatter plot.
 * 
 * It extends the Chart class and implements methods to update the chart with
 * information about edge devices,
 * 
 * edge data centers and cloud CPU utilization.
 */
public class MapChart extends Chart {

	/**
	 * 
	 * Constructor for MapChart. Initializes the chart with the given title, x and y
	 * axis titles and the SimulationManager. Sets the default series render style
	 * to Scatter and the marker size to 4. Calls the updateSize method to set the
	 * chart size based on the simulation map dimensions.
	 * 
	 * @param title             the title of the chart
	 * @param xAxisTitle        the title of the x axis
	 * @param yAxisTitle        the title of the y axis
	 * @param simulationManager the SimulationManager instance
	 */
	public MapChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
		getChart().getStyler().setMarkerSize(4);
		updateSize(0.0, (double) SimulationParameters.simulationMapWidth, 0.0,
				(double) SimulationParameters.simulationMapLength);
	}

	/**
	 * Updates the map with the current edge devices and their status.
	 */
	protected void updateEdgeDevices() {
		List<Double> xDeadDevices = new ArrayList<>();
		List<Double> yDeadDevices = new ArrayList<>();
		List<Double> xIdleDevices = new ArrayList<>();
		List<Double> yIdleDevices = new ArrayList<>();
		List<Double> xActiveDevices = new ArrayList<>();
		List<Double> yActiveDevices = new ArrayList<>();

		for (ComputingNode node : computingNodesGenerator.getMistOnlyList()) {
			ComputingNode device = node;
			double xPos = device.getMobilityModel().getCurrentLocation().getXPos();
			double yPos = device.getMobilityModel().getCurrentLocation().getYPos();

			if (device.isDead()) {
				xDeadDevices.add(xPos);
				yDeadDevices.add(yPos);
			} else if (device.isIdle()) {
				xIdleDevices.add(xPos);
				yIdleDevices.add(yPos);
			} else {
				xActiveDevices.add(xPos);
				yActiveDevices.add(yPos);
			}
		}

		updateSeries(getChart(), "Idle devices", toArray(xIdleDevices), toArray(yIdleDevices), SeriesMarkers.CIRCLE,
				Color.blue);
		updateSeries(getChart(), "Active devices", toArray(xActiveDevices), toArray(yActiveDevices),
				SeriesMarkers.CIRCLE, Color.red);
		updateSeries(getChart(), "Dead devices", toArray(xDeadDevices), toArray(yDeadDevices), SeriesMarkers.CIRCLE,
				Color.LIGHT_GRAY);
	}

	/**
	 * 
	 * Updates the map with information about edge devices, edge data centers and
	 * cloud CPU utilization.
	 */
	public void update() {
		// Add edge devices to map and display their CPU utilization
		updateEdgeDevices();
		// Add edge data centers to the map and display their CPU utilization
		updateEdgeDataCenters();
	}
	

	/**
	 * Updates the map with the current edge data centers and their status.
	 */
	protected void updateEdgeDataCenters() {
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
