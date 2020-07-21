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
package com.mechalikh.pureedgesim.SimulationManager;

import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.events.SimEvent;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.DataCentersManager.ServersManager;
import com.mechalikh.pureedgesim.Network.NetworkModelAbstract;
import com.mechalikh.pureedgesim.ScenarioManager.Scenario;
import com.mechalikh.pureedgesim.SimulationVisualizer.SimulationVisualizer;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.mechalikh.pureedgesim.TasksOrchestration.CustomBroker;
import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;

public class SimulationManagerAbstract extends CloudSimEntity {

	protected List<Task> tasksList;
	protected Orchestrator edgeOrchestrator;
	protected ServersManager serversManager;
	protected SimulationVisualizer simulationVisualizer;
	protected CloudSim simulation;
	protected int simulationId;
	protected int iteration;
	protected SimLog simLog;
	protected CustomBroker broker;
	protected NetworkModelAbstract networkModel;
	protected List<? extends DataCenter> orchestratorsList;
	protected Scenario scenario;

	public SimulationManagerAbstract(SimLog simLog, CloudSim simulation, int simulationId, int iteration, Scenario scenario) {
		super(simulation);
		this.simulation = simulation;
		this.simLog = simLog;
		this.scenario = scenario;
		this.simulationId = simulationId;
		this.iteration = iteration;
	}

	@Override
	public void processEvent(SimEvent evt) {
	}

	@Override
	protected void startEntity() {
	}

	public void setServersManager(ServersManager serversManager) {
		// Get orchestrators list from the server manager
		orchestratorsList = serversManager.getOrchestratorsList();
		this.serversManager = serversManager;

		// Submit vm list to the broker
		simLog.deepLog("SimulationManager- Submitting VM list to the broker");
		broker.submitVmList(serversManager.getVmList());
	}

	public void setTasksList(List<Task> tasksList) {
		this.tasksList = tasksList;
	}

	public void setOrchestrator(Orchestrator edgeOrchestrator) {
		this.edgeOrchestrator = edgeOrchestrator;

	}

	public void setNetworkModel(NetworkModelAbstract networkModel) {
		this.networkModel = networkModel;
	}

	public List<Task> getTasksList() {
		return tasksList;
	}

	public int getIterationId() {
		return this.iteration;
	}

	public NetworkModelAbstract getNetworkModel() {
		return this.networkModel;
	}

	public int getSimulationId() {
		return this.simulationId;
	}

	public SimLog getSimulationLogger() {
		return simLog;
	}

	public ServersManager getServersManager() {
		return serversManager;
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public CustomBroker getBroker() {
		return this.broker;
	}
}
