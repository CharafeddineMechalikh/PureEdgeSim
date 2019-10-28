package com.Mechalikh.PureEdgeSim.DataCentersManager;

import java.util.ArrayList;
import java.util.List;  
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import com.Mechalikh.PureEdgeSim.LocationManager.Location;
import com.Mechalikh.PureEdgeSim.LocationManager.MobilityItem;
import com.Mechalikh.PureEdgeSim.LocationManager.MobilityManager;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters; 
import com.Mechalikh.PureEdgeSim.TasksOrchestration.VmTaskMapItem;

public class EdgeDataCenter extends DatacenterSimple {
	private static final int UPDATE_STATUS = 2000; // Avoid conflicting with CloudSim Plus Tags
	private Simulation simulation;
	private SimulationParameters.TYPES deviceType; 
	private boolean isMobile = false;
	private boolean isBatteryPowered = false;
	private double batteryCapacity;  
	EnergyModel energyModel;
	private boolean isDead = false;
	private double deathTime;
	private List<MobilityItem> mobilityPath;
	private List<VmTaskMapItem> vmTaskMap;
	private int applicationType;
	private boolean isOrchestrator = false;
	private long storageMemory;
	private long availableStorageMemory;
	private double totalCpuUtilization = 0;
	private int utilizationFrequency = 0;
	private boolean isIdle = true;
	private long ramMemory;
    private MobilityManager mobilityManager;
	private int currentCpuUtilization;
	private EdgeDataCenter orchestrator; 
     
	public EdgeDataCenter(Simulation simulation, List<? extends Host> hostList, VmAllocationPolicy vmAllocationPolicy) {
		super(simulation, hostList, vmAllocationPolicy);
		this.simulation = simulation;
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
		schedule(this, SimulationParameters.INITIALIZATION_TIME, UPDATE_STATUS);

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
				schedule(this, SimulationParameters.UPDATE_INTERVAL, UPDATE_STATUS);
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
		currentCpuUtilization=0;
		
		//get the cpu usage of all vms
		for (int i = 0; i < this.getVmList().size(); i++) {
			vmUsage = this.getVmList().get(i).getCloudletScheduler().getRequestedCpuPercentUtilization(simulation.clock());
			currentCpuUtilization+=vmUsage; // the current utilization 
			totalCpuUtilization += vmUsage; 
			utilizationFrequency++; // in order to get the average usage from the total usage
			if (vmUsage != 0)
				setIdle(false); // set as active (not idle) if at least one vm is used 
		}
		
		if(this.getVmList().size()>0)
		currentCpuUtilization=currentCpuUtilization/this.getVmList().size();
		
		//update the energy consumption
		this.getEnergyModel().updateCpuEnergyConsumption(currentCpuUtilization);
		
		if (isBattery() && this.getEnergyModel().getTotalEnergyConsumption() > batteryCapacity) {
			isDead = true;
			deathTime = simulation.clock();
		}
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
		if (batteryCapacity < this.getEnergyModel().getTotalEnergyConsumption() )
			return 0;
		return batteryCapacity - this.getEnergyModel().getTotalEnergyConsumption() ;
	}

	public double getBatteryLevelPercentage() { 
		 return getBatteryLevel()* 100 / batteryCapacity;
	}
 
	public boolean isDead() {
		return isDead;
	}

	public void setMobilityPath(List<MobilityItem> MobilityPath) {
		this.mobilityPath = MobilityPath;

	}

	public List<MobilityItem> getMobilityPath() {
		return mobilityPath;

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

	@Override
	public void shutdownEntity() {
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
		if (utilizationFrequency == 0)
			utilizationFrequency = 1;
		return totalCpuUtilization * 100 / utilizationFrequency;
	}
	public boolean isIdle() {
		return isIdle;
	}

	public void setIdle(boolean isIdle) {
		this.isIdle = isIdle;
	}

	public MobilityManager getMobilityManager() {
		return mobilityManager;
	}

	public void setMobilityManager(MobilityManager mobilityManager) {
		this.mobilityManager = mobilityManager;
	}

	public void setEnergyModel(EnergyModel energyModel) {
		 this.energyModel=energyModel;
		
	}

	public EdgeDataCenter getOrchestrator() { 
		return this.orchestrator;
	}
}
