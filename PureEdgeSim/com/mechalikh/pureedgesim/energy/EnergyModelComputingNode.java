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
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.energy;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

/**
 * A class that computes the amount of energy was consumed by computing nodes.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */

public class EnergyModelComputingNode {
	protected double maxActiveConsumption; // Consumed energy when the cpu is operating at 100% in Watt
	protected double idleConsumption; // Consumed energy when idle (in Watt)
	protected double cpuEnergyConsumption = 0;
	protected double batteryCapacity;
	protected String connectivity;
	protected boolean isBatteryPowered = false;

	public static final int TRANSMISSION = 0; // used to update edge devices batteries
	public static final int RECEPTION = 1;
	protected double networkEnergyConsumption;
	protected double transmissionEnergyPerBits;
	protected double receptionEnergyPerBits;

	public EnergyModelComputingNode(double maxActiveConsumption, double idleConsumption) {
		this.setMaxActiveConsumption(maxActiveConsumption);
		this.setIdleConsumption(idleConsumption);
	}

	public void updateStaticEnergyConsumption() {
		cpuEnergyConsumption += getIdleConsumption() / 3600 * SimulationParameters.UPDATE_INTERVAL;
	}

	public double getTotalEnergyConsumption() {
		return cpuEnergyConsumption;
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
		return getBatteryCapacity() - (getTotalEnergyConsumption() + networkEnergyConsumption);
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

	public String getConnectivityType() {
		return connectivity;
	}

	public void setConnectivityType(String connectivity) {
		this.connectivity = connectivity;

		if ("cellular".equals(connectivity)) {
			transmissionEnergyPerBits = SimulationParameters.CELLULAR_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT;
			receptionEnergyPerBits = SimulationParameters.CELLULAR_DEVICE_RECEPTION_WATTHOUR_PER_BIT;
		} else if ("wifi".equals(connectivity)) {
			transmissionEnergyPerBits = SimulationParameters.WIFI_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT;
			receptionEnergyPerBits = SimulationParameters.WIFI_DEVICE_RECEPTION_WATTHOUR_PER_BIT;
		} else {
			transmissionEnergyPerBits = SimulationParameters.ETHERNET_WATTHOUR_PER_BIT / 2;
			receptionEnergyPerBits = SimulationParameters.ETHERNET_WATTHOUR_PER_BIT / 2;
		}
	}

	public void updatewirelessEnergyConsumption(double sizeInBits, ComputingNode origin, ComputingNode destination,
			int flag) {
		if (flag == RECEPTION)
			networkEnergyConsumption += sizeInBits * transmissionEnergyPerBits;
		else
			networkEnergyConsumption += sizeInBits * receptionEnergyPerBits;

	}

	public void updateDynamicEnergyConsumption(double length, double mipsCapacity) {
		cpuEnergyConsumption += ((getMaxActiveConsumption() - getIdleConsumption()) / 3600 * length / mipsCapacity);
	}

}
