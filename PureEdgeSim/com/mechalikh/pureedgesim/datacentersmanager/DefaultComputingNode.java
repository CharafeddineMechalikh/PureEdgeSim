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

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * This computing node class used by the simulator by default. PureEdgeSim's
 * users can extend it and use their custom class (@see
 * com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract#setCustomComputingNode(Class))
 * 
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class DefaultComputingNode extends LocationAwareNode {
	protected int applicationType;
	protected boolean isSensor = false;
	protected double availableStorage = 0; // in Megabytes
	protected double storage = 0; // in Megabytes
	protected boolean isIdle = true;
	protected int tasks = 0;
	protected int totalTasks = 0;
	protected double totalMipsCapacity;
	protected double mipsPerCore;
	protected int numberOfCPUCores;
	protected int availableCores;
	protected List<Task> tasksQueue = new ArrayList<>();
	protected double availableRam; // in Megabytes
	protected double ram; // in Megabytes
	protected static final int EXECUTION_FINISHED = 2;

	public DefaultComputingNode(SimulationManager simulationManager, double mipsPerCore, int numberOfCPUCores,
			double storage, double ram) {
		super(simulationManager);
		setStorage(storage);
		setAvailableStorage(storage);
		setTotalMipsCapacity(mipsPerCore * numberOfCPUCores);
		this.mipsPerCore = mipsPerCore;
		setRam(ram);
		setAvailableRam(ram);
		setNumberOfCPUCores(numberOfCPUCores);
		this.availableCores = numberOfCPUCores;
		if (mipsPerCore <= 0 || numberOfCPUCores <= 0 || storage <= 0)
			this.setAsSensor(true);
	}

	@Override
	public void processEvent(Event e) {
		super.processEvent(e);
		if (e.getTag() == EXECUTION_FINISHED)
			executionFinished(e);
	}

	public double getNumberOfCPUCores() {
		return numberOfCPUCores;
	}

	public void setNumberOfCPUCores(int numberOfCPUCores) {
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

	public void setAvailableStorage(double d) {
		this.availableStorage = d;
	}

	public double getAvgCpuUtilization() {
		if (this.getTotalMipsCapacity() == 0)
			return 0;
		Double utilization = (totalTasks * 100.0)
				/ (getTotalMipsCapacity() * simulationManager.getSimulation().clock());

		return Math.min(100, utilization);
	}

	public double getCurrentCpuUtilization() {
		if (this.getTotalMipsCapacity() == 0)
			return 0;
		Double utilization = tasks * 100.0 / getTotalMipsCapacity();
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

	public List<Task> getTasksQueue() {
		return tasksQueue;
	}

	public double getTotalStorage() {
		return storage;
	}

	public void setStorage(double storage) {
		this.storage = storage;
	}

	public double getTotalMipsCapacity() {
		return totalMipsCapacity;
	}

	public void setTotalMipsCapacity(double totalMipsCapacity) {
		this.totalMipsCapacity = totalMipsCapacity;
	}

	/**
	 * Gets the total amount of RAM (in Megabytes) that this computing node has.
	 * 
	 * @return the total amount of RAM.
	 * 
	 * @see #getAvailableRam()
	 * @see #setAvailableRam(long)
	 * @see #setRam(long)
	 */
	public double getRamCapacity() {
		return this.ram;
	}

	/**
	 * Gets the amount of RAM (in Megabytes) that is available on this computing
	 * node.
	 * 
	 * @return the amount of available RAM.
	 * 
	 * @see #getRamCapacity()
	 * @see #setAvailableRam(long)
	 * @see #setRam(long)
	 */
	public double getAvailableRam() {
		return this.availableRam;
	}

	/**
	 * Sets the total amount of RAM (in Megabytes) that this computing node has.
	 * 
	 * @param ram the total RAM on this computing node.
	 * 
	 * @see #getAvailableRam()
	 * @see #setAvailableRam(long)
	 * @see #getRamCapacity()
	 */
	public void setRam(double ram) {
		this.ram = ram;
	}

	/**
	 * Sets the amount of RAM (in Megabytes) that is available on this computing
	 * node.
	 * 
	 * @param ram the available RAM.
	 * 
	 * @see #getAvailableRam()
	 * @see #getRamCapacity()
	 * @see #setRam(long)
	 */
	public void setAvailableRam(double ram) {
		this.availableRam = ram;
	}

	@Override
	public void submitTask(Task task) {
		// The task to be executed has been received, save the arrival time
		task.setArrivalTime(getSimulation().clock());

		// Update the amount of available storage
		this.setAvailableStorage(this.availableStorage - task.getContainerSizeInMBytes());

		// If a CPU core and enough RAM are available, execute task directly
		if (availableCores > 0 && this.getAvailableRam() > task.getContainerSizeInMBytes()) {
			startExecution(task);
		}
		// Otherwise, add it to the execution queue
		else
			getTasksQueue().add(task);
	}

	protected void startExecution(Task task) {

		// Update the CPU utilization.
		addCpuUtilization(task);
		// Update the amount of RAM.
		setAvailableRam(this.getAvailableRam() - task.getContainerSizeInMBytes());
		// Update the number of available cores.
		availableCores--;
		// Record when the execution has started.
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

		// Schedule when the execution will be finished.
		schedule(this, (task.getLength() / mipsPerCore), EXECUTION_FINISHED, task);
	}

	public double getMipsPerCore() {
		return mipsPerCore;
	}

	protected void executionFinished(Event e) {

		// The execution of one task has been finished, free one more CPU core.
		availableCores++;
		// Free the RAM that has been used by the finished task.
		setAvailableRam(this.getAvailableRam() + ((Task) e.getData()).getContainerSizeInMBytes());
		// Free the storage that has been used by the finished task.
		setAvailableStorage(this.getAvailableStorage() + ((Task) e.getData()).getContainerSizeInMBytes());
		// Update CPU utilization.
		removeCpuUtilization((Task) e.getData());

		// Save the execution end time for later use.
		((Task) e.getData()).setExecutionFinishTime(this.getSimulation().clock());

		// Notify the simulation manager that a task has been finished, and it's time to
		// return the execution results.
		scheduleNow(simulationManager, SimulationManager.TRANSFER_RESULTS_TO_ORCH, e.getData());

		// If there are tasks waiting for execution
		if (!getTasksQueue().isEmpty()) {

			// Execute the first task in the queue on the available core.
			Task task = getTasksQueue().get(0);

			// Remove the task from the queue.
			getTasksQueue().remove(0);

			// Execute the task.
			startExecution(task);
		}
	}

	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
		this.applicationPlacementLocation = node;
		this.isApplicationPlaced = true;
		if ((node.getType() == SimulationParameters.TYPES.EDGE_DEVICE) && (this != node)) {
			simulationManager.getDataCentersManager().getTopology().removeLink(currentDeviceToDeviceWifiLink);
			currentDeviceToDeviceWifiLink.setDst(node);
			simulationManager.getDataCentersManager().getTopology().addLink(currentDeviceToDeviceWifiLink);
		}

	}

}
