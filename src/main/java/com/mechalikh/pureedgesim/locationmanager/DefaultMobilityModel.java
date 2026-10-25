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

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Default random-waypoint mobility model. A mobile node moves in a straight line
 * for a random duration, pauses for a random duration, and reorients when it
 * reaches the simulation area boundary. Distances are expressed in meters and
 * the position is updated every {@link SimulationParameters#updateInterval}.
 * <p>
 * It can be selected explicitly in the scenario file with
 * {@code <mobility model="random-waypoint">true</mobility>}.
 */
public class DefaultMobilityModel extends MobilityModel {
	/** True while the node is paused. */
	protected boolean pause = false;
	/** Remaining pause time, in seconds; negative when not pausing. */
	protected double pauseDuration = -1;
	/** Remaining movement time, in seconds, before the next pause. */
	protected double mobilityDuration;
	/** Current direction, in degrees. */
	protected int orientationAngle;

	/**
	 * Creates a mobility model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager.
	 * @param currentLocation   the initial location.
	 */
	public DefaultMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
		orientationAngle = random.nextInt(359);
	}

	/**
	 * Computes the location at the next update tick, applying pause, boundary
	 * avoidance, and movement.
	 *
	 * @param newLocation the current location.
	 * @return the location at the next tick.
	 */
	@Override
	protected Location getNextLocation(Location newLocation) {
		double xPosition = newLocation.getXPos(); // Get the initial X coordinate assigned to this device
		double yPosition = newLocation.getYPos(); // Get the initial y coordinate assigned to this device

		if (pause && pauseDuration > 0) {
			// The device mobility is paused until that random delay finishes
			pauseDuration -= SimulationParameters.updateInterval;
			return newLocation;
		}

		// Make sure that the device stay in the simulation area
		reoriontate(xPosition, yPosition);

		if (mobilityDuration <= 0) {
			pause();
		}

		if (pauseDuration <= 0) {
			resume();
		}

		// Update the currentLocation of this device
		return updateLocation(xPosition, yPosition);

	}

	/**
	 * Moves the node by one update interval along its current orientation.
	 *
	 * @param xPosition current X coordinate.
	 * @param yPosition current Y coordinate.
	 * @return the new location.
	 */
	protected Location updateLocation(double xPosition, double yPosition) {
		double distance = getSpeed() * SimulationParameters.updateInterval;
		double X_distance = Math.cos(Math.toRadians(orientationAngle)) * distance;
		double Y_distance = Math.sin(Math.toRadians(orientationAngle)) * distance;
		// Update the xPosition
		double X_pos = xPosition + X_distance;
		double Y_pos = yPosition + Y_distance;
		return new Location(X_pos, Y_pos);
	}

	/** Ends the pause and resumes movement at the next tick. */
	protected void resume() {
		// Resume mobility in the next iteration
		pause = false;
		// Increment time and then calculate the next coordinates in the next iteration
		// (the device is moving)
		mobilityDuration -= SimulationParameters.updateInterval;
	}

	/**
	 * Starts a pause with a random duration and picks the next movement duration
	 * and direction.
	 */
	protected void pause() {
		// Pickup random duration from minPauseDuration to maxPauseDuration
		pauseDuration = randomInRange(getMinPauseDuration(), getMaxPauseDuration());
		// Pause mobility (the device will stay in its location for the randomly
		// generated duration
		pause = true;
		// Reorientate the device to a new direction
		orientationAngle = random.nextInt(359);
		// The mobility will be resumed for the following period of time
		mobilityDuration = randomInRange(getMinMobilityDuration(), getMaxMobilityDuration());
	}

	/**
	 * Redirects the node toward the inside of the simulation area when it reaches
	 * a boundary.
	 *
	 * @param xPosition current X coordinate.
	 * @param yPosition current Y coordinate.
	 */
	protected void reoriontate(double xPosition, double yPosition) {
		if (xPosition >= getMapWidth())
			orientationAngle = -90 - random.nextInt(180);
		else if (xPosition <= 0)
			orientationAngle = -90 + random.nextInt(180);
		if (yPosition >= getMapLength())
			orientationAngle = -random.nextInt(180);
		else if (yPosition <= 0)
			orientationAngle = random.nextInt(180);
	}

}
