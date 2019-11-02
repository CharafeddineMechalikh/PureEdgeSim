package com.mechalikh.pureedgesim.DataCentersManager;

import java.util.ArrayList;
import java.util.List; 
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;

import com.mechalikh.pureedgesim.LocationManager.Location;
import com.mechalikh.pureedgesim.LocationManager.Mobility;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksOrchestration.VmTaskMapItem;

public class EdgeDataCenter extends DatacenterSimple {
	private static final int UPDATE_STATUS = 2000; // Avoid conflicting with CloudSim Plus Tags
	private simulationParameters.TYPES deviceType;
	private boolean isMobile = false;
	private boolean isBatteryPowered = false;
	private double batteryCapacity;
	private EnergyModel energyModel;
	private boolean isDead = false;
	private double deathTime;
	private List<VmTaskMapItem> vmTaskMap;
	private int applicationType;
	private boolean isOrchestrator = false;
	private long storageMemory;
	private long availableStorageMemory;
	private double totalCpuUtilization = 0;
	private int utilizationFrequency = 0;
	private boolean isIdle = true;
	private long ramMemory;
	private Mobility mobilityManager;
	private EdgeDataCenter orchestrator;
	private double currentCpuUtilization = 0;
	private SimulationManager simulationManager;

	public EdgeDataCenter(SimulationManager simulationManager, List<? extends Host> hostList) {
		super(simulationManager.getSimulation(), hostList,new VmAllocationPolicySimple());
		this.simulationManager = simulationManager;
		vmTaskMap = new ArrayList<VmTaskMapItem>();

		long memory = 0;
		long ram = 0;
		for (int i = 0; i < hostList.size(); i++) {
			memory += hostList.get(i).getStorage().getAvailableResource();
			ram += hostList.get(i).getRam().getCapacity();
		}
		setStorageMemory(memory);
		setRamMemory(ram);
	}

	@Override
	protected void startEntity() {
		super.startEntity();
		schedule(this, simulationParameters.INITIALIZATION_TIME, UPDATE_STATUS);

	}

	@Override
	public void processEvent(final SimEvent ev) {
		switch (ev.getTag()) {
		case UPDATE_STATUS:
			// Update energy consumption
			updateEnergyConsumption();

			// Update location
			if (isMobile())
				getMobilityManager().getNextLocation();

			if (!isDead()) {
				schedule(this, simulationParameters.UPDATE_INTERVAL, UPDATE_STATUS);
			}

			break;
		default:
			super.processEvent(ev);
			break;
		}
	}

	private void updateEnergyConsumption() {
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
}
