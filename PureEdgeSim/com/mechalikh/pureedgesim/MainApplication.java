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
package com.mechalikh.pureedgesim;

import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudsimplus.util.Log;
import com.mechalikh.pureedgesim.DataCentersManager.ServersManager;
import com.mechalikh.pureedgesim.Network.NetworkModelAbstract;
import com.mechalikh.pureedgesim.ScenarioManager.FilesParser;
import com.mechalikh.pureedgesim.ScenarioManager.Scenario;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.ChartsGenerator;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.mechalikh.pureedgesim.TasksGenerator.TasksGenerator;
import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;

import ch.qos.logback.classic.Level;

public class MainApplication extends MainApplicationAbstract{

	
	public static void main(String[] args) {
		launchSimulation();
	}

	public static void launchSimulation() {
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
			new MainApplication(0, 1).startSimulation();
		}

		// Simulation Finished
		Date endDate = Calendar.getInstance().getTime();
		SimLog.println("Main- Simulation took : " + simulationTime(startDate, endDate));
		SimLog.println("Main- results were saved to the folder: " + outputFolder);

	}

	private static void launchParallelSimulations() {
		cpuCores = Runtime.getRuntime().availableProcessors();
		List<MainApplication> simulationList = new ArrayList<>(cpuCores);

		// Generate the parallel simulations
		for (int fromIteration = 0; fromIteration < Math.min(cpuCores, Iterations.size()); fromIteration++) {
			// The number of parallel simulations will be limited by the minimum value
			// between cpu cores and number of iterations
			simulationList.add(new MainApplication(fromIteration, cpuCores));
		}

		// Finally then runs them
		// tag::parallelExecution[]
		simulationList.parallelStream().forEach(MainApplication::startSimulation);
		// end::parallelExecution[]

	}

	private static void loadScenarios() {
		for (int algorithmID = 0; algorithmID < SimulationParameters.ORCHESTRATION_AlGORITHMS.length; algorithmID++) {
			// Repeat the operation of the whole set of criteria
			for (int architectureID = 0; architectureID < SimulationParameters.ORCHESTRATION_ARCHITECTURES.length; architectureID++) {
				for (int devicesCount = SimulationParameters.MIN_NUM_OF_EDGE_DEVICES; devicesCount <= SimulationParameters.MAX_NUM_OF_EDGE_DEVICES; devicesCount += SimulationParameters.EDGE_DEVICE_COUNTER_STEP) {
					Iterations.add(new Scenario(devicesCount, algorithmID, architectureID));
				}
			}
		}
	}

	public MainApplication(int fromIteration, int step_) {
		this.fromIteration = fromIteration;
		step = step_;
	}

	public void startSimulation() {
		// File name prefix
		String startTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		int iteration = 1;
		int simulationId = fromIteration + 1;
		boolean isFirstIteration = true;
		SimulationManager simulationManager;
		SimLog simLog = null;
		try { // Repeat the operation for different number of devices
			for (int it = fromIteration; it < Iterations.size() && !SimulationParameters.STOP; it += step) {
				// New SimLog for each simulation (when parallelism is enabled
				simLog = new SimLog(startTime, isFirstIteration);

				// Clean output folder if it is the first iteration
				if (SimulationParameters.CLEAN_OUTPUT_FOLDER && isFirstIteration && fromIteration == 0) {
					simLog.cleanOutputFolder(outputFolder);
				}
				isFirstIteration = false;

				// New simulation instance
				CloudSim simulation = new CloudSim(0.00001);

				// Initialize the simulation manager
				simulationManager = new SimulationManager(simLog, simulation, simulationId, iteration,
						Iterations.get(it));
				simLog.initialize(simulationManager, Iterations.get(it).getDevicesCount(),
						Iterations.get(it).getOrchAlgorithm(), Iterations.get(it).getOrchArchitecture());

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

	private void pause(SimLog simLog) throws InterruptedException {
		// Take a few seconds pause to show the results
		simLog.print(SimulationParameters.PAUSE_LENGTH + " seconds peause...");
		for (int k = 1; k <= SimulationParameters.PAUSE_LENGTH; k++) {
			simLog.printSameLine(".");
			Thread.sleep(1000);
		}
		SimLog.println("");
	}

	private void loadModels(SimulationManager simulationManager) throws Exception {

		// Generate all data centers, servers, an devices
		ServersManager serversManager = new ServersManager(simulationManager, mobilityManager, energyModel,
				edgedatacenter);
		serversManager.generateDatacentersAndDevices();
		simulationManager.setServersManager(serversManager);

		// Generate tasks list
		Constructor<?> TasksGeneratorConstructor = tasksGenerator.getConstructor(SimulationManager.class);
		TasksGenerator tasksGenerator = (TasksGenerator) TasksGeneratorConstructor.newInstance(simulationManager);
		List<Task> tasksList = tasksGenerator.generate();
		simulationManager.setTasksList(tasksList);

		// Initialize the orchestrator
		Constructor<?> OrchestratorConstructor = orchestrator.getConstructor(SimulationManager.class);
		Orchestrator edgeOrchestrator = (Orchestrator) OrchestratorConstructor.newInstance(simulationManager);
		simulationManager.setOrchestrator(edgeOrchestrator);

		// Initialize the network model
		Constructor<?> networkConstructor = networkModel.getConstructor(SimulationManager.class);
		NetworkModelAbstract networkModel = (NetworkModelAbstract) networkConstructor.newInstance(simulationManager);
		simulationManager.setNetworkModel(networkModel);
	}

	protected void generateCharts(SimLog simLog) {
		if (SimulationParameters.SAVE_CHARTS && !SimulationParameters.PARALLEL && simLog != null) {
			SimLog.println("Main- Saving charts...");
			ChartsGenerator chartsGenerator = new ChartsGenerator(simLog.getFileName(".csv"));
			chartsGenerator.generate();
		}
	}

	private static String simulationTime(Date startDate, Date endDate) {
		long difference = endDate.getTime() - startDate.getTime();
		long seconds = difference / 1000 % 60;
		long minutes = difference / (60 * 1000) % 60;
		long hours = difference / (60 * 60 * 1000) % 24;
		long days = difference / (24 * 60 * 60 * 1000);
		String results = "";
		if (days > 0)
			results += days + " days, ";
		if (hours > 0)
			results += hours + " hours, ";
		results += minutes + " minutes, " + seconds + " seconds.";
		return results;
	}

	

}
