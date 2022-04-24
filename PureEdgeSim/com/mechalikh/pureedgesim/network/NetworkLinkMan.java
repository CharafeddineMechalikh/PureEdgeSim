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
public class NetworkLinkMan extends NetworkLink {

	public NetworkLinkMan(ComputingNode src, ComputingNode dst, SimulationManager simulationManager, NetworkLinkTypes type) {
		super(src, dst, simulationManager, type);
		setBandwidth(SimulationParameters.MAN_BANDWIDTH_BITS_PER_SECOND);
		setLatency(SimulationParameters.MAN_LATENCY); 
		setEnergyModel(new EnergyModelNetworkLink(SimulationParameters.MAN_WATTHOUR_PER_BIT, this));
	}

}
