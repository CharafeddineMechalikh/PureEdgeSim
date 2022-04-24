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
 
import java.util.Random;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class DefaultMobilityModel extends MobilityModel {
	private boolean pause = false;
	private double pauseDuration = -1;
	private double mobilityDuration ;
	private int orientationAngle = new Random().nextInt(359);

	public DefaultMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	@Override
	protected Location getNextLocation(Location newLocation) {
		double X_position = newLocation.getXPos(); // Get the initial X coordinate assigned to this device
		double Y_position = newLocation.getYPos(); // Get the initial y coordinate assigned to this device

		if (pause && pauseDuration > 0) {
			// The device mobility is paused until that random delay finishes
			pauseDuration -= SimulationParameters.UPDATE_INTERVAL;
			return newLocation;
		}

		// Make sure that the device stay in the simulation area
		reoriontate(X_position, Y_position);

		if (mobilityDuration <= 0) {
			pause();
		}

		if (pauseDuration <= 0) {
			resume();
		}

		// Update the currentLocation of this device
		return updateLocation(X_position, Y_position);

	}

	private Location updateLocation(double X_position, double Y_position) {
		double distance = getSpeed() * SimulationParameters.UPDATE_INTERVAL;
		double X_distance = Math.cos(Math.toRadians(orientationAngle)) * distance;
		double Y_distance = Math.sin(Math.toRadians(orientationAngle)) * distance;
		// Update the X_position
		double X_pos = X_position + X_distance;
		double Y_pos = Y_position + Y_distance;
		return new Location(X_pos, Y_pos);
	}

	private void resume() {
		// Resume mobility in the next iteration
		pause = false;
		// Increment time and then calculate the next coordinates in the next iteration
		// (the device is moving)
		mobilityDuration -= SimulationParameters.UPDATE_INTERVAL;
	}

	private void pause() {
		// Pickup random duration from 50 to 200 seconds
		pauseDuration = getMinPauseDuration()
				+ new Random().nextInt((int) (getMaxPauseDuration() - getMinPauseDuration()));
		// Pause mobility (the device will stay in its location for the randomly
		// generated duration
		pause = true;
		// Reorientate the device to a new direction
		orientationAngle = new Random().nextInt(359);
		// The mobility will be resumed for the following period of time
		mobilityDuration = new Random().nextInt((int) (getMaxMobilityDuration() - getMinMobilityDuration()))
				+ getMinMobilityDuration();
	}

	private void reoriontate(double x_position, double y_position) {
		if (x_position >= SimulationParameters.AREA_LENGTH)
			orientationAngle = -90 - new Random().nextInt(180);
		else if (x_position <= 0)
			orientationAngle = -90 + new Random().nextInt(180);
		if (y_position >= SimulationParameters.AREA_WIDTH)
			orientationAngle = -new Random().nextInt(180);
		else if (y_position <= 0)
			orientationAngle = new Random().nextInt(180);
	}

}
