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
package com.mechalikh.pureedgesim.network;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters; 
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * A cellular uplink, from a device to a base station. Its technology is set by the
 * {@code cellular_*} parameters.
 */
public class NetworkLinkCellularUp extends NetworkLinkCellular {

	/**
	 * Creates a cellular uplink from a device to a base station.
	 *
	 * @param src               the device.
	 * @param dst               the base station.
	 * @param simulationManager the simulation manager.
	 * @param type              the link category.
	 */
	public NetworkLinkCellularUp(ComputingNode src, ComputingNode dst, SimulationManager simulationManager, NetworkLinkTypes type) {
		super(src, dst, simulationManager, type);
		double energyConsumption = SimulationParameters.cellularDeviceTransmissionWattHourPerBit
				+ SimulationParameters.cellularBaseStationWattHourPerBitUpLink;
		if (type == NetworkLinkTypes.WAN) {
			energyConsumption += SimulationParameters.wanWattHourPerBit;
			setLatency(SimulationParameters.wanLatency);
			setBandwidth(Math.min(SimulationParameters.wanBandwidthBitsPerSecond, SimulationParameters.cellularBandwidthBitsPerSecond));
		}
		setEnergyModel(new EnergyModelNetworkLink(energyConsumption, this));
	}

}
