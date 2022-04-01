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
package com.mechalikh.pureedgesim.tasksgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public abstract class TasksGenerator {
	protected LinkedList<Task> taskList;
	protected List<? extends ComputingNode> devicesList;
	private SimulationManager simulationManager;

	public TasksGenerator(SimulationManager simulationManager) {
		taskList = new LinkedList<>();
		setSimulationManager(simulationManager);
		devicesList = new ArrayList<>(this.getSimulationManager().getDataCentersManager().getEdgeDevicesList());
	}

	public List<Task> getTaskList() {
		return taskList;
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	public abstract List<Task> generate();
}
