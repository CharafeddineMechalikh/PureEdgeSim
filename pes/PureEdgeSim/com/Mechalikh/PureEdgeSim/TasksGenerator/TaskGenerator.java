package com.Mechalikh.PureEdgeSim.TasksGenerator;

import java.util.ArrayList;
import java.util.List;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimulationManager;
 

public abstract class TaskGenerator {
	protected List<Task> taskList; 
	protected List<EdgeDataCenter> datacentersList;
	private SimulationManager simulationManager;
	public TaskGenerator(SimulationManager simulationManager) {
		taskList = new ArrayList<Task>();
        this.setSimulationManager(simulationManager);  
		this.datacentersList = this.getSimulationManager().getServersManager().getDatacenterList();
	}  

	public List<Task> getTaskList() { 
		return taskList;
	}

	public void generate() {  
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}
}
