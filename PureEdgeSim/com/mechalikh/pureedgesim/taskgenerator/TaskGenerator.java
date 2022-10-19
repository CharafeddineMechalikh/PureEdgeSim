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

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue; 
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public abstract class TaskGenerator {
	/**
	 * The Task class that is used in the simulation.
	 * 
	 * @see #setCustomTaskClass(Class)
	 */

	protected Class<? extends Task> taskClass = DefaultTask.class;

	/**
	 * The ordered list of offloading requests.
	 * 
	 * @see #generate()
	 */
	protected FutureQueue<Task> taskList;

	/**
	 * The list of edge devices.
	 * 
	 * @see #generate()
	 */
	protected List<ComputingNode> devicesList = new ArrayList<>();

	/**
	 * The simulation manager.
	 * 
	 */
	protected SimulationManager simulationManager;

	/**
	 * Creates a task generator.
	 */
	public TaskGenerator(SimulationManager simulationManager) {
		taskList = new FutureQueue<>();
		setSimulationManager(simulationManager);
		for (int i = 0; i < getSimulationManager().getDataCentersManager().getComputingNodesGenerator()
				.getMistOnlyList().size(); i++)
			devicesList.add(getSimulationManager().getDataCentersManager().getComputingNodesGenerator()
					.getMistOnlyList().get(i));
	}

	/**
	 * Gets the list of all offloading requests.
	 * 
	 * @return list of all generated offloading requests.
	 */
	public FutureQueue<Task> getTaskList() {
		return taskList;
	}

	/**
	 * Gets the simulation manager.
	 * 
	 * @return simulation manager.
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Sets the simulation manager.
	 * 
	 * @param simulationManager the simulation manager.
	 */
	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	/**
	 * Generates the tasks that will be offloaded during the simulation.
	 * 
	 */
	public abstract FutureQueue<Task> generate();

	/**
	 * Allows to use a custom task class in the simulation. The class must extend
	 * the {@link Task} provided by PureEdgeSim.
	 * 
	 * @param task the custom task class to use.
	 */
	public void setCustomTaskClass(Class<? extends Task> task) {
		this.taskClass = task;
	}
}
