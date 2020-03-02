package com.mechalikh.pureedgesim.DataCentersManager;

import com.mechalikh.pureedgesim.Network.FileTransferProgress; 
public abstract class EnergyModel { 
	protected double maxActiveConsumption; // consumed energy per second when the cpu is operating at 100%
	protected double idleConsumption;
	protected double cpuEnergyConsumption = 0;
	protected double wirelessEnergyConsumption = 0; 
 
	public EnergyModel(double maxActiveConsumption, double idleConsumption) {
		this.setMaxActiveConsumption(maxActiveConsumption);
		this.setIdleConsumption(idleConsumption);
	}

	public abstract void updateCpuEnergyConsumption(double cpuUtilization);

	public  abstract void updatewirelessEnergyConsumption(FileTransferProgress file, DataCenter device1,
			DataCenter device2, int flag);
 

	public double getTotalEnergyConsumption() {
		return this.wirelessEnergyConsumption + this.cpuEnergyConsumption;
	}

	public double getMaxActiveConsumption() {
		return maxActiveConsumption;
	}

	public void setMaxActiveConsumption(double maxActiveConsumption) {
		this.maxActiveConsumption = maxActiveConsumption;
	}

	public double getIdleConsumption() {
		return idleConsumption;
	}

	public void setIdleConsumption(double idleConsumption) {
		this.idleConsumption = idleConsumption;
	}

	public double getCpuEnergyConsumption() {
		return cpuEnergyConsumption;
	}

	public double getWirelessEnergyConsumption() {
		return wirelessEnergyConsumption;
	}

}
