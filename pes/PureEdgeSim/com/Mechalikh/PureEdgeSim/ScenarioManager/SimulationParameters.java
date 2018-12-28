package com.Mechalikh.PureEdgeSim.ScenarioManager;

import org.w3c.dom.Document;

public class SimulationParameters {
	public static boolean PARALLEL = false;
	public static double SIMULATION_TIME;//seconds
	public static double INTERVAL_TO_SEND_EVENT;
	public static Document fogDevicesDoc = null;
	public static Document edgeDevicesDoc = null;
	public static Document cloudDevicesDoc = null;
	public static int NUM_OF_FOG_HOSTS;
	public static int NUM_OF_FOG_DATACENTERS;
	public static int NUM_OF_CLOUD_DATACENTERS;
	public static boolean DEEP_LOGGING = false;
	public static int MIN_NUM_OF_EDGE_DEVICES;
	public static int MAX_NUM_OF_EDGE_DEVICES;
	public static int EDGE_DEVICE_COUNTER_STEP;
	public static int BANDWIDTH_WLAN;//kbits/s
	public static int PAUSE_LENGTH; // seconds
	public static int WAN_BANDWIDTH;//kbits/s
	public static int TASKS_PER_EDGE_DEVICE_PER_MINUTES;
	public static String[] ORCHESTRATOR_CRITERIA;
	public static String[] ORCHESTRATOR_POLICIES;
	public static String[] APPLICATIONS = { "AUGMENTED_REALITY", "E_HEALTH", "HEAVY_COMP_APP", "SMART_HOME" };
	public static double[][] APPLICATIONS_TABLE = new double[APPLICATIONS.length][5];
	public static boolean SAVE_LOG;
	public static boolean CLEAN_OUTPUT_FOLDER;
	public static int MIN_TIME_FOR_CHANGING_LOCATION;
	public static int MAX_TIME_FOR_CHANGING_LOCATION;
	public static boolean VM_MIGRATION;
	public static double POWER_CONS_PER_MEGABYTE; 
	public static boolean WAIT_FOR_TASKS;
	public static double WAN_PROPAGATION_DELAY;  
	public static String CPU_ALLOCATION_POLICY; 
 
	public static enum TYPES {
		CLOUD, FOG, EDGE
	};

}
