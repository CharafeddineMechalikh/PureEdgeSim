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
package com.mechalikh.pureedgesim.simulationmanager;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;  
import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.tasksgenerator.DefaultTasksGenerator;
import com.mechalikh.pureedgesim.tasksgenerator.TasksGenerator;
import com.mechalikh.pureedgesim.tasksorchestration.DefaultOrchestrator;
import com.mechalikh.pureedgesim.tasksorchestration.Orchestrator;

/**
 * An abstract class that represents the simulation environment
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public abstract class SimulationAbstract {

	public enum Files {
		SIMULATION_PARAMETERS, APPLICATIONS_FILE, EDGE_DATACENTERS_FILE, EDGE_DEVICES_FILE, CLOUD_FILE
	}

	/**
	 * The Mobility Model class that is used in the simulation.
	 * 
	 * @see #setCustomMobilityModel(Class)
	 */
	protected Class<? extends MobilityModel> mobilityModel = DefaultMobilityModel.class;

	/**
	 * The Computing Node class that is used in the simulation.
	 * 
	 * @see #setCustomEdgeDataCenters(Class)
	 */
	protected Class<? extends ComputingNode> computingNode = DefaultComputingNode.class;

	/**
	 * The Tasks Generator class that is used in the simulation.
	 * 
	 * @see #setCustomTasksGenerator(Class)
	 */
	protected Class<? extends TasksGenerator> tasksGenerator = DefaultTasksGenerator.class;

	/**
	 * The Orchestrator class that is used in the simulation.
	 * 
	 * @see #setCustomEdgeOrchestrator(Class)
	 */
	protected Class<? extends Orchestrator> orchestrator = DefaultOrchestrator.class;

	/**
	 * The Network Model class that is used in the simulation.
	 * 
	 * @see #setCustomNetworkModel(Class)
	 */
	protected Class<? extends NetworkModel> networkModel = DefaultNetworkModel.class;


	/**
	 * Allows to use a custom computing node class in the simulation. The class must
	 * extend the {@link ComputingNode} provided by PureEdgeSim.
	 * 
	 * @param customComputingNode the custom class to use.
	 */
	public void setCustomEdgeDataCenters(Class<? extends ComputingNode> customComputingNode) {
		this.computingNode = customComputingNode;
	}

	/**
	 * Allows to use a custom tasks generator class in the simulation. The class
	 * must extend the {@link TasksGenerator} provided by PureEdgeSim.
	 * 
	 * @param tasksGenerator the custom task generator class to use.
	 */
	public void setCustomTasksGenerator(Class<? extends TasksGenerator> tasksGenerator) {
		this.tasksGenerator = tasksGenerator;
	}

	/**
	 * Allows to use a custom orchestrator class in the simulation. The class must
	 * extend the {@link Orchestrator} provided by PureEdgeSim.
	 * 
	 * @param orchestrator the custom orchestrator class to use.
	 */
	public void setCustomEdgeOrchestrator(Class<? extends Orchestrator> orchestrator) {
		this.orchestrator = orchestrator;
	}

	/**
	 * Allows to use a custom mobility model in the simulation. The class must
	 * extend the {@link MobilityModel} provided in PureEdgeSim.
	 * 
	 * @param mobilityModel the custom mobility model class to use.
	 */
	public void setCustomMobilityModel(Class<? extends MobilityModel> mobilityModel) {
		this.mobilityModel = mobilityModel;
	}

	/**
	 * Allows to use a network model in the simulation. The class must extend the
	 * {@link NetworkModel} provided by PureEdgeSim.
	 * 
	 * @param networkModel the custom network model class to use.
	 */
	public void setCustomNetworkModel(Class<? extends NetworkModel> networkModel) {
		this.networkModel = networkModel;
	}

	/**
	 * Allows to change the output folder, in which the simulation results will be
	 * saved.
	 * 
	 * @param path the output folder to set.
	 */
	public void setCustomOutputFolder(String path) {
		SimulationParameters.OUTPUT_FOLDER = path;
	}

	/**
	 * Allows to change the location from which the configuration files are loaded.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setCustomSettingsFolder(String settingsFolder) {
		setCustomFilePath(settingsFolder + "simulation_parameters.properties", Files.SIMULATION_PARAMETERS);
		setCustomFilePath(settingsFolder + "applications.xml", Files.APPLICATIONS_FILE);
		setCustomFilePath(settingsFolder + "edge_datacenters.xml", Files.EDGE_DATACENTERS_FILE);
		setCustomFilePath(settingsFolder + "edge_devices.xml", Files.EDGE_DEVICES_FILE);
		setCustomFilePath(settingsFolder + "cloud.xml", Files.CLOUD_FILE);
	}

	/**
	 * Allows to set a custom path of each configuration file individually.
	 * 
	 * @param path the path from which the file will be loaded
	 * @param file the nature of the file to load
	 */
	public void setCustomFilePath(String path, Files file) {
		switch (file) {
		case SIMULATION_PARAMETERS:
			SimulationParameters.SIMULATION_PARAMETERS_FILE = path;
			break;
		case APPLICATIONS_FILE:
			SimulationParameters.APPLICATIONS_FILE = path;
			break;
		case EDGE_DATACENTERS_FILE:
			SimulationParameters.EDGE_DATACENTERS_FILE = path;
			break;
		case EDGE_DEVICES_FILE:
			SimulationParameters.EDGE_DEVICES_FILE = path;
			break;
		case CLOUD_FILE:
			SimulationParameters.CLOUD_DATACENTERS_FILE = path;
			break;
		default:
			throw new IllegalArgumentException(getClass().getSimpleName()+ " - Unknown file type");
		}
	}

}
