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

import java.awt.Color;
import java.util.LinkedList;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters; 
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class WanChart extends Chart {

	protected LinkedList<Double> wanUpUsage = new LinkedList<>();
	protected LinkedList<Double> wanDownUsage = new LinkedList<>();

	public WanChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, 0.0, 0.0, SimulationParameters.wanBandwidthBitsPerSecond / 1000000.0);// 0.0, clock, ...
	}

	public void update() {
		// get wan usage in Mbps
		double wanUp = simulationManager.getNetworkModel().getWanUpUtilization() / 1000000.0;
		double wanDown = simulationManager.getNetworkModel().getWanDownUtilization() / 1000000.0;

		wanUpUsage.add(wanUp);
		wanDownUsage.add(wanDown);

		while (wanUpUsage.size() > 300 / SimulationParameters.chartsUpdateInterval) {
			wanUpUsage.removeFirst();
			wanDownUsage.removeFirst();
		}

		double[] time = new double[wanUpUsage.size()];
		double currentTime = simulationManager.getSimulation().clock();
		for (int i = wanUpUsage.size() - 1; i > 0; i--)
			time[i] = currentTime - ((wanUpUsage.size() - i) * SimulationParameters.chartsUpdateInterval);

		updateSize(currentTime - 200, currentTime, 0.0, SimulationParameters.wanBandwidthBitsPerSecond / 1000000.0);
		updateSeries(getChart(), "WanUp", time, toArray(wanUpUsage), SeriesMarkers.NONE, Color.BLACK);
		updateSeries(getChart(), "WanDown", time, toArray(wanDownUsage), SeriesMarkers.NONE, Color.BLACK);
	}
}