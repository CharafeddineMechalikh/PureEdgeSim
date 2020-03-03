package com.mechalikh.pureedgesim;

import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudsimplus.util.Log;
import com.mechalikh.pureedgesim.DataCentersManager.DefaultDataCenter;
import com.mechalikh.pureedgesim.DataCentersManager.EnergyModel;
import com.mechalikh.pureedgesim.DataCentersManager.ServersManager;
import com.mechalikh.pureedgesim.LocationManager.Mobility;
import com.mechalikh.pureedgesim.LocationManager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.Network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.Network.NetworkModel;
import com.mechalikh.pureedgesim.ScenarioManager.FilesParser;
import com.mechalikh.pureedgesim.ScenarioManager.Scenario;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.ChartsGenerator;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.DefaultTasksGenerator;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.mechalikh.pureedgesim.TasksGenerator.TasksGenerator;
import com.mechalikh.pureedgesim.TasksOrchestration.DefaultEdgeOrchestrator;
import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;
import com.mechalikh.pureedgesim.DataCentersManager.DefaultEnergyModel;
import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;

import ch.qos.logback.classic.Level;

public class MainApplication {

	public static enum Files {
		SIMULATION_PARAMETERS, APPLICATIONS_FILE, EDGE_DATACENTERS_FILE, EDGE_DEVICES_FILE, CLOUD_FILE
	}

	// Simulation scenario files
	protected static String simConfigfile = "PureEdgeSim/settings/simulation_parameters.properties";
	protected static String applicationsFile = "PureEdgeSim/settings/applications.xml";
	protected static String edgeDataCentersFile = "PureEdgeSim/settings/edge_datacenters.xml";
	protected static String edgeDevicesFile = "PureEdgeSim/settings/edge_devices.xml";
	protected static String cloudFile = "PureEdgeSim/settings/cloud.xml";
	protected static String outputFolder = "PureEdgeSim/output/";

	// Parallel simulation Parameters
	protected int fromIteration;
	protected int step = 1;
	protected static int cpuCores;
	protected static List<Scenario> Iterations = new ArrayList<>();
	protected static Class<? extends Mobility> mobilityManager = DefaultMobilityModel.class;
	protected static Class<? extends DataCenter> edgedatacenter = DefaultDataCenter.class;
	protected static Class<? extends TasksGenerator> tasksGenerator = DefaultTasksGenerator.class;
	protected static Class<? extends Orchestrator> orchestrator = DefaultEdgeOrchestrator.class;
	protected static Class<? extends EnergyModel> energyModel = DefaultEnergyModel.class;
	protected static Class<? extends NetworkModel> networkModel = DefaultNetworkModel.class;

	public static void main(String[] args) {
		launchSimulation();
	}

	public static void launchSimulation() {
		SimLog.println("Main- Loading simulation files...");

		// Check files
		FilesParser fp = new FilesParser();
		if (!fp.checkFiles(simConfigfile, edgeDevicesFile, edgeDataCentersFile, applicationsFile, cloudFile))
			Runtime.getRuntime().exit(0); // if files aren't correct stop everything.

		// Disable cloudsim plus log
		if (!simulationParameters.DEEP_LOGGING)
			Log.setLevel(Level.OFF);
		else
			Log.setLevel(Level.ALL);

		Date startDate = Calendar.getInstance().getTime();

		// Walk through all orchestration scenarios
		loadScenarios();

		if (simulationParameters.PARALLEL) {
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
		for (int algorithmID = 0; algorithmID < simulationParameters.ORCHESTRATION_AlGORITHMS.length; algorithmID++) {
			// Repeat the operation of the whole set of criteria
			for (int architectureID = 0; architectureID < simulationParameters.ORCHESTRATION_ARCHITECTURES.length; architectureID++) {
				for (int devicesCount = simulationParameters.MIN_NUM_OF_EDGE_DEVICES; devicesCount <= simulationParameters.MAX_NUM_OF_EDGE_DEVICES; devicesCount += simulationParameters.EDGE_DEVICE_COUNTER_STEP) {
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
			for (int it = fromIteration; it < Iterations.size() && !simulationParameters.STOP; it += step) {
				// New SimLog for each simulation (when parallelism is enabled
				simLog = new SimLog(startTime, isFirstIteration);

				// Clean output folder if it is the first iteration
				if (simulationParameters.CLEAN_OUTPUT_FOLDER && isFirstIteration && fromIteration == 0) {
					simLog.cleanOutputFolder(outputFolder);
				}
				isFirstIteration = false;

				// New simulation instance
				CloudSim simulation = new CloudSim();

				// Initialize the simulation manager
				simulationManager = new SimulationManager(simLog, simulation, simulationId, iteration,
						Iterations.get(it));
				simLog.initialize(simulationManager, Iterations.get(it).getDevicesCount(),
						Iterations.get(it).getOrchAlgorithm(), Iterations.get(it).getOrchArchitecture());

				// Load custom classes and models
				loadModels(simulationManager);

				// Finally launch the simulation
				simulationManager.startSimulation();

				if (!simulationParameters.PARALLEL) {
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
			if (!simulationParameters.STOP) // if no error happened
				generateCharts(simLog);

		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("Main- The simulation has been terminated due to an unexpected error");
		}
	}

	private void pause(SimLog simLog) throws InterruptedException {
		// Take a few seconds pause to show the results
		simLog.print(simulationParameters.PAUSE_LENGTH + " seconds peause...");
		for (int k = 1; k <= simulationParameters.PAUSE_LENGTH; k++) {
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
		NetworkModel networkModel = (NetworkModel) networkConstructor.newInstance(simulationManager);
		simulationManager.setNetworkModel(networkModel);
	}

	protected void generateCharts(SimLog simLog) {
		if (simulationParameters.SAVE_CHARTS && !simulationParameters.PARALLEL && simLog != null) {
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

	public static String getOutputFolder() {
		return outputFolder;
	}

	protected static void setCustomEdgeDataCenters(Class<? extends DataCenter> edgedatacenter2) {
		edgedatacenter = edgedatacenter2;
	}

	protected static void setCustomTasksGenerator(Class<? extends TasksGenerator> tasksGenerator2) {
		tasksGenerator = tasksGenerator2;
	}

	protected static void setCustomEdgeOrchestrator(Class<? extends Orchestrator> orchestrator2) {
		orchestrator = orchestrator2;
	}

	protected static void setCustomMobilityModel(Class<? extends Mobility> mobilityManager2) {
		mobilityManager = mobilityManager2;
	}

	protected static void setCustomEnergyModel(Class<? extends EnergyModel> energyModel2) {
		energyModel = energyModel2;
	}

	protected static void setCustomNetworkModel(Class<? extends NetworkModel> networkModel2) {
		networkModel = networkModel2;
	}

	protected static void setCustomOutputFolder(String outputFolder2) {
		outputFolder = outputFolder2;
	}

	protected static void setCustomSettingsFolder(String settingsFolder) {
		setCustomFilePath(settingsFolder + "simulation_parameters.properties", Files.SIMULATION_PARAMETERS);
		setCustomFilePath(settingsFolder + "applications.xml", Files.APPLICATIONS_FILE);
		setCustomFilePath(settingsFolder + "edge_datacenters.xml", Files.EDGE_DATACENTERS_FILE);
		setCustomFilePath(settingsFolder + "edge_devices.xml", Files.EDGE_DEVICES_FILE);
		setCustomFilePath(settingsFolder + "cloud.xml", Files.CLOUD_FILE);
	}

	protected static void setCustomFilePath(String path, Files file) {
		switch (file) {
		case SIMULATION_PARAMETERS:
			simConfigfile = path;
			break;
		case APPLICATIONS_FILE:
			applicationsFile = path;
			break;
		case EDGE_DATACENTERS_FILE:
			edgeDataCentersFile = path;
			break;
		case EDGE_DEVICES_FILE:
			edgeDevicesFile = path;
			break;
		case CLOUD_FILE:
			cloudFile = path;
			break;
		default:
			SimLog.println("Unknown file type");
			break;
		}
	}

}
