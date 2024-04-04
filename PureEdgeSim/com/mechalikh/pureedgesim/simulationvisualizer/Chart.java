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

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.Marker;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

import java.awt.*;
import java.util.List;

/**
 * An abstract class that represents a chart.
 */
public abstract class Chart {

	/**
	 * The computing nodes generator used to generate data.
	 */
	protected ComputingNodesGenerator computingNodesGenerator;

	/**
	 * The XYChart used to display data.
	 */
	protected XYChart chart;

	/**
	 * The height of the chart.
	 */
	protected static int height = 270;

	/**
	 * The width of the chart.
	 */
	protected static int width = 450;

	/**
	 * The clock used to track the simulation time.
	 */
	protected int clock = -1;

	/**
	 * The simulation manager used to manage the simulation.
	 */
	protected SimulationManager simulationManager;

	/**
	 * Constructs a new Chart object.
	 * 
	 * @param title             the title of the chart
	 * @param xAxisTitle        the title of the X axis
	 * @param yAxisTitle        the title of the Y axis
	 * @param simulationManager the simulation manager used to manage the simulation
	 */
	public Chart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		chart = new XYChartBuilder().height(height).width(width).theme(ChartTheme.Matlab).title(title)
				.xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).build();
		chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
		chart.getStyler().setLegendVisible(true);
		this.simulationManager = simulationManager;
		computingNodesGenerator = simulationManager.getDataCentersManager().getComputingNodesGenerator();

	}

	/**
	 * Updates the chart.
	 */
	protected abstract void update();

	/**
	 * Updates the size of the chart.
	 * 
	 * @param minXValue the minimum value of the X axis
	 * @param maxXValue the maximum value of the X axis
	 * @param minYValue the minimum value of the Y axis
	 * @param maxYValue the maximum value of the Y axis
	 */
	protected void updateSize(Double minXValue, Double maxXValue, Double minYValue, Double maxYValue) {
		chart.getStyler().setXAxisMin(minXValue);
		chart.getStyler().setXAxisMax(maxXValue);
		chart.getStyler().setYAxisMin(minYValue);
		chart.getStyler().setYAxisMax(maxYValue);
	}

	/**
	 * Updates a series in the chart.
	 * 
	 * @param chart  the chart to update
	 * @param name   the name of the series
	 * @param X      the X values of the series
	 * @param Y      the Y values of the series
	 * @param marker the marker type of the series
	 * @param color  the color of the series
	 */
	protected static void updateSeries(XYChart chart, String name, double[] X, double[] Y, Marker marker, Color color) {
		if (chart.getSeriesMap().containsKey(name)) {
			chart.updateXYSeries(name, X, Y, null);
		} else {
			XYSeries series = chart.addSeries(name, X, Y, null);
			series.setMarker(marker); // Marker type: circle, rectangle, diamond..
			series.setMarkerColor(color); // The color: blue, red, green, yellow, gray..
			series.setLineStyle(new BasicStroke());
		}
	}

	/**
	 * Gets the chart.
	 * 
	 * @return the chart
	 */
	public XYChart getChart() {
		return chart;
	}

	/**
	 * Converts a list of doubles to an array of doubles.
	 * 
	 * @param list the list to convert
	 * @return the array of doubles
	 */
	protected double[] toArray(List<Double> list) {
		if (list.size() == 0)
			list.add(-100.0);
		return list.stream().mapToDouble(Double::doubleValue).toArray();
	}

}