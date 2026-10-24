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
 *     @since PureEdgeSim 2.0
 **/
package com.mechalikh.pureedgesim.simulationvisualizer;

import java.awt.Color;
import java.util.ArrayList; 
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters; 
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * A chart that displays the WAN up and down utilization over time.
 */
public class WanChart extends Chart {
    
    // We can use an ArrayList instead of a LinkedList for better random access performance.
    protected ArrayList<Double> wanUpUsage = new ArrayList<>();
    protected ArrayList<Double> wanDownUsage = new ArrayList<>();

    /**
     * Constructs a new WAN chart with the given title, X and Y axis titles, and simulation manager.
     *
     * @param title             the title of the chart
     * @param xAxisTitle        the title of the X axis
     * @param yAxisTitle        the title of the Y axis
     * @param simulationManager the simulation manager
     */
    public WanChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
        super(title, xAxisTitle, yAxisTitle, simulationManager);
        getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
        // We can use the constant directly instead of computing it every time.
        updateSize(0.0, 0.0, 0.0, SimulationParameters.wanBandwidthBitsPerSecond / 1000000.0);
    }

    /**
     * Updates the WAN up and down utilization data for the chart.
     */
    public void update() {
        // Get WAN usage in Mbps.
        double wanUp = simulationManager.getNetworkModel().getWanUpUtilization() / 1000000.0;
        double wanDown = simulationManager.getNetworkModel().getWanDownUtilization() / 1000000.0;

        wanUpUsage.add(wanUp);
        wanDownUsage.add(wanDown);

        // Remove old data points.
        int maxDataPoints = (int) (300 / SimulationParameters.chartsUpdateInterval);
        while (wanUpUsage.size() > maxDataPoints) {
            wanUpUsage.remove(0);
            wanDownUsage.remove(0);
        }

        // Compute the time values for the data points.
        double[] time = new double[wanUpUsage.size()];
        double currentTime = simulationManager.getSimulation().clock();
        for (int i = wanUpUsage.size() - 1; i >= 0; i--) {
            time[i] = currentTime - ((wanUpUsage.size() - i) * SimulationParameters.chartsUpdateInterval);
        }

        // Update the chart with the new data.
        updateSize(currentTime - 200, currentTime, 0.0, SimulationParameters.wanBandwidthBitsPerSecond / 1000000.0);
        updateSeries(getChart(), "WanUp", time, toArray(wanUpUsage), SeriesMarkers.NONE, Color.BLACK);
        updateSeries(getChart(), "WanDown", time, toArray(wanDownUsage), SeriesMarkers.NONE, Color.BLACK);
    }
}
