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
package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;

/**
 * A class that implements the Null Object Design Pattern for the
 * {@link MobilityModel} class. Needed to avoid {@link NullPointerException}
 * when using the NULL object instead of attributing null to MobilityModel
 * variables.
 */
public class MobilityModelNull extends MobilityModel {

	public Location getCurrentLocation() {
		return new Location(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	@Override
	public Location getNextLocation(Location location) {
		return new Location(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	@Override
	public double distanceTo(ComputingNode device2) {
		return Double.POSITIVE_INFINITY;
	}
	
	@Override
	public void generatePath() {
		// Do nothing.
	}
}