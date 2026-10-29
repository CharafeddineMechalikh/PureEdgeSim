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

/**
 * Generates the aggregate result charts from a sequential simulation CSV file.
 * Each metric is drawn twice: once with one chart per orchestration algorithm
 * (architectures as series) and once with one chart per architecture
 * (algorithms as series). The images are written under
 * {@code Final results/} next to the CSV file.
 */
public class ChartsGenerator {

	/** CSV rows, including the header row at index 0. */
	protected List<String[]> records = new ArrayList<>(50);
	/** Path to the CSV file that provides the data. */
	protected String fileName;
	/** Output subfolder suffix, for example {@code /Energy}. */
	protected String folder;
	/** CSV columns plotted as energy charts. */
	protected List<String> energyChartsList = List.of("Energy consumption of computing nodes (Wh)",
			"Average energy consumption (Wh/Computing node)", "Cloud energy consumption (Wh)",
			"Average Cloud energy consumption (Wh/Data center)", "Edge energy consumption (Wh)",
			"Average Edge energy consumption (Wh/Data center)", "Mist energy consumption (Wh)",
			"Average Mist energy consumption (Wh/Device)", "WAN energy consumption (Wh)", "MAN energy consumption (Wh)",
			"LAN energy consumption (Wh)", "WiFi energy consumption (Wh)", "Cellular energy consumption (Wh)",
			"Ethernet energy consumption (Wh)");

	/** CSV columns plotted as CPU utilization charts. */
	protected List<String> cpuChartsList = List.of("Average CPU usage (%)", "Average CPU usage (Cloud) (%)",
			"Average CPU usage (Edge) (%)", "Average CPU usage (Mist) (%)");

	/** CSV columns plotted as task outcome charts. */
	protected List<String> tasksChartsList = List.of("Tasks successfully executed", "Tasks failed (delay)",
			"Tasks failed (device dead)", "Tasks failed (mobility)", "Tasks not generated due to the death of devices",
			"Total tasks executed (Cloud)", "Tasks successfully executed (Cloud)", "Total tasks executed (Edge)",
			"Tasks successfully executed (Edge)", "Total tasks executed (Mist)", "Tasks successfully executed (Mist)");
	/** CSV columns plotted as delay charts. */
	protected List<String> delaysChartsList = List.of("Average waiting time (s)", "Average execution delay (s)");

	/** CSV columns plotted as network usage charts. */
	protected List<String> networkChartsList = List.of("Network usage (s)", "Wan usage (s)", "Containers wan usage (s)",
			"Containers lan usage (s)");

	/**
	 * Creates a generator and loads the CSV file.
	 *
	 * @param fileName path to the simulation CSV file.
	 */
	public ChartsGenerator(String fileName) {
		this.fileName = fileName;
		loadFile();
	}

	/** Reads the CSV file into {@link #records}. */
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

	/**
	 * Returns the index of a column in the CSV header.
	 *
	 * @param name the column name.
	 * @return the zero-based column index, or -1 if the column does not exist.
	 */
	protected int getColumnIndex(String name) {
		for (int j = 0; j < records.get(0).length; j++) {
			if (records.get(0)[j].trim().equals(name.trim())) {
				return j;
			}
		}
		return -1;
	}

	/**
	 * Generates both variants of a metric chart: one grouped by algorithm and one
	 * grouped by architecture.
	 *
	 * @param y_series       the CSV column to plot.
	 * @param y_series_label the Y axis label.
	 * @param folder         the output subfolder suffix.
	 */
	public void displayChart(String y_series, String y_series_label, String folder) {
		this.folder = folder;
		// Create the charts filtered by algorithms (byAlgorithm = true), in order to
		// compare the orchestration algorithms
		generateChart("Edge devices count", y_series, y_series_label, true);
		// Create charts that are filtered by architectures (byAlgorithm = false)
		generateChart("Edge devices count", y_series, y_series_label, false);

	}

	/**
	 * Generates the charts for one metric.
	 *
	 * @param x_series       the CSV column used for the X axis.
	 * @param y_series       the CSV column used for the Y axis.
	 * @param y_series_label the Y axis label.
	 * @param byAlgorithms   {@code true} to create one chart per algorithm with
	 *                       architectures as series; {@code false} to create one
	 *                       chart per architecture with algorithms as series.
	 */
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

	/**
	 * Returns the series names used for grouping.
	 *
	 * @param byAlgorithms {@code true} for the orchestration algorithms,
	 *                     {@code false} for the orchestration architectures.
	 * @return the selected names.
	 */
	protected String[] getArray(boolean byAlgorithms) {
		return (byAlgorithms ? SimulationParameters.orchestrationAlgorithms
				: SimulationParameters.orchestrationArchitectures);
	}

	/**
	 * Creates an empty chart with the common styling.
	 *
	 * @param x_series       the X axis title.
	 * @param y_series       the chart title prefix.
	 * @param y_series_label the Y axis title.
	 * @param title          the title suffix, usually the series group name.
	 * @return the chart.
	 */
	protected XYChart initChart(String x_series, String y_series, String y_series_label, String title) {
		XYChart chart = new XYChartBuilder().height(400).width(600).theme(ChartTheme.Matlab).xAxisTitle(x_series)
				.yAxisTitle(y_series_label).build();
		chart.setTitle(y_series + " (" + title + ")");
		chart.getStyler().setLegendVisible(true);
		return chart;
	}

	/**
	 * Saves a chart as a 300 DPI PNG under {@code Final results/}.
	 *
	 * @param chart  the chart to save.
	 * @param folder the output subfolder, relative to {@code Final results/}.
	 * @param name   the file name; slashes are replaced with " per ".
	 */
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

	/**
	 * Extracts one column for the rows that match an architecture and an algorithm.
	 *
	 * @param name the CSV column name.
	 * @param orch the orchestration architecture to select.
	 * @param alg  the orchestration algorithm to select.
	 * @return the selected values, in CSV row order.
	 */
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

	/**
	 * Converts a list of values to the array expected by the chart library.
	 *
	 * @param list the values.
	 * @return the values as a primitive array.
	 */
	protected double[] toArray(List<Double> list) {
		double[] results = new double[list.size()];
		for (int i = 0; i < list.size(); i++)
			results[i] = list.get(i);
		return results;
	}

	/** Generates every chart group: tasks, network, CPU, and energy. */
	public void generate() {
		generateTasksCharts();
		generateNetworkCharts();
		generateCpuCharts();
		generateEnergyCharts();
	}

	/** Generates the energy charts. */
	protected void generateEnergyCharts() {
		for (String value : energyChartsList)
			displayChart(value, "Consumed energy (Wh)", "/Energy");
	}

	/** Generates the CPU utilization charts. */
	protected void generateCpuCharts() {
		for (String value : cpuChartsList)
			displayChart(value, "CPU utilization (%)", "/CPU Utilization");
	}

	/** Generates the network usage charts. */
	protected void generateNetworkCharts() {
		for (String value : networkChartsList)
			displayChart(value, "Utilization (s)", "/Network");
	}

	/** Generates the task outcome and delay charts. */
	protected void generateTasksCharts() {
		for (String value : tasksChartsList)
			displayChart(value, "Number of tasks", "/Tasks");

		for (String value : delaysChartsList)
			displayChart(value, "Time (s)", "/Tasks");
	}

}
