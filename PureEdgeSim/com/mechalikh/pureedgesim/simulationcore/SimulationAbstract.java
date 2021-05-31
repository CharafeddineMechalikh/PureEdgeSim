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
package com.mechalikh.pureedgesim.simulationcore;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultDataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultEnergyModel;
import com.mechalikh.pureedgesim.datacentersmanager.EnergyModel;
import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.tasksgenerator.DefaultTasksGenerator;
import com.mechalikh.pureedgesim.tasksgenerator.TasksGenerator;
import com.mechalikh.pureedgesim.tasksorchestration.DefaultOrchestrator;
import com.mechalikh.pureedgesim.tasksorchestration.Orchestrator;

/**
 * An abstract class that represents the main application used to launch
 * simulation.
 * 
 * @author Charafeddine Mechalikh
 */

public abstract class SimulationAbstract {
	
	
	public  enum Files {
		SIMULATION_PARAMETERS, APPLICATIONS_FILE, EDGE_DATACENTERS_FILE, EDGE_DEVICES_FILE, CLOUD_FILE
	}

	// Simulation scenario files
	/**
	 * @see #setCustomFilePath(String, Files)
	 */
	protected  String simConfigfile = "PureEdgeSim/settings/simulation_parameters.properties";
	
	/**
	 * @see #setCustomFilePath(String, Files)
	 */
	protected  String applicationsFile = "PureEdgeSim/settings/applications.xml";
	
	/**
	 * @see #setCustomFilePath(String, Files)
	 */
	protected  String edgeDataCentersFile = "PureEdgeSim/settings/edge_datacenters.xml";
	
	/**
	 * @see #setCustomFilePath(String, Files)
	 */
	protected  String edgeDevicesFile = "PureEdgeSim/settings/edge_devices.xml";
	
	/**
	 * @see #setCustomFilePath(String, Files)
	 */
	protected  String cloudFile = "PureEdgeSim/settings/cloud.xml";
	
	/**
	 * @see #setCustomOutputFolder(String)
	 */
	protected static  String outputFolder = "PureEdgeSim/output/";
	
	/**
	 * @see #setCustomMobilityModel(Class)
	 */
	protected  Class<? extends MobilityModel> mobilityModel = DefaultMobilityModel.class;
	
	/**
	 * @see #setCustomEdgeDataCenters(Class)
	 */
	protected  Class<? extends DataCenter> edgedatacenter = DefaultDataCenter.class;
	
	/**
	 * @see #setCustomTasksGenerator(Class)
	 */
	protected  Class<? extends TasksGenerator> tasksGenerator = DefaultTasksGenerator.class;
	
	/**
	 * @see #setCustomEdgeOrchestrator(Class)
	 */
	protected  Class<? extends Orchestrator> orchestrator = DefaultOrchestrator.class;
	
	/**
	 * @see #setCustomEnergyModel(Class)
	 */
	protected  Class<? extends EnergyModel> energyModel = DefaultEnergyModel.class;
	
	/**
	 * @see #setCustomNetworkModel(Class)
	 */
	protected  Class<? extends NetworkModel> networkModel = DefaultNetworkModel.class;

	/**
	 * Returns the path where the simulation results are saved.
	 * 
	 * @return outputFolder the path where simulation results are saved
	 */
	public static String getOutputFolder() {
		return outputFolder;
	}

	/**
	 * Allows to use a custom data center class in the simulation.
	 * The class must extend the {@link DataCenter}  provided by PureEdgeSim.
	 * 
	 * @param edgedatacenter the custom data center class to use
	 */
	public void setCustomEdgeDataCenters(Class<? extends DataCenter> edgedatacenter) {
		this.edgedatacenter = edgedatacenter;
	}

	/**
	 * Allows to use a custom tasks generator class in the simulation.
	 * The class must extend the {@link TasksGenerator} provided by PureEdgeSim.
	 * 
	 * @param tasksGenerator the custom task generator class to use
	 */
	public void setCustomTasksGenerator(Class<? extends TasksGenerator> tasksGenerator) {
		this.tasksGenerator = tasksGenerator;
	}

	/**
	 * Allows to use a custom orchestrator class in the simulation.
	 * The class must extend the {@link Orchestrator} provided by PureEdgeSim.
	 * 
	 * @param orchestrator the custom orchestrator class to use
	 */
	public void setCustomEdgeOrchestrator(Class<? extends Orchestrator> orchestrator) {
		this.orchestrator = orchestrator;
	}

	/**
	 * Allows to use a custom mobility model in the simulation.
	 * The class must extend the {@link MobilityModel}  provided in PureEdgeSim.
	 * 
	 * @param mobilityModel the custom mobility model class to use
	 */
	public void setCustomMobilityModel(Class<? extends MobilityModel> mobilityModel) {
		this.mobilityModel = mobilityModel;
	}

	/**
	 * Allows to use a custom tasks generator class in the simulation
	 * The class must extend the {@link TasksGenerator}  provided by PureEdgeSim
	 * 
	 * @param energyModel the custom energy model class to use
	 */
	public void setCustomEnergyModel(Class<? extends EnergyModel> energyModel) {
		this.energyModel = energyModel;
	}

	/**
	 * Allows to use a network model in the simulation.
	 * The class must extend the {@link NetworkModel}  provided by PureEdgeSim.
	 * 
	 * @param networkModel the custom network model class to use
	 */
	public void setCustomNetworkModel(Class<? extends NetworkModel> networkModel) {
		this.networkModel = networkModel;
	}

	/**
	 * Allows to change the output folder, in which the simulation results will be saved.
	 * 
	 * @param outputFolder the output folder to set
	 */
	public void setCustomOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	/**
	 * Allows to change the location from which the configuration files are loaded.
	 * 
	 * @param settingsFolder the new settings folder to use
	 */
	public void setCustomSettingsFolder(String settingsFolder) {
		setCustomFilePath(settingsFolder + "simulation_parameters.properties", Files.SIMULATION_PARAMETERS);
		setCustomFilePath(settingsFolder + "applications.xml", Files.APPLICATIONS_FILE);
		setCustomFilePath(settingsFolder + "edge_datacenters.xml", Files.EDGE_DATACENTERS_FILE);
		setCustomFilePath(settingsFolder + "edge_devices.xml", Files.EDGE_DEVICES_FILE);
		setCustomFilePath(settingsFolder + "cloud.xml", Files.CLOUD_FILE);
	}

	/**
	 * Allows to set a custom path of each configuration  file individually.
	 * 
	 * @param file the nature of the file to load
	 * @param path the path from which the file will be loaded
	 */
	public void setCustomFilePath(String path, Files file) {
		switch (file) {
		case SIMULATION_PARAMETERS:
			simConfigfile = path;
			break;
		case APPLICATIONS_FILE:
			applicationsFile = path;
			break;
		case EDGE_DATACENTERS_FILE:
			edgeDataCentersFile = path;
			break;
		case EDGE_DEVICES_FILE:
			edgeDevicesFile = path;
			break;
		case CLOUD_FILE:
			cloudFile = path;
			break;
		default:
			SimLog.println("Unknown file type");
			break;
		}
	}

}
