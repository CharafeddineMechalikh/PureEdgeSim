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
package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.List;

import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.tasksgenerator.Task;

public class Resources {
	private long storageMemory = 0;
	private long availableStorageMemory = 0;
	private long ramMemory = 0;
	private boolean isIdle = true;
	private double tasks = 0;
	private double totalTasks = 0;
	private long totalMips = 0;
	private Simulation simulation;
	private List<? extends Vm> vmList;

	public Resources(List<? extends Vm> vmList, Simulation simulation) {
		this.simulation = simulation;
		this.setVmList(vmList);
		for (Vm vm : vmList) {
			storageMemory += vm.getStorage().getAvailableResource();
			ramMemory += vm.getRam().getCapacity();
			totalMips += vm.getMips();
		}
		availableStorageMemory = storageMemory;
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
		return totalTasks / (totalMips * simulation.clock()) > 1 ? 100
				: totalTasks / (totalMips * simulation.clock()) * 100;
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
		setIdle(false);
	}

	public void removeCpuUtilization(Task task) {
		tasks -= task.getLength();
		if(tasks<=0)
			setIdle(true);
	}

	public double getTotalMips() {
		return totalMips;
	}

	public List<? extends Vm> getVmList() {
		return vmList;
	}

	public void setVmList(List<? extends Vm> vmList) {
		this.vmList = vmList;
	}

}
