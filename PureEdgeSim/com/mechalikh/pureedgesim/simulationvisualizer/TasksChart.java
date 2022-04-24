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

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class TasksChart extends Chart {

	private List<Double> tasksFailedList = new ArrayList<>();
	public TasksChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, null, 100.0);
	}

	public void update() {
		if (((int) simulationManager.getSimulation().clockInMinutes()) != clock) {
			clock = (int) simulationManager.getSimulation().clockInMinutes();
			double tasksFailed = 100 - simulationManager.getFailureRate();
			double[] time = new double[clock + 1];
			for (int i = 0; i <= clock; i++)
				time[i] = i;
			tasksFailedList.add(tasksFailed);
			updateSeries(getChart(), "Failed tasks", time, toArray(tasksFailedList),
					SeriesMarkers.NONE, Color.BLACK);
		}
	}
}
