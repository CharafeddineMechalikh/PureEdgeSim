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
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;

public abstract class DataCenter extends DatacenterSimple {
	private SimulationParameters.TYPES deviceType;
	private EnergyModel energyModel;
	private int applicationType;
	protected boolean isOrchestrator = false;
	private MobilityModel mobilityManager;
	private DataCenter orchestrator;
	protected SimulationManager simulationManager;
	private boolean generateTasks = false;
	private Resources resources;
	protected boolean isDead = false;
	protected double deathTime;

	public DataCenter(SimulationManager simulationManager, List<? extends Host> hostList, List<? extends Vm> vmList) {
		super(simulationManager.getSimulation(), hostList, new VmAllocationPolicyFirstFit());
		this.simulationManager = simulationManager;
		this.resources = new Resources(vmList, simulationManager.getSimulation());
	}

	public EnergyModel getEnergyModel() {
		return energyModel;
	}

	public SimulationParameters.TYPES getType() {
		return deviceType;
	}

	public void setType(SimulationParameters.TYPES type) {
		this.deviceType = type;
	}

	public boolean isOrchestrator() {
		return isOrchestrator;
	}

	public void setAsOrchestrator(boolean isOrchestrator) {
		this.isOrchestrator = isOrchestrator;
	}

	public MobilityModel getMobilityManager() {
		return mobilityManager;
	}

	public void setMobilityManager(Object mobilityManager) {
		this.mobilityManager = (MobilityModel) mobilityManager;
	}

	public void setEnergyModel(Object energyModel) {
		this.energyModel = (EnergyModel) energyModel;
	}

	public DataCenter getOrchestrator() {
		return orchestrator;
	}

	public void setTasksGeneration(boolean generateTasks) {
		this.generateTasks = generateTasks;
	}

	public boolean isGeneratingTasks() {
		return this.generateTasks;
	}

	public Resources getResources() {
		return resources;
	}

	public boolean isDead() {
		return isDead;
	}

	public double getDeathTime() {
		return deathTime;
	}

	public void setDeath(Boolean dead, double deathTime2) {
		isDead = dead;
		deathTime = deathTime2;
	}

	public int getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(int applicationType) {
		this.applicationType = applicationType;
	}
}
