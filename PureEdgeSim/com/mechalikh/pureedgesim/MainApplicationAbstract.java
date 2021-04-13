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
package com.mechalikh.pureedgesim;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultDataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultEnergyModel;
import com.mechalikh.pureedgesim.datacentersmanager.EnergyModel;
import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.locationmanager.Mobility;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModelAbstract;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.tasksgenerator.DefaultTasksGenerator;
import com.mechalikh.pureedgesim.tasksgenerator.TasksGenerator;
import com.mechalikh.pureedgesim.tasksorchestration.DefaultEdgeOrchestrator;
import com.mechalikh.pureedgesim.tasksorchestration.Orchestrator;

public abstract class MainApplicationAbstract {
	public static enum Files {
		SIMULATION_PARAMETERS, APPLICATIONS_FILE, EDGE_DATACENTERS_FILE, EDGE_DEVICES_FILE, CLOUD_FILE
	}

	// Simulation scenario files
	protected static String simConfigfile = "PureEdgeSim/settings/simulation_parameters.properties";
	protected static String applicationsFile = "PureEdgeSim/settings/applications.xml";
	protected static String edgeDataCentersFile = "PureEdgeSim/settings/edge_datacenters.xml";
	protected static String edgeDevicesFile = "PureEdgeSim/settings/edge_devices.xml";
	protected static String cloudFile = "PureEdgeSim/settings/cloud.xml";
	protected static String outputFolder = "PureEdgeSim/output/";

	// Parallel simulation Parameters
	protected int fromIteration;
	protected int step = 1;
	protected static int cpuCores;
	protected static List<Scenario> Iterations = new ArrayList<>();
	protected static Class<? extends Mobility> mobilityManager = DefaultMobilityModel.class;
	protected static Class<? extends DataCenter> edgedatacenter = DefaultDataCenter.class;
	protected static Class<? extends TasksGenerator> tasksGenerator = DefaultTasksGenerator.class;
	protected static Class<? extends Orchestrator> orchestrator = DefaultEdgeOrchestrator.class;
	protected static Class<? extends EnergyModel> energyModel = DefaultEnergyModel.class;
	protected static Class<? extends NetworkModelAbstract> networkModel = NetworkModel.class;

	public static String getOutputFolder() {
		return outputFolder;
	}

	protected static void setCustomEdgeDataCenters(Class<? extends DataCenter> edgedatacenter2) {
		edgedatacenter = edgedatacenter2;
	}

	protected static void setCustomTasksGenerator(Class<? extends TasksGenerator> tasksGenerator2) {
		tasksGenerator = tasksGenerator2;
	}

	protected static void setCustomEdgeOrchestrator(Class<? extends Orchestrator> orchestrator2) {
		orchestrator = orchestrator2;
	}

	protected static void setCustomMobilityModel(Class<? extends Mobility> mobilityManager2) {
		mobilityManager = mobilityManager2;
	}

	protected static void setCustomEnergyModel(Class<? extends EnergyModel> energyModel2) {
		energyModel = energyModel2;
	}

	protected static void setCustomNetworkModel(Class<? extends NetworkModelAbstract> networkModel2) {
		networkModel = networkModel2;
	}

	protected static void setCustomOutputFolder(String outputFolder2) {
		outputFolder = outputFolder2;
	}

	protected static void setCustomSettingsFolder(String settingsFolder) {
		setCustomFilePath(settingsFolder + "simulation_parameters.properties", Files.SIMULATION_PARAMETERS);
		setCustomFilePath(settingsFolder + "applications.xml", Files.APPLICATIONS_FILE);
		setCustomFilePath(settingsFolder + "edge_datacenters.xml", Files.EDGE_DATACENTERS_FILE);
		setCustomFilePath(settingsFolder + "edge_devices.xml", Files.EDGE_DEVICES_FILE);
		setCustomFilePath(settingsFolder + "cloud.xml", Files.CLOUD_FILE);
	}

	protected static void setCustomFilePath(String path, Files file) {
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
