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

import com.mechalikh.pureedgesim.Main;
import com.mechalikh.pureedgesim.DataCentersManager.EdgeDataCenter;
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
	private List<Double> edgeUsage = new ArrayList<>();
	private List<Double> fogUsage = new ArrayList<>();
	private List<Double> wanUsage = new ArrayList<>();
	private List<Double> currentTime = new ArrayList<>();
	private List<Double> tasksFailedList = new ArrayList<>();
	private List<XYChart> charts = new ArrayList<XYChart>();
	private SimulationManager simulationManager;
	private double clock = -1;
	private boolean firstTime = true;

	public SimulationVisualizer(SimulationManager simulationManager) {
		setSimulationManager(simulationManager);
	}

	private void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	void updateCharts() {
		if (isFirstTime()) {
			initCharts();
			charts.add(mapChart);
			charts.add(cpuUtilizationChart);
			charts.add(networkUtilizationChart);
			charts.add(tasksSuccessChart);
			displayCharts();
			setFirstTime(false);
		}
		mapChart();
		networkUtilizationChart();
		tasksSucessRateChart();
		utilizationChart();
		displayCharts();
	}

	private void setFirstTime(boolean firstTime) {
		this.firstTime = firstTime;
	}

	private boolean isFirstTime() {
		return firstTime;
	}

	private void initCharts() {

		mapChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
		mapChart.getStyler().setMarkerSize(4);
		updateStyle(mapChart, 0.0, (double) simulationParameters.AREA_WIDTH, 0.0,
				(double) simulationParameters.AREA_LENGTH);

		tasksSuccessChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateStyle(tasksSuccessChart, 0.0, null, null, 100.0);

		cpuUtilizationChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateStyle(cpuUtilizationChart, simulationParameters.INITIALIZATION_TIME, null, 0.0, null);

		networkUtilizationChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateStyle(networkUtilizationChart, 0.0, getSimulationManager().getSimulation().clock(), 0.0,
				simulationParameters.WAN_BANDWIDTH / 1000.0);

	}

	void mapChart() {
		// Add edge devices to map
		addEdgeDevicesToMap();
		// Add Fog servers to the map
		addFogServersToMap();

	}

	private void tasksSucessRateChart() {
		if (((int) getSimulationManager().getSimulation().clockInMinutes()) != clock) {
			clock = (int) getSimulationManager().getSimulation().clockInMinutes();
			double tasksFailed = 100 - getSimulationManager().getFailureRate();
			getTasksFailedList().add(tasksFailed);
		} else {
			return;
		}
		updateSeries(tasksSuccessChart, "Tasks failed", null, toArray(tasksFailedList), SeriesMarkers.NONE,
				Color.BLACK);
	}

	private List<Double> getTasksFailedList() {
		return this.tasksFailedList;
	}

	void utilizationChart() {
		double cpuUsage = 0;
		double edUsage = 0;
		double fgUsage = 0;
		double edgecount = 0;
		double fogcount = 0;
		List<EdgeDataCenter> datacenterList = getSimulationManager().getServersManager().getDatacenterList();
		for (int i = 0; i < datacenterList.size(); i++) {
			if (datacenterList.get(i).getType() == TYPES.CLOUD) {
				cpuUsage = datacenterList.get(i).getCurrentCpuUtilization();

			} else if (datacenterList.get(i).getType() == TYPES.EDGE && datacenterList.get(i).getVmList().size() > 0) {
				edUsage += datacenterList.get(i).getCurrentCpuUtilization();
				edgecount++;

			} else if (datacenterList.get(i).getType() == TYPES.FOG) {
				fogcount++;
				fgUsage += datacenterList.get(i).getCurrentCpuUtilization();
			}
		}
		edUsage = edUsage / edgecount;
		fgUsage = fgUsage / fogcount;
		cloudUsage.add(cpuUsage);
		edgeUsage.add(edUsage);
		fogUsage.add(fgUsage);
		getCcurrentTime().add(getSimulationManager().getSimulation().clock());

		updateSeries(cpuUtilizationChart, "Cloud", toArray(currentTime), toArray(cloudUsage), SeriesMarkers.NONE,
				Color.BLACK);
		updateSeries(cpuUtilizationChart, "Edge", toArray(currentTime), toArray(edgeUsage), SeriesMarkers.NONE,
				Color.BLACK);
		updateSeries(cpuUtilizationChart, "Fog", toArray(currentTime), toArray(fogUsage), SeriesMarkers.NONE,
				Color.BLACK);

	}

	private List<Double> getCcurrentTime() {
		return this.currentTime;
	}

	void networkUtilizationChart() {
		double wan = getSimulationManager().getNetworkModel().getWanUtilization();

		getWanUsage().add(wan);

		while (getWanUsage().size() > 300 / simulationParameters.CHARTS_UPDATE_INTERVAL) {
			getWanUsage().remove(0);
		}
		double[] time = new double[getWanUsage().size()];
		double currentTime = getSimulationManager().getSimulation().clock() - simulationParameters.INITIALIZATION_TIME;
		for (int i = getWanUsage().size() - 1; i > 0; i--)
			time[i] = currentTime - ((getWanUsage().size() - i) * simulationParameters.CHARTS_UPDATE_INTERVAL);

		updateStyle(networkUtilizationChart, currentTime - 200, currentTime, 0.0,
				simulationParameters.WAN_BANDWIDTH / 1000.0);
		updateSeries(networkUtilizationChart, "WAN", time, toArray(wanUsage), SeriesMarkers.NONE, Color.BLACK);

	}

	private List<Double> getWanUsage() {
		return this.wanUsage;
	}

	private void addEdgeDevicesToMap() {
		// Initialize the X and Y series that will be used to draw the map
		// Dead devices series
		List<Double> x_deadEdgeDevicesList = new ArrayList<Double>();
		List<Double> y_deadEdgeDevicesList = new ArrayList<Double>();
		// Idle devices series
		List<Double> x_idleEdgeDevicesList = new ArrayList<Double>();
		List<Double> y_idleEdgeDevicesList = new ArrayList<Double>();
		// Active devices series
		List<Double> x_activeEdgeDevicesList = new ArrayList<Double>();
		List<Double> y_activeEdgeDevicesList = new ArrayList<Double>();

		// Browse all devices and create the series
		// Skip the first items (cloud data centers + fog servers)
		for (int i = simulationParameters.NUM_OF_FOG_DATACENTERS
				+ simulationParameters.NUM_OF_CLOUD_DATACENTERS; i < getSimulationManager().getServersManager()
						.getDatacenterList().size(); i++) {
			// If it is an edge device
			if (getSimulationManager().getServersManager().getDatacenterList().get(i)
					.getType() == simulationParameters.TYPES.EDGE) {

				if (getSimulationManager().getServersManager().getDatacenterList().get(i).isDead()) {
					x_deadEdgeDevicesList.add(getSimulationManager().getServersManager().getDatacenterList().get(i)
							.getLocation().getXPos());
					y_deadEdgeDevicesList.add(getSimulationManager().getServersManager().getDatacenterList().get(i)
							.getLocation().getYPos());

				} else if (getSimulationManager().getServersManager().getDatacenterList().get(i).isIdle()) {
					x_idleEdgeDevicesList.add(getSimulationManager().getServersManager().getDatacenterList().get(i)
							.getLocation().getXPos());
					y_idleEdgeDevicesList.add(getSimulationManager().getServersManager().getDatacenterList().get(i)
							.getLocation().getYPos());

				} else { // If the device is busy
					x_activeEdgeDevicesList.add(getSimulationManager().getServersManager().getDatacenterList().get(i)
							.getLocation().getXPos());
					y_activeEdgeDevicesList.add(getSimulationManager().getServersManager().getDatacenterList().get(i)
							.getLocation().getYPos());
				}
			}
		}
		updateSeries(mapChart, "Idle devices", toArray(x_idleEdgeDevicesList), toArray(y_idleEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.blue);

		updateSeries(mapChart, "Active devices", toArray(x_activeEdgeDevicesList), toArray(y_activeEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.red);

		updateSeries(mapChart, "Dead devices", toArray(x_deadEdgeDevicesList), toArray(y_deadEdgeDevicesList),
				SeriesMarkers.CIRCLE, Color.LIGHT_GRAY);
	}

	private void addFogServersToMap() {
		// Only if Fog computing is used
		if (getSimulationManager().getScenario().getStringOrchArchitecture().contains("FOG")
				|| getSimulationManager().getScenario().getStringOrchArchitecture().equals("ALL")) {
			// List of idle servers
			List<Double> x_idleFogServersList = new ArrayList<Double>();
			List<Double> y_idleFogServersList = new ArrayList<Double>();
			// List of active servers
			List<Double> x_activeFogServersList = new ArrayList<Double>();
			List<Double> y_activeFogServersList = new ArrayList<Double>();

			for (int j = simulationParameters.NUM_OF_CLOUD_DATACENTERS; j < simulationParameters.NUM_OF_FOG_DATACENTERS
					+ simulationParameters.NUM_OF_CLOUD_DATACENTERS; j++) {
				// If it is a Fog server
				if ((getSimulationManager().getScenario().getStringOrchArchitecture().contains("FOG")
						|| getSimulationManager().getScenario().getStringOrchArchitecture().equals("ALL"))
						&& getSimulationManager().getServersManager().getDatacenterList().get(j)
								.getType() == simulationParameters.TYPES.FOG
						&& simulationParameters.NUM_OF_FOG_DATACENTERS != 0) {

					if (getSimulationManager().getServersManager().getDatacenterList().get(j).isIdle()) {
						x_idleFogServersList.add(getSimulationManager().getServersManager().getDatacenterList().get(j)
								.getLocation().getXPos());
						y_idleFogServersList.add(getSimulationManager().getServersManager().getDatacenterList().get(j)
								.getLocation().getYPos());

					} else {
						x_activeFogServersList.add(getSimulationManager().getServersManager().getDatacenterList().get(j)
								.getLocation().getXPos());
						y_activeFogServersList.add(getSimulationManager().getServersManager().getDatacenterList().get(j)
								.getLocation().getYPos());

					}
				}
			}

			updateSeries(mapChart, "Idle Fog servers", toArray(x_idleFogServersList), toArray(y_idleFogServersList),
					SeriesMarkers.CROSS, Color.BLACK);

			updateSeries(mapChart, "Active Fog servers", toArray(x_activeFogServersList),
					toArray(y_activeFogServersList), SeriesMarkers.CROSS, Color.red);

		}
	}

	private void displayCharts() {
		if (firstTime) {
			swingWrapper = new SwingWrapper<XYChart>(charts);
			simulationResultsFrame = swingWrapper.displayChartMatrix(); // Display charts
			simulationResultsFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		} else {
			simulationResultsFrame.repaint();

		}

		// Display simulation time
		double time = this.simulationManager.getSimulation().clock() - simulationParameters.INITIALIZATION_TIME;
		simulationResultsFrame.setTitle("Simulation time = " + ((int) time / 60) + " min : " + ((int) time % 60)
				+ " seconds  -  number of edge devices = " + getSimulationManager().getScenario().getDevicesCount()
				+ " -  Architecture = " + getSimulationManager().getScenario().getStringOrchArchitecture()
				+ " -  Algorithm = " + getSimulationManager().getScenario().getStringOrchAlgorithm());
	}

	private void updateStyle(XYChart chart, Double xMin, Double xMax, Double yMin, Double yMax) {
		if (xMin != null)
			chart.getStyler().setXAxisMin(xMin);
		if (xMax != null)
			chart.getStyler().setXAxisMax(xMax);
		if (yMin != null)
			chart.getStyler().setYAxisMin(yMin);
		if (yMax != null)
			chart.getStyler().setYAxisMax(yMax);
		chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
		chart.getStyler().setLegendVisible(true);

	}

	private SimulationManager getSimulationManager() {
		return this.simulationManager;
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

	public SwingWrapper<XYChart> getSwingWrapper() {
		return swingWrapper;
	}

	public void setSwingWrapper(SwingWrapper<XYChart> swingWrapper) {
		this.swingWrapper = swingWrapper;
	}

	public void saveCharts() throws IOException {

		String folderName = Main.getOutputFolder() + "/"
				+ getSimulationManager().getSimulationLogger().getSimStartTime() + "/simulation_"
				+ getSimulationManager().getSimulationId() + "/iteration_" + getSimulationManager().getIterationId()
				+ "__" + getSimulationManager().getScenario().toString();
		new File(folderName).mkdirs();
		BitmapEncoder.saveBitmapWithDPI(mapChart, folderName + "/map_chart", BitmapFormat.PNG, 600);
		BitmapEncoder.saveBitmapWithDPI(networkUtilizationChart, folderName + "/network_usage", BitmapFormat.PNG, 600);
		BitmapEncoder.saveBitmapWithDPI(cpuUtilizationChart, folderName + "/cpu_usage", BitmapFormat.PNG, 600);
		BitmapEncoder.saveBitmapWithDPI(tasksSuccessChart, folderName + "/tasks_success_rate", BitmapFormat.PNG, 600);

	}

}
