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
package com.mechalikh.pureedgesim.simulationmanager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

import com.mechalikh.pureedgesim.scenariomanager.ApplicationFileParser;
import com.mechalikh.pureedgesim.scenariomanager.DatacentersParser;
import com.mechalikh.pureedgesim.scenariomanager.EdgeDevicesParser;

/**
 * The {@code Simulation} class represents the main class of PureEdgeSim. By
 * which the simulation can be run. All PureEdgeSim simulations are instances of
 * this class.
 * <p>
 * The easiest way to run simulations is to use the following line of code:
 * <blockquote>
 * 
 * <pre>
 * new Simulation().launchSimulation();
 * </pre>
 * 
 * </blockquote>
 * 
 * While this may be sufficient in many cases, it only runs simulations using
 * the predefined models, such as the
 * {@link com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel
 * DefaultMobilityModel}, the
 * {@link com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode
 * DefaultComputingNode}, the
 * {@link com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator
 * DefaultOrchestrator}, the
 * {@link com.mechalikh.pureedgesim.network.DefaultNetworkModel
 * DefaultNetworkModel}, etc. Fortunately, PureEdgeSim allows users to integrate
 * their custom models into the simulation. Here, is a quick example of how to
 * do it:
 * <p>
 * **<blockquote>*
 * 
 * <pre>
 * // Create a PureEdgeSim simulation
 * Simulation sim = new Simulation();
 *
 * // To change the mobility model
 * sim.setCustomMobilityModel(Example2CustomMobilityModel.class);
 *
 * // To change the tasks orchestrator
 * sim.setCustomEdgeOrchestrator(Example8FuzzyLogicOrchestrator.class);
 *
 * // To change the tasks generator
 * sim.setCustomTasksGenerator(DefaultTasksGenerator.class);
 *
 * // To use a custom energy model
 * sim.setCustomEnergyModel(Example3CustomEnergyModel.class);
 *
 * // Finally,you can launch the simulation
 * sim.launchSimulation();
 * </pre>
 * 
 * **</blockquote>*
 * <p>
 * For more details on how to integrate custom models, see the provided
 * {@code examples}. You should have received a set of examples along with
 * PureEdgeSim. If not, <a href=
 * "https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/PureEdgeSim/examples">see
 * our examples on github</a>.
 * 
 * 
 * @see #loadScenarios()
 * @see #launchSimulation()
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 4.2
 */
public class Simulation extends SimulationAbstract {

	/**
	 * The number of CPU cores on this machine. It is used to launch parallel
	 * simulations.
	 * 
	 * @see #launchParallelSimulations()
	 */
	protected int cpuCores;

	/**
	 * The list of all simulation scenarios (i.e. iterations).
	 * 
	 * @see #loadScenarios()
	 */
	protected List<Scenario> iterations = new ArrayList<>(20);

	/**
	 * List of simulation threads. i.e. The list of the simulations that will be
	 * launched in parallel by each CPU Core.
	 * 
	 * @see #launchParallelSimulations()
	 */
	protected List<SimulationThread> simulationList;

	/**
	 * Creates a PureEdgeSim simulation with default predefined models.
	 */
	public Simulation() {
	}

	/**
	 * Checks the simulation parameters, loads the simulation scenarios, and starts
	 * the simulation.
	 * 
	 * @see #loadScenarios()
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 */
	public void launchSimulation() {
		SimLog.println("%s - Loading simulation files...", getClass().getSimpleName());

		// Check files.
		if (!checkFiles())
			// If any of the input files is not correct stop everything.
			return;

		// Save the simulation starting time.
		Date startTime = Calendar.getInstance().getTime();

		// Walk through all orchestration scenarios.
		loadScenarios();

		if (SimulationParameters.parallelism_enabled) {
			// Parallel simulation.
			launchParallelSimulations();
		} else {
			// Sequential execution.
			new SimulationThread(this, 0, 1).startSimulation();
		}

		// At this point, the simulation has finished. So, save the finish time.
		Date finishTime = Calendar.getInstance().getTime();

		// Then, print the simulation duration
		SimLog.println("%s - Simulation took : %s", getClass().getSimpleName(),
				simulatioDuration(startTime, finishTime));
		SimLog.println("%s - results were saved to the folder: %s", getClass().getSimpleName(), SimulationParameters.outputFolder);

	}

	/**
	 * Checks the input files.
	 */
	protected boolean checkFiles() {
		return (new EdgeDevicesParser(SimulationParameters.edgeDevicesFile).parse()
				&& new DatacentersParser(SimulationParameters.edgeDataCentersFile, TYPES.EDGE_DATACENTER).parse()
				&& new DatacentersParser(SimulationParameters.cloudDataCentersFile, TYPES.CLOUD).parse()
				&& new ParametersParser(SimulationParameters.simulationParametersFile).parse()
				&& new ApplicationFileParser(SimulationParameters.applicationFile).parse());
	}

	/**
	 * Allows to run parallel simulations to take advantage of available CPU cores,
	 * when this option is enabled in the simulation settings.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract
	 */
	protected void launchParallelSimulations() {

		// Get the number of CPU cores
		cpuCores = Runtime.getRuntime().availableProcessors();

		// Initialize the list of parallel simulations
		List<SimulationThread> simulationList = new ArrayList<>(cpuCores);

		// Generate the parallel simulations.
		for (int fromIteration = 0; fromIteration < Math.min(cpuCores, iterations.size()); fromIteration++) {
			// The number of parallel simulations will be limited by the minimum value
			// between CPU cores and number of scenarios.
			simulationList.add(new SimulationThread(this, fromIteration, cpuCores));
		}

		// Finally, run them.
		simulationList.parallelStream().forEach(SimulationThread::startSimulation);

	}

	/**
	 * Checks the configuration files and loads the simulation parameters. to know
	 * how to personalize the location from which these files are loaded:
	 * 
	 * @see #setCustomFilePath(String, Files)
	 * @see #setCustomSettingsFolder(String)
	 */
	protected void loadScenarios() {
		// Save the different simulation runs (i.e., scenarios) in the scenarios List
		for (int algorithmID = 0; algorithmID < SimulationParameters.orchestrationAlgorithms.length; algorithmID++) {
			for (int architectureID = 0; architectureID < SimulationParameters.orchestrationArchitectures.length; architectureID++) {
				for (int devicesCount = SimulationParameters.minNumberOfEdgeDevices; devicesCount <= SimulationParameters.maxNumberOfEdgeDevices; devicesCount += SimulationParameters.edgeDevicesIncrementationStepSize) {
					iterations.add(new Scenario(devicesCount, algorithmID, architectureID));
				}
			}
		}
	}

	/**
	 * @return duration The simulation duration.
	 * 
	 * @param startTime The time when simulation was started
	 * @param endTime   The time when simulation has been finished.
	 */
	protected String simulatioDuration(Date startTime, Date endTime) {
		long difference = endTime.getTime() - startTime.getTime();
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

	/**
	 * Gets the list of simulation scenarios.
	 * 
	 * @return the simulations runs.
	 */
	public List<Scenario> getScenarios() {
		return this.iterations;
	}

}
