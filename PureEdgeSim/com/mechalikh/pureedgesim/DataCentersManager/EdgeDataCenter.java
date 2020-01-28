package com.mechalikh.pureedgesim.DataCentersManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.LocationManager.Location;
import com.mechalikh.pureedgesim.LocationManager.Mobility;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksOrchestration.VmTaskMapItem;

public abstract class EdgeDataCenter extends DatacenterSimple {
	protected static final int UPDATE_STATUS = 2000; // Avoid conflicting with CloudSim Plus Tags
	protected simulationParameters.TYPES deviceType;
	protected boolean isMobile = false;
	protected boolean isBatteryPowered = false;
	protected double batteryCapacity;
	protected EnergyModel energyModel;
	protected boolean isDead = false;
	protected double deathTime;
	protected List<VmTaskMapItem> vmTaskMap;
	protected int applicationType;
	protected boolean isOrchestrator = false;
	protected long storageMemory;
	protected long availableStorageMemory;
	protected double totalCpuUtilization = 0;
	protected int utilizationFrequency = 0;
	protected boolean isIdle = true;
	protected long ramMemory;
	protected Mobility mobilityManager;
	protected EdgeDataCenter orchestrator;
	protected double currentCpuUtilization = 0;
	protected SimulationManager simulationManager;
	private boolean generateTasks = true;

	public EdgeDataCenter(SimulationManager simulationManager, List<? extends Host> hostList) {
		super(simulationManager.getSimulation(), hostList, new VmAllocationPolicySimple());
		this.simulationManager = simulationManager;
		vmTaskMap = new ArrayList<>();

		long memory = 0;
		long ram = 0;
		for (Host host : hostList) {
			memory += host.getStorage().getAvailableResource();
			ram += host.getRam().getCapacity();
		}
		setStorageMemory(memory);
		setRamMemory(ram);
	}

	protected void updateEnergyConsumption() {
		setIdle(true);
		double vmUsage = 0;
		currentCpuUtilization = 0;

		// get the cpu usage of all vms
		for (int i = 0; i < this.getVmList().size(); i++) {
			vmUsage = this.getVmList().get(i).getCloudletScheduler()
					.getRequestedCpuPercentUtilization(simulationManager.getSimulation().clock());
			currentCpuUtilization += vmUsage; // the current utilization
			totalCpuUtilization += vmUsage;
			utilizationFrequency++; // in order to get the average usage from the total usage
			if (vmUsage != 0)
				setIdle(false); // set as active (not idle) if at least one vm is used
		}

		if (this.getVmList().size() > 0)
			currentCpuUtilization = currentCpuUtilization / this.getVmList().size();

		// update the energy consumption
		this.getEnergyModel().updateCpuEnergyConsumption(currentCpuUtilization);

		if (isBattery() && this.getEnergyModel().getTotalEnergyConsumption() > batteryCapacity) {
			isDead = true;
			deathTime = simulationManager.getSimulation().clock();
		}
	}

	public EnergyModel getEnergyModel() {
		return energyModel;
	}

	public simulationParameters.TYPES getType() {
		return deviceType;
	}

	public void setType(simulationParameters.TYPES type) {
		this.deviceType = type;
	}

	public Location getLocation() {
		return getMobilityManager().getCurrentLocation();
	}

	public boolean isMobile() {
		return isMobile;
	}

	public void setMobile(boolean mobile) {
		isMobile = mobile;
	}

	public boolean isBattery() {
		return isBatteryPowered;
	}

	public void setBattery(boolean battery) {
		this.isBatteryPowered = battery;
	}

	public double getBatteryCapacity() {
		return batteryCapacity;
	}

	public void setBatteryCapacity(double batteryCapacity) {
		this.batteryCapacity = batteryCapacity;
	}

	public double getBatteryLevel() {
		if (!isBattery())
			return 0;
		if (batteryCapacity < this.getEnergyModel().getTotalEnergyConsumption())
			return 0;
		return batteryCapacity - this.getEnergyModel().getTotalEnergyConsumption();
	}

	public double getBatteryLevelPercentage() {
		return getBatteryLevel() * 100 / batteryCapacity;
	}

	public boolean isDead() {
		return isDead;
	}

	public double getDeathTime() {
		return deathTime;
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

	public long getAvailableMemory() {
		return availableStorageMemory;
	}

	public void setAvailableMemory(long availableMemory) {
		this.availableStorageMemory = availableMemory;
	}

	public double getTotalCpuUtilization() {
		if (utilizationFrequency == 0)
			utilizationFrequency = 1;
		return totalCpuUtilization * 100 / utilizationFrequency;
	}

	public double getCurrentCpuUtilization() {
		return currentCpuUtilization * 100;
	}

	public boolean isIdle() {
		return isIdle;
	}

	public void setIdle(boolean isIdle) {
		this.isIdle = isIdle;
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

	public EdgeDataCenter getOrchestrator() {
		return this.orchestrator;
	}

	public void setTasksGeneration(boolean generateTasks) {
		this.generateTasks = generateTasks;
	}

	public boolean isGeneratingTasks() {
		return this.generateTasks;
	}

	public List<Vm> getVmList() {
		return (List<Vm>) Collections
				.unmodifiableList(getHostList().stream().flatMap(h -> h.getVmList().stream()).collect(Collectors.toList()));
	}
}
