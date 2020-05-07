package examples;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;

public class CustomOrchestrator extends Orchestrator {

	public CustomOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	protected int findVM(String[] architecture, Task task) {
		if ("TRADE_OFF".equals(algorithm)) {
			return tradeOff(architecture, task);
		} else {
			SimLog.println("");
			SimLog.println("Custom Orchestrator- Unknown orchestration algorithm '" + algorithm
					+ "', please check the simulation parameters file...");
			// Cancel the simulation
			SimulationParameters.STOP = true;
			simulationManager.getSimulation().terminate();
		}
		return -1;
	}

	private int tradeOff(String[] architecture, Task task) {
		int vm = -1;
		double min = -1;
		double new_min;// vm with minimum assigned tasks;

		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				// the weight below represent the priority, the less it is, the more it is
				// suitable for offlaoding, you can change it as you want
				double weight = 1; 
				
				if (((DataCenter) vmList.get(i).getHost().getDatacenter())
						.getType() == SimulationParameters.TYPES.CLOUD) {
					weight = 1.5; // this is the cloud, it consumes more energy and results in high latency, so
									// better to avoid it
				} 
				
				new_min = (orchestrationHistory.get(i).size() + 1) * weight * task.getLength()
						/ vmList.get(i).getMips();
				if (min == -1|| min > new_min) { 
					min = new_min;
					// set the first vm as the best one
					vm = i; 
				}  
			}
		}
		// assign the tasks to the found vm
		return vm;
	}

	@Override
	public void resultsReturned(Task task) {
	}

}
