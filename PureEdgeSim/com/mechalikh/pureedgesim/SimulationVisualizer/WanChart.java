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
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.simulationvisualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;

public class WanChart extends Chart {

	private List<Double> wanUsage = new ArrayList<>();

	public WanChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, 0.0, 0.0, SimulationParameters.WAN_BANDWIDTH / 1000.0);// 0.0, clock, ...
	}

	public void update() {
		double wan = simulationManager.getNetworkModel().getWanUtilization();

		wanUsage.add(wan);

		while (wanUsage.size() > 300 / SimulationParameters.CHARTS_UPDATE_INTERVAL) {
			wanUsage.remove(0);
		}
		double[] time = new double[wanUsage.size()];
		double currentTime = simulationManager.getSimulation().clock() - SimulationParameters.INITIALIZATION_TIME;
		for (int i = wanUsage.size() - 1; i > 0; i--)
			time[i] = currentTime - ((wanUsage.size() - i) * SimulationParameters.CHARTS_UPDATE_INTERVAL);

		updateSize(currentTime - 200, currentTime, 0.0, SimulationParameters.WAN_BANDWIDTH / 1000.0);
		updateSeries(getChart(), "WAN", time, toArray(wanUsage), SeriesMarkers.NONE, Color.BLACK);
	}
}
