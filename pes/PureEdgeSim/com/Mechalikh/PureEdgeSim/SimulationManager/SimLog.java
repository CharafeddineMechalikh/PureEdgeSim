package com.Mechalikh.PureEdgeSim.SimulationManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.Mechalikh.PureEdgeSim.Main;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.Network.FileTransferProgress;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public class SimLog {
	public static final int NO_TIME = 0;
	public static final int SAME_LINE = 1;
	public static final int DEFAULT = 2;
	private List<String> resultsList = new ArrayList<String>();
	private static DecimalFormat decimalFormat;
	private List<String> log = new ArrayList<String>();
	private BufferedWriter bufferedWriter;
	private String outputFolder;
	private int generatedTasksCount;
	private String currentOrchArchitecture;
	private String currentOrchAlgorithm;
	private int currentEdgeDevicesCount;
	private int notGeneratedBecDeviceDead = 0;
	private String outputFilesName = "";
	private String simStartTime;
	private int failedResourcesUnavailable = 0;// tasks failed due to unavailability of resources
	private SimulationManager simulationManager;
	private boolean isFirstIteration;

	public SimLog(String time, boolean isFirstIteration) {
		this.setSimStartTime(time);
		this.isFirstIteration = isFirstIteration;

		// Use this format for all numbers
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
		otherSymbols.setDecimalSeparator('.'); // use the dot "." as separation symbole, since the comma ","
												// is used in csv files as a separator
		decimalFormat = new DecimalFormat("######.####", otherSymbols);

		if (isFirstIteration) {
			// Add the CSV file header
			addCsvHeader();
		}
	}

	private void addCsvHeader() {
		resultsList.add("Orchestration architecture,Orchestration algorithm,Edge devices count,"
				+ "Tasks execution delay (s),Average execution delay (s),Tasks waiting time (s),"
				+ "Average wainting time (s),Generated tasks,Tasks successfully executed,"
				+ "Task not executed (No resources available or long waiting time),Tasks failed (delay),Tasks failed (device dead),"
				+ "Tasks failed (mobility),Tasks not generated due to the death of devices,Total tasks executed (Cloud),"
				+ "Tasks successfully executed (Cloud),Total tasks executed (Fog),Tasks successfully executed (Fog),"
				+ "Total tasks executed (Edge),Tasks successfully executed (Edge),"
				+ "Network usage (s),Wan usage (s),Lan usage (s), Containers wan usage (s), Containers lan usage (s),Average bandwidth per task (Mbps),Average VM CPU usage (%),"
				+ "Average VM CPU usage (Cloud) (%),Average VM CPU usage (Fog) (%),Average VM CPU usage (Edge) (%),"
				+ "Energy consumption (Wh),Average energy consumption (Wh/Data center),Cloud energy consumption (Wh),"
				+ "Average Cloud energy consumption (Wh/Data center),Fog energy consumption (Wh),Average Fog energy consumption (Wh/Data center),"
				+ "Edge energy consumption (Wh),Average Edge energy consumption (Wh/Device),Dead devices count,"
				+ "Average remaining power (Wh),Average remaining power (%), First edge device death time (s),"
				+ "List of remaining power (%) (only battery powered devices / 0 = dead),List of the time when each device died (s)");
	}

	public void showIterationResults(List<Task> finishedTasks) {
		printTasksRelatedResults(finishedTasks);
		printNetworkRelatedResults();
		printResourcesUtilizationResults(finishedTasks);
		String s = "\n";
		for (int i = 0; i < log.size(); i++) {
			s += log.get(i) + "\n";
		}
		System.out.print(s);
		// update the log
		saveLog();

	}

	public void printTasksRelatedResults(List<Task> finishedTasks) {

		double tasksCount = finishedTasks.size();
		Task task;
		float averageExecutionTime = 0;
		float averageWaitingTime = 0;
		int successfulTasksCount = 0;
		int tasksExecutedOnCloud = 0;
		int totalCloudTasks = 0;
		int tasksExecutedOnFog = 0;
		int totalFogTasks = 0;
		int tasksExecutedOnEdge = 0;
		int totalEdgeTasks = 0;
		int tasksFailedDelay = 0;// tasks failed due to high delay
		int tasksFailedPower = 0;// tasks failed due to device death (no energy ledt in battery)
		int tasksFailedMobility = 0;// tasks failed due to devices mobility ( no vm migration)

		for (int i = 0; i < tasksCount; i++) {
			task = finishedTasks.get(i);

			if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.CLOUD) {
				totalCloudTasks++;
			} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.FOG) {
				totalFogTasks++;
			} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.EDGE) {
				totalEdgeTasks++;
			}

			if (task.getStatus().name().equals("SUCCESS")) {
				successfulTasksCount++;// successfully executed
				// calculating all the transmitted Bytes in order to get the average task size
				if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.CLOUD) {
					tasksExecutedOnCloud++;
				} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.FOG) {
					tasksExecutedOnFog++;
				} else if (((EdgeVM) task.getVm()).getType() == SimulationParameters.TYPES.EDGE) {
					tasksExecutedOnEdge++;
				}

			} else
				failedResourcesUnavailable++;

			if (task.getFailureReason() == Task.Status.FAILED_DUE_TO_LATENCY)
				tasksFailedDelay++;
			else if (task.getFailureReason() == Task.Status.FAILED_BECAUSE_DEVICE_DEAD)
				tasksFailedPower++;
			else if (task.getFailureReason() == Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY)
				tasksFailedMobility++;
			else if (task.getFailureReason() == Task.Status.FAILED_NO_RESOURCES)
				failedResourcesUnavailable++;

			if (task.getExecStartTime() > 0)
				averageWaitingTime += task.getExecStartTime() - task.getTime();
			if (task.getActualCpuTime() > 0)
				averageExecutionTime += task.getActualCpuTime();
		} // end of "for" loop

		int tasksSent = generatedTasksCount - notGeneratedBecDeviceDead;

		// No resources were available, as result:long waiting time, so the simulation
		// ended without the execution of these tasks
		int tasksNotExecuted = generatedTasksCount
				- (successfulTasksCount + tasksFailedMobility + tasksFailedPower + tasksFailedDelay);

		// average network usage (seconds)
		double execRate = ((double) successfulTasksCount * 100) / (double) tasksSent;
		double delayFailRate = ((double) tasksFailedDelay * 100) / (double) tasksSent;
		double powerFailRate = ((double) tasksFailedPower * 100) / (double) tasksSent;
		double mobilityFailRate = ((double) tasksFailedMobility * 100) / (double) tasksSent;
		double notSentDueToDeath = ((double) notGeneratedBecDeviceDead * 100) / (double) generatedTasksCount;
		double notExecutedDueToLongWaitTime = ((double) tasksNotExecuted * 100) / (double) generatedTasksCount;

		// printing results
		print("");
		print("------------------------------------------------------- OUTPUT -------------------------------------------------------");
		print("");
		print("SimLog- Tasks not sent because device was dead                                  :"
				+ padLeftSpaces(decimalFormat.format(notSentDueToDeath), 20) + " % (" + notGeneratedBecDeviceDead
				+ " tasks)");
		print("SimLog- Tasks sent from edge devices                                            :"
				+ padLeftSpaces("" + decimalFormat.format(((double) tasksSent * 100) / ((double) generatedTasksCount)),
						20)
				+ " % (" + tasksSent + " among " + generatedTasksCount + " generated tasks)");

		print("-------------------------------------All values below are based on the sent tasks-------------------------------------");
		print("SimLog- Tasks execution delay                                                   :"
				+ padLeftSpaces(decimalFormat.format(averageExecutionTime), 20) + " seconds");
		print("SimLog- Average tasks execution delay                                           :" + padLeftSpaces(
				decimalFormat.format(averageExecutionTime / (double) (tasksSent - tasksFailedPower)), 20) + " seconds");
		print("SimLog- Tasks waiting time (from task submiting to the execution start)         :"
				+ padLeftSpaces(decimalFormat.format(averageWaitingTime), 20) + " seconds");
		print("SimLog- Average tasks waiting time (from task submiting to the execution start) :"
				+ padLeftSpaces(decimalFormat.format(averageWaitingTime / (double) (tasksSent - tasksFailedPower)), 20)
				+ " seconds");
		print("SimLog- Tasks successfully executed                                             :"
				+ padLeftSpaces("" + decimalFormat.format(execRate), 20) + " % (" + successfulTasksCount + " among "
				+ tasksSent + " sent tasks)");

		print("SimLog- Tasks failures");
		print("                                       Not executed due to resources unavailable:"
				+ padLeftSpaces(decimalFormat.format(notExecutedDueToLongWaitTime), 20) + " % (" + tasksNotExecuted
				+ " tasks)");
		print("                              Successfully executed but failed due to high delay:"
				+ padLeftSpaces(decimalFormat.format(delayFailRate), 20) + " % (" + tasksFailedDelay + " tasks from "
				+ tasksSent + " successfully sent tasks)");
		print("                 Tasks execution results not returned because the device is dead:"
				+ padLeftSpaces(decimalFormat.format(powerFailRate), 20) + " % (" + tasksFailedPower + " tasks)");
		print("                     Tasks execution results not returned due to device mobility:"
				+ padLeftSpaces(decimalFormat.format(mobilityFailRate), 20) + " % (" + tasksFailedMobility + " tasks)");

		print("SimLog- Tasks executed on each level                                            :" + " Cloud="
				+ padLeftSpaces("" + totalCloudTasks, 13) + " tasks (where " + tasksExecutedOnCloud
				+ " were successfully executed )");
		print("                                                                                 " + " Fog="
				+ padLeftSpaces("" + totalFogTasks, 15) + " tasks (where " + tasksExecutedOnFog
				+ " were successfully executed )");
		print("                                                                                 " + " Edge="
				+ padLeftSpaces("" + totalEdgeTasks, 14) + " tasks (where " + tasksExecutedOnEdge
				+ " were successfully executed )");

		resultsList.add(currentOrchArchitecture + "," + currentOrchAlgorithm + "," + currentEdgeDevicesCount + ","
				+ decimalFormat.format(averageExecutionTime) + ","
				+ decimalFormat.format(averageExecutionTime / (double) (tasksSent - tasksFailedPower)) + ","
				+ decimalFormat.format(averageWaitingTime) + ","
				+ decimalFormat.format(averageWaitingTime / (double) (tasksSent - tasksFailedPower)) + ","
				+ generatedTasksCount + "," + successfulTasksCount + "," + tasksNotExecuted + "," + tasksFailedDelay
				+ "," + tasksFailedPower + "," + tasksFailedMobility + "," + notGeneratedBecDeviceDead + ","
				+ totalCloudTasks + "," + tasksExecutedOnCloud + "," + totalFogTasks + "," + tasksExecutedOnFog + ","
				+ totalEdgeTasks + "," + tasksExecutedOnEdge + ",");
	}

	public void printNetworkRelatedResults() {
		double bandwidth = 0;
		double networkUsage = 0;
		double lanUsage = 0;
		double wanUsage = 0;
		double lanUsedByContainers = 0;
		double wanUsedByContainers = 0;
		double networkTraffic = 0;

		List<FileTransferProgress> transferProgressList = getSimulationManager().getNetworkModel()
				.getTransferProgressList();
		for (int i = 0; i < transferProgressList.size(); i++) {
			lanUsage += transferProgressList.get(i).getLanNetworkUsage();
			wanUsage += transferProgressList.get(i).getWanNetworkUsage();
			networkTraffic += transferProgressList.get(i).getFileSize();
			if (transferProgressList.get(i).getLanNetworkUsage() > 0) {
				bandwidth += transferProgressList.get(i).getFileSize()
						/ transferProgressList.get(i).getLanNetworkUsage();
			}
			if (transferProgressList.get(i).getTransferType() == FileTransferProgress.CONTAINER) {
				lanUsedByContainers += transferProgressList.get(i).getLanNetworkUsage();
				wanUsedByContainers += transferProgressList.get(i).getWanNetworkUsage();
			}
		}

		networkTraffic = networkTraffic / 1000;
		bandwidth = bandwidth / (1000 * transferProgressList.size());
		networkUsage = lanUsage; // WAN cannot be be used unless the LAN is used, therefore the total usage in
									// this case will be equivalant to lan usage

		if (wanUsage > 0)
			wanUsedByContainers = wanUsedByContainers * 100 / wanUsage;
		if (lanUsage > 0)
			lanUsedByContainers = lanUsedByContainers * 100 / lanUsage;
		print("SimLog- Network usage                                                           :"
				+ padLeftSpaces(decimalFormat.format(networkUsage), 20) + " seconds (The total traffic: "
				+ networkTraffic + " (MB) )");
		print("                                                                                 " + " Wan="
				+ padLeftSpaces(decimalFormat.format(wanUsage), 15) + " seconds ("
				+ decimalFormat.format(wanUsage * 100 / networkUsage)
				+ " % of total usage, WAN used when downloading containers=" + decimalFormat.format(wanUsedByContainers)
				+ " % of WAN usage )");
		print("                                                                                 " + " Lan="
				+ padLeftSpaces(decimalFormat.format(lanUsage), 15) + " seconds ("
				+ decimalFormat.format(lanUsage * 100 / networkUsage)
				+ " % of total usage, LAN used when downloading containers="
				+ decimalFormat.format(lanUsedByContainers) + " % of LAN usage )");
		print("                                                            Average bandwidth per transfer="
				+ padLeftSpaces(decimalFormat.format(bandwidth), 10) + " Mbps  ");
		// Add these values to the las item of the results list
		resultsList.set(resultsList.size() - 1, resultsList.get(resultsList.size() - 1) + networkUsage + "," + wanUsage
				+ "," + lanUsage + "," + wanUsedByContainers + "," + lanUsedByContainers + "," + bandwidth + ",");
	}

	public void printResourcesUtilizationResults(List<Task> finishedTasks) {
		int edgeDevicesCount = 0;
		double averageCpuUtilization = 0;
		double averageCloudCpuUtilization = 0;
		double averageEdgeCpuUtilization = 0;
		double averageFogCpuUtilization = 0;
		EdgeDataCenter dc;
		int deadEdgeDevicesCount = 0;
		double energyConsumption = 0;
		double cloudEnConsumption = 0;
		double edgeEnConsumption = 0;
		double fogEnConsumption = 0;
		double averageRemainingPowerWh = 0;
		double averageRemainingPower = 0;
		List<Double> remainingPower = new ArrayList<Double>();
		double firstDeviceDeathTime = -1; // -1 means invalid
		List<Double> devicesDeathTime = new ArrayList<Double>();
		int batteryPoweredDevicesCount = 0;
		int aliveBatteryPoweredDevicesCount = 0;
		List<EdgeDataCenter> datacentersList = getSimulationManager().getServersManager().getDatacenterList();

		for (int j = 0; j < datacentersList.size(); j++) {
			dc = (EdgeDataCenter) datacentersList.get(j);
			if (dc.getType() == SimulationParameters.TYPES.CLOUD) {
				cloudEnConsumption += dc.getEnergyModel().getTotalEnergyConsumption();
				averageCloudCpuUtilization += dc.getTotalCpuUtilization();
			}

			else if (dc.getType() == SimulationParameters.TYPES.FOG) {
				fogEnConsumption += dc.getEnergyModel().getTotalEnergyConsumption();
				averageFogCpuUtilization += dc.getTotalCpuUtilization();
			} else if (dc.getType() == SimulationParameters.TYPES.EDGE) {
				edgeEnConsumption += dc.getEnergyModel().getTotalEnergyConsumption();
				if (dc.getVmList().size() > 0) {
					// only devices with computing capability
					// the devices that have no VM are considered simple sensors, and will not be
					// counted here
					averageEdgeCpuUtilization += dc.getTotalCpuUtilization();
					edgeDevicesCount++;
				}
				if (dc.isDead()) {
					devicesDeathTime.add(dc.getDeathTime());
					remainingPower.add(0.0);
					deadEdgeDevicesCount++;
					if (firstDeviceDeathTime == -1)
						firstDeviceDeathTime = dc.getDeathTime();
					else if (firstDeviceDeathTime > dc.getDeathTime())
						firstDeviceDeathTime = dc.getDeathTime();
				} else {
					if (dc.isBattery()) {
						averageRemainingPowerWh += dc.getBatteryLevel();
						averageRemainingPower += dc.getBatteryLevelPercentage();
						remainingPower.add(dc.getBatteryLevelPercentage());
						aliveBatteryPoweredDevicesCount++;
					}
				}
			}

		}
		averageCpuUtilization = (averageCloudCpuUtilization + averageEdgeCpuUtilization + averageFogCpuUtilization)
				/ (edgeDevicesCount + SimulationParameters.NUM_OF_FOG_DATACENTERS
						+ SimulationParameters.NUM_OF_CLOUD_DATACENTERS);
		batteryPoweredDevicesCount = aliveBatteryPoweredDevicesCount + deadEdgeDevicesCount;
		// escape from devision by 0
		if (aliveBatteryPoweredDevicesCount == 0)
			aliveBatteryPoweredDevicesCount = 1;
		averageCloudCpuUtilization = averageCloudCpuUtilization / SimulationParameters.NUM_OF_CLOUD_DATACENTERS;
		averageFogCpuUtilization = averageFogCpuUtilization / SimulationParameters.NUM_OF_FOG_DATACENTERS;
		averageEdgeCpuUtilization = averageEdgeCpuUtilization / edgeDevicesCount;

		energyConsumption = cloudEnConsumption + fogEnConsumption + edgeEnConsumption;
		averageRemainingPower = averageRemainingPower / (double) aliveBatteryPoweredDevicesCount;
		averageRemainingPowerWh = averageRemainingPowerWh / (double) aliveBatteryPoweredDevicesCount;
		double averageCloudEnConsumption = cloudEnConsumption / SimulationParameters.NUM_OF_CLOUD_DATACENTERS;
		double averageFogEnConsumption = fogEnConsumption / SimulationParameters.NUM_OF_FOG_DATACENTERS;
		double averageEdgeEnConsumption = edgeEnConsumption / getSimulationManager().getScenario().getDevicesCount();

		print("SimLog- Average vm CPU utilization                                              :"
				+ padLeftSpaces(decimalFormat.format(averageCpuUtilization), 20) + " %");
		print("SimLog- Average vm CPU utilization per level                                    : Cloud= "
				+ padLeftSpaces(decimalFormat.format(averageCloudCpuUtilization), 12) + " %");
		print("                                                                                  Fog= "
				+ padLeftSpaces(decimalFormat.format(averageFogCpuUtilization), 14) + " %");
		print("                                                                                  Edge= "
				+ padLeftSpaces(decimalFormat.format(averageEdgeCpuUtilization), 13) + " %");
		print("SimLog- Energy consumption                                                      :"
				+ padLeftSpaces(decimalFormat.format(energyConsumption), 20) + " Wh (Average: "
				+ decimalFormat.format(energyConsumption / datacentersList.size()) + " Wh/data center(or device))");
		print("                                                                                :"
				+ padLeftSpaces("", 20) + "    (Average: "
				+ decimalFormat.format(energyConsumption / (double) finishedTasks.size()) + " Wh/task)");
		print("SimLog- Energy Consumption per level                                            : Cloud="
				+ padLeftSpaces(decimalFormat.format(cloudEnConsumption), 13) + " Wh (Average: "
				+ decimalFormat.format(cloudEnConsumption / SimulationParameters.NUM_OF_CLOUD_DATACENTERS)
				+ " Wh/data center)");
		print("                                                                                  Fog="
				+ padLeftSpaces(decimalFormat.format(fogEnConsumption), 15) + " Wh (Average: "
				+ decimalFormat.format(fogEnConsumption / SimulationParameters.NUM_OF_FOG_DATACENTERS)
				+ " Wh/data center)");
		print("                                                                                  Edge="
				+ padLeftSpaces(decimalFormat.format(edgeEnConsumption), 14) + " Wh (Average: "
				+ decimalFormat.format(edgeEnConsumption / currentEdgeDevicesCount) + " Wh/edge device)");
		print("SimLog- Dead edge devices due to battery drain                                  :"
				+ padLeftSpaces(decimalFormat.format(deadEdgeDevicesCount), 20) + " devices (Among "
				+ batteryPoweredDevicesCount + " devices with batteries ("
				+ decimalFormat.format(((double) deadEdgeDevicesCount) * 100 / (double) batteryPoweredDevicesCount)
				+ " %))");
		print("SimLog- Average remaining power (devices with batteries that are still alive)   :"
				+ padLeftSpaces(decimalFormat.format(averageRemainingPowerWh), 20) + " Wh (Average: "
				+ decimalFormat.format(averageRemainingPower) + " %)");
		if (firstDeviceDeathTime != -1)
			print("SimLog- First device died at                                                    :"
					+ padLeftSpaces("" + firstDeviceDeathTime, 20) + " seconds");

		// Add these values to the las item of the results list
		resultsList.set(resultsList.size() - 1, resultsList.get(resultsList.size() - 1)
				+ decimalFormat.format(averageCpuUtilization) + "," + decimalFormat.format(averageCloudCpuUtilization)
				+ "," + decimalFormat.format(averageFogCpuUtilization) + ","
				+ decimalFormat.format(averageEdgeCpuUtilization) + "," + decimalFormat.format(energyConsumption) + ","
				+ decimalFormat.format(energyConsumption / datacentersList.size()) + ","
				+ decimalFormat.format(cloudEnConsumption) + "," + decimalFormat.format(averageCloudEnConsumption) + ","
				+ decimalFormat.format(fogEnConsumption) + "," + decimalFormat.format(averageFogEnConsumption) + ","
				+ decimalFormat.format(edgeEnConsumption) + "," + decimalFormat.format(averageEdgeEnConsumption) + ","
				+ decimalFormat.format(deadEdgeDevicesCount) + "," + decimalFormat.format(averageRemainingPowerWh) + ","
				+ decimalFormat.format(averageRemainingPower) + "," + firstDeviceDeathTime + ","
				+ remainingPower.toString().replace(",", "-") + "," + devicesDeathTime.toString().replace(",", "-"));
	}

	public String padLeftSpaces(String str, int n) {
		return String.format("%1$" + n + "s", str);
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

	public void cleanOutputFolder(String outputFolder) throws IOException {
		// Clean the folder where the results files will be saved
		if (isFirstIteration) {
			print("SimLog- Cleaning the outputfolder...");
			isFirstIteration = false;
			Path dir = new File(outputFolder).toPath();
			deleteDirectoryRecursion(dir);
		}
	}

	void deleteDirectoryRecursion(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					deleteDirectoryRecursion(entry);
				}
			}
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			print("SimLog- Could not delete file/folder: " + path.toString());
		}
	}

	public void saveLog() {
		// writing results in csv file
		writeFile(getFileName(".csv"), resultsList);

		if (SimulationParameters.SAVE_LOG == false) {
			print("SimLog- no log saving");
			System.exit(0);
		}

		writeFile(getFileName(".txt"), log);

	}

	public void writeFile(String fileName, List<String> Lines) {
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(fileName, true));
			for (String str : Lines) {
				bufferedWriter.append(str);
				bufferedWriter.newLine();
			}
			Lines.clear();
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getFileName(String extension) {
		outputFolder = Main.outputFolder;
		outputFilesName = outputFolder + "/" + simStartTime;
		new File(outputFilesName).mkdirs();
		if (SimulationParameters.PARALLEL)
			outputFilesName += "/Parallel_simulation_" + getSimulationManager().getSimulationId();
		else
			outputFilesName += "/Sequential_simulation";

		return outputFilesName + extension;
	}

	public void print(String line, int flag) {
		if (getSimulationManager() == null) {
			System.out.println("    0.0" + " : " + line);
		} else {
			switch (flag) {
			case DEFAULT:
				if (getSimulationManager().getSimulation().clock() < SimulationParameters.INITIALIZATION_TIME)
					line = padLeftSpaces("0", 7) + " (s) : " + line;
				else
					line = padLeftSpaces(decimalFormat.format(
							getSimulationManager().getSimulation().clock() - SimulationParameters.INITIALIZATION_TIME),
							7) + " (s) : " + line;
				log.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " - simulation time " + line);
				break;
			case NO_TIME:
				log.add(line);
				break;
			case SAME_LINE:
				log.set(log.size() - 1, log.get(log.size() - 1) + line);
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

	public int getGeneratedTasks() {
		return generatedTasksCount;
	}

	public void setGeneratedTasks(int generatedTasks) {
		this.generatedTasksCount = generatedTasks;
	}

	public String getCurrentOrchPolicy() {
		return currentOrchArchitecture;
	}

	public void setCurrentOrchPolicy(String currentOrchPolicy) {
		this.currentOrchArchitecture = currentOrchPolicy;
	}

	public void initialize(SimulationManager simulationManager, int dev, int alg, int arch) {
		this.currentEdgeDevicesCount = dev;
		this.currentOrchAlgorithm = SimulationParameters.ORCHESTRATION_AlGORITHMS[alg];
		this.currentOrchArchitecture = SimulationParameters.ORCHESTRATION_ARCHITECTURES[arch];
		this.setSimulationManager(simulationManager);
	}

	public int getNotGeneratedBecauseDead() {
		// the number of tasks that aren't generated because the device was turned off
		// (no remaining energy)
		return notGeneratedBecDeviceDead;
	}

	public void setNotGeneratedBecauseDead(int i) {
		// the number of tasks that were not generated because the device was turned off
		// (no remaining energy)
		this.notGeneratedBecDeviceDead = i;

	}

	public String getSimStartTime() {
		return simStartTime;
	}

	public void setSimStartTime(String simStartTime) {
		this.simStartTime = simStartTime;
	}

	public int getTasksFailedRessourcesUnavailable() {
		return failedResourcesUnavailable;
	}

	public void setFailedDueToResourcesUnavailablity(int i) {
		this.failedResourcesUnavailable = i;
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

}
