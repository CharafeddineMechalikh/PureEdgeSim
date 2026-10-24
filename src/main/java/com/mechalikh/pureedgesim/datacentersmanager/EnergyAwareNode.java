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

public abstract class EnergyAwareNode extends NetworkingNode {
	protected EnergyModelComputingNode energyModel = EnergyModelComputingNode.NULL;
	protected boolean isDead = false;
	protected double deathTime;

	protected EnergyAwareNode(SimulationManager simulationManager) {
		super(simulationManager);
	}

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

	public boolean isDead() {
		return isDead;
	}

	public double getDeathTime() {
		return deathTime;
	}

	protected void setDeath(Boolean dead, double time) {
		isDead = dead;
		deathTime = time;
	}

	public EnergyModelComputingNode getEnergyModel() {
		return energyModel;
	}

	public void setEnergyModel(EnergyModelComputingNode energyModel) {
		this.energyModel = energyModel;
	}

}
