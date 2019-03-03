package com.Mechalikh.PureEdgeSim.SimulationManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.cloudbus.cloudsim.core.CloudSim;

import com.Mechalikh.PureEdgeSim.Main;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.DataCentersManager.ServersManager;
import com.Mechalikh.PureEdgeSim.Network.NetworkModel;
import com.Mechalikh.PureEdgeSim.Network.taskTransferProgress;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class SimLog {
	private List<String> ListCSV = new ArrayList<String>();
	private static DecimalFormat dft;
	private CloudSim simulation;
	public static final int NO_TIME = 0;
	public static final int SAME_LINE = 1;
	public static final int DEFAULT = 2;
	private List<String> Log = new ArrayList<String>();
	private BufferedWriter writer;
	private String outputFolder;
	private int generatedTasks;
	private int clouddc = 0;
	private int fogdc = 0;
	private String currentOrchPolicy;
	private String currentOrchCriteria;
	private int currentEdgeDevicesCount;
	private int notGeneratedBecDeviceDead = 0;
	private double vmMigrationNetworkUsage = 0;
	private double vmMigrationPowerConsumption = 0;
	private int vmMigrationAttempt = 0;
	String outputFilesName = "";
	private int simulationId;
	private String simStartTime;
	private NetworkModel networkModel;
	private int failedResourcesUnavailable = 0;// tasks failed due to unavailability of resources

	public SimLog(int id, String time, boolean isFirstIteration) {
		this.simulationId = id;
		this.setSimStartTime(time);
		// use this format for all numbers
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
		otherSymbols.setDecimalSeparator('.'); // use the dot "." as separation symbole, since the comma "," is used in
												// csv files as a separator
		dft = new DecimalFormat("######.####", otherSymbols);
		if (isFirstIteration) {
			ListCSV.add("Orchestration Policy,Orchestration Critera,Edge Devices Count,"
					+ "Tasks Execution Delay (s),Average Execution Delay (s),Tasks Waiting Time (s),"
					+ "Average Wainting Time (s),Generated Tasks,Tasks Successfully Executed,"
					+ "Task Not Executed (No resources available* as a result: long waiting time so the simulation eneded without the execution), Tasks Failed(Delay),Tasks Failed(Device dead),"
					+ "Tasks Failed(Mobility),Tasks not generated because devices were dead,Total tasks Executed(Cloud),Tasks Successfully Executed(Cloud),Total Tasks Executed(Fog),Tasks Successfully Executed(Fog),Total Tasks Executed(Edge),Tasks Successfully Executed(Edge),"
					+ "Network Usage (s),Wan Usage (s),Lan Usage (s),Average Bandwidth per Task (Mbps),VM Migration attempts,Average VM CPU usage (%),"
					+ "Average VM CPU Usage(Cloud) (%),Average VM CPU Usage(Fog) (%),Average VM CPU Usage(Edge) (%),"
					+ "Energy Consumption (Wh),Average Energy Consumption (Wh/Data center),Cloud Energy Consumption (Wh),"
					+ "Average Cloud Energy Consumption (Wh/Data center),Fog Energy Consumption (Wh),Average Fog Energy Consumption (Wh/Data center),"
					+ "Edge Energy Consumption (Wh),Average Edge Energy Consumption (Wh/Device),Dead Devices Count,"
					+ "Average remaining power (Wh),Average remaining power (%), First Edge Device death Time (s),"
					+ "List of remaining power (%) (only devices with batteries/ 0= dead),List of devices death time(s)");
		}
	}

	public void print(String line, int flag) {
		if (getSimulation() == null) {
			System.out.println("    0.0" + " : " + line);
		} else {
			switch (flag) {
			case DEFAULT:
				line = padLeftSpaces(dft.format(getSimulation().clock()), 7) + " : " + line;
				Log.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " - simulation time " + line);
				break;
			case NO_TIME:
				Log.add(line);
				break;
			case SAME_LINE:
				Log.set(Log.size() - 1, Log.get(Log.size() - 1) + line);
			}
		}
	}

	public void print(String line) {
		print(line, DEFAULT);
	}

	public static void println(String line) {
		System.out.println(line);
	}

	public boolean isEnabled() {
		return SimulationParameters.DEEP_LOGGING;
	}

	public void deepLog(String line) {
		if (SimulationParameters.DEEP_LOGGING)
			print(line, DEFAULT);

	}

	public void deepLog(String line, int flag) {
		if (SimulationParameters.DEEP_LOGGING) {
			print(line, flag);
		}
		switch (flag) {
		case DEFAULT:
			line = padLeftSpaces(dft.format(getSimulation().clock()), 7) + " : " + line;
			Log.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " - simulation time " + line);
			break;
		case NO_TIME:
			Log.add(line);
			break;
		case SAME_LINE:
			Log.set(Log.size() - 1, Log.get(Log.size() - 1) + line);
		}
	}

	public void printWithoutTime(String line) {
		print(line, NO_TIME);
	}

	public void printSameLine(String line, String color) {
		if (color.toLowerCase().equals("red"))
			System.err.print(line);
		else
			System.out.print(line);

	}

	public void printSameLine(String line) {
		System.out.print(line);

	}

	public void printVmLoad(ServersManager SM, List<Task> finishedTasks) {
		double averageLoad = 0;
		double averageCloud = 0;
		double averageEdge = 0;
		double averageFog = 0;
		int cloud = 0;
		int edge = 0;
		int fog = 0;
		EdgeDataCenter dc;
		int deadEdgeDevices = 0;
		double energyConsumption = 0;

		double cloudEnConsumption = 0;
		double edgeEnConsumption = 0;
		double fogEnConsumption = vmMigrationPowerConsumption; // add the energy consumption of vms migration
		double averageRemainingPowerWh = 0;
		double averageRemainingPower = 0;
		List<Double> remainingPower = new ArrayList<Double>();
		double firstDeviceDeathTime = -1; // -1 means invalid
		List<Double> devicesDeathTime = new ArrayList<Double>();
		int devicesWithBatteriesCount = 0;
		int devicesAliveWithBatteriesCount = 0;
		for (int j = 0; j < SM.getDatacenterList().size(); j++) {
			dc = (EdgeDataCenter) SM.getDatacenterList().get(j);
			if (dc.getType() == SimulationParameters.TYPES.CLOUD) {
				cloud++;
				cloudEnConsumption += dc.getConsumption();
				averageCloud += dc.getUtilization();
			}

			else if (dc.getType() == SimulationParameters.TYPES.FOG) {
				fog++;
				fogEnConsumption += dc.getConsumption();
				averageFog += dc.getUtilization();
			} else if (dc.getType() == SimulationParameters.TYPES.EDGE) {
				edge++;
				edgeEnConsumption += dc.getConsumption();
				averageEdge += dc.getUtilization();
				if (dc.isDead()) {
					devicesDeathTime.add(dc.getDeathTime());
					remainingPower.add(0.0);
					deadEdgeDevices++;
					if (firstDeviceDeathTime == -1)
						firstDeviceDeathTime = dc.getDeathTime();
					else if (firstDeviceDeathTime > dc.getDeathTime())
						firstDeviceDeathTime = dc.getDeathTime();
				} else {
					if (dc.isBattery()) {
						averageRemainingPowerWh += dc.getBatteryLevel();
						averageRemainingPower += dc.getBatteryLevelPercentage();
						remainingPower.add(dc.getBatteryLevelPercentage());
						devicesAliveWithBatteriesCount++;
					}
				}
			}

		}
		averageLoad = (averageCloud + averageEdge + averageFog) / SM.getDatacenterList().size();
		devicesWithBatteriesCount = devicesAliveWithBatteriesCount + deadEdgeDevices;
		// escape from devision by 0
		if (devicesAliveWithBatteriesCount == 0)
			devicesAliveWithBatteriesCount = 1;

		energyConsumption = cloudEnConsumption + fogEnConsumption + edgeEnConsumption;
		averageRemainingPower = averageRemainingPower / (double) devicesAliveWithBatteriesCount;
		averageRemainingPowerWh = averageRemainingPowerWh / (double) devicesAliveWithBatteriesCount;
		print("SimLog, Average vm CPU utilization                                              :"
				+ padLeftSpaces(dft.format(averageLoad), 20) + " %");
		print("SimLog, Average vm CPU utilization per level                                    : Cloud= "
				+ padLeftSpaces(dft.format(averageCloud / cloud), 12) + " %");
		print("                                                                                  Fog= "
				+ padLeftSpaces(dft.format(averageFog / fog), 14) + " %");
		print("                                                                                  Edge= "
				+ padLeftSpaces(dft.format(averageEdge / edge), 13) + " %");
		print("SimLog, Energy consumption                                                      :"
				+ padLeftSpaces(dft.format(energyConsumption), 20) + " Wh (Average: "
				+ dft.format(energyConsumption / SM.getDatacenterList().size()) + " Wh/data center(or device))");
		print("                                                                                :"
				+ padLeftSpaces("", 20) + "    (Average: "
				+ dft.format(energyConsumption / (double) finishedTasks.size()) + " Wh/task)");
		print("SimLog, Energy Consumption per level                                            : Cloud="
				+ padLeftSpaces(dft.format(cloudEnConsumption), 13) + " Wh (Average: "
				+ dft.format(cloudEnConsumption / clouddc) + " Wh/data center)");
		print("                                                                                  Fog="
				+ padLeftSpaces(dft.format(fogEnConsumption), 15) + " Wh (Average: "
				+ dft.format(fogEnConsumption / fogdc) + " Wh/data center)");
		print("                                                                                  Edge="
				+ padLeftSpaces(dft.format(edgeEnConsumption), 14) + " Wh (Average: "
				+ dft.format(edgeEnConsumption / currentEdgeDevicesCount) + " Wh/edge device)");
		print("SimLog, Dead edge devices due to battery drain                                  :"
				+ padLeftSpaces(dft.format(deadEdgeDevices), 20) + " devices (Among " + devicesWithBatteriesCount
				+ " devices with batteries ("
				+ dft.format(((double) deadEdgeDevices) * 100 / (double) devicesWithBatteriesCount) + " %))");
		print("SimLog, Average remaining power (devices with batteries that are still alive)   :"
				+ padLeftSpaces(dft.format(averageRemainingPowerWh), 20) + " Wh (Average: "
				+ dft.format(averageRemainingPower) + " %)");
		if (firstDeviceDeathTime != -1)
			print("SimLog, First device died at                                                    :"
					+ padLeftSpaces("" + firstDeviceDeathTime, 20) + " seconds");

			ListCSV.set(ListCSV.size() - 1, ListCSV.get(ListCSV.size() - 1) + dft.format(averageLoad) + ","
				+ dft.format(averageCloud/cloud) + "," + dft.format(averageFog/fog) + "," + dft.format(averageEdge/edge) + ","
				+ dft.format(energyConsumption) + "," + dft.format(energyConsumption / SM.getDatacenterList().size())
				+ "," + dft.format(cloudEnConsumption) + "," + dft.format(cloudEnConsumption / cloud) + ","
				+ dft.format(fogEnConsumption) + "," + dft.format(fogEnConsumption / fog) + ","
				+ dft.format(edgeEnConsumption) + "," + dft.format(edgeEnConsumption / edge) + ","
				+ dft.format(deadEdgeDevices) + "," + dft.format(averageRemainingPowerWh) + ","
				+ dft.format(averageRemainingPower) + "," + firstDeviceDeathTime + ","
				+ remainingPower.toString().replace(",", "-") + "," + devicesDeathTime.toString().replace(",", "-"));
	}

	public String padLeftSpaces(String str, int n) {
		return String.format("%1$" + n + "s", str);
	}

	public void showIterationResults(ServersManager SM, List<Task> finishedTasks) {
		printCloudletResults(finishedTasks);
		printVmLoad(SM, finishedTasks);
		String s = "\n";
		for (int i = 0; i < Log.size(); i++) {
			s += Log.get(i) + "\n";

		}
		System.out.print(s);
		// update the log
		saveLog();

	}

	public CloudSim getSimulation() {
		return simulation;
	}

	public void setSimulation(CloudSim simulation) {
		this.simulation = simulation;
	}

	class VmLoadLogItem {
		private double time;
		private double vmLoad;
		private int vmId;

		VmLoadLogItem(double time, double vmLoad, int vmId) {
			this.time = time;
			this.vmLoad = vmLoad;
			this.vmId = vmId;
		}

		public double getLoad() {
			return vmLoad;
		}

		public int getVmId() {
			return vmId;
		}

		public String toString() {
			return time + " is " + vmLoad;
		}
	}

	public void cleanOutputFolder(String outputFolder) {
		// clean the folder where the results files will be saved
		File dir = new File(outputFolder);
		print("SimLog, Cleaning output folder");
		if (dir.exists() && dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				if (f.exists() && f.isFile()) {
					if (!f.delete()) {
						// print("SimLog, File cannot be cleared: " + f.getAbsolutePath());
					}
				}
			}
		} else {
			print("SimLog, Output folder is not available: " + outputFolder);
			System.exit(0);
		}
	}

	public void saveLog() {
		// writing results in csv file
		writeFile(getFileName(".csv"), ListCSV);

		if (SimulationParameters.SAVE_LOG == false) {
			print("SimLog, no log saving");
			System.exit(0);
		}

		writeFile(getFileName(".txt"), Log);

	}

	public void writeFile(String fileName, List<String> Lines) {
		try {
			writer = new BufferedWriter(new FileWriter(fileName, true));
			for (String str : Lines) {
				writer.append(str);
				writer.newLine();
			}
			Lines.clear();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getFileName(String extension) {
		if (!outputFilesName.equals(""))
			return outputFilesName + extension;
		if (SimulationParameters.PARALLEL)
			outputFilesName = "_Parallel_simulation_" + simulationId;
		else
			outputFilesName = "_Sequential_simulation";
		outputFilesName = simStartTime + outputFilesName;
		outputFolder = Main.outputFolder;
		outputFilesName = outputFolder + outputFilesName;
		return outputFilesName + extension;
	}

	public void printCloudletResults(List<Task> finishedTasks) {

		double size = finishedTasks.size();
		Task task;
		print("");
		print("======================================================= OUTPUT =======================================================");
		float averageExecutionTime = 0;
		float averageWaitingTime = 0;
		int successTasks = 0;
		int cloudTasks = 0;
		int totalCloudTasks = 0;
		int fogTasks = 0;
		int totalFogTasks = 0;
		int edgeTasks = 0;
		int totalEdgeTasks = 0;
		int failedDelay = 0;// tasks failed due to high delay
		int failedPower = 0;// tasks failed due to device death (no energy ledt in battery)
		int failedMobility = 0;// tasks failed due to devices mobility ( no vm migration)
		double bandwidth = 0;
		double networkUsage = vmMigrationNetworkUsage;// add vm migration network usage
		double lanUsage = vmMigrationNetworkUsage;// add vm migration network usage
		double wanUsage = 0;
		double lanusageContainer = 0;
		double WanusageContainer = 0;
		double lantraffic = 0;
		///
		///
		for (int i = 0; i < networkModel.getTaskProgressList().size(); i++) {
			lanUsage += networkModel.getTaskProgressList().get(i).getLanNetworkUsage();
			wanUsage += networkModel.getTaskProgressList().get(i).getWanNetworkUsage();
			if (networkModel.getTaskProgressList().get(i).getType() == taskTransferProgress.CONTAINER) {
				lanusageContainer += networkModel.getTaskProgressList().get(i).getLanNetworkUsage();
				WanusageContainer += networkModel.getTaskProgressList().get(i).getWanNetworkUsage();
			}
			if (networkModel.getTaskProgressList().get(i).getLanNetworkUsage() > 0) {
				lantraffic += networkModel.getTaskProgressList().get(i).getFileSize(); 
			}
		}
	
		lantraffic = lantraffic / 1000;	
		bandwidth = lantraffic / lanUsage;
		for (int i = 0; i < size; i++) {
			task = finishedTasks.get(i);

			if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.CLOUD) {
				totalCloudTasks++;
			} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.FOG) {
				totalFogTasks++;
			} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.EDGE) {
				totalEdgeTasks++;
			}

			if (task.getStatus().name().equals("SUCCESS")) {
				successTasks++;// successfully executed
				// calculating all the transmitted Bytes in order to get the average task size
				if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.CLOUD) {
					cloudTasks++;
				} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.FOG) {
					fogTasks++;
				} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.EDGE) {
					edgeTasks++;
				}

			} else
				failedResourcesUnavailable++;

			if (task.getFailureReason() == Task.Status.FAILED_DUE_TO_LATENCY)
				failedDelay++;
			else if (task.getFailureReason() == Task.Status.FAILED_BECAUSE_DEVICE_DEAD)
				failedPower++;
			else if (task.getFailureReason() == Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY)
				failedMobility++;
			else if (task.getFailureReason() == Task.Status.FAILED_NO_RESSOURCES)
				failedResourcesUnavailable++;

			if (task.getExecStartTime() > 0)
				averageWaitingTime += task.getExecStartTime() - task.getTime();
			if (task.getActualCpuTime() > 0)
				averageExecutionTime += task.getActualCpuTime();
		} // end of "for" loop
			// full network usage
		networkUsage = Math.max(wanUsage, lanUsage);

		if (wanUsage != 0)
			WanusageContainer = WanusageContainer * 100 / wanUsage;
		int tasksSent = generatedTasks - notGeneratedBecDeviceDead;
		int tasksNotExecuted = generatedTasks - (successTasks + failedMobility + failedPower + failedDelay); // No
																												// resources
																												// available,
																												// as a
																												// result:
																												// long
																												// waiting
																												// time
																												// so
																												// the
																												// simulation
																												// eneded
																												// without
																												// the
																												// execution
		// average network usage (seconds)
		double execRate = ((double) successTasks * 100) / (double) tasksSent;
		double delayFailRate = ((double) failedDelay * 100) / (double) tasksSent;
		double powerFailRate = ((double) failedPower * 100) / (double) tasksSent;
		double mobilityFailRate = ((double) failedMobility * 100) / (double) tasksSent;
		double notSentDueToDeath = ((double) notGeneratedBecDeviceDead * 100) / (double) generatedTasks;
		double notExecutedDueToLongWaitTime = ((double) tasksNotExecuted * 100) / (double) generatedTasks;

		// printing results
		print("");
		print("SimLog, Tasks not sent because device was dead                                  :"
				+ padLeftSpaces(dft.format(notSentDueToDeath), 20) + " % (" + notGeneratedBecDeviceDead + " tasks)");
		print("SimLog, Tasks sent from edge devices                                            :"
				+ padLeftSpaces("" + dft.format(((double) tasksSent * 100) / ((double) generatedTasks)), 20) + " % ("
				+ tasksSent + " among " + generatedTasks + " generated tasks)");

		print("-------------------------------------All values below are based on the sent tasks-------------------------------------");
		print("SimLog, Tasks execution delay                                                   :"
				+ padLeftSpaces(dft.format(averageExecutionTime), 20) + " seconds");
		print("SimLog, Average tasks execution delay                                           :"
				+ padLeftSpaces(dft.format(averageExecutionTime / (double) (tasksSent - failedPower)), 20)
				+ " seconds");
		print("SimLog, Tasks waiting time (from task submiting to the execution start)         :"
				+ padLeftSpaces(dft.format(averageWaitingTime), 20) + " seconds");
		print("SimLog, Average tasks waiting time (from task submiting to the execution start) :"
				+ padLeftSpaces(dft.format(averageWaitingTime / (double) (tasksSent - failedPower)), 20) + " seconds");
		print("SimLog, Tasks successfully executed                                             :"
				+ padLeftSpaces("" + dft.format(execRate), 20) + " % (" + successTasks + " among " + tasksSent
				+ " sent tasks)");

		print("SimLog, Tasks failures");
		print("                                      Not executed due to ressources unavailable:"
				+ padLeftSpaces(dft.format(notExecutedDueToLongWaitTime), 20) + " % (" + tasksNotExecuted + " tasks)");
		print("                              Successfully executed but failed due to high delay:"
				+ padLeftSpaces(dft.format(delayFailRate), 20) + " % (" + failedDelay + " tasks from " + tasksSent
				+ " successfully sent tasks)");
		print("                 Tasks execution results not returned because the device is dead:"
				+ padLeftSpaces(dft.format(powerFailRate), 20) + " % (" + failedPower + " tasks)");
		print("                     Tasks execution results not returned due to device mobility:"
				+ padLeftSpaces(dft.format(mobilityFailRate), 20) + " % (" + failedMobility + " tasks)");

		print("SimLog, Tasks executed on each level                                            :" + " Cloud="
				+ padLeftSpaces("" + totalCloudTasks, 13) + " tasks (where " + cloudTasks
				+ " were successfully executed )");
		print("                                                                                 " + " Fog="
				+ padLeftSpaces("" + totalFogTasks, 15) + " tasks (where " + fogTasks
				+ " were successfully executed )");
		print("                                                                                 " + " Edge="
				+ padLeftSpaces("" + totalEdgeTasks, 14) + " tasks (where " + edgeTasks
				+ " were successfully executed )");
		print("SimLog, Network usage                                                           :"
				+ padLeftSpaces(dft.format(networkUsage), 20) + " seconds (The total traffic: " + lantraffic
				+ " (MB) )");
		print("                                                                                 " + " Wan="
				+ padLeftSpaces(dft.format(wanUsage), 15) + " seconds (" + dft.format(wanUsage * 100 / networkUsage)
				+ " % of total usage, WAN used when downloading containers=" + dft.format(WanusageContainer)
				+ " % of WAN usage )");
		print("                                                                                 " + " Lan="
				+ padLeftSpaces(dft.format(lanUsage), 15) + " seconds (" + dft.format(lanUsage * 100 / networkUsage)
				+ " % of total usage, LAN used when downloading containers="
				+ dft.format(lanusageContainer * 100 / lanUsage) + " % of LAN usage )");
		print("                                                            Average bandwidth per transfer="
				+ padLeftSpaces(dft.format(bandwidth), 10) + " Mbps  ");

		// print("Virtual machines migrations attempts count :"
		// + padLeftSpaces(""+vmMigrationAttempt, 20)+ " times");
		ListCSV.add(currentOrchPolicy + "," + currentOrchCriteria + "," + currentEdgeDevicesCount + ","
				+ dft.format(averageExecutionTime) + "," + dft.format(averageExecutionTime / size) + ","
				+ dft.format(averageWaitingTime) + "," + dft.format(averageWaitingTime / size) + "," + generatedTasks
				+ "," + successTasks + "," + tasksNotExecuted + "," + failedDelay + "," + failedPower + ","
				+ failedMobility + "," + notGeneratedBecDeviceDead + "," + totalCloudTasks + "," + cloudTasks + ","
				+ totalFogTasks + "," + fogTasks + "," + totalEdgeTasks + "," + edgeTasks + "," + networkUsage + ","
				+ wanUsage + "," + lanUsage + "," + bandwidth + "," + vmMigrationAttempt + ",");
	}

	public int getGeneratedTasks() {
		return generatedTasks;
	}

	public void setGeneratedTasks(int generatedTasks) {
		this.generatedTasks = generatedTasks;
	}

	public String getCurrentOrchPolicy() {
		return currentOrchPolicy;
	}

	public void setCurrentOrchPolicy(String currentOrchPolicy) {
		this.currentOrchPolicy = currentOrchPolicy;
	}

	public void init(int dev, int orch, int cri, CloudSim simulation) {
		this.currentEdgeDevicesCount = dev;
		this.currentOrchCriteria = SimulationParameters.ORCHESTRATOR_CRITERIA[cri];
		this.currentOrchPolicy = SimulationParameters.ORCHESTRATOR_POLICIES[orch];
		this.simulation = simulation;

	}

	public int getNotGeneratedBecauseDead() {
		// the number of tasks that aren't generated because the device was turned off
		// (no remaining energy)
		return notGeneratedBecDeviceDead;
	}

	public void setNotGeneratedBecauseDead(int i) {
		// the number of tasks that aren't generated because the device was turned off
		// (no remaining energy)
		this.notGeneratedBecDeviceDead = i;

	}

	public void addVmMigrationNetworkUsage(double delay) {
		this.vmMigrationNetworkUsage += delay;
	}

	public void addVmMigrationPowerConsumption(double powerCon) {
		this.vmMigrationPowerConsumption += powerCon;

	}

	public void incVmMigrationAttempt() {
		this.vmMigrationAttempt++;
	}

	public int getVmMigrationAttempts() {
		return vmMigrationAttempt;
	}

	public void setNetworkModel(NetworkModel networkModel) {
		this.networkModel = networkModel;

	}

	public String getSimStartTime() {
		return simStartTime;
	}

	public void setSimStartTime(String simStartTime) {
		this.simStartTime = simStartTime;
	}

	public int getClouddc() {
		return clouddc;
	}

	public void incClouddc() {
		this.clouddc++;
	}

	public int getFogdc() {
		return fogdc;
	}

	public void incFogdc() {
		this.fogdc++;
	}

	public int getTasksFailedRessourcesUnavailable() {

		return failedResourcesUnavailable;
	}

	public void setFailedDueToResourcesUnavailablity(int i) {
		this.failedResourcesUnavailable = i;

	}

}
