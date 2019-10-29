package com.mechalikh.pureedgesim.SimulationManager;

import java.awt.BasicStroke;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;

public class ChartsGenerator {

	private List<String[]> records = new ArrayList<>();
	private String fileName;

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
			SimLog.println("Problem reading file.");
		}
	}

	public double[] getColumn(String name) {
		double[] results = new double[records.size() - 1];
		int column = getColumnIndex(name);
		for (int line = 1; line < records.size(); line++) {
			results[line - 1] = Double.parseDouble(records.get(line)[column]);
		}
		return results;
	}

	private int getColumnIndex(String name) {
		for (int j = 0; j < records.get(0).length; j++) {
			if (records.get(0)[j].trim().equals(name.trim())) {
				return j;
			}
		}
		return -1;
	}

	public void displayChart(String x_series, String y_series, String y_series_label, String folder) {
		byAlgorithms(x_series, y_series, y_series_label, folder);
		byArchitectures(x_series, y_series, y_series_label, folder);
	}

	private void byAlgorithms(String x_series, String y_series, String y_series_label, String folder) {
		for (int orch = 0; orch < simulationParameters.ORCHESTRATION_ARCHITECTURES.length; orch++) {
			XYChart algorithmsChart = new XYChartBuilder().height(400).width(600).theme(ChartTheme.Matlab)
					.xAxisTitle(x_series).yAxisTitle(y_series_label).build();
			algorithmsChart.setTitle(y_series + " (" + simulationParameters.ORCHESTRATION_ARCHITECTURES[orch] + ")");
			algorithmsChart.getStyler().setLegendVisible(true);
			for (int alg = 0; alg < simulationParameters.ORCHESTRATION_AlGORITHMS.length; alg++) {
				double[] xData = getColumn(x_series, simulationParameters.ORCHESTRATION_ARCHITECTURES[orch],
						simulationParameters.ORCHESTRATION_AlGORITHMS[alg]);
				double[] yData = getColumn(y_series, simulationParameters.ORCHESTRATION_ARCHITECTURES[orch],
						simulationParameters.ORCHESTRATION_AlGORITHMS[alg]);
				XYSeries series = algorithmsChart.addSeries(simulationParameters.ORCHESTRATION_AlGORITHMS[alg], xData,
						yData);
				series.setMarker(SeriesMarkers.CIRCLE); // Marker type :circle,rectangle, diamond..
				series.setLineStyle(new BasicStroke());
			}
			// Save the chart
			saveBitmap(algorithmsChart, "Algorithms" + folder + "/",
					y_series + "__" + simulationParameters.ORCHESTRATION_ARCHITECTURES[orch]);

		}
	}

	public void byArchitectures(String x_series, String y_series, String y_series_label, String folder) {
		for (int alg = 0; alg < simulationParameters.ORCHESTRATION_AlGORITHMS.length; alg++) {
			XYChart architecturesChart = new XYChartBuilder().height(400).width(600).theme(ChartTheme.Matlab)
					.xAxisTitle(x_series).yAxisTitle(y_series_label).build();
			architecturesChart.setTitle(y_series + " (" + simulationParameters.ORCHESTRATION_AlGORITHMS[alg] + ")");
			architecturesChart.getStyler().setLegendVisible(true);
			for (int orch = 0; orch < simulationParameters.ORCHESTRATION_ARCHITECTURES.length; orch++) {
				double[] xData = getColumn(x_series, simulationParameters.ORCHESTRATION_ARCHITECTURES[orch],
						simulationParameters.ORCHESTRATION_AlGORITHMS[alg]);
				double[] yData = getColumn(y_series, simulationParameters.ORCHESTRATION_ARCHITECTURES[orch],
						simulationParameters.ORCHESTRATION_AlGORITHMS[alg]);
				XYSeries series = architecturesChart.addSeries(simulationParameters.ORCHESTRATION_ARCHITECTURES[orch],
						xData, yData);
				series.setMarker(SeriesMarkers.CIRCLE); // Marker type :circle,rectangle, diamond..
				series.setLineStyle(new BasicStroke());
			}
			// Save the chart
			saveBitmap(architecturesChart, "Architectures" + folder + "/",
					y_series + "__" + simulationParameters.ORCHESTRATION_AlGORITHMS[alg]);
		}
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

	private double[] getColumn(String name, String orch, String alg) {
		List<Double> list = new ArrayList<>();
		int column = getColumnIndex(name);
		for (int line = 1; line < records.size(); line++) {
			if (records.get(line)[0].trim().equals(orch.trim()) && records.get(line)[1].trim().equals(alg.trim())) {
				list.add(Double.parseDouble(records.get(line)[column]));
			}
		}
		double[] results = new double[list.size()];
		for (int i = 0; i < list.size(); i++)
			results[i] = list.get(i);
		return results;
	}

}
