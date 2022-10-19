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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager; 

public class SimulationVisualizer {
	protected JFrame simulationResultsFrame;
	protected SimulationManager simulationManager;
	protected List<Chart> charts = new ArrayList<Chart>(4);
	protected boolean firstTime = true;

	public SimulationVisualizer(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
		Chart mapChart = new MapChart("Simulation map", "Width (meters)", "Length (meters)", simulationManager);
		Chart cpuUtilizationChart = new CPUChart("CPU utilization", "Time (s)", "Utilization (%)", simulationManager);
		Chart tasksSuccessChart = new TasksChart("Tasks success rate", "Time (minutes)", "Success rate (%)",
				simulationManager);
		charts.addAll(List.of(mapChart, cpuUtilizationChart, tasksSuccessChart));

		if (SimulationParameters.useOneSharedWanLink) {
			Chart networkUtilizationChart = new WanChart("Network utilization", "Time (s)", "Utilization (Mbps)",
					simulationManager);
			charts.add(networkUtilizationChart);
		}
	}

	public void updateCharts() {
		if (firstTime) {
			SwingWrapper<XYChart> swingWrapper = new SwingWrapper<>(
					charts.stream().map(Chart::getChart).collect(Collectors.toList()));
			simulationResultsFrame = swingWrapper.displayChartMatrix(); // Display charts
			simulationResultsFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		}
		firstTime = false;
		repaint();

		// Display simulation time
		double time = simulationManager.getSimulation().clock();
		simulationResultsFrame.setTitle("Simulation time = " + ((int) time / 60) + " min : " + ((int) time % 60)
				+ " seconds  -  number of edge devices = " + simulationManager.getScenario().getDevicesCount()
				+ " -  Architecture = " + simulationManager.getScenario().getStringOrchArchitecture()
				+ " -  Algorithm = " + simulationManager.getScenario().getStringOrchAlgorithm());
	}

	protected void repaint() {
		charts.forEach(chart -> chart.update());
		simulationResultsFrame.repaint();
	}

	public void close() {
		simulationResultsFrame.dispose();
	}

	public void saveCharts() throws IOException {
		String folderName = SimulationParameters.outputFolder + "/"
				+ simulationManager.getSimulationLogger().getSimStartTime() + "/simulation_"
				+ simulationManager.getSimulationId() + "/iteration_" + simulationManager.getIteration() + "__"
				+ simulationManager.getScenario().toString();
		new File(folderName).mkdirs();
		BitmapEncoder.saveBitmapWithDPI(charts.get(0).getChart(), folderName + "/map_chart", BitmapFormat.PNG, 300);
		BitmapEncoder.saveBitmapWithDPI(charts.get(1).getChart(), folderName + "/cpu_usage", BitmapFormat.PNG, 300);
		BitmapEncoder.saveBitmapWithDPI(charts.get(2).getChart(), folderName + "/tasks_success_rate", BitmapFormat.PNG,
				300);
		BitmapEncoder.saveBitmapWithDPI(charts.get(3).getChart(), folderName + "/network_usage", BitmapFormat.PNG, 300);

	}

}
