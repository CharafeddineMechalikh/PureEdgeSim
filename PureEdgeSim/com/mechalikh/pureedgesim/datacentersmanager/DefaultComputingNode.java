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
package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.LinkedList;

import org.jgrapht.GraphPath;

import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DefaultComputingNode extends LocationAwareNode {
	protected int applicationType;
	protected boolean isSensor = false;
	protected double availableStorage = 0;
	protected double Storage = 0;
	protected boolean isIdle = true;
	protected double tasks = 0;
	protected double totalTasks = 0;
	protected double totalMipsCapacity;
	protected double mipsPerCore;
	protected double numberOfCPUCores;
	protected double availableCores;
	protected LinkedList<Task> tasksQueue = new LinkedList<>();
	protected static final int EXECUTION_FINISHED = 2;

	public DefaultComputingNode(SimulationManager simulationManager, double mipsPerCore, long numberOfCPUCores,
			long storage) {
		super(simulationManager);
		setStorage(storage);
		setAvailableStorage(storage);
		setTotalMipsCapacity(mipsPerCore * numberOfCPUCores);
		this.mipsPerCore = mipsPerCore; 
		setNumberOfCPUCores(numberOfCPUCores);
		availableCores = numberOfCPUCores;
		if (mipsPerCore <= 0 || numberOfCPUCores <= 0 || storage <= 0)
			this.setAsSensor(true);
	}

	@Override
	public void processEvent(Event e) {
		switch (e.getTag()) {
		case EXECUTION_FINISHED:
			executionFinished(e);
			break;
		default:
			super.processEvent(e);
			break;
		}
	}

	public double getNumberOfCPUCores() {
		return numberOfCPUCores;
	}

	public void setNumberOfCPUCores(double numberOfCPUCores) {
		this.numberOfCPUCores = numberOfCPUCores;
	}

	public int getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(int applicationType) {
		this.applicationType = applicationType;
	}

	public double getAvailableStorage() {
		return availableStorage;
	}

	public void setAvailableStorage(double availableStorage) {
		this.availableStorage = availableStorage;
	}

	public double getAvgCpuUtilization() {
		if (this.getTotalMipsCapacity() == 0)
			return 0;
		double utilization = totalTasks / (getTotalMipsCapacity() * simulationManager.getSimulation().clock());
		return utilization > 1 ? 100 : utilization * 100;
	}

	public double getCurrentCpuUtilization() {
		if (this.getTotalMipsCapacity() == 0)
			return 0;
		double utilization = tasks * 100 / getTotalMipsCapacity();
		return utilization > 100 ? 100 : utilization;
	}

	public boolean isIdle() {
		return isIdle;
	}

	public void setIdle(boolean isIdle) {
		this.isIdle = isIdle;
	}

	public void addCpuUtilization(Task task) {
		tasks += task.getLength();
		totalTasks += task.getLength();
		setIdle(false);
	}

	public void removeCpuUtilization(Task task) {
		tasks -= task.getLength();
		if (tasks <= 0)
			setIdle(true);
	}

	public boolean isSensor() {
		return isSensor;
	}

	public void setAsSensor(boolean isSensor) {
		this.isSensor = isSensor;
	}

	public LinkedList<Task> getTasksQueue() {
		return tasksQueue;
	}

	public double getTotalStorage() {
		return Storage;
	}

	public void setStorage(double storage) {
		Storage = storage;
	}

	public double getTotalMipsCapacity() {
		return totalMipsCapacity;
	}

	public void setTotalMipsCapacity(double totalMipsCapacity) {
		this.totalMipsCapacity = totalMipsCapacity;
	}

	@Override
	public void submitTask(Task task) {
		// The task to be executed has been received, save the arrival time
		task.setArrivalTime(getSimulation().clock());

		// If a CPU core is available, execute task directly
		if (availableCores > 0) {

			startExecution(task);
		}
		// Otherwise, add it to the execution queue
		else
			getTasksQueue().add(task);
	}

	private void startExecution(Task task) {
		// Update the CPU utilization
		addCpuUtilization(task);
		availableCores--;
		task.setExecutionStartTime(getSimulation().clock());

		/*
		 * Arguably, the correct way to get energy consumption measurement is to place
		 * the following line of code within the processEvent(Event e) method of the
		 * EnergyAwareComputingNode:
		 * 
		 * getEnergyModel().updateCpuEnergyConsumption(getCurrentCpuUtilization());
		 * 
		 * I mean, this makes sense right?. The problem with this is that it will depend
		 * on the update interval. To get more accurate results, you need to set the
		 * update interval as low as possible, this will in turn increase the simulation
		 * duration, which is clearly not convenient. One way around it, is to make the
		 * measurement here, when the task is being executed. The problem with this is
		 * that if we don't receive a task, the static energy consumption will not be
		 * measured. So the best approach is to measure the dynamic one here, and add
		 * the static one there.
		 */

		getEnergyModel().updateDynamicEnergyConsumption(task.getLength(), this.getTotalMipsCapacity());

		schedule(this, ((double) task.getLength() / mipsPerCore), EXECUTION_FINISHED, task);
	}

	public double getMipsPerCore() {
		return mipsPerCore;
	}

	private void executionFinished(Event e) {

		// The execution of one task has been finished, free one more CPU core.
		availableCores++;

		// Update CPU utilization.
		removeCpuUtilization((Task) e.getData());

		// Save the execution end time for later use.
		((Task) e.getData()).setExecutionEndTime(this.getSimulation().clock());

		// Notify the simulation manager that a task has been finished, and it's time to
		// return the execution results.
		scheduleNow(simulationManager, SimulationManager.TRANSFER_RESULTS_TO_ORCH, e.getData());

		// If there are tasks waiting for execution
		if (!getTasksQueue().isEmpty()) {

			// Execute the first task in the queue on the available core.
			Task task = getTasksQueue().getFirst();

			// Remove the task from the queue.
			getTasksQueue().removeFirst();

			// Execute the task.
			startExecution(task);
		}

	}

	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
		this.applicationPlacementLocation = node;
		this.isApplicationPlaced = true;
		if (this != node) {
			if (node.getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
				simulationManager.getDataCentersManager().getTopology().removeLink(currentDeviceToDeviceWifiLink);
				currentDeviceToDeviceWifiLink.setDst(node);
				simulationManager.getDataCentersManager().getTopology().addLink(currentDeviceToDeviceWifiLink);
			}

			GraphPath<ComputingNode, NetworkLink> path = simulationManager.getDataCentersManager().getTopology()
					.getPath(this, node);

			vertexList.addAll(path.getVertexList());
			edgeList.addAll(path.getEdgeList());
		}
	}

}
