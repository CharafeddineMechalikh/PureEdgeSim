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
package com.mechalikh.pureedgesim.TasksGenerator;

import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;

public class DefaultTasksGenerator extends TasksGenerator {
	public DefaultTasksGenerator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	public List<Task> generate() {
		// get simulation time in minutes (excluding the initialization time)
		double simulationTime = (SimulationParameters.SIMULATION_TIME - SimulationParameters.INITIALIZATION_TIME) / 60;
		for (int dev = 0; dev < datacentersList.size(); dev++) { // for each device
			if (datacentersList.get(dev).isGeneratingTasks()) {
				int app = new Random().nextInt(SimulationParameters.APPLICATIONS_LIST.size()); // pickup a random
																								// application type for
																								// every device
				datacentersList.get(dev).setApplication(app); // assign this application to that device
				for (int st = 0; st < simulationTime; st++) { // for each minute
					// generating tasks
					// first get time in seconds
					int time = st * 60;

					// Then pick up random second in this minute "st"
					// Shift the time by the defined value "INITIALIZATION_TIME"
					// in order to start after generating all the resources
					time += new Random().nextInt(15) + SimulationParameters.INITIALIZATION_TIME;
					insert(time, app, dev);
				}
			}
		}
		return this.getTaskList();
	}

	private void insert(int time, int app, int dev) {
		int time2 = time;
		double maxLatency = SimulationParameters.APPLICATIONS_LIST.get(app).getLatency(); // Load length from
																							// application file
		long length = (long) SimulationParameters.APPLICATIONS_LIST.get(app).getTaskLength(); // Load length from
																								// application file
		long requestSize = SimulationParameters.APPLICATIONS_LIST.get(app).getRequestSize();
		long outputSize = SimulationParameters.APPLICATIONS_LIST.get(app).getResultsSize();
		int pesNumber = SimulationParameters.APPLICATIONS_LIST.get(app).getNumberOfCores();
		long containerSize = SimulationParameters.APPLICATIONS_LIST.get(app).getContainerSize(); // the size of the
																									// container
		Task[] task = new Task[SimulationParameters.APPLICATIONS_LIST.get(app).getRate()];
		int id;

		// generate tasks for every edge device
		for (int i = 0; i < SimulationParameters.APPLICATIONS_LIST.get(app).getRate(); i++) {
			id = taskList.size();
			UtilizationModel utilizationModeldynamic = new UtilizationModelDynamic();
			task[i] = new Task(id, length, pesNumber);
			task[i].setFileSize(requestSize).setOutputSize(outputSize).setUtilizationModelBw(utilizationModeldynamic)
					.setUtilizationModelRam(utilizationModeldynamic).setUtilizationModelCpu(new UtilizationModelFull());
			time2 += 60 / SimulationParameters.APPLICATIONS_LIST.get(app).getRate();
			task[i].setTime(time2);
			task[i].setContainerSize(containerSize);
			task[i].setMaxLatency(maxLatency);
			task[i].setEdgeDevice(datacentersList.get(dev)); // the device that generate this task (the origin)
			task[i].setRegistry(datacentersList.get(0)); // set the cloud as registry
			taskList.add(task[i]);
			getSimulationManager().getSimulationLogger()
					.deepLog("BasicTasksGenerator, Task " + id + " with execution time " + time2 + " (s) generated.");
		}
	}

}
