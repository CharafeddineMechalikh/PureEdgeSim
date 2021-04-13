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

import com.mechalikh.pureedgesim.network.FileTransferProgress;

public abstract class EnergyModel {
	protected double maxActiveConsumption; // consumed energy per second when the cpu is operating at 100%
	protected double idleConsumption;
	protected double cpuEnergyConsumption = 0;
	protected double wirelessEnergyConsumption = 0;
	protected double batteryCapacity;
	protected boolean isBatteryPowered = false;

	public EnergyModel(double maxActiveConsumption, double idleConsumption) {
		this.setMaxActiveConsumption(maxActiveConsumption);
		this.setIdleConsumption(idleConsumption);
	}

	public abstract void updateCpuEnergyConsumption(double cpuUtilization);

	public abstract void updatewirelessEnergyConsumption(FileTransferProgress file, DataCenter device1,
			DataCenter device2, int flag);

	public double getTotalEnergyConsumption() {
		return getWirelessEnergyConsumption() + getCpuEnergyConsumption();
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

	public double getBatteryCapacity() {
		return batteryCapacity;
	}

	public void setBatteryCapacity(double batteryCapacity) {
		this.batteryCapacity = batteryCapacity;
	}

	public double getBatteryLevel() {
		if (!isBatteryPowered())
			return 100;
		if (getBatteryCapacity() < getTotalEnergyConsumption())
			return 0;
		return getBatteryCapacity() - getTotalEnergyConsumption();
	}

	public double getBatteryLevelPercentage() {
		return getBatteryLevel() * 100 / getBatteryCapacity();
	}

	public boolean isBatteryPowered() {
		return isBatteryPowered;
	}

	public void setBattery(boolean battery) {
		this.isBatteryPowered = battery;
	}

}
