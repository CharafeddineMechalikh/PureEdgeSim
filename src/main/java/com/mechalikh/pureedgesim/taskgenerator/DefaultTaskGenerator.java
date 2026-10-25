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

import java.util.Random;
import java.util.stream.IntStream;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.distribution.Distribution;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Default task generator. Each device is assigned an application profile
 * according to the profile usage percentages, then generates tasks at the rate
 * of that profile for the whole simulation.
 * <p>
 * Every task is drawn independently from the distributions of its application,
 * so request and result sizes, container size, task length and latency
 * requirement vary from one task to the next. When an application declares an
 * arrival pattern, its tasks are spread over the simulation using the sampled
 * gaps instead of the fixed per-minute burst.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 3.0
 */
public class DefaultTaskGenerator extends TaskGenerator {
	/**
	 * Used to generate random values. Shared with the rest of the simulation, so it
	 * honors the {@code random_seed} parameter.
	 * 
	 * @see #generate()
	 * @see #generateTasksForDevice(ComputingNode, int)
	 */
	protected Random random;
	/** Counter used to assign a unique ID to every generated task. */
	protected int id = 0;
	/** Simulation duration used for generation, in minutes. */
	protected double simulationTime;

	/**
	 * Creates a task generator using the randomness source of the current thread.
	 *
	 * @param simulationManager the simulation manager
	 */
	public DefaultTaskGenerator(SimulationManager simulationManager) {
		super(simulationManager);
		random = Randomness.getRandom();
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
		for (int app = 0; app < SimulationParameters.applicationList.size() - 1; app++) {
			int numberOfDevices = (int) (SimulationParameters.applicationList.get(app).getUsagePercentage()
					* devicesCount / 100);
			for (int i = 0; i < numberOfDevices && !devicesList.isEmpty(); i++) {
				ComputingNode dev = devicesList.remove(random.nextInt(devicesList.size()));
				dev.setApplicationType(app);
				generateTasksForDevice(dev, app);
			}
		}

		devicesList.forEach(dev -> generateTasksForDevice(dev, SimulationParameters.applicationList.size() - 1));
		return this.getTaskList();
	}

	/**
	 * Generates tasks that will be offloaded during simulation for the given device
	 * and application.
	 * <p>
	 * Applications without an arrival pattern get one burst of tasks per minute,
	 * evenly spaced inside it. Applications with an arrival pattern get one task per
	 * sampled gap, whatever minute it falls in.
	 * 
	 * @param dev the device to generate tasks for
	 * @param app the application type
	 */
	protected void generateTasksForDevice(ComputingNode dev, int app) {
		Application appParams = SimulationParameters.applicationList.get(app);
		if (appParams.hasRandomArrival()) {
			generateRandomlyArrivingTasks(dev, app, appParams);
			return;
		}
		IntStream.range(0, (int) simulationTime)
				// First get time in seconds
				.forEach(st -> insert((st * 60)
						// Then pick up random second in this minute "st". Shift the time by a random
						// value
						+ random.nextInt(15), app, dev));
	}

	/**
	 * Generates one task each time the arrival distribution says that a new request
	 * is sent by the device, from the beginning of the simulation until its end.
	 * 
	 * @param dev       the device to generate tasks for
	 * @param app       the application type
	 * @param appParams the application whose distributions are used
	 */
	protected void generateRandomlyArrivingTasks(ComputingNode dev, int app, Application appParams) {
		Distribution arrival = appParams.getArrivalDistribution();
		double simulationSeconds = simulationTime * 60;
		// The device is not assumed to be busy at the very beginning of the simulation.
		double time = random.nextDouble() * Math.max(1, arrival.sample());
		while (time < simulationSeconds) {
			insert(app, dev, appParams, (int) time);
			// A gap of less than one second would keep the loop busy without moving.
			time += Math.max(1, arrival.sample());
		}
	}

	/**
	 * Inserts a burst of tasks into the task list, evenly spaced after the given
	 * time. The number of tasks is drawn from the rate distribution of the
	 * application.
	 * 
	 * @param time the time in seconds at which the first task should be executed
	 * @param app  the application type of the task
	 * @param dev  the device that generates the task
	 */
	protected void insert(int time, int app, ComputingNode dev) {
		Application appParams = SimulationParameters.applicationList.get(app);
		int tasksPerMinute = appParams.sampleTasksPerMinute();
		if (tasksPerMinute == 0) {
			return;
		}
		int taskDuration = 60 / tasksPerMinute;

		for (int i = 0; i < tasksPerMinute; i++) {
			time += taskDuration;
			insert(app, dev, appParams, time);
		}
	}

	/**
	 * Creates a single task from the distributions of the given application and
	 * adds it to the task list at the given time.
	 * 
	 * @param app       the application type of the task
	 * @param dev       the device that generates the task
	 * @param appParams the application whose distributions are sampled
	 * @param time      the time in seconds at which the task is offloaded
	 */
	protected void insert(int app, ComputingNode dev, Application appParams, int time) {
		Task task = createTask(++id).setType(appParams.getType()).setFileSizeInBits(appParams.sampleRequestSize())
				.setOutputSizeInBits(appParams.sampleResultsSize())
				.setContainerSizeInBits(appParams.sampleContainerSize()).setApplicationID(app)
				.setMaxLatency(appParams.sampleLatency()).setLength(appParams.sampleTaskLength()).setEdgeDevice(dev)
				.setRegistry(getSimulationManager().getDataCentersManager().getComputingNodesGenerator()
						.getCloudOnlyList().get(0));

		task.setTime(time);

		taskList.add(task);
		getSimulationManager().getSimulationLogger()
				.deepLog("BasicTasksGenerator, Task " + id + " with execution time " + time + " (s) generated.");
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
