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
package com.mechalikh.pureedgesim.DataCentersManager;

import org.cloudbus.cloudsim.core.Simulation;
 
import com.mechalikh.pureedgesim.TasksGenerator.Task;

public class Resources {
	private long storageMemory;
	private long availableStorageMemory; 
	private long ramMemory; 
	private boolean isIdle = true;
	private double tasks = 0;
	private double totalTasks = 0;
	private long totalMips = 0;
	private Simulation simulation;

	public Resources(long ram, long storage, long mips, Simulation simulation) {
		setStorageMemory(storage);
		setRamMemory(ram);
		setTotalMips(mips); 
		this.simulation = simulation;
	}

	public long getStorageMemory() {
		return storageMemory;
	}

	public void setStorageMemory(long storage) {
		this.storageMemory = storage;
		setAvailableMemory(storage);
	}

	public long getRam() {
		return ramMemory;
	}

	public void setRamMemory(long ram) {
		this.ramMemory = ram;
	}

	public long getAvailableStorage() {
		return availableStorageMemory;
	}

	public void setAvailableMemory(long availableMemory) {
		this.availableStorageMemory = availableMemory;
	}

	public double getAvgCpuUtilization() {
		if (totalMips == 0)
			return 0;
		return totalTasks / (totalMips * simulation.clock()) > 1 ? 100 : totalTasks / (totalMips * simulation.clock()) * 100;
	}

	public double getCurrentCpuUtilization() {
		if (totalMips == 0)
			return 0;
		return tasks / totalMips > 1 ? 100 : tasks / totalMips * 100;
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
	}

	public void removeCpuUtilization(Task task) {
		tasks -= task.getLength();
	}

	public double getTotalMips() {
		return totalMips;
	}

	public void setTotalMips(long totalMips) { 
		this.totalMips = totalMips;
	}

}
