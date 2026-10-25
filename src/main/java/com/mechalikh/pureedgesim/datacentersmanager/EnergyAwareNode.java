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
package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode; 
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * A networking node with an energy model that can die when its battery is
 * depleted.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 2.0
 */
public abstract class EnergyAwareNode extends NetworkingNode {
	/** The energy model used to track power consumption and battery level. */
	protected EnergyModelComputingNode energyModel = EnergyModelComputingNode.NULL;
	/** Whether this node is dead and can no longer execute or receive tasks. */
	protected boolean isDead = false;
	/** Simulation time at which the node died, in seconds. */
	protected double deathTime;

	/**
	 * Creates an energy-aware node.
	 *
	 * @param simulationManager the simulation manager
	 */
	protected EnergyAwareNode(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Adds idle energy consumption for the current update interval and marks the
	 * node as dead when a battery-powered node reaches an empty battery. Dynamic
	 * energy consumption is accounted for separately when tasks execute.
	 */
	@Override
	protected void updateStatus() {
		// Check if the device is dead
		if (isDead())
			return;
		// Update the static energy consumption, the dynamic one is measure separately
		// in DefaultComputingNode.startExecution() for performance and accuracy reasons
		getEnergyModel().updateStaticEnergyConsumption();

		if (getEnergyModel().isBatteryPowered() && getEnergyModel().getBatteryLevelWattHour() <= 0) {
			setDeath(true, simulationManager.getSimulation().clock());
		}

	}

	/**
	 * Returns whether this node is dead.
	 *
	 * @return {@code true} if the node has died
	 */
	public boolean isDead() {
		return isDead;
	}

	/**
	 * Returns the simulation time at which the node died.
	 *
	 * @return the death time in seconds, or 0 if the node has not died
	 */
	public double getDeathTime() {
		return deathTime;
	}

	/**
	 * Marks the node as dead or alive and records the time of death.
	 *
	 * @param dead whether the node is dead
	 * @param time the simulation time of death, in seconds
	 */
	protected void setDeath(Boolean dead, double time) {
		if (dead && !isDead)
			simulationManager.getSimulationLogger().incrementDeadDevices();
		isDead = dead;
		deathTime = time;
	}

	/**
	 * Returns the energy model of this node.
	 *
	 * @return the energy model
	 */
	public EnergyModelComputingNode getEnergyModel() {
		return energyModel;
	}

	/**
	 * Sets the energy model of this node.
	 *
	 * @param energyModel the energy model
	 */
	public void setEnergyModel(EnergyModelComputingNode energyModel) {
		this.energyModel = energyModel;
	}

}
