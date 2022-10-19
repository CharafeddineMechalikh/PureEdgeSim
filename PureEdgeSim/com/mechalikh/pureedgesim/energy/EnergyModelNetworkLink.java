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

import com.mechalikh.pureedgesim.network.NetworkLink;

/**
 * The linear power model for network links. It implements the Null Object
 * Design Pattern in order to start avoiding {@link NullPointerException} when using the
 * NULL object instead of attributing null to EnergyModelNetworkLink variables.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class EnergyModelNetworkLink {

	/**
	 * The network link to monitor.
	 */
	protected NetworkLink link;

	/**
	 * The amount of energy consumed by each bit of transmitted data.
	 */
	protected double energyPerBit;

	/**
	 * An attribute that implements the Null Object Design Pattern to avoid {@link NullPointerException} when using the
	 * NULL object instead of attributing null to EnergyModelNetworkLink variables.
	 */
	public static final EnergyModelNetworkLink NULL = new EnergyModelNetworkLinkNull();

	public EnergyModelNetworkLink(final double energyPerBit, NetworkLink link) {
		this.energyPerBit = energyPerBit;
		this.link = link;
	}

	/**
	 * Gets the corresponding network link.
	 * @return The network link
	 */
	public NetworkLink getLink() {
		return link;
	}

	/**
	 * Gets the consumed energy at this instant.
	 * @return The current energy consumption
	 */
	public double getCurrentEnergyConsumption() {
		return getEnergyPerBit() * (getLink().getUsedBandwidth());
	}

	/**
	 * Gets the total consumed energy since the beginning of the simulation.
	 * @return The total consumed energy
	 */
	public double getTotalEnergyConsumption() {
		return getEnergyPerBit() * (getLink().getTotalTransferredData());
	}

	/**
	 * Gets The amount of energy consumed by each bit of transmitted data.
	 * @return The energy consumed per each bit of transferred data
	 */
	public double getEnergyPerBit() {
		return energyPerBit;
	}

}
