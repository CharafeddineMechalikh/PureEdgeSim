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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.IntStream;

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

	/**
	 * Generates a queue of tasks based on the simulation parameters.
	 *
	 * @return a queue of tasks
	 */
	public FutureQueue<Task> generate() {
		// Get simulation time in minutes (excluding the initialization time)
		simulationTime = SimulationParameters.simulationDuration / 60;

		// Remove devices that do not generate
		devicesList.removeIf(dev -> !dev.isGeneratingTasks());

		int devicesCount = devicesList.size();

		// Browse all applications
		IntStream.range(0, SimulationParameters.applicationList.size() - 1).forEach(app -> {
			int numberOfDevices = (int) (SimulationParameters.applicationList.get(app).getUsagePercentage()
					* devicesCount / 100);
			IntStream.range(0, numberOfDevices).mapToObj(i -> devicesList.remove(random.nextInt(devicesList.size())))
					.peek(dev -> dev.setApplicationType(app)).forEach(dev -> generateTasksForDevice(dev, app));
		});

		devicesList.forEach(dev -> generateTasksForDevice(dev, SimulationParameters.applicationList.size() - 1));
		return this.getTaskList();
	}

	/**
	 * Generates tasks that will be offloaded during simulation for the given device
	 * and application.
	 * 
	 * @param device the device to generate tasks for
	 * @param app    the application type
	 */
	protected void generateTasksForDevice(ComputingNode dev, int app) {
		IntStream.range(0, (int) simulationTime)
				// First get time in seconds
				.forEach(st -> insert((st * 60)
						// Then pick up random second in this minute "st". Shift the time by a random
						// value
						+ random.nextInt(15), app, dev));
	}

	/**
	 * Inserts a task into the task list.
	 * 
	 * @param time   the time in seconds at which the task should be executed
	 * @param app    the application type of the task
	 * @param device the device that generates the task
	 */
	protected void insert(int time, int app, ComputingNode dev) {
		Application appParams = SimulationParameters.applicationList.get(app);
		long requestSize = appParams.getRequestSize();
		long outputSize = appParams.getResultsSize();
		long containerSize = appParams.getContainerSizeInBits();
		double maxLatency = appParams.getLatency();
		long length = (long) appParams.getTaskLength();
		int rate = appParams.getRate();
		int taskDuration = 60 / rate;

		for (int i = 0; i < rate; i++) {
			Task task = createTask(++id).setType(appParams.getType()).setFileSizeInBits(requestSize)
					.setOutputSizeInBits(outputSize).setContainerSizeInBits(containerSize).setApplicationID(app)
					.setMaxLatency(maxLatency).setLength(length).setEdgeDevice(dev).setRegistry(getSimulationManager()
							.getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList().get(0));

			time += taskDuration;
			task.setTime(time);

			taskList.add(task);
			getSimulationManager().getSimulationLogger()
					.deepLog("BasicTasksGenerator, Task " + id + " with execution time " + time + " (s) generated.");
		}
	}

	/**
	 * 
	 * Creates a new instance of Task using the specified ID.
	 * 
	 * @param id the ID to assign to the new task
	 * @return the new Task instance
	 */
	protected Task createTask(int id) {
		try {
			return taskClass.getConstructor(int.class).newInstance(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
