package com.Mechalikh.PureEdgeSim.TasksOrchestration;

import java.util.List;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeVM;
import com.Mechalikh.PureEdgeSim.ScenarioManager.Scenario;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public abstract class Orchestrator {
	protected List<EdgeVM> vmList;
	protected DatacenterBroker broker;
	protected Scenario scenario;

	public Orchestrator(DatacenterBroker broker2, List<EdgeVM> list, Scenario scenario) {
		this.broker = broker2;
		this.vmList = list;
		this.scenario=scenario;
	}
 

	public  void initialize(Task task) {
		sendTask(task);
	}


	public abstract void sendTask(Task task); 
 
}
