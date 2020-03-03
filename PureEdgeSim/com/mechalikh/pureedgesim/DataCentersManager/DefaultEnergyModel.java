package com.mechalikh.pureedgesim.DataCentersManager;

import com.mechalikh.pureedgesim.Network.FileTransferProgress;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters.TYPES;

/*
 * The adopted energy model can be found in the paper
 * Abidi, W., Lirathni, H., & Ezzedine, T. (2017). 
 * EEZC : Energy Efficient Zone based Clustering protocol for Heterogeneous Wireless Sensor Networks.
 */
public class DefaultEnergyModel extends EnergyModel {
	public static final int TRANSMISSION = 0;
	public static final int RECEPTION = 1;
	// The power consumption for each transferred bit (in joul per bit : J/bit)
	private double E_elec = simulationParameters.CONSUMED_ENERGY_PER_BIT;

	// Energy consumption of the transmit amplifier in free space channel model ( in
	// joul per bit per meter^2 : J/bit/m^2)
	private double E_fs = simulationParameters.AMPLIFIER_DISSIPATION_FREE_SPACE;

	// Energy consumption of the transmit amplifier in multipath fading channel
	// model ( in joul per bit per meter^4 : J/bit/m^4)
	private double E_mp = simulationParameters.AMPLIFIER_DISSIPATION_MULTIPATH;

	public DefaultEnergyModel(double maxActiveConsumption, double idleConsumption) {
		super(maxActiveConsumption, idleConsumption);
	}

	public void updateCpuEnergyConsumption(double cpuUtilization) {
		double consumption = (idleConsumption + ((maxActiveConsumption - idleConsumption) * cpuUtilization)) / 3600
				* simulationParameters.UPDATE_INTERVAL; // the energy consumption value is for 1 hour, it will be
														// divided by 3600 to get how much each second costs
		this.cpuEnergyConsumption += consumption;
	}

	public void updatewirelessEnergyConsumption(FileTransferProgress file, DataCenter device1, DataCenter device2,
			int flag) {

		double distance;
		if (device1.getType() == TYPES.CLOUD || device2.getType() == TYPES.CLOUD
				|| device1.getType() == TYPES.EDGE_DATACENTER || device2.getType() == TYPES.EDGE_DATACENTER) {
			distance = 10;
		} else {
			distance = Math
					.abs(Math
							.sqrt(Math
									.pow((device1.getMobilityManager().getCurrentLocation().getXPos()
											- device2.getMobilityManager().getCurrentLocation().getXPos()), 2)
									+ Math.pow(
											(device1.getMobilityManager().getCurrentLocation().getYPos()
													- device2.getMobilityManager().getCurrentLocation().getYPos()),
											2)));
		}

		int sizeInBits = (int) (file.getFileSize() * 1000);

		if (flag == RECEPTION)
			receptionEnergyConsumption(sizeInBits);
		else
			transmissionEnergyConsumption(sizeInBits, distance);
	}

	private void transmissionEnergyConsumption(int sizeInBits, double distance) {
		double consumption = 0;

		// distance threshold that determines the multipath and free space choices.
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
