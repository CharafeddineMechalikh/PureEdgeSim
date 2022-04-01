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
package com.mechalikh.pureedgesim.scenariomanager;

import java.util.List;

import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract.Files;
import com.mechalikh.pureedgesim.tasksgenerator.Application;
import com.mechalikh.pureedgesim.tasksgenerator.Task;

public class SimulationParameters {
	/**
	 * The path to the configuration file.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract#setCustomFilePath(String path, Files file) 
	 */
	public static String SIMULATION_PARAMETERS_FILE = "PureEdgeSim/settings/simulation_parameters.properties";

	/**
	 * The path to the applications characteristics file.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String APPLICATIONS_FILE = "PureEdgeSim/settings/applications.xml";

	/**
	 * The path to the edge data centers characteristics file.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String EDGE_DATACENTERS_FILE = "PureEdgeSim/settings/edge_datacenters.xml";

	/**
	 * The path to the edge devices characteristics file.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String EDGE_DEVICES_FILE = "PureEdgeSim/settings/edge_devices.xml";

	/**
	 * The path to the cloud characteristics file.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract#setCustomFilePath(String,
	 *      Files)
	 */
	public static String CLOUD_DATACENTERS_FILE = "PureEdgeSim/settings/cloud.xml";

	/**
	 * The output folder path.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract#setCustomOutputFolder(String)
	 */
	public static String OUTPUT_FOLDER = "PureEdgeSim/output/";

	/**
	 * If true simulations will be launched in parallel
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.Simulation#launchSimulation()
	 */
	public static boolean PARALLEL = false;

	/**
	 * Simualtion time in seconds.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationManager#startInternal()
	 */
	public static double SIMULATION_TIME;

	/**
	 * Pause between iterations (in seconds)
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#pause(SimLog
	 *      simLog)
	 */
	public static int PAUSE_LENGTH;

	/**
	 * Update interval (Mobility and other events) (in seconds)
	 * 
	 */
	public static double UPDATE_INTERVAL;

	/**
	 * If true, real-time charts will be displayed
	 * 
	 * @see com.mechalikh.pureedgesim.simulationvisualizer.SimulationVisualizer#updateCharts()
	 */
	public static boolean DISPLAY_REAL_TIME_CHARTS;

	/**
	 * If true, real-time charts are automatically closed when simulation finishes
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationManager#processEvent(com.mechalikh.pureedgesim.simulationengine.Event)
	 */
	public static boolean AUTO_CLOSE_REAL_TIME_CHARTS;

	/**
	 * Charts refresh interval in seconds
	 * 
	 * @see com.mechalikh.pureedgesim.simulationvisualizer.SimulationVisualizer#updateCharts()
	 */
	public static double CHARTS_UPDATE_INTERVAL;

	/**
	 * If true, charts are automatically generated at the end of the simulation and
	 * saved in bitmap format in the {@link SimulationParameters#OUTPUT_FOLDER}
	 * 
	 * @see com.mechalikh.pureedgesim.simulationvisualizer.SimulationVisualizer#updateCharts()
	 */
	public static boolean SAVE_CHARTS;

	/**
	 * The length of simulation map in meters.
	 * 
	 * @see com.mechalikh.pureedgesim.locationmanager.MobilityModel
	 * @see com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel
	 */
	public static int AREA_LENGTH;

	/**
	 * The width of simulation map in meters.
	 * 
	 * @see com.mechalikh.pureedgesim.locationmanager.MobilityModel
	 * @see com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel
	 */
	public static int AREA_WIDTH;

	/**
	 * The number of edge data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.scenariomanager.DatacentersParser#typeSpecificChecking(org.w3c.dom.Document)
	 */
	public static int NUM_OF_EDGE_DATACENTERS;

	/**
	 * The number of cloud data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.scenariomanager.DatacentersParser#typeSpecificChecking(org.w3c.dom.Document)
	 */
	public static int NUM_OF_CLOUD_DATACENTERS;

	/**
	 * The minimum number of edge devices.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.Simulation#loadScenarios()
	 */
	public static int MIN_NUM_OF_EDGE_DEVICES;

	/**
	 * The maximum number of edge devices.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.Simulation#loadScenarios()
	 */
	public static int MAX_NUM_OF_EDGE_DEVICES;

	/**
	 * The incremental step of edge devices
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.Simulation#loadScenarios()
	 */
	public static int EDGE_DEVICE_COUNTER_STEP;

	/**
	 * The types of computing nodes.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator#generateDatacentersAndDevices()
	 * @see com.mechalikh.pureedgesim.datacentersmanager.ComputingNode#setType(TYPES)
	 */
	public static enum TYPES {
		CLOUD, EDGE_DATACENTER, EDGE_DEVICE
	};

	/**
	 * Whether deep logging is enabled or not.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimLog#deepLog(String)
	 */
	public static boolean DEEP_LOGGING;

	/**
	 * Whether to save the log or not.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimLog#saveLog()
	 */
	public static boolean SAVE_LOG;

	/**
	 * If true, it delete previous logs and simulation results.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimLog#cleanOutputFolder()
	 */
	public static boolean CLEAN_OUTPUT_FOLDER;

	/**
	 * The WAN (core+data center network) bandwidth in bits per second.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setBandwidth(double)
	 */
	public static double WAN_BANDWIDTH_BITS_PER_SECOND;

	/**
	 * The WAN (core+data center network) latency in seconds.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setLatency(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWanUp
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWanDown
	 */
	public static double WAN_LATENCY;

	/**
	 * The WAN (core+data center network) energy consumption in watthour per bit.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWanUp
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWanDown
	 */
	public static double WAN_WATTHOUR_PER_BIT;

	/**
	 * If true, all data sent to /received from the cloud will be transmitted
	 * through the same WAN network (i.e. share the same bandwidth).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setBandwidth(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWanUp
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWanDown
	 */
	public static boolean ONE_SHARED_WAN_NETWORK;// If all the data transferred to the cloud will sent over the same wan
	// network.

	/**
	 * The MAN (the links between edge data centers) bandwidth in bits per second.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setBandwidth(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkMan
	 */
	public static double MAN_BANDWIDTH_BITS_PER_SECOND;

	/**
	 * The MAN (the links between edge data centers) latency in seconds.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setLatency(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkMan
	 */
	public static double MAN_LATENCY;

	/**
	 * The MAN (the links between edge data centers) energy consumption in watthour
	 * per bit.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkMan
	 */
	public static double MAN_WATTHOUR_PER_BIT;

	/**
	 * The WiFI bandwidth in bits per second.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setBandwidth(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifi
	 */
	public static double WIFI_BANDWIDTH_BITS_PER_SECOND;

	/**
	 * The energy consumed by the device when transmitting data (in watthour per bit).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifi
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifiDeviceToDevice
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifiUp
	 */
	public static double WIFI_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT;

	/**
	 * The energy consumed by the device when receiving data (in watthour per bit).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifi
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifiDeviceToDevice
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifiDown
	 */
	public static double WIFI_DEVICE_RECEPTION_WATTHOUR_PER_BIT;

	/**
	 * The energy consumed by the WiFi access point when transmitting data in watthour per bit.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifi
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifiDown
	 */
	public static double WIFI_ACCESS_POINT_TRANSMISSION_WATTHOUR_PER_BIT;

	/**
	 * The energy consumed by the WiFi access point when receiving data in watthour per bit.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifi
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifiUp
	 */
	public static double WIFI_ACCESS_POINT_RECEPTION_WATTHOUR_PER_BIT;

	/**
	 * The WiFi latency in seconds.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setLatency(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkWifi
	 */
	public static double WIFI_LATENCY;

	/**
	 * The Ethernet bandwidth in bits per second.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setBandwidth(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkEthernet
	 */
	public static double ETHERNET_BANDWIDTH_BITS_PER_SECOND;

	/**
	 * The Ethernet energy consumption in watthour per bit.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkEthernet
	 */
	public static double ETHERNET_WATTHOUR_PER_BIT;

	/**
	 * The Ethernet latency in seconds.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setLatency(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkEthernet
	 */
	public static double ETHERNET_LATENCY;

	/**
	 * The mobile communication/ cellular network (e.g. 3G, 4G, 5G) bandwidth in
	 * bits per second.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setBandwidth(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkCellular
	 */
	public static double CELLULAR_BANDWIDTH_BITS_PER_SECOND;

	/**
	 * The energy consumed by an edge device when transmitting data using a cellular connection (e.g. 3G,
	 * 4G, 5G) (in watthour per bit).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkCellularUp 
	 */
	public static double CELLULAR_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT;

	/**
	 * The energy consumed by an edge device when receiving data using a cellular connection (e.g. 3G,
	 * 4G, 5G) (in watthour per bit).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkCellularUp 
	 */
	public static double CELLULAR_DEVICE_RECEPTION_WATTHOUR_PER_BIT;

	/**
	 * The mobile base station uplink network (e.g. 3G, 4G, 5G) energy consumption
	 * (in watthour per bit).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkCellularUp
	 */
	public static double CELLULAR_BASE_STATION_WATTHOUR_PER_BIT_UP_LINK;

	/**
	 * The mobile base station downlink network (e.g. 3G, 4G, 5G) energy consumption
	 * (in watthour per bit).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink#getEnergyPerBit()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#getEnergyModel()
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkCellularDown
	 */
	public static double CELLULAR_BASE_STATION_WATTHOUR_PER_BIT_DOWN_LINK;

	/**
	 * The mobile communication/ cellular (e.g. 3G, 4G, 5G) network latency in
	 * seconds.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator#generateTopologyGraph()
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#setLatency(double)
	 * @see com.mechalikh.pureedgesim.network.NetworkLinkCellular
	 */
	public static double CELLULAR_LATENCY;

	/**
	 * The WiFi range of edge devices when using a device to device connection( in
	 * meters).
	 * 
	 * @see com.mechalikh.pureedgesim.locationmanager.MobilityModel#distanceTo(com.mechalikh.pureedgesim.datacentersmanager.ComputingNode)
	 */
	public static int EDGE_DEVICES_RANGE;

	/**
	 * The edge data centers coverage area (in meters) in which edge devices can
	 * connect with them directly (one hop).
	 * 
	 * @see com.mechalikh.pureedgesim.locationmanager.MobilityModel#distanceTo(com.mechalikh.pureedgesim.datacentersmanager.ComputingNode)
	 */
	public static int EDGE_DATACENTERS_RANGE;

	/**
	 * The network model update interval.
	 * 
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#startInternal()
	 */
	public static double NETWORK_UPDATE_INTERVAL;

	/**
	 * If true, the network model will be more realistic and gives more accurate
	 * results, but will increase simulation duration.
	 * 
	 * @see com.mechalikh.pureedgesim.network.NetworkLink#updateTransfer(TransferProgress
	 *      transfer)
	 */
	public static boolean REALISTIC_NETWORK_MODEL;

	/**
	 * If true, the tasks will be sent for another computing node (i.e. the
	 * orchestrator) in order to make offlaoding decision, before being sent to the
	 * destination (the node that actually executes the task).
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.ComputingNode#getOrchestrator()
	 * @see examples.Example7
	 */
	public static boolean ENABLE_ORCHESTRATORS;

	/**
	 * Where the orchestrator(s) are deployed, e.g. on cloud data centers, edge data
	 * centers, edge device, or custom strategy.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.ComputingNode#getOrchestrator()
	 * @see examples.Example7
	 */
	public static String DEPLOY_ORCHESTRATOR;

	/**
	 * The algorithm that will be used in the simulation to orchestrate the tasks.
	 * 
	 * @see com.mechalikh.pureedgesim.tasksorchestration.DefaultOrchestrator#findComputingNode(String[]
	 *      architecture, Task task)
	 */
	public static String[] ORCHESTRATION_AlGORITHMS;

	/**
	 * The architecture/paradigms to use in the simulation
	 * 
	 * @see com.mechalikh.pureedgesim.tasksorchestration.Orchestrator#orchestrate(Task)
	 */
	public static String[] ORCHESTRATION_ARCHITECTURES;

	/**
	 * If enable, a container will be pulled from the registry before executing the
	 * task.
	 * 
	 * @see examples.Example7
	 */
	public static boolean ENABLE_REGISTRY;

	/**
	 * Sets a custom strategy for downloading containers.
	 * 
	 * @see examples.Example7
	 */
	public static String registry_mode;

	/**
	 * The list of applications.
	 * 
	 * @see com.mechalikh.pureedgesim.scenariomanager.ApplicationFileParser
	 * @see com.mechalikh.pureedgesim.tasksgenerator.DefaultTasksGenerator#generate()
	 */
	public static List<Application> APPLICATIONS_LIST;

	/**
	 * After the end of the simulation time, some tasks may still have not executed
	 * yet, enabling this will force the simulation to wait for the execution of all tasks.
	 */
	public static boolean WAIT_FOR_TASKS;


}
