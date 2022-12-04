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
package com.mechalikh.pureedgesim.simulationmanager;

import java.awt.BasicStroke;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

public class ChartsGenerator {

	protected List<String[]> records = new ArrayList<>(50);
	protected String fileName;
	protected String folder;
	protected List<String> energyChartsList = List.of("Energy consumption of computing nodes (Wh)",
			"Average energy consumption (Wh/Computing node)", "Cloud energy consumption (Wh)",
			"Average Cloud energy consumption (Wh/Data center)", "Edge energy consumption (Wh)",
			"Average Edge energy consumption (Wh/Data center)", "Mist energy consumption (Wh)",
			"Average Mist energy consumption (Wh/Device)", "WAN energy consumption (Wh)", "MAN energy consumption (Wh)",
			"LAN energy consumption (Wh)", "WiFi energy consumption (Wh)", "LTE energy consumption (Wh)",
			"Ethernet energy consumption (Wh)");

	protected List<String> cpuChartsList = List.of("Average CPU usage (%)", "Average CPU usage (Cloud) (%)",
			"Average CPU usage (Edge) (%)", "Average CPU usage (Mist) (%)");

	protected List<String> tasksChartsList = List.of("Tasks successfully executed", "Tasks failed (delay)",
			"Tasks failed (device dead)", "Tasks failed (mobility)", "Tasks not generated due to the death of devices",
			"Total tasks executed (Cloud)", "Tasks successfully executed (Cloud)", "Total tasks executed (Edge)",
			"Tasks successfully executed (Edge)", "Total tasks executed (Mist)", "Tasks successfully executed (Mist)");
	protected List<String> delaysChartsList = List.of("Average waiting time (s)", "Average execution delay (s)");

	protected List<String> networkChartsList = List.of("Network usage (s)", "Wan usage (s)", "Containers wan usage (s)",
			"Containers lan usage (s)");

	public ChartsGenerator(String fileName) {
		this.fileName = fileName;
		loadFile();
	}

	protected void loadFile() {
		try (BufferedReader file = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = file.readLine()) != null) {
				records.add(line.split(","));
			} 
		} catch (Exception e) {
			SimLog.println("%s - Problem reading CSV file.",this.getClass().getSimpleName());
		}
	}

	protected int getColumnIndex(String name) {
		for (int j = 0; j < records.get(0).length; j++) {
			if (records.get(0)[j].trim().equals(name.trim())) {
				return j;
			}
		}
		return -1;
	}

	public void displayChart(String y_series, String y_series_label, String folder) {
		this.folder = folder;
		// Create the charts filtered by algorithms (byAlgorithm = true), in order to
		// compare the orchestration algorithms
		generateChart("Edge devices count", y_series, y_series_label, true);
		// Create charts that are filtered by architectures (byAlgorithm = false)
		generateChart("Edge devices count", y_series, y_series_label, false);

	}

	public void generateChart(String x_series, String y_series, String y_series_label, boolean byAlgorithms) {
		XYChart chart;
		for (int i = 0; i < (byAlgorithms ? SimulationParameters.orchestrationAlgorithms.length
				: SimulationParameters.orchestrationArchitectures.length); i++) {
			chart = initChart(x_series, y_series, y_series_label, getArray(byAlgorithms)[i]);
			for (int j = 0; j < (byAlgorithms ? SimulationParameters.orchestrationArchitectures.length
					: SimulationParameters.orchestrationAlgorithms.length); j++) {
				double[] xData = toArray(
						getColumn(x_series, SimulationParameters.orchestrationArchitectures[(byAlgorithms ? j : i)],
								SimulationParameters.orchestrationAlgorithms[(byAlgorithms ? i : j)]));
				double[] yData = toArray(
						getColumn(y_series, SimulationParameters.orchestrationArchitectures[(byAlgorithms ? j : i)],
								SimulationParameters.orchestrationAlgorithms[(byAlgorithms ? i : j)]));

				XYSeries series = chart.addSeries(getArray(!byAlgorithms)[j], xData, yData);
				series.setMarker(SeriesMarkers.CIRCLE); // Marker type: circle,rectangle, diamond..
				series.setLineStyle(new BasicStroke());
			}
			// Save the chart
			saveBitmap(chart, (byAlgorithms ? "Architectures" : "Algorithms") + folder + "/",
					y_series + "__" + getArray(byAlgorithms)[i]);
		}
	}

	protected String[] getArray(boolean byAlgorithms) {
		return (byAlgorithms ? SimulationParameters.orchestrationAlgorithms
				: SimulationParameters.orchestrationArchitectures);
	}

	protected XYChart initChart(String x_series, String y_series, String y_series_label, String title) {
		XYChart chart = new XYChartBuilder().height(400).width(600).theme(ChartTheme.Matlab).xAxisTitle(x_series)
				.yAxisTitle(y_series_label).build();
		chart.setTitle(y_series + " (" + title + ")");
		chart.getStyler().setLegendVisible(true);
		return chart;
	}

	protected void saveBitmap(XYChart chart, String folder, String name) {
		try {
			File file = new File(new File(fileName).getParent() + "/Final results/" + folder);
			file.mkdirs();
			BitmapEncoder.saveBitmapWithDPI(chart, file.getPath() + "/" + name.replace("/", " per "), BitmapFormat.PNG,
					300);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected List<Double> getColumn(String name, String orch, String alg) {
		List<Double> list = new ArrayList<>();
		int column = getColumnIndex(name);
		for (int line = 1; line < records.size(); line++) {
			if (records.get(line)[0].trim().equals(orch.trim()) && records.get(line)[1].trim().equals(alg.trim())) {
				list.add(Double.parseDouble(records.get(line)[column]));
			}
		}
		return list;
	}

	protected double[] toArray(List<Double> list) {
		double[] results = new double[list.size()];
		for (int i = 0; i < list.size(); i++)
			results[i] = list.get(i);
		return results;
	}

	public void generate() {
		generateTasksCharts();
		generateNetworkCharts();
		generateCpuCharts();
		generateEnergyCharts();
	}

	protected void generateEnergyCharts() {
		for (String value : energyChartsList)
			displayChart(value, "Consumed energy (Wh)", "/Energy");
	}

	protected void generateCpuCharts() {
		for (String value : cpuChartsList)
			displayChart(value, "CPU utilization (%)", "/CPU Utilization");
	}

	protected void generateNetworkCharts() {
		for (String value : networkChartsList)
			displayChart(value, "Utilization (s)", "/Network");
	}

	protected void generateTasksCharts() {
		for (String value : tasksChartsList)
			displayChart(value, "Number of tasks", "/Tasks");

		for (String value : delaysChartsList)
			displayChart(value, "Time (s)", "/Tasks");
	}

}
