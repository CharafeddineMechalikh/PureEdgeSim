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

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

/**
 * The linear power model for computing nodes. It implements the Null Object
 * Design Pattern in order to start avoiding {@link NullPointerException} when
 * using the NULL object instead of attributing null to EnergyModelNetworkLink
 * variables.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class EnergyModelComputingNode {
	/** Power consumed when the CPU operates at 100% load, in watts. */
	protected double maxActiveConsumption;
	/** Power consumed when the CPU is idle, in watts. */
	protected double idleConsumption;
	/** Energy consumed by the CPU so far, in watt-hours. */
	protected double cpuEnergyConsumption = 0;
	/** Battery capacity, in watt-hours. */
	protected double batteryCapacity;
	/** Initial battery level as a fraction of the capacity, between 0 and 1. */
	protected double initialBatteryLevel = 1;
	/** Connectivity type used to select the network energy rates. */
	protected String connectivity;
	/** Whether this node runs on a battery that can be depleted. */
	protected boolean isBatteryPowered = false;
	/** Flag passed to {@link #updatewirelessEnergyConsumption(double, int)} for sent data. */
	public static final int TRANSMISSION = 0;
	/** Flag passed to {@link #updatewirelessEnergyConsumption(double, int)} for received data. */
	public static final int RECEPTION = 1;

	/**
	 * An attribute that implements the Null Object Design Pattern to avoid
	 * {@link NullPointerException} when using the NULL object instead of
	 * attributing null to EnergyModelComputingNode variables.
	 */
	public static final EnergyModelComputingNode NULL = EnergyModelComputingNodeNull.getInstance();
	/** Energy consumed by network transmissions and receptions so far, in watt-hours. */
	protected double networkEnergyConsumption;
	/** Transmission energy rate for the current connectivity, in watt-hours per bit. */
	protected double transmissionEnergyPerBits;
	/** Reception energy rate for the current connectivity, in watt-hours per bit. */
	protected double receptionEnergyPerBits;

	/**
	 * Creates a power model with the given power ratings.
	 *
	 * @param maxActiveConsumption power consumed when the CPU operates at 100%
	 *                             load, in watts
	 * @param idleConsumption      power consumed when the CPU is idle, in watts
	 */
	public EnergyModelComputingNode(double maxActiveConsumption, double idleConsumption) {
		this.setMaxActiveConsumption(maxActiveConsumption);
		this.setIdleConsumption(idleConsumption);
	}

	/**
	 * Adds the energy consumed while the node is idle during one update interval.
	 * Called at every simulation step for nodes that are not executing a task.
	 */
	public void updateStaticEnergyConsumption() {
		cpuEnergyConsumption += getIdleConsumption() / 3600 * SimulationParameters.updateInterval;
	}

	/**
	 * Returns the energy consumed by the CPU so far.
	 *
	 * @return the CPU energy consumption in watt-hours
	 */
	public double getCpuEnergyConsumption() {
		return cpuEnergyConsumption;
	}

	/**
	 * Returns the total energy consumed so far, including CPU and network usage.
	 *
	 * @return the total energy consumption in watt-hours
	 */
	public double getTotalEnergyConsumption() {
		return cpuEnergyConsumption + networkEnergyConsumption;
	}

	/**
	 * Returns the power consumed when the CPU operates at 100% load.
	 *
	 * @return the maximum active power in watts
	 */
	public double getMaxActiveConsumption() {
		return maxActiveConsumption;
	}

	/**
	 * Sets the power consumed when the CPU operates at 100% load.
	 *
	 * @param maxActiveConsumption the maximum active power in watts
	 */
	public void setMaxActiveConsumption(double maxActiveConsumption) {
		this.maxActiveConsumption = maxActiveConsumption;
	}

	/**
	 * Returns the power consumed when the CPU is idle.
	 *
	 * @return the idle power in watts
	 */
	public double getIdleConsumption() {
		return idleConsumption;
	}

	/**
	 * Sets the power consumed when the CPU is idle.
	 *
	 * @param idleConsumption the idle power in watts
	 */
	public void setIdleConsumption(double idleConsumption) {
		this.idleConsumption = idleConsumption;
	}

	/**
	 * Returns the battery capacity.
	 *
	 * @return the battery capacity in watt-hours
	 */
	public double getBatteryCapacity() {
		return batteryCapacity;
	}

	/**
	 * Sets the battery capacity.
	 *
	 * @param batteryCapacity the battery capacity in watt-hours
	 */
	public void setBatteryCapacity(double batteryCapacity) {
		this.batteryCapacity = batteryCapacity;
	}

	/**
	 * Returns the remaining battery energy. The level is clamped to zero once the
	 * initial energy has been fully consumed.
	 *
	 * @return the remaining energy in watt-hours, or -1 when the node is not
	 *         battery powered
	 */
	public double getBatteryLevelWattHour() {
		if (!isBatteryPowered())
			return -1;
		if (getBatteryCapacity() * this.initialBatteryLevel < getTotalEnergyConsumption())
			return 0;
		return (getBatteryCapacity() * this.initialBatteryLevel) - getTotalEnergyConsumption();
	}

	/**
	 * Returns the remaining battery energy as a percentage of the battery
	 * capacity. The result is only meaningful for battery-powered nodes.
	 *
	 * @return the remaining battery level as a percentage
	 */
	public double getBatteryLevelPercentage() {
		return getBatteryLevelWattHour() * 100 / getBatteryCapacity();
	}

	/**
	 * Returns whether this node runs on a battery.
	 *
	 * @return {@code true} if the node is battery powered
	 */
	public boolean isBatteryPowered() {
		return isBatteryPowered;
	}

	/**
	 * Sets whether this node runs on a battery.
	 *
	 * @param battery {@code true} for a battery-powered node
	 */
	public void setBattery(boolean battery) {
		this.isBatteryPowered = battery;
	}

	/**
	 * Sets the initial battery level, which is the starting point from which the
	 * consumed energy is subtracted.
	 *
	 * @param batteryLevel the initial level as a percentage of the battery
	 *                     capacity, between 0 and 100
	 */
	public void setIntialBatteryPercentage(double batteryLevel) {
			this.initialBatteryLevel = batteryLevel / 100.0;
	}

	/**
	 * Returns the connectivity type used to select the network energy rates.
	 *
	 * @return the connectivity type
	 */
	public String getConnectivityType() {
		return connectivity;
	}

	/**
	 * Sets the connectivity type and the corresponding per-bit transmission and
	 * reception energy rates. The values {@code "cellular"} and {@code "wifi"} are
	 * recognized (case sensitive); any other value splits the Ethernet energy rate
	 * equally between transmission and reception.
	 *
	 * @param connectivity the connectivity type
	 */
	public void setConnectivityType(String connectivity) {
		this.connectivity = connectivity;

		if ("cellular".equals(connectivity)) {
			transmissionEnergyPerBits = SimulationParameters.cellularDeviceTransmissionWattHourPerBit;
			receptionEnergyPerBits = SimulationParameters.cellularDeviceReceptionWattHourPerBit;
		} else if ("wifi".equals(connectivity)) {
			transmissionEnergyPerBits = SimulationParameters.wifiDeviceTransmissionWattHourPerBit;
			receptionEnergyPerBits = SimulationParameters.wifiDeviceReceptionWattHourPerBit;
		} else {
			transmissionEnergyPerBits = SimulationParameters.ethernetWattHourPerBit / 2;
			receptionEnergyPerBits = SimulationParameters.ethernetWattHourPerBit / 2;
		}
	}

	/**
	 * Adds the network energy consumed by a transmission or a reception. The rate
	 * applied for {@link #RECEPTION} is the transmission rate defined for the
	 * current connectivity, and the reception rate is applied for
	 * {@link #TRANSMISSION}.
	 *
	 * @param sizeInBits the amount of data transferred, in bits
	 * @param flag       {@link #TRANSMISSION} or {@link #RECEPTION}
	 */
	public void updatewirelessEnergyConsumption(double sizeInBits, int flag) {
		if (flag == RECEPTION)
			networkEnergyConsumption += sizeInBits * transmissionEnergyPerBits;
		else
			networkEnergyConsumption += sizeInBits * receptionEnergyPerBits;
	}

	/**
	 * Adds the energy consumed by the CPU while executing a task. The active power
	 * above the idle power is scaled by the time the task occupies the CPU.
	 *
	 * @param length       the amount of work executed, in million instructions
	 * @param mipsCapacity the CPU capacity used for the execution, in million
	 *                     instructions per second
	 */
	public void updateDynamicEnergyConsumption(double length, double mipsCapacity) {
		cpuEnergyConsumption += ((getMaxActiveConsumption() - getIdleConsumption()) / 3600 * length / mipsCapacity);
	}

}
