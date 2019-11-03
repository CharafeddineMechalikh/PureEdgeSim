package com.mechalikh.pureedgesim.SimulationManager;

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

import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.MainApplication;
import com.mechalikh.pureedgesim.DataCentersManager.EdgeDataCenter;
import com.mechalikh.pureedgesim.DataCentersManager.EdgeVM;
import com.mechalikh.pureedgesim.Network.FileTransferProgress;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.TasksGenerator.Task;

public class SimLog {
	public static final int NO_TIME = 0;
	public static final int SAME_LINE = 1;
	public static final int DEFAULT = 2;
	private List<String> resultsList = new ArrayList<String>();
	private DecimalFormat decimalFormat;
	private List<String> log = new ArrayList<String>();
	private String currentOrchArchitecture;
	private String currentOrchAlgorithm;
	private int currentEdgeDevicesCount;
	private String simStartTime;
	private SimulationManager simulationManager;
	private boolean isFirstIteration;

	// Tasks execution results
	private int generatedTasksCount = 0;
	private int tasksSent = 0;
	private int tasksFailed = 0;
	private int tasksFailedLatency = 0;
	private int tasksFailedMobility = 0;
	private int tasksFailedRessourcesUnavailable = 0;
	private int tasksFailedBeacauseDeviceDead = 0;
	private int notGeneratedBecDeviceDead = 0;
	private double totalExecutionTime = 0;
	private double totalWaitingTime = 0;
	private int executedTasksCount = 0;
	private int tasksExecutedOnCloud = 0;
	private int tasksExecutedOnFog = 0;
	private int tasksExecutedOnEdge = 0;
	private int tasksFailedCloud = 0;
	private int tasksFailedFog = 0;
	private int tasksFailedEdge = 0;

	// Network utilization
	private double totalLanUsage = 0;
	private double totalWanUsage = 0;
	private double totalBandwidth = 0;
	private int transfersCount = 0;
	private double containersLanUsage = 0;
	private double containersWanUsage = 0;
	private double totalTraffic = 0;

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
			resultsList.add("Orchestration architecture,Orchestration algorithm,Edge devices count,"
					+ "Tasks execution delay (s),Average execution delay (s),Tasks waiting time (s),"
					+ "Average wainting time (s),Generated tasks,Tasks successfully executed,"
					+ "Task not executed (No resources available or long waiting time),Tasks failed (delay),Tasks failed (device dead),"
					+ "Tasks failed (mobility),Tasks not generated due to the death of devices,Total tasks executed (Cloud),"
					+ "Tasks successfully executed (Cloud),Total tasks executed (Fog),Tasks successfully executed (Fog),"
					+ "Total tasks executed (Edge),Tasks successfully executed (Edge),"
					+ "Network usage (s),Wan usage (s),Lan usage (s), Total network traffic (MBytes), Containers wan usage (s), Containers lan usage (s),Average bandwidth per task (Mbps),Average VM CPU usage (%),"
					+ "Average VM CPU usage (Cloud) (%),Average VM CPU usage (Fog) (%),Average VM CPU usage (Edge) (%),"
					+ "Energy consumption (Wh),Average energy consumption (Wh/Data center),Cloud energy consumption (Wh),"
					+ "Average Cloud energy consumption (Wh/Data center),Fog energy consumption (Wh),Average Fog energy consumption (Wh/Data center),"
					+ "Edge energy consumption (Wh),Average Edge energy consumption (Wh/Device),Dead devices count,"
					+ "Average remaining power (Wh),Average remaining power (%), First edge device death time (s),"
					+ "List of remaining power (%) (only battery powered devices / 0 = dead),List of the time when each device died (s)");
		}
	}

	public void showIterationResults(List<Task> finishedTasks) {
		printTasksRelatedResults();
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

	public void printTasksRelatedResults() {
		print("");
		print("------------------------------------------------------- OUTPUT -------------------------------------------------------");
		print("");
		print("SimLog- Tasks not sent because device died (low energy)                         :"
				+ padLeftSpaces(decimalFormat.format(notGeneratedBecDeviceDead / generatedTasksCount), 20) + " % ("
				+ notGeneratedBecDeviceDead + " tasks)");
		print("SimLog- Tasks sent from edge devices                                            :"
				+ padLeftSpaces("" + decimalFormat.format(((double) tasksSent * 100) / ((double) generatedTasksCount)),
						20)
				+ " % (" + tasksSent + " among " + generatedTasksCount + " generated tasks)");

		print("-------------------------------------All values below are based on the sent tasks-------------------------------------");
		print("SimLog- Tasks execution delay                                                   :"
				+ padLeftSpaces(decimalFormat.format(totalExecutionTime), 20) + " seconds");
		print("SimLog- Average tasks execution delay                                           :"
				+ padLeftSpaces(decimalFormat.format(totalExecutionTime / executedTasksCount), 20) + " seconds");
		print("SimLog- Tasks waiting time (from task submiting to the execution start)         :"
				+ padLeftSpaces(decimalFormat.format(totalWaitingTime), 20) + " seconds");
		print("SimLog- Average tasks waiting time (from task submiting to the execution start) :"
				+ padLeftSpaces(decimalFormat.format(totalWaitingTime / executedTasksCount), 20) + " seconds");
		print("SimLog- Tasks successfully executed                                             :"
				+ padLeftSpaces("" + decimalFormat.format((double) (tasksSent - tasksFailed) * 100 / tasksSent), 20)
				+ " % (" + (tasksSent - tasksFailed) + " among " + tasksSent + " sent tasks)");

		print("SimLog- Tasks failures");
		print("                                       Not executed due to resources unavailable:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedRessourcesUnavailable * 100 / tasksSent), 20)
				+ " % (" + tasksFailedRessourcesUnavailable + " tasks)");
		print("                              Successfully executed but failed due to high delay:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedLatency * 100 / tasksSent), 20) + " % ("
				+ tasksFailedLatency + " tasks from " + tasksSent + " successfully sent tasks)");
		print("                 Tasks execution results not returned because the device is dead:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedBeacauseDeviceDead * 100 / tasksSent), 20)
				+ " % (" + tasksFailedBeacauseDeviceDead + " tasks)");
		print("                     Tasks execution results not returned due to device mobility:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedMobility * 100 / tasksSent), 20) + " % ("
				+ tasksFailedMobility + " tasks)");

		print("SimLog- Tasks executed on each level                                            :" + " Cloud="
				+ padLeftSpaces("" + tasksExecutedOnCloud, 13) + " tasks (where "
				+ (tasksExecutedOnCloud - tasksFailedCloud) + " were successfully executed )");
		print("                                                                                 " + " Fog="
				+ padLeftSpaces("" + tasksExecutedOnFog, 15) + " tasks (where " + (tasksExecutedOnFog - tasksFailedFog)
				+ " were successfully executed )");
		print("                                                                                 " + " Edge="
				+ padLeftSpaces("" + tasksExecutedOnEdge, 14) + " tasks (where "
				+ (tasksExecutedOnEdge - tasksFailedEdge) + " were successfully executed )");

		resultsList.add(currentOrchArchitecture + "," + currentOrchAlgorithm + "," + currentEdgeDevicesCount + ","
				+ decimalFormat.format(totalExecutionTime) + ","
				+ decimalFormat.format(totalExecutionTime / executedTasksCount) + ","
				+ decimalFormat.format(totalWaitingTime) + ","
				+ decimalFormat.format(totalWaitingTime / executedTasksCount) + "," + generatedTasksCount + ","
				+ (tasksSent - tasksFailed) + "," + tasksFailed + "," + tasksFailedLatency + ","
				+ tasksFailedBeacauseDeviceDead + "," + tasksFailedMobility + "," + notGeneratedBecDeviceDead + ","
				+ tasksExecutedOnCloud + "," + (tasksExecutedOnCloud - tasksFailedCloud) + "," + tasksExecutedOnFog
				+ "," + (tasksExecutedOnFog - tasksFailedFog) + "," + tasksExecutedOnEdge + ","
				+ (tasksExecutedOnEdge - tasksFailedEdge) + ",");
	}

	public void printNetworkRelatedResults() {
		print("SimLog- Network usage                                                           :"
				+ padLeftSpaces(decimalFormat.format(totalLanUsage), 20) + " seconds (The total traffic: "
				+ decimalFormat.format(totalTraffic) + " (MBytes) )");
		print("                                                                                 " + " Wan="
				+ padLeftSpaces(decimalFormat.format(totalWanUsage), 15) + " seconds ("
				+ decimalFormat.format(totalWanUsage * 100 / totalLanUsage)
				+ " % of total usage, WAN used when downloading containers="
				+ decimalFormat.format(containersWanUsage * 100 / totalWanUsage) + " % of WAN usage )");
		print("                                                                                 " + " Lan="
				+ padLeftSpaces(decimalFormat.format(totalLanUsage), 15) + " seconds ("
				+ decimalFormat.format(totalLanUsage * 100 / totalLanUsage)
				+ " % of total usage, LAN used when downloading containers="
				+ decimalFormat.format(containersLanUsage * 100 / totalLanUsage) + " % of LAN usage )");
		print("                                                            Average bandwidth per transfer="
				+ padLeftSpaces(decimalFormat.format(totalBandwidth / transfersCount), 10) + " Mbps  ");
		// Add these values to the las item of the results list
		resultsList.set(resultsList.size() - 1,
				resultsList.get(resultsList.size() - 1) + totalLanUsage + "," + totalWanUsage + "," + totalLanUsage
						+ "," + totalTraffic + "," + containersWanUsage + "," + containersLanUsage + ","
						+ (totalBandwidth / transfersCount) + ",");
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
		List<? extends EdgeDataCenter> datacentersList = simulationManager.getServersManager().getDatacenterList();

		for (int j = 0; j < datacentersList.size(); j++) {
			dc = (EdgeDataCenter) datacentersList.get(j);
			if (dc.getType() == simulationParameters.TYPES.CLOUD) {
				cloudEnConsumption += dc.getEnergyModel().getTotalEnergyConsumption();
				averageCloudCpuUtilization += dc.getTotalCpuUtilization();
			}

			else if (dc.getType() == simulationParameters.TYPES.FOG) {
				fogEnConsumption += dc.getEnergyModel().getTotalEnergyConsumption();
				averageFogCpuUtilization += dc.getTotalCpuUtilization();
			} else if (dc.getType() == simulationParameters.TYPES.EDGE) {
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
				/ (edgeDevicesCount + simulationParameters.NUM_OF_FOG_DATACENTERS
						+ simulationParameters.NUM_OF_CLOUD_DATACENTERS);
		batteryPoweredDevicesCount = aliveBatteryPoweredDevicesCount + deadEdgeDevicesCount;
		// escape from devision by 0
		if (aliveBatteryPoweredDevicesCount == 0)
			aliveBatteryPoweredDevicesCount = 1;
		averageCloudCpuUtilization = averageCloudCpuUtilization / simulationParameters.NUM_OF_CLOUD_DATACENTERS;
		averageFogCpuUtilization = averageFogCpuUtilization / simulationParameters.NUM_OF_FOG_DATACENTERS;
		averageEdgeCpuUtilization = averageEdgeCpuUtilization / edgeDevicesCount;

		energyConsumption = cloudEnConsumption + fogEnConsumption + edgeEnConsumption;
		averageRemainingPower = averageRemainingPower / (double) aliveBatteryPoweredDevicesCount;
		averageRemainingPowerWh = averageRemainingPowerWh / (double) aliveBatteryPoweredDevicesCount;
		double averageCloudEnConsumption = cloudEnConsumption / simulationParameters.NUM_OF_CLOUD_DATACENTERS;
		double averageFogEnConsumption = fogEnConsumption / simulationParameters.NUM_OF_FOG_DATACENTERS;
		double averageEdgeEnConsumption = edgeEnConsumption / simulationManager.getScenario().getDevicesCount();

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
				+ decimalFormat.format(cloudEnConsumption / simulationParameters.NUM_OF_CLOUD_DATACENTERS)
				+ " Wh/data center)");
		print("                                                                                  Fog="
				+ padLeftSpaces(decimalFormat.format(fogEnConsumption), 15) + " Wh (Average: "
				+ decimalFormat.format(fogEnConsumption / simulationParameters.NUM_OF_FOG_DATACENTERS)
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

	public void cleanOutputFolder(String outputFolder) throws IOException {
		// Clean the folder where the results files will be saved
		if (isFirstIteration) {
			print("SimLog- Cleaning the outputfolder...");
			isFirstIteration = false;
			Path dir = new File(outputFolder).toPath();
			deleteDirectoryRecursion(dir);
		}
	}

	private void deleteDirectoryRecursion(Path path) throws IOException {
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
		writeFile(getFileName(".csv"), getResultsList());

		if (!simulationParameters.SAVE_LOG) {
			print("SimLog- no log saving");
			Runtime.getRuntime().exit(0);
		}

		writeFile(getFileName(".txt"), log);

	}

	private List<String> getResultsList() {
		return this.resultsList;
	}

	public void writeFile(String fileName, List<String> Lines) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName, true));
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
		String outputFilesName = MainApplication.getOutputFolder() + "/" + simStartTime;
		new File(outputFilesName).mkdirs();
		if (simulationParameters.PARALLEL)
			outputFilesName += "/Parallel_simulation_" + simulationManager.getSimulationId();
		else
			outputFilesName += "/Sequential_simulation";

		return outputFilesName + extension;
	}

	public void print(String line, int flag) {
		String newLine = line;
		if (simulationManager == null) {
			System.out.println("    0.0" + " : " + newLine);
		} else {
			switch (flag) {
			case DEFAULT:
				if (simulationManager.getSimulation().clock() < simulationParameters.INITIALIZATION_TIME)
					newLine = padLeftSpaces("0", 7) + " (s) : " + newLine;
				else
					newLine = padLeftSpaces(decimalFormat.format(
							simulationManager.getSimulation().clock() - simulationParameters.INITIALIZATION_TIME), 7)
							+ " (s) : " + newLine;
				log.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " - simulation time "
						+ newLine);
				break;
			case NO_TIME:
				log.add(newLine);
				break;
			case SAME_LINE:
				log.set(log.size() - 1, log.get(log.size() - 1) + newLine);
				break;
			default:
				break;
			}
		}
	}

	public void print(String line) {
		print(line, DEFAULT);
	}

	public static void println(String line) {
		System.out.println(line);
	}

	public void deepLog(String line) {
		if (simulationParameters.DEEP_LOGGING)
			print(line, DEFAULT);
	}

	public void deepLog(String line, int flag) {
		if (simulationParameters.DEEP_LOGGING) {
			print(line, flag);
		}
	}

	public void printWithoutTime(String line) {
		print(line, NO_TIME);
	}

	public void printSameLine(String line, String color) {
		if ("red".equalsIgnoreCase(color))
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

	public void setCurrentOrchPolicy(String currentOrchPolicy) {
		this.currentOrchArchitecture = currentOrchPolicy;
	}

	public void initialize(SimulationManager simulationManager, int dev, int alg, int arch) {
		this.currentEdgeDevicesCount = dev;
		this.currentOrchAlgorithm = simulationParameters.ORCHESTRATION_AlGORITHMS[alg];
		this.currentOrchArchitecture = simulationParameters.ORCHESTRATION_ARCHITECTURES[arch];
		this.simulationManager = simulationManager;
	}

	public String getSimStartTime() {
		return simStartTime;
	}

	public void setSimStartTime(String simStartTime) {
		this.simStartTime = simStartTime;
	}

	public void incrementTasksSent() {
		this.tasksSent++;
	}

	public void incrementTasksFailed(Task task) {
		this.tasksFailed++;
		if (task.getVm() == Vm.NULL)
			return;
		if (((EdgeVM) task.getVm()).getType() == simulationParameters.TYPES.CLOUD) {
			this.tasksFailedCloud++;
		} else if (((EdgeVM) task.getVm()).getType() == simulationParameters.TYPES.FOG) {
			this.tasksFailedFog++;
		} else if (((EdgeVM) task.getVm()).getType() == simulationParameters.TYPES.EDGE) {
			this.tasksFailedEdge++;
		}
	}

	public void incrementFailedBeacauseDeviceDead(Task task) {
		this.tasksFailedBeacauseDeviceDead++;
		incrementTasksFailed(task);
	}

	public void incrementNotGeneratedBeacuseDeviceDead() {
		this.notGeneratedBecDeviceDead++;
	}

	public void incrementTasksFailedLatency(Task task) {
		this.tasksFailedLatency++;
		incrementTasksFailed(task);
	}

	public void incrementTasksFailedMobility(Task task) {
		this.tasksFailedMobility++;
		incrementTasksFailed(task);
	}

	public void incrementTasksFailedLackOfRessources(Task task) {
		this.tasksFailedRessourcesUnavailable++;
		incrementTasksFailed(task);
	}

	public void getTasksExecutionInfos(Task task) {
		this.totalExecutionTime += task.getActualCpuTime();
		this.totalWaitingTime += task.getExecStartTime() - task.getTime();
		this.executedTasksCount++;
		if (((EdgeVM) task.getVm()).getType() == simulationParameters.TYPES.CLOUD) {
			this.tasksExecutedOnCloud++;
		} else if (((EdgeVM) task.getVm()).getType() == simulationParameters.TYPES.FOG) {
			this.tasksExecutedOnFog++;
		} else if (((EdgeVM) task.getVm()).getType() == simulationParameters.TYPES.EDGE) {
			this.tasksExecutedOnEdge++;
		}
	}

	public void updateNetworkUsage(FileTransferProgress transfer) {
		this.totalLanUsage += transfer.getLanNetworkUsage();
		this.totalWanUsage += transfer.getWanNetworkUsage();
		this.totalBandwidth += transfer.getFileSize() / (transfer.getLanNetworkUsage() * 1000); // Kbits/s to Mbits/s
		this.totalTraffic += transfer.getFileSize() / 8000; // Kbits to Mbytes

		if (transfer.getTransferType() == FileTransferProgress.CONTAINER) {
			this.containersLanUsage += transfer.getLanNetworkUsage();
			this.containersWanUsage += transfer.getWanNetworkUsage();
		}
		this.transfersCount++;

	}

}
