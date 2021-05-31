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
package com.mechalikh.pureedgesim.tasksgenerator;

import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;

public class DefaultTasksGenerator extends TasksGenerator {

	private double simulationTime;
	
	public DefaultTasksGenerator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	public List<Task> generate() {
		// get simulation time in minutes (excluding the initialization time)
		simulationTime = (SimulationParameters.SIMULATION_TIME - SimulationParameters.INITIALIZATION_TIME) / 60;

		// Remove devices that do not generate
		int dev = 0;
		while (dev < datacentersList.size()) {
			if (!datacentersList.get(dev).isGeneratingTasks()) {
				datacentersList.remove(dev);
			} else
				dev++;
		}
		int devices_count = datacentersList.size();
		// Browse all applications
		for (int app = 0; app < SimulationParameters.APPLICATIONS_LIST.size() - 1; app++) {
			// get the number of devices that use the current application
			int numberOfDevices = (int) SimulationParameters.APPLICATIONS_LIST.get(app).getUsagePercentage()
					* devices_count / 100;

			for (int i = 0; i < numberOfDevices; i++) {
				// Pickup a random application type for every device
				dev = new Random().nextInt(datacentersList.size());

				// Assign this application to that device
				datacentersList.get(dev).setApplicationType(app);

				generateTasksForDevice(datacentersList.get(dev), app);

				// Remove this device from the list
				datacentersList.remove(dev);
			}
		}
		for (int j = 0; j < datacentersList.size(); j++)
			generateTasksForDevice(datacentersList.get(j), SimulationParameters.APPLICATIONS_LIST.size() - 1);

		return this.getTaskList();
	}

	private void generateTasksForDevice(DataCenter dev, int app) {
		// Generating tasks that will be offloaded during simulation
		for (int st = 0; st < simulationTime; st++) { // for each minute

			// First get time in seconds
			int time = st * 60;

			// Then pick up random second in this minute "st". Shift the time by the defined
			// value "INITIALIZATION_TIME" in order to start after generating all the
			// resources
			time += new Random().nextInt(15) + SimulationParameters.INITIALIZATION_TIME;
			insert(time, app, dev);
		}
	}

	private void insert(int time, int app, DataCenter dev) {
		// Get the task latency sensitivity (seconds)
		double maxLatency = SimulationParameters.APPLICATIONS_LIST.get(app).getLatency();

		// Get the task length (MI: million instructions)
		long length = (long) SimulationParameters.APPLICATIONS_LIST.get(app).getTaskLength();

		// Get the offloading request size (KB)
		long requestSize = SimulationParameters.APPLICATIONS_LIST.get(app).getRequestSize();

		// Get the size of the returned results (KB)
		long outputSize = SimulationParameters.APPLICATIONS_LIST.get(app).getResultsSize();

		// Get the number of required CPU cores
		int pesNumber = SimulationParameters.APPLICATIONS_LIST.get(app).getNumberOfCores();

		// The size of the container (KB)
		long containerSize = SimulationParameters.APPLICATIONS_LIST.get(app).getContainerSize();

		Task[] task = new Task[SimulationParameters.APPLICATIONS_LIST.get(app).getRate()];
		int id;

		// generate tasks for every edge device
		for (int i = 0; i < SimulationParameters.APPLICATIONS_LIST.get(app).getRate(); i++) {
			id = taskList.size();
			UtilizationModel utilizationModeldynamic = new UtilizationModelDynamic();
			task[i] = new Task(id, length, pesNumber);
			task[i].setFileSize(requestSize).setOutputSize(outputSize).setUtilizationModelBw(utilizationModeldynamic)
					.setUtilizationModelRam(utilizationModeldynamic).setUtilizationModelCpu(new UtilizationModelFull());
			time += 60 / SimulationParameters.APPLICATIONS_LIST.get(app).getRate();
			task[i].setTime(time);
			task[i].setContainerSize(containerSize);
			task[i].setApplicationID(app);
			task[i].setMaxLatency(maxLatency);
			task[i].setEdgeDevice(dev); // the device that generate this task (the origin) 
			task[i].setRegistry(this.getSimulationManager().getDataCentersManager().getDatacenterList().get(0)); // set the cloud as registry
			taskList.add(task[i]);
			getSimulationManager().getSimulationLogger()
					.deepLog("BasicTasksGenerator, Task " + id + " with execution time " + time + " (s) generated.");
		}
	}

}
