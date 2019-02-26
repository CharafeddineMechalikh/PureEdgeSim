package com.Mechalikh.PureEdgeSim.DataCentersManager;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.vms.Vm;

import com.Mechalikh.PureEdgeSim.LocationManager.Location;
import com.Mechalikh.PureEdgeSim.LocationManager.MobilityItem;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimLog;
import com.Mechalikh.PureEdgeSim.TasksOrchestration.VmTaskMapItem; 

public class EdgeDataCenter extends DatacenterSimple {
	private static final int UPDATE_STATUS = 99;
	private Simulation simulation; 
	private SimulationParameters.TYPES type;// the type of this datacenter : cloud datacenter/ fog datacenter/ edge device
	private Location location;// the location of the device.
	private boolean Mobile = false;// the device is mobile or fixed.
	private boolean battery = false;// the device relies battery or power supply.
	private double batteryCapacity;// The capacity of the battery, if the device has one 
	private double idleConsumption;
	private double maxConsumption;
	private double consumption = 0;
	private boolean died = false;
	private double deathTime;  
	private List<MobilityItem> locationChanges; 
	private List<VmTaskMapItem> vmTaskMap;
	private SimLog simLog;
	private int app;
	private boolean isOrchestrator=false; 
	private long Memory; 
	private long AvailableMemory; 
 
	private double Utilization=0;
	private int Count=0; 
	public EdgeDataCenter(Simulation simulation, List<? extends Host> hostList, VmAllocationPolicy vmAllocationPolicy) {
		super(simulation, hostList, vmAllocationPolicy);
		this.simulation = simulation;
		vmTaskMap=new ArrayList<VmTaskMapItem>();
		long memory=0;
		for(int i=0;i<hostList.size();i++) {
			memory+=hostList.get(i).getStorage().getAvailableResource();
		}
		setMemory(memory);
	}
	
	@Override
	protected void startEntity() {
		super.startEntity();
		schedule(this, 1, UPDATE_STATUS);
	}

	@Override
	public void processEvent(final SimEvent ev) {
		switch (ev.getTag()) {
		case UPDATE_STATUS:
			// update energy consumption 
			consumption += idleConsumption;
			double vmUsage = 0;
			List<EdgeVM> vmList = this.getVmList();
			for (int i = 0; i < vmList.size(); i++) { 
				vmUsage = vmList.get(i).getCloudletScheduler().getRequestedCpuPercentUtilization(simulation.clock());
				Count++;
				Utilization +=vmUsage; 
				consumption += vmUsage * maxConsumption*SimulationParameters.VM_UPDATE_INTERVAL / (double) vmList.size();
				if (isBattery() && consumption > batteryCapacity) {
					died = true;
					deathTime=simulation.clock();
				}
			}
			// Update location
			if (isMobile() && locationChanges!=null) {
				if ( locationChanges.size()>0 && simulation.clock() == locationChanges.get(0).getTime()) {
					// Updating Location
					setLocation(locationChanges.get(0).getLocation());
					
					//now we need to find the vm to migrate (the vm which is executing this devices task)
				/*	EdgeVM vmToMigrate=findVm();	 
					if(vmToMigrate!=null) {
					if(SimulationParameters.VM_MIGRATION && vmToMigrate.getType()==SimulationParameters.TYPES.FOG) {
					    //the fog host where the device is connected now, after location changed
						Host des=locationChanges.get(0).getFogHost();
						
						//which is the host/datacenter where the vm should be migrated
						simLog.incVmMigrationAttempt();					
						
						//compute migration delay
						double delay=timeToMigrateVm(vmToMigrate,des);
						
						//add the delay to the network usage
						simLog.addVmMigrationNetworkUsage(delay);
						
						//update the power consumption
						//counted two times (x2) , one for emission and one for reception
						simLog.addVmMigrationPowerConsumption(2*vmToMigrate.getRam().getCapacity()*SimulationParameters.POWER_CONS_PER_MEGABYTE);
				
						//migrate the vm
						MapVmHost<EdgeVM,Host> map=new MapVmHost<EdgeVM,Host>(vmToMigrate,des);
						send(des.getDatacenter(),delay,CloudSimTags.VM_MIGRATE, map); 
						
						}
					}  */
	 
					locationChanges.remove(0); //remove the item after using it
				}
			}
			if (!isDead()) {
				schedule(this, SimulationParameters.VM_UPDATE_INTERVAL, UPDATE_STATUS);
			}

			break;
		default:
			super.processEvent(ev);
			break;
		}
	}

	private EdgeVM findVm() {
		for (int i=0;i<vmTaskMap.size();i++) {
			if (vmTaskMap.get(i).getTask().getTime()<simulation.clock()) { //task executed or it is being executing
				if(vmTaskMap.get(i).getTask().isFinished()) {
					//the task is still in execution
					return (EdgeVM) vmTaskMap.get(i).getTask().getVm();
				}
			}
		}
		return null;
	}

	public SimulationParameters.TYPES getType() {
		return type;
	}

	public void setType(SimulationParameters.TYPES type) {
		this.type = type;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public boolean isMobile() {
		return Mobile;
	}

	public void setMobile(boolean mobile) {
		Mobile = mobile;
	}

	public boolean isBattery() {
		return battery;
	}

	public void setBattery(boolean battery) {
		this.battery = battery;
	}

	public double getBatteryCapacity() {
		return batteryCapacity;
	}

	public void setBatteryCapacity(double batteryCapacity) {
		this.batteryCapacity = batteryCapacity; 
	}

	public double getBatteryLevel() {
		if(!isBattery())return 0;
		if(batteryCapacity<consumption) return 0;
		return batteryCapacity-consumption;
	}

	 
	public double getBatteryLevelPercentage() {
		if(!isBattery())return 0;
		if(batteryCapacity<consumption) return 0;
		return (batteryCapacity-consumption) * 100 / batteryCapacity;
	}

	public double getIdleConsumption() {
		return idleConsumption;
	}

	public void setIdleConsumption(double idleConsumption) {
		this.idleConsumption = idleConsumption;
	}

	public double getMaxConsumption() {
		return maxConsumption;
	}

	public void setMaxConsumption(double maxConsumption) {
		this.maxConsumption = maxConsumption;
	}

	public double getConsumption() {
		return consumption;
	}

	public void addConsumption(double cons) {
		this.consumption += cons;
	}

	public boolean isDead() {
		return died;
	}

	public void setLocationChanges(List<MobilityItem> locationChanges) {
		this.locationChanges = locationChanges;

	}

	public List<MobilityItem> getLocationChanges() {
		return locationChanges;

	}

	public double getDeathTime() {
		return deathTime;
	}

 
	public List<VmTaskMapItem> getVmTaskMap() {
	 
		return vmTaskMap;
	}

	 private double timeToMigrateVm(final Vm vm, final Host targetHost) {
	        return vm.getRam().getCapacity() / Conversion.bitesToBytes(targetHost.getBw().getCapacity() * getBandwidthPercentForMigration());
	    }

	public void setLogger(SimLog simLog) {
		this.simLog=simLog;
		
	}

	public void setApplication(int app) {
		this.app=app;
		
	}
	public int getApplication() {
		return app;
		
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

	 
	public long getMemory() {
		return Memory;
	}

	public void setMemory(long memory) {
		Memory = memory;
		setAvailableMemory(Memory);  
	}

	public long getAvailableMemory() {
		return AvailableMemory;
	}

	public void setAvailableMemory(long availableMemory) {
		AvailableMemory = availableMemory;
	}
 
  
	public double getUtilization() {
		if(Count==0) Count=1; 
		return Utilization*100/Count;
	} 
}
