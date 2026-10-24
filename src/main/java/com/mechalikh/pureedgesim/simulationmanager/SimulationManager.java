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
package com.mechalikh.pureedgesim.simulationmanager;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationvisualizer.SimulationVisualizer;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * The abstract class that is extended by the default simulation manager.
 * 
 * @see com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public abstract class SimulationManager extends SimEntity {

	/**
	 * Simulation manager tags.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationengine.SimEntity#schedule(SimEntity
	 *      simEntity, Double time, int tag)
	 **/
	/** Tag of the event that prints the simulation log. */
	protected static final int PRINT_LOG = 1;
	/** Tag of the event that updates the simulation progress display. */
	protected static final int SHOW_PROGRESS = 2;
	/** Tag of the event that starts executing a task on its destination node. */
	public static final int EXECUTE_TASK = 3;
	/** Tag of the event that transfers task results back to the orchestrator. */
	public static final int TRANSFER_RESULTS_TO_ORCH = 4;
	/** Tag of the event signaling that results reached the source edge device. */
	public static final int RESULT_RETURN_FINISHED = 5;
	/** Tag of the event that sends a task to the orchestrator. */
	public static final int SEND_TO_ORCH = 6;
	/** Tag of the event that refreshes the real-time charts. */
	public static final int UPDATE_REAL_TIME_CHARTS = 7;
	/** Tag of the event that sends a task from the orchestrator to its destination. */
	public static final int SEND_TASK_FROM_ORCH_TO_DESTINATION = 8;
	/** Tag of the event that releases the next batch of generated tasks. */
	protected static final int NEXT_BATCH = 9; 

	/** The orchestrator that decides where tasks run. */
	protected Orchestrator edgeOrchestrator;
	/** Manager of the cloud, edge, and mist computing nodes. */
	protected DataCentersManager dataCentersManager;
	/** The real-time chart window, when visualization is enabled. */
	protected SimulationVisualizer simulationVisualizer;
	/** The underlying event engine. */
	protected PureEdgeSim simulation;
	/** Identifier of this simulation, used in log output. */
	protected int simulationId;
	/** Index of this simulation run within the scenario sweep. */
	protected int iteration;
	/** Logger that accumulates CPU, energy, network, and task metrics. */
	protected SimLog simLog;
	/** The network model used to transfer tasks, containers, and results. */
	protected NetworkModel networkModel;
	/** Tasks whose results have returned to their source devices. */
	protected List<Task> finishedTasks = new ArrayList<>();
	/** The algorithm, architecture, and device-count combination being simulated. */
	protected Scenario scenario;
	/** Queue of generated tasks, ordered by arrival time. */
	protected FutureQueue<Task> taskList;

	/**
	 * Initializes the simulation manager.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @param simLog       The simulation logger
	 * @param pureEdgeSim  The CloudSim simulation engine.
	 * @param simulationId The simulation ID
	 * @param iteration    Which simulation run
	 * @param scenario     The scenario is composed of the algorithm and
	 *                     architecture that are being used, and the number of edge
	 *                     devices.
	 */
	public SimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(pureEdgeSim);
		this.simulation = pureEdgeSim;
		this.simLog = simLog;
		this.scenario = scenario;
		this.simulationId = simulationId;
		this.iteration = iteration;

	}

	/**
	 * Starts PureEdgeSim simulation engine
	 * 
	 * @see com.mechalikh.pureedgesim.simulationengine.PureEdgeSim#start()
	 */
	public abstract void startSimulation();

	/**
	 * Sets the data centers manager.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param dataCentersManager The data centers manager that is used in this
	 *                           simulation
	 */
	public void setDataCentersManager(DataCentersManager dataCentersManager) {
		this.dataCentersManager = dataCentersManager; 
	}
	
	/**
	 * Sets the list of ordered offlaoding requests
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param taskList the ordered list of offlaoding requests.
	 */
	public void setTaskList(FutureQueue<Task> taskList) {
		this.taskList = taskList;
	}
	
	/**
	 * Returns the list of finished tasks.
	 * 
	 * @return the list of finished tasks.
	 */
	public List<Task> getFinishedTaskList() {
		return this.finishedTasks; 
	}

	/**
	 * Sets the orchestrator that is used in this simulation. Used when offloading
	 * the tasks.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param edgeOrchestrator the orchestrator.
	 */
	public void setOrchestrator(Orchestrator edgeOrchestrator) {
		this.edgeOrchestrator = edgeOrchestrator;

	}

	/**
	 * Sets the network model that is used in this simulation.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @param networkModel the network model.
	 */
	public void setNetworkModel(NetworkModel networkModel) {
		this.networkModel = networkModel;
	}

	/**
	 * Returns the iteration number.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @return The iteration number.
	 */
	public int getIteration() {
		return iteration;
	}

	/**
	 * Returns the network model.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @return The network model.
	 */
	public NetworkModel getNetworkModel() {
		return networkModel;
	}

	/**
	 * Returns the simulation ID.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @return The simulation ID.
	 */
	public int getSimulationId() {
		return simulationId;
	}

	/**
	 * Returns the simulation logger.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimLog
	 * 
	 * @return The simulation logger.
	 */
	public SimLog getSimulationLogger() {
		return simLog;
	}

	/**
	 * Returns the data centers manager that is used in this simulation.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * 
	 * @return The data centers manager.
	 */
	public DataCentersManager getDataCentersManager() {
		return this.dataCentersManager;
	}

	/**
	 * Returns the name of orchestration algorithm, the architecture in use, as well
	 * as the number of devices.
	 * 
	 * @return The simulation scenario.
	 */
	public Scenario getScenario() {
		return scenario;
	}
	
	/**
	 * Used to get the task failure rate.
	 * 
	 * @return The failure rate.
	 */
	public abstract double getFailureRate();

}
