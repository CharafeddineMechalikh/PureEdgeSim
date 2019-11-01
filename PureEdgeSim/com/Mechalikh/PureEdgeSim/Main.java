package com.mechalikh.pureedgesim;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudsimplus.util.Log;
import org.xml.sax.SAXException;

import com.mechalikh.pureedgesim.ScenarioManager.FilesParser;
import com.mechalikh.pureedgesim.ScenarioManager.Scenario;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.ChartsGenerator;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;

import ch.qos.logback.classic.Level;

public class Main {
	// Simulation scenario files
	private static String simConfigfile;
	private static String applicationsFile;
	private static String fogDevicesFile;
	private static String edgeDevicesFile;
	private static String cloudFile;
	private static String outputFolder;

	// Parallel simulation Parameters
	private int fromIteration;
	private int step = 1;
	private static int cpuCores;
	private static List<Scenario> Iterations = new ArrayList<Scenario>();

	public static void main(String[] args) {
		SimLog.println("Main- Loading simulation files...");
		simConfigfile = "PureEdgeSim/settings/simulation_parameters.properties";
		applicationsFile = "PureEdgeSim/settings/applications.xml";
		fogDevicesFile = "PureEdgeSim/settings/fog_servers.xml";
		edgeDevicesFile = "PureEdgeSim/settings/edge_devices.xml";
		cloudFile = "PureEdgeSim/settings/cloud.xml";
		outputFolder = "PureEdgeSim/output/";

		// Check files
		FilesParser fp = new FilesParser();
		try {
			if (!fp.checkFiles(simConfigfile, edgeDevicesFile, fogDevicesFile, applicationsFile, cloudFile))
				Runtime.getRuntime().exit(0); // if files aren't correct stop everything.
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		// Disable cloudsim plus log
		Log.setLevel(Level.OFF);

		Date startDate = Calendar.getInstance().getTime();

		// Walk through all orchestration scenarios
		for (int algorithmID = 0; algorithmID < simulationParameters.ORCHESTRATION_AlGORITHMS.length; algorithmID++) {
			// Repeat the operation of the whole set of criteria
			for (int architectureID = 0; architectureID < simulationParameters.ORCHESTRATION_ARCHITECTURES.length; architectureID++) {
				for (int devicesCount = simulationParameters.MIN_NUM_OF_EDGE_DEVICES; devicesCount <= simulationParameters.MAX_NUM_OF_EDGE_DEVICES; devicesCount += simulationParameters.EDGE_DEVICE_COUNTER_STEP) {
					Iterations.add(new Scenario(devicesCount, algorithmID, architectureID));
				}
			}
		}
		if (simulationParameters.PARALLEL) {
			cpuCores = Runtime.getRuntime().availableProcessors();
			List<Main> simulationList = new ArrayList<>(cpuCores);

			// Generate the parallel simulations
			for (int fromIteration = 0; fromIteration < Math.min(cpuCores, Iterations.size()); fromIteration++) {
				// The number of parallel simulations will be limited by the minimum value
				// between cpu cores and number of iterations
				simulationList.add(new Main(fromIteration, cpuCores));
			}

			// Finally then runs them
			// tag::parallelExecution[]
			simulationList.parallelStream().forEach(Main::startSimulation);
			// end::parallelExecution[]

		} else // Sequential execution
			new Main(0, 1).startSimulation();

		// Simulation Finished
		Date endDate = Calendar.getInstance().getTime();
		SimLog.println("Main- Simulation took : " + simulationTime(startDate, endDate));
		SimLog.println("Main- results were saved to the folder: " + outputFolder);

	}

	public Main(int fromIteration, int step_) {
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
			for (int it = fromIteration; it < Iterations.size(); it += step) {
				// New simlog for each simulation (when parallelism is enabled
				simLog = new SimLog(startTime, isFirstIteration);

				if (simulationParameters.CLEAN_OUTPUT_FOLDER && isFirstIteration && fromIteration == 0) {
					simLog.cleanOutputFolder(outputFolder);
				}
				isFirstIteration = false;

				// New simulation instance
				CloudSim simulation = new CloudSim();

				// Starting simulation
				simulationManager = new SimulationManager(simLog, simulation, simulationId, iteration,
						Iterations.get(it));
				simLog.initialize(simulationManager, Iterations.get(it).getDevicesCount(),
						Iterations.get(it).getOrchAlgorithm(), Iterations.get(it).getOrchArchitecture());

				simulationManager.startSimulation();

				if (!simulationParameters.PARALLEL) {
					// Take a few seconds pause to show the results
					simLog.print(simulationParameters.PAUSE_LENGTH + " seconds peause...");
					for (int k = 1; k <= simulationParameters.PAUSE_LENGTH; k++) {
						simLog.printSameLine(".");
						Thread.sleep(1000);
					}
					SimLog.println("");
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
			generateCharts(simLog);

		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("Main- The simulation has been terminated due to an unexpected error");
		} 
	}

	private void generateCharts(SimLog simLog) { 
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
}
