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
package com.mechalikh.pureedgesim.scenariomanager;
 
import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.tasksgenerator.Application;
public class SimulationParameters { 

	public static String EDGE_DEVICES_FILE="";     // Edge devices xml file path
	public static String EDGE_DATACENTERS_FILE=""; // Edge datacenters xml file path
	public static String CLOUD_DATACENTERS_FILE="";// Cloud data centers xml file path
	
	public static boolean STOP = false;           // used as alternative to System.exit(0); 
	public static boolean PARALLEL = false;       // Enable parallelism
	public static double SIMULATION_TIME;         // Simulation time (in seconds) 
	public static int PAUSE_LENGTH;               // Pause between scenarios (in seconds)
	public static double UPDATE_INTERVAL;         // Event interval (vm utilization update, mobility update...) (in seconds) 
	public static double INITIALIZATION_TIME;     // Time required to generate the different resources (in seconds)
	
	// Charts settings
	public static boolean DISPLAY_REAL_TIME_CHARTS;   // Show real time charts
	public static boolean AUTO_CLOSE_REAL_TIME_CHARTS;// Close real time charts when simlation finishes 
	public static double CHARTS_UPDATE_INTERVAL;	  // Charts refresh interval in seconds
	public static boolean SAVE_CHARTS;                // Save charts to bitmap format (*.png files)  
	
	// Simulation area
	public static int AREA_LENGTH;                         
	public static int AREA_WIDTH;
	
	// Edge devices, server,datacenters..
	public static int NUM_OF_EDGE_DATACENTERS;    // Number of edge data centers
	public static int NUM_OF_CLOUD_DATACENTERS;   // Number of Cloud data centers
	public static int MIN_NUM_OF_EDGE_DEVICES;    // Min number of edge devices
	public static int MAX_NUM_OF_EDGE_DEVICES;    // Max number of edge devices  
	public static int EDGE_DEVICE_COUNTER_STEP;   // Edge devices growing rate    
	public static enum TYPES {                    // Types of resources  
		CLOUD, EDGE_DATACENTER, EDGE_DEVICE
	};
	
	// Simulation logger parameters
	public static boolean DEEP_LOGGING = false;   // Deep logging (to show every detail)
	public static boolean SAVE_LOG;               // To save log file 
	public static boolean CLEAN_OUTPUT_FOLDER;    // If true, it delete previous logs and simulation results
	
	// Network parameters 
	public static int BANDWIDTH_WLAN;             // wlan bandwidth (in kbits/s)
	public static int WAN_BANDWIDTH;              // wan (cloud) bandwidth (in kbits/s) 
	public static double POWER_CONS_PER_MEGABYTE; // Power consumption by every transferred MBytes (in Wh)
	public static int EDGE_DEVICES_RANGE;         // The range of edge devices (in meters)
	public static int EDGE_DATACENTERS_RANGE;     // The range of edge servers (in meters)
	public static double NETWORK_UPDATE_INTERVAL; // Network model update interval (in seconds) 
	public static double WAN_PROPAGATION_DELAY;   // Wan propagation delay (in seconds)
	public static boolean REALISTIC_NETWORK_MODEL;// Enabling this will give more accurate results, but also will increase the simulation duration
	
	// Energy model parameters
	public static double AMPLIFIER_DISSIPATION_FREE_SPACE; // The power consumption for each transferred bit (in joul per bit :  J/bit)
	public static double CONSUMED_ENERGY_PER_BIT;          // Energy consumption of the transmit amplifier in free space channel model ( in  joul per bit per meter^2 : J/bit/m^2)
	public static double AMPLIFIER_DISSIPATION_MULTIPATH;  // Energy consumption of the transmit amplifier in multipath fading channel model ( in  joul per bit per meter^4 : J/bit/m^4)
	
	// Tasks orchestration parameters
	public static boolean ENABLE_ORCHESTRATORS;          // Whether the tasks will be sent to the orchestrator or directly to destination
	public static String[] ORCHESTRATION_AlGORITHMS;     // Tasks orchestration algorithms
	public static String[] ORCHESTRATION_ARCHITECTURES;  // The used paradigms : Cloud, Edge, Mist Computing..
	public static boolean ENABLE_REGISTRY;               // To download the container image or execute the task directly    
	public static String registry_mode;                  // Where the containers will be downloaded from
	public static List<Application> APPLICATIONS_LIST = new ArrayList<>();  // The applications characteristics
	public static String CPU_ALLOCATION_POLICY;          // CPU allocation policy : TIME_SHARED (results in long simulation time) or SPACE_SHARED 
	public static String DEPLOY_ORCHESTRATOR="";         // The location where the orchestrators are deployed (Edge devices, Cloud, Edge data centers)
	public static boolean WAIT_FOR_TASKS;                // After the end of the simulation time, some tasks may still not be executed yet,
                                                         // this variable will allow the user to wait for the execution of all tasks or to 
                                                         // end the simulation when the predifined time ends.
	
	
}
