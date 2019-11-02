package com.mechalikh.pureedgesim.TasksOrchestration;
 
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;

public abstract class Orchestrator { 
	protected SimulationManager simulationManager;

	public Orchestrator(SimulationManager simulationManager) {
		this.simulationManager=simulationManager;
	}
 

	public  void initialize(Task task) {
		sendTask(task);
	}
 
	public abstract void sendTask(Task task);

  
 
}
