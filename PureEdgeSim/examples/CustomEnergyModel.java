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
package examples;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.datacentersmanager.EnergyModel;
import com.mechalikh.pureedgesim.network.FileTransferProgress;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

public class CustomEnergyModel extends EnergyModel {

	/*
	 * This class is an example of how to create a custom energy model The class
	 * needs to be inherited form the EnergyModel class.
	 */
	public CustomEnergyModel(double maxActiveConsumption, double idleConsumption) {
		super(maxActiveConsumption, idleConsumption);
	}

	@Override
	public void updateCpuEnergyConsumption(double cpuUtilization) {
		// Here we update the cpu consumption.
		// if the CPU operates at 100% it will consumes the amount of
		// maxActiveConsumption.
		// the energy consumed each second will be between [idleConsumption,
		// maxActiveConsumption],
		// for any cpu usage [0-->100%].
		// so 0% will give the idle consumption,
		// and 100% will give the max consumption (we have to avoid counting the idle
		// consumption twice).
		// the idleConsumption and maxActiveConsumption values are for 1 hour, which
		// need to be divided by 3600 to get the values for 1 second.
		// the update unterval can be < 1 second (e.g., 0.1) or > 1 second (e.g., 2
		// seconds). so we need to multiply the results by the update interval in order
		// to get the correct measuring
		double consumedEnergy = (idleConsumption + ((maxActiveConsumption - idleConsumption) * cpuUtilization)) / 3600
				* SimulationParameters.UPDATE_INTERVAL; // the energy consumption value is for 1 hour, it will be
														// divided by 3600 to get how much each second costs

		// the cpu and energy update interval ( see the update_interval in simulation
		// parameters) can be set to any value , for example 0.1 second
		// however the conusmed energy that we calculated is for 1 seconds,
		// we need to multiply it by the interval in order to get the correct
		// consumption

		consumedEnergy = consumedEnergy * SimulationParameters.UPDATE_INTERVAL;

		// add the results to the consumed energy
		this.cpuEnergyConsumption += consumedEnergy;
	}

	@Override
	public void updatewirelessEnergyConsumption(FileTransferProgress file, DataCenter device1, DataCenter device2,
			int flag) {
		// Here things gets complicated
		// to simplify this example, let's assume that's every transferred Kilobyte will
		// consume 0.0000001 Wh of energy
		// the file size is in Kbits, it needs to be converted to Bytes ( divided by 8)
		double filesize = file.getFileSize() / 8;

		// then the energy consumed during the transfer of this file is
		this.wirelessEnergyConsumption += filesize * 0.0000001;

		// of course this model is oversimplified
		// for a more reaslitic model, please check the default PureEdgeSim Energy Model
		// (DefaultEnergyModel)
	}

}
