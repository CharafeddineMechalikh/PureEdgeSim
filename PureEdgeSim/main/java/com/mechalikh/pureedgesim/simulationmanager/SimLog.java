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
package com.mechalikh.pureedgesim.simulationmanager;

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
import java.util.stream.Collectors;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters; 
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Collects the metrics of one simulation, prints the text report, and writes the
 * CSV and log files. A {@code SimLog} is created per simulation run; in parallel
 * mode each worker gets its own instance and therefore its own CSV file.
 */
public class SimLog {
	/** Print flag that omits the simulation time prefix. */
	public static final int NO_TIME = 0;
	/** Print flag that appends to the previous log line. */
	public static final int SAME_LINE = 1;
	/** Print flag that prefixes the line with the simulation time. */
	public static final int DEFAULT = 2;
	/** Accumulated CSV row fragments for the current scenario. */
	protected ArrayList<String> resultsList = new ArrayList<String>();
	/** Formatter that uses a dot as decimal separator. */
	protected DecimalFormat decimalFormat;
	/** Text lines of the current simulation log. */
	protected List<String> log = new ArrayList<String>();
	/** Orchestration architecture of the current scenario. */
	protected String currentOrchArchitecture;
	/** Orchestration algorithm of the current scenario. */
	protected String currentOrchAlgorithm;
	/** Number of edge devices in the current scenario. */
	protected int currentEdgeDevicesCount;
	/** Timestamp shared by all files of one simulation campaign. */
	protected String simStartTime;
	/** Simulation manager that provides the current simulation time. */
	protected SimulationManager simulationManager;
	/** True while the output folder has not been cleaned yet. */
	protected boolean isFirstIteration;

	// Tasks execution results
	/** Tasks generated for the scenario. */
	protected int generatedTasksCount = 0;
	/** Tasks sent from their device to the orchestrator. */
	protected int tasksSent = 0;
	/** Tasks that failed, for any reason. */
	protected int tasksFailed = 0;
	/** Tasks that failed because their total delay exceeded the latency limit. */
	protected int tasksFailedLatency = 0;
	/** Tasks that failed because the device or destination moved away. */
	protected int tasksFailedMobility = 0;
	/** Tasks that failed because storage or RAM was insufficient. */
	protected int tasksFailedRessourcesUnavailable = 0;
	/** Tasks that failed because the device or destination died. */
	protected int tasksFailedBeacauseDeviceDead = 0;
	/** Tasks that were never sent because their device died. */
	protected int notGeneratedBecDeviceDead = 0;
	/** Subtasks that were never offloaded because a dependency failed. */
	protected int tasksFailedDependency = 0;
	/** Optional subtasks that were skipped because a dependency failed. */
	protected int tasksSkipped = 0;
	/** Application requests generated for the scenario. */
	protected int generatedRequests = 0;
	/** Requests whose every required subtask completed successfully. */
	protected int successfulRequests = 0;
	/** Requests that failed, because a required subtask failed. */
	protected int failedRequests = 0;
	/** Sum of the makespans of the successful requests, in seconds. */
	protected double totalRequestMakespan = 0;
	/** Edge devices that died because their battery ran out. */
	protected int deadDevicesCount = 0;
	/** Sum of actual CPU execution times of finished tasks, in seconds. */
	protected Double totalExecutionTime = 0.0;
	/** Sum of queue waiting times of finished tasks, in seconds. */
	protected Double totalWaitingTime = 0.0;
	/** Tasks whose execution time and waiting time were recorded. */
	protected int executedTasksCount = 0;
	/** Tasks sent to cloud data centers. */
	protected int tasksExecutedOnCloud = 0;
	/** Tasks sent to edge data centers. */
	protected int tasksExecutedOnEdge = 0;
	/** Tasks sent to edge devices. */
	protected int tasksExecutedOnMist = 0;
	/** Cloud tasks that failed. */
	protected int tasksFailedCloud = 0;
	/** Edge data center tasks that failed. */
	protected int tasksFailedEdge = 0;
	/** Edge device tasks that failed. */
	protected int tasksFailedMist = 0;

	// Network utilization
	/** Total LAN usage, in seconds. */
	protected Double totalLanUsage = 0.0;
	/** Total MAN usage, in seconds. */
	protected Double totalManUsage = 0.0;
	/** Total WAN usage, in seconds. */
	protected Double totalWanUsage = 0.0;
	/** Sum of the average bandwidth of each transfer, in Mbps. */
	protected Double totalBandwidth = 0.0;
	/** Number of recorded transfers. */
	protected int transfersCount = 0;
	/** LAN usage caused by container downloads, in seconds. */
	protected Double containersLanUsage = 0.0;
	/** WAN usage caused by container downloads, in seconds. */
	protected Double containersWanUsage = 0.0;
	/** MAN usage caused by container downloads, in seconds. */
	protected Double containersManUsage = 0.0;
	/** Total transferred data, in megabytes. */
	protected Double totalTraffic = 0.0;

	/**
	 * Creates a logger for one simulation.
	 *
	 * @param startTime       timestamp shared by the files of the campaign.
	 * @param isFirstIteration true for the first simulation of the campaign; the
	 *                         CSV header is written in that case.
	 */
	public SimLog(String startTime, boolean isFirstIteration) {
		this.setSimStartTime(startTime);
		this.isFirstIteration = isFirstIteration;

		// Use this format for all numbers
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
		otherSymbols.setDecimalSeparator('.'); // use the dot "." as separation symbole, since the comma ","
												// is used in csv files as a separator
		decimalFormat = new DecimalFormat("######.####", otherSymbols);

		if (isFirstIteration) {
			// Add the CSV file header
			resultsList.add("Orchestration architecture,Orchestration algorithm,Edge devices count,"
					+ "Total tasks execution delay (s),Average execution delay (s),Total tasks waiting time (s),"
					+ "Average waiting time (s),Number of generated tasks,Tasks successfully executed,"
					+ "Task not executed (No resources available or long waiting time),Tasks failed (delay),Tasks failed (device dead),"
					+ "Tasks failed (mobility),Tasks not generated due to the death of devices,"
					+ "Tasks not executed because a dependency failed,Total tasks executed (Cloud),"
					+ "Tasks successfully executed (Cloud),Total tasks executed (Edge),Tasks successfully executed (Edge),"
					+ "Total tasks executed (Mist),Tasks successfully executed (Mist),"
					+ "Network usage (s),Wan usage (s),Lan usage (s), Total network traffic (MBytes), Containers wan usage (s), Containers lan usage (s),Average bandwidth per task (Mbps),Average CPU usage (%),"
					+ "Average CPU usage (Cloud) (%),Average CPU usage (Edge) (%),Average CPU usage (Mist) (%),"
					+ "Energy consumption of computing nodes (Wh),Average energy consumption (Wh/Computing node),Cloud energy consumption (Wh),"
					+ "Average Cloud energy consumption (Wh/Data center),Edge energy consumption (Wh),Average Edge energy consumption (Wh/Data center),"
					+ "Mist energy consumption (Wh),Average Mist energy consumption (Wh/Device),"
					+ "WAN energy consumption (Wh), MAN energy consumption (Wh), LAN energy consumption (Wh),"
					+ "WiFi energy consumption (Wh), Cellular energy consumption (Wh), Ethernet energy consumption (Wh),"
					+ "Dead devices count,Average remaining power (Wh),Average remaining power (%), First edge device death time (s),"
					+ "List of remaining power (%) (only battery powered devices / 0 = dead),List of the time when each device died (s),"
					+ "Tasks skipped because their optional input failed,Requests generated,Successful requests,Average request makespan (s)");
		}
	}

	/**
	 * Prints all result sections, flushes the text log to disk, and keeps the CSV
	 * row ready for the next scenario.
	 *
	 * @param finishedTasks tasks whose results were returned during the scenario.
	 */
	public void showIterationResults(List<Task> finishedTasks) {
		printTasksRelatedResults();
		printNetworkRelatedResults();
		printCPUUtilizationResults();
		printPowerConsumptionResults(finishedTasks);
		StringBuilder s = new StringBuilder("\n");
		for (String value : log) {
			s.append(value).append("\n");
		}
		System.out.print(s);
		// update the log
		saveLog();
	}

	/**
	 * Prints the CPU utilization section and appends the four CPU columns to the
	 * current CSV row. The average per level is the mean across nodes of that
	 * level; sensors are excluded from the mist population.
	 */
	protected void printCPUUtilizationResults() {
		double averageCpuUtilization = 0;
		double averageCloudCpuUtilization = getCpuUtilizationForNodeType(SimulationParameters.TYPES.CLOUD);
		double averageEdgeCpuUtilization = getCpuUtilizationForNodeType(SimulationParameters.TYPES.EDGE_DATACENTER);
		double averageMistCpuUtilization = getCpuUtilizationForNodeType(SimulationParameters.TYPES.EDGE_DEVICE);

		int totalNodes = simulationManager.getDataCentersManager()
				.getComputingNodesGenerator().getMistOnlyListSensorsExcluded().size() + SimulationParameters.numberOfEdgeDataCenters
				+ SimulationParameters.numberOfCloudDataCenters;
		if (totalNodes > 0) {
			averageCpuUtilization = (averageCloudCpuUtilization + averageMistCpuUtilization + averageEdgeCpuUtilization)
					/ totalNodes;
		}

		averageCloudCpuUtilization /= SimulationParameters.numberOfCloudDataCenters;
		averageEdgeCpuUtilization /= SimulationParameters.numberOfEdgeDataCenters;
		averageMistCpuUtilization /= simulationManager.getDataCentersManager().getComputingNodesGenerator().getMistOnlyListSensorsExcluded().size();

		print("Average CPU utilization                                                 :"
				+ padLeftSpaces(decimalFormat.format(averageCpuUtilization), 20) + " %%");
		print("Average CPU utilization per level                                       :Cloud= "
				+ padLeftSpaces(decimalFormat.format(averageCloudCpuUtilization), 13) + " %%");
		print("                                                                          Edge= "
				+ padLeftSpaces(decimalFormat.format(averageEdgeCpuUtilization), 13) + " %%");
		print("                                                                          Mist= "
				+ padLeftSpaces(decimalFormat.format(averageMistCpuUtilization), 13) + " %%");

		resultsList.set(resultsList.size() - 1,
				resultsList.get(resultsList.size() - 1) + decimalFormat.format(averageCpuUtilization) + ","
						+ decimalFormat.format(averageCloudCpuUtilization) + ","
						+ decimalFormat.format(averageEdgeCpuUtilization) + ","
						+ decimalFormat.format(averageMistCpuUtilization) + ",");
	}



	private double getCpuUtilizationForNodeType(SimulationParameters.TYPES nodeType) {
		List<ComputingNode> nodes = getNodesByType(nodeType);
		return getCpuUtilization(nodes);
	}

	/**
	 * Returns all computing nodes of a given type.
	 *
	 * @param nodeType the type to select.
	 * @return the matching nodes.
	 */
	public List<ComputingNode> getNodesByType(SimulationParameters.TYPES nodeType) {
	    return simulationManager.getDataCentersManager().getComputingNodesGenerator()
	            .getAllNodesList()
	            .stream()
	            .filter(node -> node.getType().equals(nodeType))
	            .collect(Collectors.toList());
	}

	/**
	 * Sums the average CPU utilization of the given nodes.
	 *
	 * @param list the nodes to measure.
	 * @return the sum of their lifetime average utilization, in percent.
	 */
	protected double getCpuUtilization(List<ComputingNode> list) {
		double averageCpuUtilization = 0;
		for (ComputingNode node : list) {
			averageCpuUtilization += node.getAvgCpuUtilization();
		}
		return averageCpuUtilization;
	}

	/**
	 * Prints the task outcome section and appends the task columns to the current
	 * CSV row. All percentages here are relative to the tasks that were sent from
	 * the devices.
	 */
	public void printTasksRelatedResults() {
		print(getClass().getSimpleName() + " - Printing iteration output...");
		print("------------------------------------------------------- OUTPUT -------------------------------------------------------");
		print("");
		print("Tasks not sent because device died (low energy)                         :"
				+ padLeftSpaces(decimalFormat.format(
						generatedTasksCount == 0 ? 0.0 : (double) notGeneratedBecDeviceDead * 100 / generatedTasksCount),
						20)
				+ " %% (" + notGeneratedBecDeviceDead + " tasks)");
		print("Tasks not executed because a dependency failed                          :"
				+ padLeftSpaces(decimalFormat.format(
						generatedTasksCount == 0 ? 0.0 : (double) tasksFailedDependency * 100 / generatedTasksCount),
						20)
				+ " %% (" + tasksFailedDependency + " tasks)");
		print("Tasks skipped because their optional input failed                       :"
				+ padLeftSpaces(decimalFormat.format(
						generatedTasksCount == 0 ? 0.0 : (double) tasksSkipped * 100 / generatedTasksCount), 20)
				+ " %% (" + tasksSkipped + " tasks)");
		print("Tasks sent from edge devices                                            :"
				+ padLeftSpaces("" + decimalFormat.format(((double) tasksSent * 100) / ((double) generatedTasksCount)),
						20)
				+ " %% (" + tasksSent + " among " + generatedTasksCount + " generated tasks)");

		print("-------------------------------------All values below are based on the sent tasks-------------------------------------");
		print("Total tasks execution time                                              :"
				+ padLeftSpaces(decimalFormat.format(totalExecutionTime), 20) + " seconds");
		print("Average task execution time                                             :"
				+ padLeftSpaces(decimalFormat.format(totalExecutionTime / executedTasksCount), 20) + " seconds");
		print("Total waiting time (from submitting the tasks to when execution started):"
				+ padLeftSpaces(decimalFormat.format(totalWaitingTime), 20) + " seconds");
		print("Average task waiting time                                               :"
				+ padLeftSpaces(decimalFormat.format(totalWaitingTime / executedTasksCount), 20) + " seconds");
		print("Tasks successfully executed                                             :"
				+ padLeftSpaces("" + decimalFormat.format((double) (tasksSent - tasksFailed) * 100 / tasksSent), 20)
				+ " %% (" + (tasksSent - tasksFailed) + " among " + tasksSent + " sent tasks)");

		print("Tasks failures");
		print("                              Not executed due to resource unavailablity:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedRessourcesUnavailable * 100 / tasksSent), 20)
				+ " %% (" + tasksFailedRessourcesUnavailable + " tasks)");
		print("                                   Executed but failed due to high delay:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedLatency * 100 / tasksSent), 20) + " %% ("
				+ tasksFailedLatency + " tasks from " + tasksSent + " successfully sent tasks)");
		print("               Tasks execution results not returned due to devices death:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedBeacauseDeviceDead * 100 / tasksSent), 20)
				+ " %% (" + tasksFailedBeacauseDeviceDead + " tasks)");
		print("            Tasks execution results not returned due to devices mobility:"
				+ padLeftSpaces(decimalFormat.format((double) tasksFailedMobility * 100 / tasksSent), 20) + " %% ("
				+ tasksFailedMobility + " tasks)");

		print("Tasks executed on each level                                            :" + "Cloud= "
				+ padLeftSpaces("" + tasksExecutedOnCloud, 13) + " tasks (where "
				+ (tasksExecutedOnCloud - tasksFailedCloud) + " were successfully executed )");
		print("                                                                         " + " Edge="
				+ padLeftSpaces("" + tasksExecutedOnEdge, 14) + " tasks (where "
				+ (tasksExecutedOnEdge - tasksFailedEdge) + " were successfully executed )");
		print("                                                                         " + " Mist="
				+ padLeftSpaces("" + tasksExecutedOnMist, 14) + " tasks (where "
				+ (tasksExecutedOnMist - tasksFailedMist) + " were successfully executed )");

		print("Requests");
		print("                                                         Requests generated:"
				+ padLeftSpaces("" + generatedRequests, 20) + " requests");
		print("                                          Requests that completed successfully:"
				+ padLeftSpaces(decimalFormat.format(
						generatedRequests == 0 ? 0.0 : (double) successfulRequests * 100 / generatedRequests), 20)
				+ " %% (" + successfulRequests + " among " + generatedRequests + " requests)");
		print("                                                  Requests that failed:"
				+ padLeftSpaces("" + failedRequests, 20) + " requests");
		print("                                        Average request makespan (end to end):"
				+ padLeftSpaces(decimalFormat
						.format(successfulRequests == 0 ? 0.0 : totalRequestMakespan / successfulRequests), 20)
				+ " seconds");

		resultsList.add(currentOrchArchitecture + "," + currentOrchAlgorithm + "," + currentEdgeDevicesCount + ","
				+ decimalFormat.format(totalExecutionTime) + ","
				+ decimalFormat.format(totalExecutionTime / executedTasksCount) + ","
				+ decimalFormat.format(totalWaitingTime) + ","
				+ decimalFormat.format(totalWaitingTime / executedTasksCount) + "," + generatedTasksCount + ","
				+ (tasksSent - tasksFailed) + "," + tasksFailedRessourcesUnavailable + "," + tasksFailedLatency + ","
				+ tasksFailedBeacauseDeviceDead + "," + tasksFailedMobility + "," + notGeneratedBecDeviceDead + ","
				+ tasksFailedDependency + ","
				+ tasksExecutedOnCloud + "," + (tasksExecutedOnCloud - tasksFailedCloud) + "," + tasksExecutedOnEdge
				+ "," + (tasksExecutedOnEdge - tasksFailedEdge) + "," + tasksExecutedOnMist + ","
				+ (tasksExecutedOnMist - tasksFailedMist) + ",");
	}

	/**
	 * Prints the network section and appends the network columns to the current CSV
	 * row.
	 */
	public void printNetworkRelatedResults() {
		print("Network usage                                                           :"
				+ padLeftSpaces(decimalFormat.format(totalLanUsage + totalManUsage + totalWanUsage), 20)
				+ " seconds (The total traffic: " + decimalFormat.format(totalTraffic) + " (MBytes) )");
		print("                                                                         " + "  Wan="
				+ padLeftSpaces(decimalFormat.format(totalWanUsage), 14) + " seconds ("
				+ decimalFormat.format(totalWanUsage * 100 / (totalLanUsage + totalManUsage + totalWanUsage))
				+ " %% of total usage, WAN used when downloading containers="
				+ decimalFormat.format(totalWanUsage == 0 ? 0 : containersWanUsage * 100 / totalWanUsage)
				+ " %% of WAN usage )");
		print("                                                                         " + "  Man="
				+ padLeftSpaces(decimalFormat.format(totalManUsage), 14) + " seconds ("
				+ decimalFormat.format(totalManUsage * 100 / (totalLanUsage + totalManUsage + totalWanUsage))
				+ " %% of total usage, MAN used when downloading containers="
				+ decimalFormat.format(totalManUsage == 0 ? 0 : containersManUsage * 100 / totalManUsage)
				+ " %% of MAN usage )");
		print("                                                                         " + "  Lan="
				+ padLeftSpaces(decimalFormat.format(totalLanUsage), 14) + " seconds ("
				+ decimalFormat.format(totalLanUsage * 100 / (totalLanUsage + totalManUsage + totalWanUsage))
				+ " %% of total usage, LAN used when downloading containers="
				+ decimalFormat.format(containersLanUsage * 100 / totalLanUsage) + " %% of LAN usage )");
		print("Average transfer speed                                                  :"
				+ padLeftSpaces(decimalFormat.format(totalBandwidth / transfersCount), 20) + " Mbps  ");
		// Add these values to the las item of the results list
		resultsList.set(resultsList.size() - 1,
				resultsList.get(resultsList.size() - 1) + (totalLanUsage + totalManUsage + totalWanUsage) + ","
						+ totalWanUsage + "," + totalLanUsage + "," + totalTraffic + "," + containersWanUsage + ","
						+ containersLanUsage + "," + (totalBandwidth / transfersCount) + ",");
	}

	/**
	 * Prints the energy and battery section and appends the energy, dead-device,
	 * and remaining-power columns to the current CSV row.
	 *
	 * @param finishedTasks tasks that completed during the scenario; used for the
	 *                      energy-per-task value.
	 */
	public void printPowerConsumptionResults(List<Task> finishedTasks) {
		int deadEdgeDevicesCount = getDeadDevicesCount();
		double energyConsumption = 0;
		double cloudEnConsumption = 0;
		double mistEnConsumption = 0;
		double edgeEnConsumption = 0;
		double averageRemainingPowerWh = 0;
		double averageRemainingPower = 0;
		List<Double> remainingPower = new ArrayList<>();
		double firstDeviceDeathTime = -1; // -1 means invalid
		List<Double> devicesDeathTime = new ArrayList<>();
		int batteryPoweredDevicesCount = 0;
		int aliveBatteryPoweredDevicesCount = 0;

		double wan = simulationManager.getDataCentersManager().getTopology().getWanLinks().stream()
				.map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double man = simulationManager.getDataCentersManager().getTopology().getManLinks().stream()
				.map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double lan = simulationManager.getDataCentersManager().getTopology().getLanLinks().stream()
				.map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double cellular = simulationManager.getDataCentersManager().getTopology().getCellularLinks().stream()
				.map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double eth = simulationManager.getDataCentersManager().getTopology().getEthernetLinks().stream()
				.map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		double wifi = simulationManager.getDataCentersManager().getTopology().getWifiLinks().stream()
				.map(NetworkLink::getEnergyModel)
				.collect(Collectors.summingDouble(EnergyModelNetworkLink::getTotalEnergyConsumption));

		for (ComputingNode node : simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getCloudOnlyList())
			cloudEnConsumption = node.getEnergyModel().getTotalEnergyConsumption();

		for (ComputingNode node : simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getEdgeOnlyList())
			edgeEnConsumption += node.getEnergyModel().getTotalEnergyConsumption();

		for (ComputingNode node : simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistOnlyList()) {
			ComputingNode edgeDevice = node;
			mistEnConsumption += edgeDevice.getEnergyModel().getTotalEnergyConsumption();

			if (edgeDevice.isDead()) {
				devicesDeathTime.add(edgeDevice.getDeathTime());
				remainingPower.add(0.0);
				if (firstDeviceDeathTime == -1)
					firstDeviceDeathTime = edgeDevice.getDeathTime();
				else if (firstDeviceDeathTime > edgeDevice.getDeathTime())
					firstDeviceDeathTime = edgeDevice.getDeathTime();
			} else {
				if (edgeDevice.getEnergyModel().isBatteryPowered()) {
					averageRemainingPowerWh += edgeDevice.getEnergyModel().getBatteryLevelWattHour();
					averageRemainingPower += edgeDevice.getEnergyModel().getBatteryLevelPercentage();
					remainingPower.add(edgeDevice.getEnergyModel().getBatteryLevelPercentage());
					aliveBatteryPoweredDevicesCount++;
				}
			}

		}
		batteryPoweredDevicesCount = aliveBatteryPoweredDevicesCount + deadEdgeDevicesCount;
		// escape from devision by 0
		if (aliveBatteryPoweredDevicesCount == 0)
			aliveBatteryPoweredDevicesCount = 1;
		energyConsumption = cloudEnConsumption + edgeEnConsumption + mistEnConsumption;
		averageRemainingPower = averageRemainingPower / (double) aliveBatteryPoweredDevicesCount;
		averageRemainingPowerWh = averageRemainingPowerWh / (double) aliveBatteryPoweredDevicesCount;
		double averageCloudEnConsumption = cloudEnConsumption / SimulationParameters.numberOfCloudDataCenters;
		double averageEdgeEnConsumption = edgeEnConsumption / SimulationParameters.numberOfEdgeDataCenters;
		double averageMistEnConsumption = mistEnConsumption / simulationManager.getScenario().getDevicesCount();

		print("Energy consumption                                                      :"
				+ padLeftSpaces(decimalFormat.format(energyConsumption), 20) + " Wh (Average: "
				+ decimalFormat.format(energyConsumption
						/ (SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters
								+ simulationManager.getScenario().getDevicesCount()))
				+ " Wh/data center(or device))");
		print("                                                                        :" + padLeftSpaces("", 19)
				+ "     (Average: " + decimalFormat.format(energyConsumption / (double) finishedTasks.size())
				+ " Wh/task)");
		print("Energy Consumption per level                                            :Cloud= "
				+ padLeftSpaces(decimalFormat.format(cloudEnConsumption), 13) + " Wh (Average: "
				+ decimalFormat.format(cloudEnConsumption / SimulationParameters.numberOfCloudDataCenters)
				+ " Wh/data center)");
		print("                                                                          Edge="
				+ padLeftSpaces(decimalFormat.format(edgeEnConsumption), 14) + " Wh (Average: "
				+ decimalFormat.format(edgeEnConsumption / SimulationParameters.numberOfEdgeDataCenters)
				+ " Wh/data center)");
		print("                                                                          Mist="
				+ padLeftSpaces(decimalFormat.format(mistEnConsumption), 14) + " Wh (Average: "
				+ decimalFormat.format(mistEnConsumption / currentEdgeDevicesCount) + " Wh/edge device)");

		print("Energy Consumption per network                                          :  WAN="
				+ padLeftSpaces(decimalFormat.format(wan), 14) + " Wh");
		print("                                                                           MAN="
				+ padLeftSpaces(decimalFormat.format(man), 14) + " Wh ");
		print("                                                                           LAN="
				+ padLeftSpaces(decimalFormat.format(lan), 14) + " Wh ");

		print("Energy Consumption per technology                                       : WiFi="
				+ padLeftSpaces(decimalFormat.format(wifi), 14) + " Wh");
		print("                                                                      Cellular="
				+ padLeftSpaces(decimalFormat.format(cellular), 14) + " Wh ");
		print("                                                                      Ethernet="
				+ padLeftSpaces(decimalFormat.format(eth), 14) + " Wh ");

		print("Dead edge devices due to battery drain                                  :"
				+ padLeftSpaces(decimalFormat.format(deadEdgeDevicesCount), 20) + " devices (Among "
				+ batteryPoweredDevicesCount + " devices with batteries ("
				+ decimalFormat.format(((double) deadEdgeDevicesCount) * 100 / (double) batteryPoweredDevicesCount)
				+ " %%))");
		print("Average remaining power (devices with batteries that are still alive)   :"
				+ padLeftSpaces(decimalFormat.format(averageRemainingPowerWh), 20) + " Wh (Average: "
				+ decimalFormat.format(averageRemainingPower) + " %%)");
		if (firstDeviceDeathTime != -1)
			print("First device died at                                                    :"
					+ padLeftSpaces("" + firstDeviceDeathTime, 20) + " seconds");

		// Add these values to the las item of the results list
		resultsList.set(resultsList.size() - 1, resultsList.get(resultsList.size() - 1)
				+ decimalFormat.format(energyConsumption) + ","
				+ decimalFormat.format(energyConsumption
						/ (SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters
								+ simulationManager.getScenario().getDevicesCount()))
				+ "," + decimalFormat.format(cloudEnConsumption) + "," + decimalFormat.format(averageCloudEnConsumption)
				+ "," + decimalFormat.format(edgeEnConsumption) + "," + decimalFormat.format(averageEdgeEnConsumption)
				+ "," + decimalFormat.format(mistEnConsumption) + "," + decimalFormat.format(averageMistEnConsumption)
				+ "," + decimalFormat.format(wan) + "," + decimalFormat.format(man) + "," + decimalFormat.format(lan)
				+ "," + decimalFormat.format(wifi) + "," + decimalFormat.format(cellular) + "," + decimalFormat.format(eth)
				+ "," + decimalFormat.format(deadEdgeDevicesCount) + "," + decimalFormat.format(averageRemainingPowerWh)
				+ "," + decimalFormat.format(averageRemainingPower) + "," + firstDeviceDeathTime + ","
				+ remainingPower.toString().replace(",", "-") + "," + devicesDeathTime.toString().replace(",", "-")
				+ "," + tasksSkipped + "," + generatedRequests + "," + successfulRequests + ","
				+ decimalFormat.format(successfulRequests == 0 ? 0.0 : totalRequestMakespan / successfulRequests));
	}

	/**
	 * Pads a string on the left with spaces.
	 *
	 * @param str the string to pad.
	 * @param n   the desired width.
	 * @return the padded string.
	 */
	public String padLeftSpaces(String str, int n) {
		return String.format("%1$" + n + "s", str);
	}

	/**
	 * Deletes the configured output folder, but only during the first iteration of
	 * the campaign.
	 *
	 * @throws IOException if the folder cannot be traversed or deleted.
	 */
	public void cleanOutputFolder() throws IOException {
		// Clean the folder where the results files will be saved
		if (isFirstIteration) {
			print(getClass().getSimpleName() + " - Cleaning the outputfolder...");
			isFirstIteration = false;
			Path dir = new File(SimulationParameters.outputFolder).toPath();
			deleteDirectory(dir);
		}
	}

	/**
	 * Recursively deletes a file or directory. Failures are logged and do not
	 * interrupt the deletion of the remaining entries.
	 *
	 * @param path the path to delete.
	 * @throws IOException if the directory cannot be listed.
	 */
	protected void deleteDirectory(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					deleteDirectory(entry);
				}
			}
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			print(getClass().getSimpleName() + " - Could not delete file/folder: " + path.toString());
		}
	}

	/**
	 * Writes the accumulated CSV row and, when enabled, the text log. The CSV is
	 * always written; the text file depends on
	 * {@link SimulationParameters#saveLog}.
	 */
	public void saveLog() {
		// writing results in csv file
		writeFile(getFileName(".csv"), getResultsList());

		if (!SimulationParameters.saveLog) {
			println("%s - No log saving", getClass().getSimpleName());
			return;
		}

		writeFile(getFileName(".txt"), log);

	}

	/**
	 * Returns the accumulated CSV rows.
	 *
	 * @return the CSV lines, including the header on the first iteration.
	 */
	protected List<String> getResultsList() {
		return this.resultsList;
	}

	/**
	 * Appends lines to a file and clears the provided list.
	 *
	 * @param fileName the destination file.
	 * @param Lines    the lines to append; the list is emptied afterwards.
	 */
	public void writeFile(String fileName, List<String> Lines) {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName, true))) {
			for (String str : Lines) {
				bufferedWriter.append(str);
				bufferedWriter.newLine();
			}
			Lines.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the output file path for the current simulation. Parallel runs use
	 * {@code Parallel_simulation_<id>}; sequential runs use
	 * {@code Sequential_simulation}. The parent folder is created if needed.
	 *
	 * @param extension the file extension, including the dot.
	 * @return the full file path.
	 */
	public String getFileName(String extension) {
		String outputFilesName = SimulationParameters.outputFolder + "/" + simStartTime;
		new File(outputFilesName).mkdirs();
		if (SimulationParameters.parallelism_enabled)
			outputFilesName += "/Parallel_simulation_" + simulationManager.getSimulationId();
		else
			outputFilesName += "/Sequential_simulation";

		return outputFilesName + extension;
	}

	/**
	 * Prints a line with the requested formatting flag.
	 *
	 * @param flag    {@link #DEFAULT}, {@link #NO_TIME}, or {@link #SAME_LINE}.
	 * @param newLine the line format, possibly containing {@code %s} placeholders.
	 * @param args    the format arguments.
	 */
	public void print(int flag, String newLine, Object... args) {
		if (args != null)
			newLine = String.format(newLine, args);

		if (simulationManager == null) {
			System.out.format("    0.0 : %s \n", newLine, args);
		} else {
			switch (flag) {
			case DEFAULT:

				newLine = padLeftSpaces(decimalFormat.format(simulationManager.getSimulation().clock()), 7) + " (s) : "
						+ newLine;
				log.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " - simulation time "
						+ newLine);
				break;
			case NO_TIME:
				log.add(newLine);
				break;
			case SAME_LINE:
				log.set(log.size() - 1, log.get(log.size() - 1) + String.format(newLine, args));
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Prints a line prefixed with the simulation time.
	 *
	 * @param line the line format, possibly containing {@code %s} placeholders.
	 * @param args the format arguments.
	 */
	public void print(String line, Object... args) {
		print(DEFAULT, line, args);
	}

	/**
	 * Prints a line to the console without adding it to the simulation log.
	 *
	 * @param line the line format, possibly containing {@code %s} placeholders.
	 * @param args the format arguments.
	 */
	public static void println(String line, Object... args) {
		System.out.format(line + "\n", args);
	}

	/**
	 * Writes a debug line to the log and the console when deep logging is enabled.
	 *
	 * @param line the line format, possibly containing {@code %s} placeholders.
	 * @param args the format arguments.
	 */
	public void deepLog(String line, Object... args) {
		if (SimulationParameters.deepLoggingEnabled) {
			print(DEFAULT, line, args);
			System.out.format(line, args);
		}
	}

	/**
	 * Writes a debug line with a formatting flag when deep logging is enabled.
	 *
	 * @param flag the print flag.
	 * @param line the line format.
	 * @param args the format arguments.
	 */
	public void deepLog(int flag, String line, Object... args) {
		if (SimulationParameters.deepLoggingEnabled) {
			print(flag, line, args);
		}
	}

	/**
	 * Prints a line to the log without the simulation time prefix.
	 *
	 * @param line the line format.
	 * @param args the format arguments.
	 */
	public void printWithoutTime(String line, Object... args) {
		print(NO_TIME, line, args);
	}

	/**
	 * Prints text on the current console line, in red on the error stream when
	 * requested.
	 *
	 * @param line  the text.
	 * @param color {@code "red"} for the error stream, anything else for stdout.
	 */
	public void printSameLine(String line, String color) {
		if ("red".equalsIgnoreCase(color))
			System.err.print(line);
		else
			System.out.print(line);
	}

	/**
	 * Prints text on the current console line.
	 *
	 * @param line the text.
	 */
	public void printSameLine(String line) {
		System.out.print(line);
	}

	/**
	 * Returns the number of generated tasks.
	 *
	 * @return the generated task count.
	 */
	public int getGeneratedTasks() {
		return generatedTasksCount;
	}

	/**
	 * Sets the number of generated tasks.
	 *
	 * @param generatedTasks the generated task count.
	 */
	public void setGeneratedTasks(int generatedTasks) {
		this.generatedTasksCount = generatedTasks;
	}

	/**
	 * Sets the orchestration architecture name used in the CSV row.
	 *
	 * @param currentOrchPolicy the architecture name.
	 */
	public void setCurrentOrchPolicy(String currentOrchPolicy) {
		this.currentOrchArchitecture = currentOrchPolicy;
	}

	/**
	 * Binds the logger to the simulation that is about to start.
	 *
	 * @param simulationManager the simulation manager.
	 * @param dev               number of edge devices in the scenario.
	 * @param alg               index of the orchestration algorithm in
	 *                          {@link SimulationParameters#orchestrationAlgorithms}.
	 * @param arch              index of the architecture in
	 *                          {@link SimulationParameters#orchestrationArchitectures}.
	 */
	public void initialize(SimulationManager simulationManager, int dev, int alg, int arch) {
		this.currentEdgeDevicesCount = dev;
		this.currentOrchAlgorithm = SimulationParameters.orchestrationAlgorithms[alg];
		this.currentOrchArchitecture = SimulationParameters.orchestrationArchitectures[arch];
		this.simulationManager = simulationManager;
	}

	/**
	 * Returns the timestamp shared by the files of the campaign.
	 *
	 * @return the start time.
	 */
	public String getSimStartTime() {
		return simStartTime;
	}

	/**
	 * Sets the timestamp shared by the files of the campaign.
	 *
	 * @param simStartTime the start time.
	 */
	public void setSimStartTime(String simStartTime) {
		this.simStartTime = simStartTime;
	}

	/** Increments the number of tasks sent from their device to the orchestrator. */
	public void incrementTasksSent() {
		this.tasksSent++;
	}

	/**
	 * Counts a failed task and attributes it to the level of its offloading
	 * destination, when one was selected.
	 *
	 * @param task the failed task.
	 */
	public void incrementTasksFailed(Task task) {
		this.tasksFailed++;
		if (task.getOffloadingDestination() == null)
			return;
		SimulationParameters.TYPES type = task.getOffloadingDestination().getType();

		if (type == SimulationParameters.TYPES.CLOUD) {
			this.tasksFailedCloud++;
		} else if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			this.tasksFailedEdge++;
		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			this.tasksFailedMist++;
		}
	}

	/**
	 * Counts a task that failed because its device or destination died.
	 *
	 * @param task the failed task.
	 */
	public void incrementFailedBeacauseDeviceDead(Task task) {
		this.tasksFailedBeacauseDeviceDead++;
		incrementTasksFailed(task);
	}

	/**
	 * Counts a task that was not sent because its device had already died.
	 */
	public void incrementNotGeneratedBeacuseDeviceDead() {
		this.notGeneratedBecDeviceDead++;
	}

	/**
	 * Counts a subtask that was never offloaded because one of its dependencies
	 * failed, so it could never run.
	 */
	public void incrementTasksFailedDependency() {
		this.tasksFailedDependency++;
	}

	/**
	 * Counts an optional subtask that was skipped because one of its dependencies
	 * failed. A skipped subtask does not fail the request.
	 */
	public void incrementTasksSkipped() {
		this.tasksSkipped++;
	}

	/** Counts one application request at generation time. */
	public void incrementGeneratedRequests() {
		this.generatedRequests++;
	}

	/** Counts a request that failed because a required subtask failed. */
	public void incrementFailedRequests() {
		this.failedRequests++;
	}

	/**
	 * Records a request whose required subtasks all completed.
	 *
	 * @param makespan the time elapsed between the arrival of the request and the
	 *                 completion of its last subtask, in seconds
	 */
	public void requestSucceeded(double makespan) {
		this.successfulRequests++;
		this.totalRequestMakespan += makespan;
	}

	/** Counts a node that died because its battery ran out. */
	public void incrementDeadDevices() {
		this.deadDevicesCount++;
	}

	/**
	 * Returns the number of devices that died because their battery ran out.
	 *
	 * @return the death counter of the current scenario.
	 */
	public int getDeadDevicesCount() {
		return this.deadDevicesCount;
	}

	/**
	 * Counts a task that failed because its total delay exceeded the latency
	 * limit.
	 *
	 * @param task the failed task.
	 */
	public void incrementTasksFailedLatency(Task task) {
		this.tasksFailedLatency++;
		incrementTasksFailed(task);
	}

	/**
	 * Counts a task that failed because the device or destination moved out of
	 * range.
	 *
	 * @param task the failed task.
	 */
	public void incrementTasksFailedMobility(Task task) {
		this.tasksFailedMobility++;
		incrementTasksFailed(task);
	}

	/**
	 * Counts a task that failed because the destination lacked storage or RAM.
	 *
	 * @param task the failed task.
	 */
	public void incrementTasksFailedLackOfRessources(Task task) {
		this.tasksFailedRessourcesUnavailable++;
		incrementTasksFailed(task);
	}

	/**
	 * Records the execution and waiting times of a finished task. Only actual CPU
	 * time and queue waiting time are accumulated; network delays are not included.
	 *
	 * @param task the finished task.
	 */
	public void getTasksExecutionInfos(Task task) {
		this.totalExecutionTime += task.getActualCpuTime();
		this.totalWaitingTime += task.getWatingTime();
		this.executedTasksCount++;
	}

	/**
	 * Counts a task sent from the orchestrator to its destination, by level.
	 *
	 * @param task the sent task.
	 */
	public void taskSentFromOrchToDest(Task task) {
		SimulationParameters.TYPES type = task.getOffloadingDestination().getType();
		if (type == SimulationParameters.TYPES.CLOUD) {
			this.tasksExecutedOnCloud++;
		} else if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			this.tasksExecutedOnEdge++;
		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			this.tasksExecutedOnMist++;
		}
	}

	/**
	 * Accumulates the network usage of a finished transfer. Container transfers are
	 * also counted separately so their share of each network type can be reported.
	 *
	 * @param transfer the finished transfer.
	 */
	public void updateNetworkUsage(TransferProgress transfer) {
		this.totalLanUsage += transfer.getLanNetworkUsage();
		this.totalManUsage += transfer.getManNetworkUsage();
		this.totalWanUsage += transfer.getWanNetworkUsage();
		this.totalBandwidth += transfer.getAverageBandwidth() / 1000000; // bits/s to Mbits/s
		this.totalTraffic += transfer.getFileSize() / 8000000; // bits to Mbytes

		if (transfer.getTransferType() == TransferProgress.Type.CONTAINER) {
			this.containersLanUsage += transfer.getLanNetworkUsage();
			this.containersWanUsage += transfer.getWanNetworkUsage();
			this.containersManUsage += transfer.getManNetworkUsage();
		}
		this.transfersCount++;

	}

}
