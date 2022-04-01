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

import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.tasksgenerator.Task;
import com.mechalikh.pureedgesim.tasksgenerator.TasksGenerator;
import com.mechalikh.pureedgesim.tasksorchestration.Orchestrator;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;

/**
 * The {@code SimulationThread} class allows to run parallel simulations.
 * <p>
 * When parallelism is enabled in the {@code simulation_parameters.propoerties}
 * file (i.e. {@code parallel_simulation = true}), the simulations will be
 * distributed among the available CPU cores and instances of this class will be
 * created, where each of them handles a part of the simulations. These
 * instances are then run in parallel to reduce simulation time.
 * <p>
 * In the opposite case, i.e. sequential simulation, only one instance will be
 * created and will be responsible for all simulations.
 * <p>
 * This distribution takes place in
 * {@link Simulation#launchParallelSimulations() launchParallelSimulations()}.
 * Once done, the {@link #startSimulation()} method is called to initialize the
 * different modules and then launch the discrete event simulation.
 * 
 * @see #startSimulation()
 * @see com.mechalikh.pureedgesim.simulationmanager.Simulation#launchParallelSimulations()
 * @see com.mechalikh.pureedgesim.simulationengine.PureEdgeSim#start()
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 4.2
 */
public class SimulationThread {

	/**
	 * The iteration/scenario from which loop will start.
	 */
	private int fromIteration;

	/**
	 * The iteration step.
	 */
	private int step;

	/**
	 * PureEdgeSim simulation object.
	 */
	private Simulation simulation;

	/**
	 * Used to run parallel simulations. When parallelism is enabled in the
	 * simulation settings, the simulation runs are divided between the CPU cores.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.Simulation#launchParallelSimulations()
	 * @see #startSimulation()
	 * 
	 * @param simulation    PureEdgeSim simulation object.
	 * @param fromIteration The iteration/scenario from which loop will start.
	 * @param step          The iteration step.
	 */
	public SimulationThread(Simulation simulation, int fromIteration, int step) {
		this.simulation = simulation;
		this.fromIteration = fromIteration;
		this.step = step;
	}

	/**
	 * Loops through the different scenarios, and launches the simulation manager
	 * and the other modules.
	 */
	public void startSimulation() {

		// The results file name prefix.
		String startTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

		int iteration = 1;
		int simulationId = fromIteration + 1;
		boolean isFirstIteration = true;
		SimulationManager simulationManager;
		SimLog simLog = null;
		try {
			// Repeat the operation for different numbers of devices.
			for (int it = fromIteration; it < simulation.getScenarios().size(); it += step) {

				// New SimLog for each simulation (when parallelism is enabled).
				simLog = new SimLog(startTime, isFirstIteration);

				// Clean output folder if it is the first iteration.
				if (SimulationParameters.CLEAN_OUTPUT_FOLDER && isFirstIteration && fromIteration == 0) {
					simLog.cleanOutputFolder();
				}
				isFirstIteration = false;

				// New instance of the PureEdgeSim simulation engine.
				PureEdgeSim pureEdgeSim = new PureEdgeSim();

				// Initialize the simulation manager.
				simulationManager = new SimulationManager(simLog, pureEdgeSim, simulationId, iteration,
						simulation.getScenarios().get(it));
				simLog.initialize(simulationManager, simulation.getScenarios().get(it).getDevicesCount(),
						simulation.getScenarios().get(it).getOrchAlgorithm(),
						simulation.getScenarios().get(it).getOrchArchitecture());

				// Load custom classes and models.
				loadModels(simulationManager);

				// Finally, launch the simulation.
				simulationManager.startSimulation();

				// Take a few seconds pause to display results, if parallelism is disabled.
				if (!SimulationParameters.PARALLEL) {
					pause(simLog);
				}
				iteration++;
				SimLog.println("");
				SimLog.println(getClass().getSimpleName() + " - Iteration finished...");
				SimLog.println("");
				SimLog.println(
						"######################################################################################################################################################################");

			}
			SimLog.println("Main- Simulation Finished!");
			// Generate and save charts.
			generateCharts(simLog);

		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println(getClass().getSimpleName()+" - The simulation has been terminated due to an unexpected error");
		}
	}

	/**
	 * A pause of a few seconds between iterations. The duration of the pause is
	 * defined in the simulation parameters.
	 * 
	 * @param simLog The simulation logger.
	 */
	private void pause(SimLog simLog) throws InterruptedException {
		// Take a few seconds pause to show the results.
		simLog.print(SimulationParameters.PAUSE_LENGTH + " seconds peause...");
		for (int k = 1; k <= SimulationParameters.PAUSE_LENGTH; k++) {
			simLog.printSameLine(".");
			Thread.sleep(1000);
		}
		SimLog.println("");
	}

	/**
	 * Loads the custom models and classes that are used in the simulation, if any.
	 * 
	 * @see #setCustomEdgeDataCenters(Class)
	 * @see #setCustomEdgeOrchestrator(Class)
	 * @see #setCustomEnergyModel(Class)
	 * @see #setCustomMobilityModel(Class)
	 * @see #setCustomNetworkModel(Class)
	 * @see #setCustomTasksGenerator(Class)
	 * 
	 * @param simulationManager the simulation manager
	 */
	private void loadModels(SimulationManager simulationManager) throws Exception {

		// Initialize the network model
		Constructor<?> networkConstructor = simulation.networkModel.getConstructor(SimulationManager.class);
		NetworkModel networkModel = (NetworkModel) networkConstructor.newInstance(simulationManager);
		simulationManager.setNetworkModel(networkModel);

		// Generate all data centers, servers, an devices
		new DataCentersManager(simulationManager, simulation.mobilityModel, simulation.computingNode);

		// Generate tasks list
		Constructor<?> TasksGeneratorConstructor = simulation.tasksGenerator.getConstructor(SimulationManager.class);
		TasksGenerator tasksGenerator = (TasksGenerator) TasksGeneratorConstructor.newInstance(simulationManager);
		List<Task> tasksList = tasksGenerator.generate();
		simulationManager.setTasksList(tasksList);

		// Initialize the orchestrator
		Constructor<?> OrchestratorConstructor = simulation.orchestrator.getConstructor(SimulationManager.class);
		Orchestrator edgeOrchestrator = (Orchestrator) OrchestratorConstructor.newInstance(simulationManager);
		simulationManager.setOrchestrator(edgeOrchestrator);

	}

	/**
	 * Generates graphical plots of the results at the end of the simulation (if
	 * enabled).
	 * 
	 * @param simLog the simulation logger.
	 */
	protected void generateCharts(SimLog simLog) {
		if (SimulationParameters.SAVE_CHARTS && !SimulationParameters.PARALLEL && simLog != null) {
			SimLog.println(getClass().getSimpleName() + " - Saving charts...");
			ChartsGenerator chartsGenerator = new ChartsGenerator(simLog.getFileName(".csv"));
			chartsGenerator.generate();
		}
	}

}
