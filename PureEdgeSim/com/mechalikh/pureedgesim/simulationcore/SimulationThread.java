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

import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;

import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.tasksgenerator.Task;
import com.mechalikh.pureedgesim.tasksgenerator.TasksGenerator;
import com.mechalikh.pureedgesim.tasksorchestration.Orchestrator;

public class SimulationThread {

	private int fromIteration;
	private int step;
	private Simulation simulation;

	/**
	 * Used to run parallel simulations. When parallelism is enabled in the
	 * simulation settings, the simulation runs are divided between the CPU cores.
	 * This division is done by splitting the loop that launches the different
	 * scenarios.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationcore.Simulation#launchParallelSimulations()
	 * @see #startSimulation()
	 * 
	 * @param simulation PureEdgeSim simulation object.
	 * @param fromIteration the iteration from which loop will start.
	 * @param step the iteration steps.
	 */
	public SimulationThread(Simulation simulation, int fromIteration, int step) {
		this.simulation= simulation;
		this.fromIteration = fromIteration;
		this.step = step;
	}

	/**
	 * Launches the simulation manager, and loops through the different scenarios.
	 */
	public void startSimulation() {
		// File name prefix
		String startTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		int iteration = 1;
		int simulationId = fromIteration + 1;
		boolean isFirstIteration = true;
		SimulationManager simulationManager;
		SimLog simLog = null;
		try { // Repeat the operation for different numbers of devices
			for (int it = fromIteration; it < simulation.getIterations().size()
					&& !SimulationParameters.STOP; it += step) {
				// New SimLog for each simulation (when parallelism is enabled
				simLog = new SimLog(startTime, isFirstIteration);

				// Clean output folder if it is the first iteration
				if (SimulationParameters.CLEAN_OUTPUT_FOLDER && isFirstIteration && fromIteration == 0) {
					simLog.cleanOutputFolder(SimulationAbstract.getOutputFolder());
				}
				isFirstIteration = false;

				// New simulation instance
				CloudSim cloudsim = new CloudSim(0.00001);

				// Initialize the simulation manager
				simulationManager = new SimulationManager(simLog, cloudsim, simulationId, iteration,
						simulation.getIterations().get(it));
				simLog.initialize(simulationManager, simulation.getIterations().get(it).getDevicesCount(),
						simulation.getIterations().get(it).getOrchAlgorithm(), simulation.getIterations().get(it).getOrchArchitecture());

				// Load custom classes and models
				loadModels(simulationManager);

				// Finally launch the simulation
				simulationManager.startSimulation();

				if (!SimulationParameters.PARALLEL) {
					pause(simLog);
				}
				iteration++;
				SimLog.println("");
				SimLog.println("SimLog- Iteration finished...");
				SimLog.println("");
				SimLog.println(
						"######################################################################################################################################################################");

			}
			SimLog.println("Main- Simulation Finished!");
			// Generate and save charts
			if (!SimulationParameters.STOP) // if no error happened
				generateCharts(simLog);

		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("Main- The simulation has been terminated due to an unexpected error");
		}
	}

	/**
	 * A pause of a few seconds between iterations. The duration of the pause is
	 * defined in the simulation parameters.
	 */
	private void pause(SimLog simLog) throws InterruptedException {
		// Take a few seconds pause to show the results
		simLog.print(SimulationParameters.PAUSE_LENGTH + " seconds peause...");
		for (int k = 1; k <= SimulationParameters.PAUSE_LENGTH; k++) {
			simLog.printSameLine(".");
			Thread.sleep(1000);
		}
		SimLog.println("");
	}

	/**
	 * Load the custom models and classes that are used in the simulation, if any.
	 * 
	 * @see #setCustomEdgeDataCenters(Class)
	 * @see #setCustomEdgeOrchestrator(Class)
	 * @see #setCustomEnergyModel(Class)
	 * @see #setCustomMobilityModel(Class)
	 * @see #setCustomNetworkModel(Class)
	 * @see #setCustomTasksGenerator(Class)
	 */
	private void loadModels(SimulationManager simulationManager) throws Exception {

		// Generate all data centers, servers, an devices
		DataCentersManager dataCentersManager = new DataCentersManager(simulationManager, simulation.mobilityModel, simulation.energyModel,
				simulation.edgedatacenter);
		dataCentersManager.generateDatacentersAndDevices();
		simulationManager.setDataCentersManager(dataCentersManager);

		// Generate tasks list
		Constructor<?> TasksGeneratorConstructor = simulation.tasksGenerator.getConstructor(SimulationManager.class);
		TasksGenerator tasksGenerator = (TasksGenerator) TasksGeneratorConstructor.newInstance(simulationManager);
		List<Task> tasksList = tasksGenerator.generate();
		simulationManager.setTasksList(tasksList);

		// Initialize the orchestrator
		Constructor<?> OrchestratorConstructor = simulation.orchestrator.getConstructor(SimulationManager.class);
		Orchestrator edgeOrchestrator = (Orchestrator) OrchestratorConstructor.newInstance(simulationManager);
		simulationManager.setOrchestrator(edgeOrchestrator);

		// Initialize the network model
		Constructor<?> networkConstructor = simulation.networkModel.getConstructor(SimulationManager.class);
		NetworkModel networkModel = (NetworkModel) networkConstructor.newInstance(simulationManager);
		simulationManager.setNetworkModel(networkModel);
	}

	/**
	 * Generate graphical plots of the results at the end of the simulation.
	 * 
	 * @param simLog the simulation logger.
	 */
	protected void generateCharts(SimLog simLog) {
		if (SimulationParameters.SAVE_CHARTS && !SimulationParameters.PARALLEL && simLog != null) {
			SimLog.println("Main- Saving charts...");
			ChartsGenerator chartsGenerator = new ChartsGenerator(simLog.getFileName(".csv"));
			chartsGenerator.generate();
		}
	}

}
