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
package com.mechalikh.pureedgesim.taskgenerator;

import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom; 
import java.util.Random;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters; 
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class DefaultTaskGenerator extends TaskGenerator {
	/**
	 * Used to generate random values.
	 * 
	 * @see #generate()
	 * @see #generateTasksForDevice(ComputingNode, int)
	 */
	protected Random random;
	protected int id = 0;
	protected double simulationTime;

	public DefaultTaskGenerator(SimulationManager simulationManager) {
		super(simulationManager);
		try {
			random = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public FutureQueue<Task> generate() {
		// Get simulation time in minutes (excluding the initialization time)
		simulationTime = SimulationParameters.simulationDuration / 60;

		// Remove devices that do not generate
		int dev = 0;
		while (dev < devicesList.size()) {
			if (!devicesList.get(dev).isGeneratingTasks()) {
				devicesList.remove(dev);
			} else
				dev++;
		}
		int devicesCount = devicesList.size();

		// Browse all applications
		for (int app = 0; app < SimulationParameters.applicationList.size() - 1; app++) {
			// Get the number of devices that use the current application
			int numberOfDevices = (int) SimulationParameters.applicationList.get(app).getUsagePercentage()
					* devicesCount / 100;

			for (int i = 0; i < numberOfDevices; i++) {
				// Pickup a random application type for every device
				dev = random.nextInt(devicesList.size());

				// Assign this application to that device
				devicesList.get(dev).setApplicationType(app);

				generateTasksForDevice(devicesList.get(dev), app);

				// Remove this device from the list
				devicesList.remove(dev);
			}
		}
		for (int j = 0; j < devicesList.size(); j++)
			generateTasksForDevice(devicesList.get(j), SimulationParameters.applicationList.size() - 1);

		return this.getTaskList();
	}

	protected void generateTasksForDevice(ComputingNode dev, int app) {
		// Generating tasks that will be offloaded during simulation
		for (int st = 0; st < simulationTime; st++) { // for each minute

			// First get time in seconds
			int time = st * 60;

			// Then pick up random second in this minute "st". Shift the time by the defined
			// value "INITIALIZATION_TIME" in order to start after generating all the
			// resources
			time += random.nextInt(15);
			insert(time, app, dev);
		}
	}

	protected void insert(int time, int app, ComputingNode dev) {
		// Get the task latency sensitivity (seconds)
		double maxLatency = SimulationParameters.applicationList.get(app).getLatency();

		// Get the task length (MI: million instructions)
		long length = (long) SimulationParameters.applicationList.get(app).getTaskLength();

		// Get the offloading request size in bits
		long requestSize = SimulationParameters.applicationList.get(app).getRequestSize();

		// Get the size of the returned results in bits
		long outputSize = SimulationParameters.applicationList.get(app).getResultsSize();

		// The size of the container in bits
		long containerSize = SimulationParameters.applicationList.get(app).getContainerSizeInBits();

		Task[] task = new Task[SimulationParameters.applicationList.get(app).getRate()];

		// Generate tasks for every edge device
		for (int i = 0; i < SimulationParameters.applicationList.get(app).getRate(); i++) {
			id++;
			task[i] = createTask(id);
			task[i].setType(SimulationParameters.applicationList.get(app).getType());
			task[i].setFileSizeInBits(requestSize).setOutputSizeInBits(outputSize);
			time += 60 / SimulationParameters.applicationList.get(app).getRate();
			task[i].setTime(time);
			task[i].setContainerSizeInBits(containerSize);
			task[i].setApplicationID(app);
			task[i].setMaxLatency(maxLatency);
			task[i].setLength(length);
			task[i].setEdgeDevice(dev); // the device that generate this task (the origin)

			// Set the cloud as registry
			task[i].setRegistry(getSimulationManager().getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList().get(0));
			task[i].setId(id);
			taskList.add(task[i]);
			getSimulationManager().getSimulationLogger()
					.deepLog("BasicTasksGenerator, Task " + id + " with execution time " + time + " (s) generated.");
		}
	}

	protected Task createTask(int id) {
		Constructor<?> taskConstructor;
		Task task = null;
		try {
			taskConstructor = taskClass.getConstructor(int.class);
			task = (Task) taskConstructor.newInstance(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return task;
	}


}
