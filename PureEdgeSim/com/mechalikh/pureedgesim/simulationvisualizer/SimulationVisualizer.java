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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager; 
/**
 * The {@code SimulationVisualizer} class provides a GUI to visualize the
 * simulation results in the form of charts.
 */
public class SimulationVisualizer {

	 // JFrame that displays the charts
    protected JFrame simulationResultsFrame;

    // SimulationManager instance that manages the simulation
    protected SimulationManager simulationManager;

    // List of charts to display
    protected List<Chart> charts = new ArrayList<Chart>(4);

    // Flag that indicates if it is the first time charts are updated
    protected boolean firstTime = true;

    /**
     * Constructs a new simulation visualizer with the given simulation manager.
     * The visualizer contains a list of charts that display the simulation
     * results.
     *
     * @param simulationManager the simulation manager
     */
    public SimulationVisualizer(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;

        // Create charts
        Chart mapChart = new MapChart("Simulation map", "Width (meters)", "Length (meters)", simulationManager);
        Chart cpuUtilizationChart = new CPUChart("CPU utilization", "Time (s)", "Utilization (%)", simulationManager);
        Chart tasksSuccessChart = new TasksChart("Tasks success rate", "Time (minutes)", "Success rate (%)",
                simulationManager);
        charts.addAll(List.of(mapChart, cpuUtilizationChart, tasksSuccessChart));

        // Add network utilization chart if the useOneSharedWanLink parameter is true
        if (SimulationParameters.useOneSharedWanLink) {
            Chart networkUtilizationChart = new WanChart("Network utilization", "Time (s)", "Utilization (Mbps)",
                    simulationManager);
            charts.add(networkUtilizationChart);
        }
    }

    /**
     * Updates the charts with the latest simulation results and displays them if
     * it is the first time. The method also updates the simulation time in the
     * frame title.
     */
    public void updateCharts() {
        if (firstTime) {
            SwingWrapper<XYChart> swingWrapper = new SwingWrapper<>(
                    charts.stream().map(Chart::getChart).collect(Collectors.toList()));
            simulationResultsFrame = swingWrapper.displayChartMatrix(); // Display charts
            simulationResultsFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        }
        firstTime = false;
        repaint();

        // Display simulation time and other scenario parameters
        double time = simulationManager.getSimulation().clock();
        simulationResultsFrame.setTitle("Simulation time = " + ((int) time / 60) + " min : " + ((int) time % 60)
                + " seconds  -  number of edge devices = " + simulationManager.getScenario().getDevicesCount()
                + " -  Architecture = " + simulationManager.getScenario().getStringOrchArchitecture()
                + " -  Algorithm = " + simulationManager.getScenario().getStringOrchAlgorithm());
    }

    /**
     * Repaints the charts with the latest simulation results.
     */
    protected void repaint() {
        charts.forEach(Chart::update);
        simulationResultsFrame.repaint();
    }

    /**
     * Closes the simulation results window.
     */
    public void close() {
        simulationResultsFrame.dispose();
    }

    /**
     * Saves the charts to disk as PNG images with a resolution of 300 DPI.
     * 
     * @throws IOException if an error occurs while saving the images
     */
    /**
     * Saves the generated charts as PNG images in a specified directory.
     * The directory structure will be as follows:
     * outputFolder/simStartTime/simulation_simulationId/iteration_iterationNumber__scenarioString/
     * 
     * @throws IOException if there is an error creating the directory or saving the images
     */
    public void saveCharts() throws IOException {
        // Create the directory path for saving the images
        String folderName = SimulationParameters.outputFolder + "/"
                + simulationManager.getSimulationLogger().getSimStartTime() + "/simulation_"
                + simulationManager.getSimulationId() + "/iteration_" + simulationManager.getIteration() + "__"
                + simulationManager.getScenario().toString();
        // Create the directory if it does not exist
        new File(folderName).mkdirs();
        
        // Save the charts as PNG images
        BitmapEncoder.saveBitmapWithDPI(charts.get(0).getChart(), folderName + "/map_chart", BitmapFormat.PNG, 300);
        BitmapEncoder.saveBitmapWithDPI(charts.get(1).getChart(), folderName + "/cpu_usage", BitmapFormat.PNG, 300);
        BitmapEncoder.saveBitmapWithDPI(charts.get(2).getChart(), folderName + "/tasks_success_rate", BitmapFormat.PNG, 300);
        if (SimulationParameters.useOneSharedWanLink) {
            BitmapEncoder.saveBitmapWithDPI(charts.get(3).getChart(), folderName + "/network_usage", BitmapFormat.PNG, 300);
        }
    }


}
