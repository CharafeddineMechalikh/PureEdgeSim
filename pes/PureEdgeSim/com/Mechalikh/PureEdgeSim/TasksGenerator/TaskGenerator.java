package com.Mechalikh.PureEdgeSim.TasksGenerator;

import java.util.ArrayList;
import java.util.List;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
 

public abstract class TaskGenerator {
	protected List<Task> taskList; 
	protected List<EdgeDataCenter> datacentersList;
	public TaskGenerator() {
		taskList = new ArrayList<Task>();

	}  

	public List<Task> getTaskList() { 
		return taskList;
	}

	public void generate(double simulationTime, int deviceNumber, int tasksPerDevicePerMinute, int fogDatacentersCount,
			List<EdgeDataCenter> list) {  
		this.datacentersList = list;
	}
}
