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

/**
 * A class that implements the Null Object Design Pattern for the
 * {@link EnergyModelComputingNode} class. Needed to avoid
 * {@link NullPointerException} when using the NULL object instead of
 * attributing null to EnergyModelComputingNode variables.
 */
public class EnergyModelComputingNodeNull extends EnergyModelComputingNode {

	public EnergyModelComputingNodeNull(double maxActiveConsumption, double idleConsumption) {
		super(maxActiveConsumption, idleConsumption);
	}

	@Override
	public double getTotalEnergyConsumption() {
		return 0;
	}

	@Override
	public double getMaxActiveConsumption() {
		return 0;
	}

	@Override
	public double getIdleConsumption() {
		return 0;
	}

	@Override
	public double getBatteryCapacity() {
		return 0;
	}

	@Override
	public double getBatteryLevelWattHour() {
		return 0;
	}

	@Override
	public double getBatteryLevelPercentage() {
		return 0;
	}

	@Override
	public boolean isBatteryPowered() {
		return false;
	}

	@Override
	public String getConnectivityType() {
		return "";
	}

}
