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

public abstract class Chart {

	protected ComputingNodesGenerator computingNodesGenerator;
	protected XYChart chart;
	protected static int height = 270;
	protected static int width = 450;
	protected int clock = -1;
	protected SimulationManager simulationManager;

	public Chart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		chart = new XYChartBuilder().height(height).width(width).theme(ChartTheme.Matlab).title(title)
				.xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).build();
		chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
		chart.getStyler().setLegendVisible(true);
		this.simulationManager = simulationManager;
		computingNodesGenerator = simulationManager.getDataCentersManager().getComputingNodesGenerator();
	}

	protected abstract void update();

	protected void updateSize(Double minXValue, Double maxXValue, Double minYValue, Double maxYValue) {
		chart.getStyler().setXAxisMin(minXValue);
		chart.getStyler().setXAxisMax(maxXValue);
		chart.getStyler().setYAxisMin(minYValue);
		chart.getStyler().setYAxisMax(maxYValue);
	}

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

	public XYChart getChart() {
		return chart;
	}

	protected double[] toArray(List<Double> list) {
		int size = Math.max(list.size(), 1); // To prevent returning empty arrays
		double[] array = new double[size];
		array[0] = -100; // To prevent returning empty arrays
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}
}