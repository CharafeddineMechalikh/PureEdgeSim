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

	private List<String[]> records = new ArrayList<>(50);
	private String fileName;
	private String folder;
	private List<String[]> energyChartsList = List.of(
			new String[] { "Energy consumption of computing nodes (Wh)", "Consumed energy (Wh)" },
			new String[] { "Average energy consumption (Wh/Computing node)", "Consumed energy (Wh)" },
			new String[] { "Cloud energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "Average Cloud energy consumption (Wh/Data center)", "Consumed energy (Wh)" },
			new String[] { "Edge energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "Average Edge energy consumption (Wh/Data center)", "Consumed energy (Wh)", },
			new String[] { "Mist energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "Average Mist energy consumption (Wh/Device)", "Consumed energy (Wh)" },
			new String[] { "WAN energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "MAN energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "LAN energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "WiFi energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "LTE energy consumption (Wh)", "Consumed energy (Wh)" },
			new String[] { "Ethernet energy consumption (Wh)", "Consumed energy (Wh)" },
            new String[] { "Dead devices count", "Count" },
			new String[] { "Average remaining power (Wh)", "Remaining energy (Wh)" },
			new String[] { "Average remaining power (%)", "Remaining energy (%)" },
			new String[] { "First edge device death time (s)", "Time (s)" });

	private List<String[]> cpuChartsList = List.of(new String[] { "Average CPU usage (%)", "CPU utilization (%)" },
			new String[] { "Average CPU usage (Cloud) (%)", "CPU utilization (%)" },
			new String[] { "Average CPU usage (Edge) (%)", "CPU utilization (%)" },
			new String[] { "Average CPU usage (Mist) (%)", "CPU utilization (%)" });

	private List<String[]> tasksChartsList = List.of(new String[] { "Tasks successfully executed", "Number of tasks" },
			new String[] { "Tasks failed (delay)", "Number of tasks" },
			new String[] { "Tasks failed (device dead)", "Number of tasks" },
			new String[] { "Tasks failed (mobility)", "Number of tasks" },
			new String[] { "Tasks not generated due to the death of devices", "Number of tasks" },
			new String[] { "Total tasks executed (Cloud)", "Number of tasks" },
			new String[] { "Tasks successfully executed (Cloud)", "Number of tasks" },
			new String[] { "Total tasks executed (Edge)", "Number of tasks" },
			new String[] { "Tasks successfully executed (Edge)", "Number of tasks" },
			new String[] { "Total tasks executed (Mist)", "Number of tasks" },
			new String[] { "Tasks successfully executed (Mist)", "Number of tasks" },
			new String[] { "Average waiting time (s)", "Time (s)" },
			new String[] { "Average execution delay (s)", "Time (s)" });

	private List<String[]> networkChartsList = List.of(new String[] { "Network usage (s)", "Time (s)" },
			new String[] { "Wan usage (s)", "Time (s)" },
			new String[] { "Average bandwidth per task (Mbps)", "Bandwidth (Mbps)" },
			new String[] { "Containers wan usage (s)", "Time (s)" },
			new String[] { "Containers lan usage (s)", "Time (s)" });

	public ChartsGenerator(String fileName) {
		this.fileName = fileName;
		loadFile();
	}

	private void loadFile() {
		try {
			BufferedReader file = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = file.readLine()) != null) {
				records.add(line.split(","));
			}
			file.close();
		} catch (Exception e) {
			SimLog.println(this.getClass().getSimpleName() + " - Problem reading CSV file.");
		}
	}

	private int getColumnIndex(String name) {
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
		for (int i = 0; i < (byAlgorithms ? SimulationParameters.ORCHESTRATION_AlGORITHMS.length
				: SimulationParameters.ORCHESTRATION_ARCHITECTURES.length); i++) {
			chart = initChart(x_series, y_series, y_series_label, getArray(byAlgorithms)[i]);
			for (int j = 0; j < (byAlgorithms ? SimulationParameters.ORCHESTRATION_ARCHITECTURES.length
					: SimulationParameters.ORCHESTRATION_AlGORITHMS.length); j++) {
				double[] xData = toArray(
						getColumn(x_series, SimulationParameters.ORCHESTRATION_ARCHITECTURES[(byAlgorithms ? j : i)],
								SimulationParameters.ORCHESTRATION_AlGORITHMS[(byAlgorithms ? i : j)]));
				double[] yData = toArray(
						getColumn(y_series, SimulationParameters.ORCHESTRATION_ARCHITECTURES[(byAlgorithms ? j : i)],
								SimulationParameters.ORCHESTRATION_AlGORITHMS[(byAlgorithms ? i : j)]));

				XYSeries series = chart.addSeries(getArray(!byAlgorithms)[j], xData, yData);
				series.setMarker(SeriesMarkers.CIRCLE); // Marker type: circle,rectangle, diamond..
				series.setLineStyle(new BasicStroke());
			}
			// Save the chart
			saveBitmap(chart, (byAlgorithms ? "Architectures" : "Algorithms") + folder + "/",
					y_series + "__" + getArray(byAlgorithms)[i]);
		}
	}

	private String[] getArray(boolean byAlgorithms) {
		return (byAlgorithms ? SimulationParameters.ORCHESTRATION_AlGORITHMS
				: SimulationParameters.ORCHESTRATION_ARCHITECTURES);
	}

	private XYChart initChart(String x_series, String y_series, String y_series_label, String title) {
		XYChart chart = new XYChartBuilder().height(400).width(600).theme(ChartTheme.Matlab).xAxisTitle(x_series)
				.yAxisTitle(y_series_label).build();
		chart.setTitle(y_series + " (" + title + ")");
		chart.getStyler().setLegendVisible(true);
		return chart;
	}

	private void saveBitmap(XYChart chart, String folder, String name) {
		try {
			File file = new File(new File(fileName).getParent() + "/Final results/" + folder);
			file.mkdirs();
			BitmapEncoder.saveBitmapWithDPI(chart, file.getPath() + "/" + name.replace("/", " per "), BitmapFormat.PNG,
					300);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private List<Double> getColumn(String name, String orch, String alg) {
		List<Double> list = new ArrayList<>();
		int column = getColumnIndex(name);
		for (int line = 1; line < records.size(); line++) {
			if (records.get(line)[0].trim().equals(orch.trim()) && records.get(line)[1].trim().equals(alg.trim())) {
				list.add(Double.parseDouble(records.get(line)[column]));
			}
		}
		return list;
	}

	private double[] toArray(List<Double> list) {
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

	private void generateEnergyCharts() {
		for (String[] values : energyChartsList)
			displayChart(values[0], values[1], "/Energy");
	}

	private void generateCpuCharts() {
		for (String[] values : cpuChartsList)
			displayChart(values[0], values[1], "/CPU Utilization");
	}

	private void generateNetworkCharts() {
		for (String[] values : networkChartsList)
			displayChart(values[0], values[1], "/Network");
	}

	private void generateTasksCharts() {
		for (String[] values : tasksChartsList)
			displayChart(values[0], values[1], "/Tasks");
	}

}
