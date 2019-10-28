package com.Mechalikh.PureEdgeSim;

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
import com.Mechalikh.PureEdgeSim.ScenarioManager.FilesParser;
import com.Mechalikh.PureEdgeSim.ScenarioManager.Scenario;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimLog;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimulationManager;
import com.Mechalikh.PureEdgeSim.SimulationManager.ChartsGenerator;

import ch.qos.logback.classic.Level;

public class Main {
	// Simulation scenario files
	static String simConfigfile;
	static String applicationsFile;
	static String fogDevicesFile;
	static String edgeDevicesFile;
	static String cloudFile;
	public static String outputFolder;

	private CloudSim simulation;

	// Parallel simulation Parameters
	private int fromIteration;
	private static int step=1;
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
				System.exit(0); // if files aren't correct stop everything.
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		// Disable cloudsim plus log
		Log.setLevel(Level.OFF);

		Date startDate = Calendar.getInstance().getTime();

		// Walk through all orchestration scenarios
		for (int algorithmID = 0; algorithmID < SimulationParameters.ORCHESTRATION_AlGORITHMS.length; algorithmID++) {
			// Repeat the operation of the whole set of criteria
			for (int architectureID = 0; architectureID < SimulationParameters.ORCHESTRATION_ARCHITECTURES.length; architectureID++) {
				for (int devicesCount = SimulationParameters.MIN_NUM_OF_EDGE_DEVICES; devicesCount <= SimulationParameters.MAX_NUM_OF_EDGE_DEVICES; devicesCount += SimulationParameters.EDGE_DEVICE_COUNTER_STEP) {
					Iterations.add(new Scenario(devicesCount, algorithmID, architectureID));
				}
			}
		}
		if (SimulationParameters.PARALLEL) {
			cpuCores = Runtime.getRuntime().availableProcessors();
			List<Main> simulationList = new ArrayList<>(cpuCores);

			// Generate the parallel simulations
			for (int fromIteration = 0; fromIteration < Math.min(cpuCores, Iterations.size()); fromIteration++) {
				// The number of parallel simulations will be limited by the minimum value
				// between cpu cores and number of iterations
				step=cpuCores;
				simulationList.add(new Main(fromIteration, step));
			}

			// Finally then runs them
			// tag::parallelExecution[]
			simulationList.parallelStream().forEach(Main::startSimulation);
			// end::parallelExecution[]
			
		} else // Sequential execution
			new Main(0, step).startSimulation();

		// Simulation Finished
		Date endDate = Calendar.getInstance().getTime();
		SimLog.println("Main- Simulation took : " + simulationTime(startDate, endDate));
		SimLog.println("Main- results were saved to the folder: " + outputFolder);

	}

	public Main(int fromIteration, int step_) {
		this.fromIteration = fromIteration;
		step=step_; 
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

				if (SimulationParameters.CLEAN_OUTPUT_FOLDER && isFirstIteration && fromIteration == 0) {
					simLog.cleanOutputFolder(outputFolder);
				}
				isFirstIteration = false;

				// New simulation instance
				simulation = new CloudSim(); 
				 
				// Starting simulation
				simulationManager = new SimulationManager(simLog, simulation, simulationId, iteration, Iterations.get(it));
				simLog.initialize(simulationManager, Iterations.get(it).getDevicesCount(),Iterations.get(it).getOrchAlgorithm(), Iterations.get(it).getOrchArchitecture());
				
				simulationManager.startSimulation(); 
			
				if (!SimulationParameters.PARALLEL) {
					// Take a few seconds pause to show the results
					simLog.print(SimulationParameters.PAUSE_LENGTH + " seconds peause...");
					for (int k = 1; k <= SimulationParameters.PAUSE_LENGTH; k++) {
						simLog.printSameLine(".");
						Thread.sleep(1000);
					}
					SimLog.println("");
				}
				iteration++;
				SimLog.println("");
				SimLog.println("SimLog- Iteration finished...");
				SimLog.println("");
				SimLog.println("######################################################################################################################################################################");

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

		if(SimulationParameters.SAVE_CHARTS && !SimulationParameters.PARALLEL && simLog!=null) { 
		SimLog.println("Main- Saving charts...");
		ChartsGenerator chartsGenerator =new ChartsGenerator(simLog.getFileName(".csv"));
		
		chartsGenerator.displayChart("Edge devices count","Average wainting time (s)", "Time (s)","/Delays") ;
		chartsGenerator.displayChart("Edge devices count","Average execution delay (s)", "Time (s)","/Delays") ;
		
		chartsGenerator.displayChart("Edge devices count","Tasks successfully executed", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Tasks failed (delay)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Tasks failed (device dead)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Tasks failed (mobility)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Tasks not generated due to the death of devices", "Number of tasks","/Tasks") ;
		
		chartsGenerator.displayChart("Edge devices count","Total tasks executed (Cloud)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Tasks successfully executed (Cloud)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Total tasks executed (Fog)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Tasks successfully executed (Fog)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Total tasks executed (Edge)", "Number of tasks","/Tasks") ;
		chartsGenerator.displayChart("Edge devices count","Tasks successfully executed (Edge)", "Number of tasks","/Tasks") ;
		
		chartsGenerator.displayChart("Edge devices count","Network usage (s)", "Time (s)","/Network") ;
		chartsGenerator.displayChart("Edge devices count","Wan usage (s)", "Time (s)","/Network") ; 
		chartsGenerator.displayChart("Edge devices count","Average bandwidth per task (Mbps)","Bandwidth (Mbps)","/Network" ) ; 
		if(SimulationParameters.ENABLE_REGISTRY) {
		chartsGenerator.displayChart("Edge devices count","Containers wan usage (s)", "Time (s)","/Network") ;
		chartsGenerator.displayChart("Edge devices count","Containers lan usage (s)", "Time (s)","/Network") ;
		} 

		chartsGenerator.displayChart("Edge devices count","Average VM CPU usage (%)", "CPU utilization (%)","/CPU Utilization") ;
		chartsGenerator.displayChart("Edge devices count","Average VM CPU usage (Cloud) (%)","CPU utilization (%)","/CPU Utilization") ;
		chartsGenerator.displayChart("Edge devices count","Average VM CPU usage (Fog) (%)","CPU utilization (%)","/CPU Utilization") ;
		chartsGenerator.displayChart("Edge devices count","Average VM CPU usage (Edge) (%)","CPU utilization (%)","/CPU Utilization") ;
		
		chartsGenerator.displayChart("Edge devices count","Energy consumption (Wh)", "Consumed energy (Wh)","/Energy") ;
		chartsGenerator.displayChart("Edge devices count","Average energy consumption (Wh/Data center)", "Consumed energy (Wh)","/Energy") ;
		chartsGenerator.displayChart("Edge devices count","Cloud energy consumption (Wh)", "Consumed energy (Wh)","/Energy") ;
		chartsGenerator.displayChart("Edge devices count","Average Cloud energy consumption (Wh/Data center)", "Consumed energy (Wh)","/Energy") ;
		chartsGenerator.displayChart("Edge devices count","Fog energy consumption (Wh)", "Consumed energy (Wh)","/Energy") ;
		chartsGenerator.displayChart("Edge devices count","Average Fog energy consumption (Wh/Data center)", "Consumed energy (Wh)","/Energy") ;
		chartsGenerator.displayChart("Edge devices count","Edge energy consumption (Wh)", "Consumed energy (Wh)","/Energy") ;
		chartsGenerator.displayChart("Edge devices count","Average Edge energy consumption (Wh/Device)", "Consumed energy (Wh)","/Energy") ;
		 
		chartsGenerator.displayChart("Edge devices count","Dead devices count", "Count", "/Edge Devices") ;
		chartsGenerator.displayChart("Edge devices count","Average remaining power (Wh)", "Remaining energy (Wh)", "/Edge Devices") ;
		chartsGenerator.displayChart("Edge devices count","Average remaining power (%)", "Remaining energy (%)", "/Edge Devices") ;
		chartsGenerator.displayChart("Edge devices count","First edge device death time (s)", "Time (s)", "/Edge Devices") ;
		
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
