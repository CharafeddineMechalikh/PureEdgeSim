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
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

/*
 * The adopted energy model can be found in the paper
 * Abidi, W., Lirathni, H., & Ezzedine, T. (2017). 
 * EEZC : Energy Efficient Zone based Clustering protocol for Heterogeneous Wireless Sensor Networks.
 */
public class DefaultEnergyModel extends EnergyModel {
	public static final int TRANSMISSION = 0;
	public static final int RECEPTION = 1;
	// The power consumption for each transferred bit (in joul per bit : J/bit)
	private double E_elec = SimulationParameters.CONSUMED_ENERGY_PER_BIT;

	// Energy consumption of the transmit amplifier in free space channel model ( in
	// joul per bit per meter^2 : J/bit/m^2)
	private double E_fs = SimulationParameters.AMPLIFIER_DISSIPATION_FREE_SPACE;

	// Energy consumption of the transmit amplifier in multipath fading channel
	// model ( in joul per bit per meter^4 : J/bit/m^4)
	private double E_mp = SimulationParameters.AMPLIFIER_DISSIPATION_MULTIPATH;

	public DefaultEnergyModel(double maxActiveConsumption, double idleConsumption) {
		super(maxActiveConsumption, idleConsumption);
	}

	public void updateCpuEnergyConsumption(double cpuUtilization) {
		cpuEnergyConsumption += (getIdleConsumption() + ((getMaxActiveConsumption() - getIdleConsumption()) * cpuUtilization)) / 3600
				* SimulationParameters.UPDATE_INTERVAL; // the energy consumption value is for 1 hour, it will be
														// divided by 3600 to get how much each second costs
	}

	public void updatewirelessEnergyConsumption(FileTransferProgress file, DataCenter device1, DataCenter device2,
			int flag) {

		double distance;
		if (device1.getType() == TYPES.CLOUD || device2.getType() == TYPES.CLOUD
				|| device1.getType() == TYPES.EDGE_DATACENTER || device2.getType() == TYPES.EDGE_DATACENTER)
			distance = SimulationParameters.EDGE_DATACENTERS_RANGE;
		else
			distance = device1.getMobilityManager().distanceTo(device1);

		int sizeInBits = (int) (file.getFileSize() * 1000);

		if (flag == RECEPTION)
			receptionEnergyConsumption(sizeInBits);
		else
			transmissionEnergyConsumption(sizeInBits, distance);
	}

	private void transmissionEnergyConsumption(int sizeInBits, double distance) {
		double consumption = 0;

		// Distance threshold that determines the multipath and free space choices.
		double D_0 = Math.sqrt(E_fs / E_mp); 
		if (distance <= D_0)
			consumption = (E_elec * sizeInBits) + (E_fs * Math.pow(distance, 2) * sizeInBits);
		else if (distance > D_0)
			consumption = (E_elec * sizeInBits) + (E_mp * Math.pow(distance, 4) * sizeInBits);
		
		this.wirelessEnergyConsumption += joulToWattHour(consumption);
	}

	private double joulToWattHour(double consumption) {
		return consumption / 3600.0;
	}

	private void receptionEnergyConsumption(int sizeInBits) {
		double consumption = (E_elec * sizeInBits);
		this.wirelessEnergyConsumption += joulToWattHour(consumption);
	}

}
