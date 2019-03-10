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

import ch.qos.logback.classic.Level;

public class Main { 
	static String simConfigfile;
	static String applicationsFile;
	static String fogDevicesFile;
	static String edgeDevicesFile;
	static String cloudFile;
	public static String outputFolder;
	private CloudSim simulation; 
	private int fromIteration;
	private int Cores; 
	private static List<Scenario> Iterations=new ArrayList<Scenario>();

	public static void main(String[] args) {
		SimLog.println("Main, Loading simulation files...");
		simConfigfile = "PureEdgeSim/settings/simulation_parameters.properties";
		applicationsFile = "PureEdgeSim/settings/applications.xml";
		fogDevicesFile = "PureEdgeSim/settings/fog_devices.xml";
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
		for (int orch = 0; orch < SimulationParameters.ORCHESTRATOR_POLICIES.length; orch ++) {
			// Repeat the operation of the whole set of criteria
					for (int cri = 0; cri < SimulationParameters.ORCHESTRATOR_CRITERIA.length; cri++) {
						for (int dev = SimulationParameters.MIN_NUM_OF_EDGE_DEVICES; dev <= SimulationParameters.MAX_NUM_OF_EDGE_DEVICES; dev += SimulationParameters.EDGE_DEVICE_COUNTER_STEP) {
							
				Iterations.add(new Scenario(dev,orch,cri));
			}}}  
		if (SimulationParameters.PARALLEL) {
			// getting the number of cores on this machine
		int	cores = Runtime.getRuntime().availableProcessors();
 

			// now we create a list to store parallel simulation instances
			List<Main> simulationList = new ArrayList<>(cores); 
			// and then we generate the parallel simulations
			for (int i = 0; i < Math.min(cores, Iterations.size()); i ++) {
				// parallel execution, helps in reducing execution time
				// to do so we will divide the orchestration policies among them

				// Initialize the simulation environment, each simulation instance with its
				// specific orchestrations policies
				simulationList.add(new Main(i,  cores )); 
			}

			// and finally then runs them
			// tag::parallelExecution[]
			simulationList.parallelStream().forEach(Main::run);
			// end::parallelExecution[]
		} else
			new Main(0,  1).run(); // Sequential execution

		Date endDate = Calendar.getInstance().getTime();
		SimLog.println("Main, Simulation took : " + timeDiff(startDate, endDate)); 
		SimLog.println("Main, results were saved to the folder: "+outputFolder);

	}

	public Main(int fromIteration,  int Cores) {
		this.fromIteration = fromIteration; 
		this.Cores = Cores;
	}

	public void run() {
		int iteration = 1;
	    String startTime=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()); //file name prefix
		boolean isFirstIteration=true;
		try { // Repeat the operation for different number of devices
			 		for (int it = fromIteration; it < Iterations.size(); it+=Cores) { 
						SimLog simlog = new SimLog(fromIteration+1, startTime,isFirstIteration); // creating new SimLog instances for each simulation instance;
					 
						if (SimulationParameters.CLEAN_OUTPUT_FOLDER  && isFirstIteration) {
							simlog.cleanOutputFolder(outputFolder); 
							isFirstIteration=false; 
						} else if(isFirstIteration) {
						isFirstIteration=false; 
						}
						simulation = new CloudSim(); // new simulation instance

						// add time stamps to the log by using simulation.clock()
						simlog.init(Iterations.get(it).getDevicesCount(), Iterations.get(it).getOrchPolicy(), Iterations.get(it).getOrchCriteria(), simulation);

						simlog.print("Main, Starting Simulation: " + (fromIteration+1) + " iteration: " + iteration);

					 
						// starting simulation
						SimulationManager SimMan = new SimulationManager(simlog, simulation, Iterations.get(it));
						SimMan.startSimulation(); 
						// take a few seconds pause to show the results (if the simulation is not
						// parallel)
						if (!SimulationParameters.PARALLEL) {
							simlog.print(SimulationParameters.PAUSE_LENGTH + " seconds peause...");
							for (int k = 1; k <= SimulationParameters.PAUSE_LENGTH; k++) {
								simlog.printSameLine(".");
								Thread.sleep(1000);
							}
						}
						iteration++;
						simlog.printWithoutTime("");
						simlog.print("SimLog, Iteration finished...");
						simlog.printWithoutTime(
								"######################################################################################################################################################################");
						simlog.printWithoutTime("");
					} 
			SimLog.println("Main, Simulation Finished!");
		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("Main, The simulation has been terminated due to an unexpected error");
		}

	}

	

	private static String timeDiff(Date startDate, Date endDate) {
		long diff = endDate.getTime() - startDate.getTime();

		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		long diffDays = diff / (24 * 60 * 60 * 1000);
		String results = "";
		if (diffDays > 0)
			results += diffDays + " days, ";
		if (diffHours > 0)
			results += diffHours + " hours, ";
		results += diffMinutes + " minutes, " + diffSeconds + " seconds.";
		return results;
	}
}
