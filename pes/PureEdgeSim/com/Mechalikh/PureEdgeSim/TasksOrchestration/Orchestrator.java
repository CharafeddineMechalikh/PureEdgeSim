package com.Mechalikh.PureEdgeSim.TasksOrchestration;
 
import com.Mechalikh.PureEdgeSim.SimulationManager.SimulationManager;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;

public abstract class Orchestrator { 
	private SimulationManager simulationManager;

	public Orchestrator(SimulationManager simulationManager) {
		this.setSimulationManager(simulationManager);
	}
 

	public  void initialize(Task task) {
		sendTask(task);
	}


	public abstract void sendTask(Task task);


	public SimulationManager getSimulationManager() {
		return simulationManager;
	}


	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	} 
 
}
