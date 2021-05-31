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
package com.mechalikh.pureedgesim.simulationcore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.cloudsimplus.util.Log;

import com.mechalikh.pureedgesim.scenariomanager.FilesParser;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

import ch.qos.logback.classic.Level;

public class Simulation extends SimulationAbstract {

	/**
	 * @see #launchParallelSimulations()
	 */
	protected int cpuCores;

	/**
	 * @see #loadScenarios()
	 */
	protected List<Scenario> Iterations = new ArrayList<>();

	/**
	 * Creates a PureEdgeSim simulation.
	 */
	public Simulation() {
	}

	/**
	 * Initializes the environment and starts the simulation.
	 */
	public void launchSimulation() {
		SimLog.println("Main- Loading simulation files...");

		// Check files
		FilesParser fp = new FilesParser();
		if (!fp.checkFiles(simConfigfile, edgeDevicesFile, edgeDataCentersFile, applicationsFile, cloudFile))
			return; // if files aren't correct stop everything.

		// Disable cloudsim plus log
		if (!SimulationParameters.DEEP_LOGGING)
			Log.setLevel(Level.OFF);
		else
			Log.setLevel(Level.ALL);

		Date startDate = Calendar.getInstance().getTime();

		// Walk through all orchestration scenarios
		loadScenarios();

		if (SimulationParameters.PARALLEL) {
			launchParallelSimulations();
		} else { // Sequential execution
			new SimulationThread(this, 0, 1).startSimulation();
		}

		// Simulation Finished
		Date endDate = Calendar.getInstance().getTime();
		SimLog.println("Main- Simulation took : " + simulationTime(startDate, endDate));
		SimLog.println("Main- results were saved to the folder: " + outputFolder);

	}

	/**
	 * Allows you to run parallel simulations to take advantage of available CPU
	 * cores, when this option is enabled in the simulation settings.
	 */
	private void launchParallelSimulations() {
		cpuCores = Runtime.getRuntime().availableProcessors();
		List<SimulationThread> simulationList = new ArrayList<>(cpuCores);

		// Generate the parallel simulations
		for (int fromIteration = 0; fromIteration < Math.min(cpuCores, Iterations.size()); fromIteration++) {
			// The number of parallel simulations will be limited by the minimum value
			// between cpu cores and number of iterations
			simulationList.add(new SimulationThread(this, fromIteration, cpuCores));
		}

		// Finally then runs them
		// tag::parallelExecution[]
		simulationList.parallelStream().forEach(SimulationThread::startSimulation);
		// end::parallelExecution[]

	}

	/**
	 * Checks the configuration files and loads the simulation parameters. to know
	 * how to personalize the location from which these files are loaded:
	 * 
	 * @see #setCustomFilePath(String, Files)
	 * @see #setCustomSettingsFolder(String)
	 */
	private void loadScenarios() {
		for (int algorithmID = 0; algorithmID < SimulationParameters.ORCHESTRATION_AlGORITHMS.length; algorithmID++) {
			// Repeat the operation of the whole set of criteria
			for (int architectureID = 0; architectureID < SimulationParameters.ORCHESTRATION_ARCHITECTURES.length; architectureID++) {
				for (int devicesCount = SimulationParameters.MIN_NUM_OF_EDGE_DEVICES; devicesCount <= SimulationParameters.MAX_NUM_OF_EDGE_DEVICES; devicesCount += SimulationParameters.EDGE_DEVICE_COUNTER_STEP) {
					Iterations.add(new Scenario(devicesCount, algorithmID, architectureID));
				}
			}
		}
	}

	/**
	 * Returns the simulation duration.
	 * 
	 */
	private String simulationTime(Date startDate, Date endDate) {
		long difference = endDate.getTime() - startDate.getTime();
		long seconds = difference / 1000 % 60;
		long minutes = difference / (60 * 1000) % 60;
		long hours = difference / (60 * 60 * 1000) % 24;
		long days = difference / (24 * 60 * 60 * 1000);
		String duration = "";
		if (days > 0)
			duration += days + " days, ";
		if (hours > 0)
			duration += hours + " hours, ";
		duration += minutes + " minutes, " + seconds + " seconds.";
		return duration;
	}

	public List<Scenario> getIterations() { 
		return this.Iterations;
	}

}
