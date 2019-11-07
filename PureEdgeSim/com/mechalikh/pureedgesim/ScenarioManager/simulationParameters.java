package com.mechalikh.pureedgesim.ScenarioManager;
 

public class simulationParameters { 

	public static String EDGE_DEVICES_FILE="";     // Edge devices xml file path
	public static String FOG_SERVERS_FILE="";      // Fog servers xml file path
	public static String CLOUD_DATACENTERS_FILE="";// Cloud datacenters xml file path

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
	public static int NUM_OF_FOG_DATACENTERS;     // Number of Fog data centers
	public static int NUM_OF_CLOUD_DATACENTERS;   // Number of Cloud data centers
	public static int MIN_NUM_OF_EDGE_DEVICES;    // Min number of edge devices
	public static int MAX_NUM_OF_EDGE_DEVICES;    // Max number of edge devices  
	public static int EDGE_DEVICE_COUNTER_STEP;   // Edge devices growing rate   
	public static double SPEED;                   // Speed of mobile devices in meters per seconds m/s
	public static enum TYPES {                    // Types of resources  
		CLOUD, FOG, EDGE
	};
	
	// Simulation logger parameters
	public static boolean DEEP_LOGGING = false;   // Deep logging (to show every detail)
	public static boolean SAVE_LOG;               // To save log file 
	public static boolean CLEAN_OUTPUT_FOLDER;    // If true, it delete previous logs and simulation results
	
	// Network parameters 
	public static int BANDWIDTH_WLAN;             // wlan bandwidth (in kbits/s)
	public static int WAN_BANDWIDTH;              // wan (cloud) bandwidth (in kbits/s) 
	public static double POWER_CONS_PER_MEGABYTE; // Power consumption by every transferred MBytes (in Wh)
	public static int EDGE_RANGE;                 // The range of edge devices (in meters)
	public static int FOG_RANGE;                  // The range of fog servers (in meters)
	public static double NETWORK_UPDATE_INTERVAL; // Network model update interval (in seconds) 
	public static double WAN_PROPAGATION_DELAY;   // Wan propagation delay (in seconds)
	
	// Energy model parameters
	public static double AMPLIFIER_DISSIPATION_FREE_SPACE; // The power consumption for each transferred bit (in joul per bit :  J/bit)
	public static double CONSUMED_ENERGY_PER_BIT;          // Energy consumption of the transmit amplifier in free space channel model ( in  joul per bit per meter^2 : J/bit/m^2)
	public static double AMPLIFIER_DISSIPATION_MULTIPATH;  // Energy consumption of the transmit amplifier in multipath fading channel model ( in  joul per bit per meter^4 : J/bit/m^4)
	
	// Tasks orchestration parameters
	public static boolean ENABLE_ORCHESTRATORS;           // Whether the tasks will be sent to the orchestrator or directly to destination
	public static int TASKS_PER_EDGE_DEVICE_PER_MINUTES; // Tasks generation rate
	public static String[] ORCHESTRATION_AlGORITHMS;     // Tasks orchestration algorithms
	public static String[] ORCHESTRATION_ARCHITECTURES;  // The used paradigms : Cloud, Edge, Fog..
	public static boolean ENABLE_REGISTRY;               // To download the container image or execute the task directly    
	public static int APPS_COUNT;                        // The number of the applications specified by the user in the Application.xml file
	public static double[][] APPLICATIONS_TABLE;         // The applications characteristics
	public static String CPU_ALLOCATION_POLICY;          // CPU allocation policy : TIME_SHARED (results in long simulation time) or SPACE_SHARED 
	public static String DEPLOY_ORCHESTRATOR="";         // The location where the orchestrators are deployed (Edge devices, Cloud, Fog servers)
	public static boolean WAIT_FOR_TASKS;                // After the end of the simulation time, some tasks may still not be executed yet,
                                                         // this variable will allow the user to wait for the execution of all tasks or to 
                                                         // end the simulation when the predifined time ends. 



 
	

}
