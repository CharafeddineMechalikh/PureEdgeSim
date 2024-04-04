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

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * This class represents a chart of failed tasks over time. It extends the Chart
 * class.
 */
public class TasksChart extends Chart {

	/**
	 * tasksFailedList is used to store the number of failed tasks for each time
	 * period. It is initialized as an empty ArrayList
	 */
	protected List<Double> tasksFailedList = new ArrayList<>();

	/**
	 * Constructs a TasksChart object with the specified title, x-axis title, y-axis
	 * title, and SimulationManager. The default series render style is set to
	 * XYSeriesRenderStyle.Line, and the initial chart size is set to (0, 100).
	 *
	 * @param title             - the title of the chart
	 * @param xAxisTitle        - the title of the x-axis
	 * @param yAxisTitle        - the title of the y-axis
	 * @param simulationManager - the SimulationManager object
	 */
	public TasksChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, null, 100.0);
	}

	/**
	 * Updates the chart with the latest number of failed tasks. If the clock in
	 * minutes has changed, the number of failed tasks is calculated, and the new
	 * data point is added to the tasksFailedList. The chart is then updated with
	 * the new data point.
	 *
	 * This method has a time complexity of O(n), where n is the number of time
	 * periods.
	 */
	public void update() {
		int currentClock = (int) simulationManager.getSimulation().clockInMinutes();
		if (currentClock != clock) {
			clock = currentClock;
			double tasksFailed = 100 - simulationManager.getFailureRate();
			double[] time = new double[clock + 1];
			for (int i = 0; i <= clock; i++)
				time[i] = i;
			tasksFailedList.add(tasksFailed);
			updateSeries(getChart(), "Failed tasks", time, toArray(tasksFailedList), SeriesMarkers.NONE, Color.BLACK);
		}
	}
}
