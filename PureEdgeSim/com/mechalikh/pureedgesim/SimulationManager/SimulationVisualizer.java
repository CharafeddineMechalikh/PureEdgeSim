package com.mechalikh.pureedgesim.SimulationManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.Marker;
import org.knowm.xchart.style.markers.SeriesMarkers;
import com.mechalikh.pureedgesim.MainApplication;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters.TYPES;

public class SimulationVisualizer {
	JFrame simulationResultsFrame;
	private SwingWrapper<XYChart> swingWrapper;
	private XYChart mapChart = new XYChartBuilder().height(270).width(450).theme(ChartTheme.Matlab)
			.title("Simulation map").xAxisTitle("Width (meters)").yAxisTitle("Length (meters)").build();
	private XYChart cpuUtilizationChart = new XYChartBuilder().height(270).width(450).theme(ChartTheme.Matlab)
			.title("CPU utilization").xAxisTitle("Time (s)").yAxisTitle("Utilization (%)").build();
	private XYChart networkUtilizationChart = new XYChartBuilder().height(270).width(450).theme(ChartTheme.Matlab)
			.title("Network utilization").xAxisTitle("Time (s)").yAxisTitle("Utilization (Mbps)").build();
	private XYChart tasksSuccessChart = new XYChartBuilder().height(270).width(450).theme(ChartTheme.Matlab)
			.title("Tasks success rate").xAxisTitle("Time (minutes)").yAxisTitle("Success rate (%)").build();
	private List<Double> cloudUsage = new ArrayList<>();
	private List<Double> mistUsage = new ArrayList<>();
	private List<Double> edgeUsage = new ArrayList<>();
	private List<Double> wanUsage = new ArrayList<>();
	private List<Double> currentTime = new ArrayList<>();
	private List<Double> tasksFailedList = new ArrayList<>();
	private List<XYChart> charts = new ArrayList<XYChart>();
	private SimulationManager simulationManager;
	private int clock = -1;
	private boolean firstTime = true;

	public SimulationVisualizer(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	void updateCharts() {
		if (firstTime) {
			initCharts();
			charts.add(mapChart);
			charts.add(cpuUtilizationChart);
			charts.add(networkUtilizationChart);
			charts.add(tasksSuccessChart);
			displayCharts();
			firstTime = false;
		}
		currentTime.add(simulationManager.getSimulation().clock());
		// Add edge devices to map and display their CPU utilization
		edgeDevicesMapAndCpu();
		// Add edge data centers to the map and display their CPU utilization
		edgeDataCentersMapAndCpu();
		// Display cloud CPU utilization
		cloudCpuUsage();
		networkUtilizationChart();
		tasksSucessRateChart();
		displayCharts();
	}

	private void initCharts() {
		mapChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
		mapChart.getStyler().setMarkerSize(4);
		updateStyle(mapChart, new Double[] { 0.0, (double) simulationParameters.AREA_WIDTH, 0.0,
				(double) simulationParameters.AREA_LENGTH });

		tasksSuccessChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateStyle(tasksSuccessChart, new Double[] { 0.0, null, null, 100.0 });

		cpuUtilizationChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateStyle(cpuUtilizationChart, new Double[] { simulationParameters.INITIALIZATION_TIME, null, 0.0, null });

		networkUtilizationChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateStyle(networkUtilizationChart, new Double[] { 0.0, simulationManager.getSimulation().clock(), 0.0,
				simulationParameters.WAN_BANDWIDTH / 1000.0 });
	}

	private void tasksSucessRateChart() {
		if (((int) simulationManager.getSimulation().clockInMinutes()) != clock) {
			clock = (int) simulationManager.getSimulation().clockInMinutes();
			double tasksFailed = 100 - simulationManager.getFailureRate();
			double[] time = new double[clock + 1];
			for (int i = 0; i <= clock; i++)
				time[i] = i;
			tasksFailedList.add(tasksFailed);
			updateSeries(tasksSuccessChart, "Tasks failed", time, toArray(tasksFailedList), SeriesMarkers.NONE,
					Color.BLACK);
		}
	}

	void cloudCpuUsage() {
		double clUsage = 0;
		for (int i = 0; i < simulationParameters.NUM_OF_CLOUD_DATACENTERS; i++) {
			if (simulationManager.getServersManager().getDatacenterList().get(i).getType() == TYPES.CLOUD) {
				clUsage = simulationManager.getServersManager().getDatacenterList().get(i).getResources()
						.getAvgCpuUtilization();

			}
		}
		cloudUsage.add(clUsage / simulationParameters.NUM_OF_CLOUD_DATACENTERS);
		

		updateSeries(cpuUtilizationChart, "Cloud", toArray(currentTime), toArray(cloudUsage), SeriesMarkers.NONE,
				Color.BLACK);
	}

	void networkUtilizationChart() {
		double wan = simulationManager.getNetworkModel().getWanUtilization();

		wanUsage.add(wan);

		while (wanUsage.size() > 300 / simulationParameters.UPDATE_INTERVAL) {
			wanUsage.remove(0);
		}
		double[] time = new double[wanUsage.size()];
		double currentTime = simulationManager.getSimulation().clock() - simulationParameters.INITIALIZATION_TIME;
		for (int i = wanUsage.size() - 1; i > 0; i--)
			time[i] = currentTime - ((wanUsage.size() - i) * simulationParameters.UPDATE_INTERVAL);

		updateStyle(networkUtilizationChart,
				new Double[] { currentTime - 200, currentTime, 0.0, simulationParameters.WAN_BANDWIDTH / 1000.0 });
		updateSeries(networkUtilizationChart, "WAN", time, toArray(wanUsage), SeriesMarkers.NONE, Color.BLACK);
	}

	private void edgeDevicesMapAndCpu() {
		double msUsage = 0;
		// Initialize the X and Y series that will be used to draw the map
		// Dead devices series
		List<Double> x_deadEdgeDevicesList = new ArrayList<>();
		List<Double> y_deadEdgeDevicesList = new ArrayList<>();
		// Idle devices series
		List<Double> x_idleEdgeDevicesList = new ArrayList<>();
		List<Double> y_idleEdgeDevicesList = new ArrayList<>();
		// Active devices series
		List<Double> x_activeEdgeDevicesList = new ArrayList<>();
		List<Double> y_activeEdgeDevicesList = new ArrayList<>();

		// Browse all devices and create the series
		// Skip the first items (cloud data centers + edge data centers)
		for (int i = simulationParameters.NUM_OF_EDGE_DATACENTERS
				+ simulationParameters.NUM_OF_CLOUD_DATACENTERS; i < simulationManager.getServersManager()
						.getDatacenterList().size(); i++) {
			// If it is an edge device
			if (simulationManager.getServersManager().getDatacenterList().get(i)
					.getType() == simulationParameters.TYPES.EDGE_DEVICE) {

				if (simulationManager.getServersManager().getDatacenterList().get(i).isDead()) {
					x_deadEdgeDevicesList.add(simulationManager.getServersManager().getDatacenterList().get(i)
							.getMobilityManager().getCurrentLocation().getXPos());
					y_deadEdgeDevicesList.add(simulationManager.getServersManager().getDatacenterList().get(i)
							.getMobilityManager().getCurrentLocation().getYPos());

				} else if (simulationManager.getServersManager().getDatacenterList().get(i).getResources().isIdle()) {
					x_idleEdgeDevicesList.add(simulationManager.getServersManager().getDatacenterList().get(i)
							.getMobilityManager().getCurrentLocation().getXPos());
					y_idleEdgeDevicesList.add(simulationManager.getServersManager().getDatacenterList().get(i)
							.getMobilityManager().getCurrentLocation().getYPos());

				} else { // If the device is busy
					msUsage += simulationManager.getServersManager().getDatacenterList().get(i).getResources()
							.getAvgCpuUtilization();
					x_activeEdgeDevicesList.add(simulationManager.getServersManager().getDatacenterList().get(i)
							.getMobilityManager().getCurrentLocation().getXPos());
					y_activeEdgeDevicesList.add(simulationManager.getServersManager().getDatacenterList().get(i)
							.getMobilityManager().getCurrentLocation().getYPos());
				}
			}
		}

		mistUsage.add(msUsage / simulationManager.getScenario().getDevicesCount());
		updateSeries(cpuUtilizationChart, "Mist", toArray(currentTime), toArray(mistUsage), SeriesMarkers.NONE,
				Color.BLACK);
		updateSeries(mapChart, "Idle devices", toArray(x_idleEdgeDevicesList), toArray(y_idleEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.blue);

		updateSeries(mapChart, "Active devices", toArray(x_activeEdgeDevicesList), toArray(y_activeEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.red);

		updateSeries(mapChart, "Dead devices", toArray(x_deadEdgeDevicesList), toArray(y_deadEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.LIGHT_GRAY);

	}

	private void edgeDataCentersMapAndCpu() {

		double edUsage = 0;
		// Only if Edge computing is used
		if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE")
				|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
			// List of idle servers
			List<Double> x_idleEdgeDataCentersList = new ArrayList<>();
			List<Double> y_idleEdgeDataCentersList = new ArrayList<>();
			// List of active servers
			List<Double> x_activeEdgeDataCentersList = new ArrayList<>();
			List<Double> y_activeEdgeDataCentersList = new ArrayList<>();

			for (int j = simulationParameters.NUM_OF_CLOUD_DATACENTERS; j < simulationParameters.NUM_OF_EDGE_DATACENTERS
					+ simulationParameters.NUM_OF_CLOUD_DATACENTERS; j++) {
				// If it is an Edge data center
				if ((simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE")
						|| simulationManager.getScenario().getStringOrchArchitecture().equals("ALL"))
						&& simulationManager.getServersManager().getDatacenterList().get(j)
								.getType() == simulationParameters.TYPES.EDGE_DATACENTER
						&& simulationParameters.NUM_OF_EDGE_DATACENTERS != 0) {

					if (simulationManager.getServersManager().getDatacenterList().get(j).getResources().isIdle()) {
						x_idleEdgeDataCentersList.add(simulationManager.getServersManager().getDatacenterList().get(j)
								.getMobilityManager().getCurrentLocation().getXPos());
						y_idleEdgeDataCentersList.add(simulationManager.getServersManager().getDatacenterList().get(j)
								.getMobilityManager().getCurrentLocation().getYPos());
					} else {
						edUsage += simulationManager.getServersManager().getDatacenterList().get(j).getResources()
								.getAvgCpuUtilization();

						x_activeEdgeDataCentersList.add(simulationManager.getServersManager().getDatacenterList().get(j)
								.getMobilityManager().getCurrentLocation().getXPos());
						y_activeEdgeDataCentersList.add(simulationManager.getServersManager().getDatacenterList().get(j)
								.getMobilityManager().getCurrentLocation().getYPos());

					}
				}
			}

			edgeUsage.add(edUsage / simulationParameters.NUM_OF_EDGE_DATACENTERS);
			updateSeries(cpuUtilizationChart, "Edge", toArray(currentTime), toArray(edgeUsage), SeriesMarkers.NONE,
					Color.BLACK);

			updateSeries(mapChart, "Idle Edge data centers", toArray(x_idleEdgeDataCentersList),
					toArray(y_idleEdgeDataCentersList), SeriesMarkers.CROSS, Color.BLACK);

			updateSeries(mapChart, "Active Edge data centers", toArray(x_activeEdgeDataCentersList),
					toArray(y_activeEdgeDataCentersList), SeriesMarkers.CROSS, Color.red);

		}
	}

	private void displayCharts() {
		if (firstTime) {
			swingWrapper = new SwingWrapper<>(charts);
			simulationResultsFrame = swingWrapper.displayChartMatrix(); // Display charts
			simulationResultsFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		} else {
			simulationResultsFrame.repaint();
		}

		// Display simulation time
		double time = this.simulationManager.getSimulation().clock() - simulationParameters.INITIALIZATION_TIME;
		simulationResultsFrame.setTitle("Simulation time = " + ((int) time / 60) + " min : " + ((int) time % 60)
				+ " seconds  -  number of edge devices = " + simulationManager.getScenario().getDevicesCount()
				+ " -  Architecture = " + simulationManager.getScenario().getStringOrchArchitecture()
				+ " -  Algorithm = " + simulationManager.getScenario().getStringOrchAlgorithm());
	}

	private void updateStyle(XYChart chart, Double[] array) {
		chart.getStyler().setXAxisMin(array[0]);
		chart.getStyler().setXAxisMax(array[1]);
		chart.getStyler().setYAxisMin(array[2]);
		chart.getStyler().setYAxisMax(array[3]);
		chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
		chart.getStyler().setLegendVisible(true);

	}

	private double[] toArray(List<Double> list) {
		int size = Math.max(list.size(), 1); // To prevent returning empty arrays
		double[] array = new double[size];
		array[0] = -100; // To prevent returning empty arrays
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	private void updateSeries(XYChart chart, String name, double[] X, double[] Y, Marker marker, Color color) {
		try {
			chart.updateXYSeries(name, X, Y, null);
		} catch (Exception e) {
			XYSeries series = chart.addSeries(name, X, Y, null);
			series.setMarker(marker); // Marker type :circle,rectangle, diamond..
			series.setMarkerColor(color); // The color: blue, red, green, yellow, gray..
			series.setLineStyle(new BasicStroke());
		}

	}

	public void close() {
		simulationResultsFrame.dispose();
	}

	public void saveCharts() throws IOException {
		String folderName = MainApplication.getOutputFolder() + "/"
				+ simulationManager.getSimulationLogger().getSimStartTime() + "/simulation_"
				+ simulationManager.getSimulationId() + "/iteration_" + simulationManager.getIterationId() + "__"
				+ simulationManager.getScenario().toString();
		new File(folderName).mkdirs();
		BitmapEncoder.saveBitmapWithDPI(mapChart, folderName + "/map_chart", BitmapFormat.PNG, 600);
		BitmapEncoder.saveBitmapWithDPI(networkUtilizationChart, folderName + "/network_usage", BitmapFormat.PNG, 600);
		BitmapEncoder.saveBitmapWithDPI(cpuUtilizationChart, folderName + "/cpu_usage", BitmapFormat.PNG, 600);
		BitmapEncoder.saveBitmapWithDPI(tasksSuccessChart, folderName + "/tasks_success_rate", BitmapFormat.PNG, 600);

	}

}
