package com.mechalikh.pureedgesim.DataCentersManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import com.mechalikh.pureedgesim.LocationManager.Mobility;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksOrchestration.VmTaskMapItem;

public abstract class DataCenter extends DatacenterSimple {
	private simulationParameters.TYPES deviceType;
	private EnergyModel energyModel;
	private List<VmTaskMapItem> vmTaskMap;
	private int applicationType;
	protected boolean isOrchestrator = false;
	private Mobility mobilityManager;
	private DataCenter orchestrator;
	protected SimulationManager simulationManager;
	private boolean generateTasks = true;
	private Resources resources;
	protected boolean isDead = false;
	protected double deathTime;

	public DataCenter(SimulationManager simulationManager, List<? extends Host> hostList) {
		super(simulationManager.getSimulation(), hostList, new VmAllocationPolicySimple());
		this.simulationManager = simulationManager;
		vmTaskMap = new ArrayList<>();
		long memory = 0;
		long ram = 0;
		for (Host host : hostList) {
			memory += host.getStorage().getAvailableResource();
			ram += host.getRam().getCapacity();
		}
		this.resources = new Resources(ram, memory);
	}

	protected abstract void updateCpuUtilization();

	public EnergyModel getEnergyModel() {
		return energyModel;
	}

	public simulationParameters.TYPES getType() {
		return deviceType;
	}

	public void setType(simulationParameters.TYPES type) {
		this.deviceType = type;
	} 

	public List<VmTaskMapItem> getVmTaskMap() {
		return vmTaskMap;
	}

	public void setApplication(int app) {
		this.applicationType = app;
	}

	public int getApplication() {
		return applicationType;
	}

	public boolean isOrchestrator() {
		return isOrchestrator;
	}

	public void setOrchestrator(boolean isOrchestrator) {
		this.isOrchestrator = isOrchestrator;
	}

	public Mobility getMobilityManager() {
		return mobilityManager;
	}

	public void setMobilityManager(Object mobilityManager) {
		this.mobilityManager = (Mobility) mobilityManager;
	}

	public void setEnergyModel(Object energyModel) {
		this.energyModel = (EnergyModel) energyModel;
	}

	public DataCenter getOrchestrator() {
		return this.orchestrator;
	}

	public void setTasksGeneration(boolean generateTasks) {
		this.generateTasks = generateTasks;
	}

	public boolean isGeneratingTasks() {
		return this.generateTasks;
	}

	public List<Vm> getVmList() {
		return (List<Vm>) Collections.unmodifiableList(
				getHostList().stream().flatMap(h -> h.getVmList().stream()).collect(Collectors.toList()));
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
}
