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
 * A 4G LTE network link.
 */
public class NetworkLinkCellularDown extends NetworkLinkCellular {

	public NetworkLinkCellularDown(ComputingNode src, ComputingNode dst, SimulationManager simulationManager, NetworkLinkTypes type) {
		super(src, dst, simulationManager, type);

		double energyConsumption = SimulationParameters.CELLULAR_DEVICE_RECEPTION_WATTHOUR_PER_BIT
				+ SimulationParameters.CELLULAR_BASE_STATION_WATTHOUR_PER_BIT_DOWN_LINK;
		if (type == NetworkLinkTypes.WAN) {
			energyConsumption += SimulationParameters.WAN_WATTHOUR_PER_BIT;
			setLatency(SimulationParameters.WAN_LATENCY);
			setBandwidth(Math.min(SimulationParameters.WAN_BANDWIDTH_BITS_PER_SECOND, SimulationParameters.CELLULAR_BANDWIDTH_BITS_PER_SECOND));
		}
		setEnergyModel(new EnergyModelNetworkLink(energyConsumption, this));
	}

	
}
