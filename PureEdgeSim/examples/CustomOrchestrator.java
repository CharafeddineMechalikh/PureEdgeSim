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
package examples;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimLog;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;
import com.mechalikh.pureedgesim.tasksorchestration.Orchestrator;

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
					// this is the cloud, it consumes more energy and results in high latency, so
					// better to avoid it
					weight = 1.5;
				}

				new_min = (orchestrationHistory.get(i).size() + 1) * weight * task.getLength()
						/ vmList.get(i).getMips();
				if (min == -1 || min > new_min) {
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
		// You can something here. e.g. if the task has not been successfully executed,
		// etc.
	}

}
