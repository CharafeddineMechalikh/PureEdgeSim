package com.mechalikh.pureedgesim.LocationManager;

import java.util.Random;

import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;

public class MobilityManager extends Mobility {
	private boolean pause = false;
	private double pauseDuration = -1;
	private double mobilityDuration = new Random().nextInt(100);
	private int orientationAngle = new Random().nextInt(359);

	public MobilityManager(Location currentLocation) {
		super(currentLocation);
	}

	public MobilityManager() { 
		super();
	}

	@Override
	public Location getNextLocation() {

		double X_position = currentLocation.getXPos(); // Get the initial X coordinate assigned to this device
		double Y_position = currentLocation.getYPos(); // Get the initial y coordinate assigned to this device

		if (pause && pauseDuration > 0) {
			// The device mobility is paused until that random delay finishes
			pauseDuration -= simulationParameters.UPDATE_INTERVAL;
			return currentLocation;
		}
		if (simulationParameters.SPEED > 0) { // The speed must be > 0 in order to move/change the location

			// Make sure that the device stay in the simulation area
			Reoriotate(X_position, Y_position);

			if (mobilityDuration <= 0) {
				pause();
			}

			if (pauseDuration <= 0) {
				resume();
			}

			double distance = simulationParameters.SPEED * simulationParameters.UPDATE_INTERVAL;
			double X_distance = Math.cos(Math.toRadians(orientationAngle)) * distance;
			double Y_distance = Math.sin(Math.toRadians(orientationAngle)) * distance;
			// Update the X_position
			X_position += X_distance;
			Y_position += Y_distance;
			// update the currentLocation of this device
			currentLocation = new Location(X_position, Y_position);
			return new Location(X_position, Y_position);

		}
		return currentLocation;

	}

	private void resume() {
		// Resume mobility in the next iteration
		pause = false;
		// Increment time and then calculate the next coordinates in the next iteration
		// (the device is moving)
		mobilityDuration -= simulationParameters.UPDATE_INTERVAL; 
	}

	private void pause() {
		// Pickup random duration from 50 to 200 seconds
		pauseDuration = 50 + new Random().nextInt(100);
		// Pause mobility (the device will stay in its location for the randomly
		// generated duration
		pause = true;
		// Reorientate the device to a new direction
		orientationAngle = new Random().nextInt(359);
		// The mobility will be resumed for the following period of time
		mobilityDuration = new Random().nextInt(100);
	}

	private void Reoriotate(double x_position, double y_position) {
		if (x_position >= simulationParameters.AREA_LENGTH)
			orientationAngle = -90 - new Random().nextInt(180);
		else if (x_position <= 0)
			orientationAngle = -90 + new Random().nextInt(180);
		if (y_position >= simulationParameters.AREA_WIDTH)
			orientationAngle = -new Random().nextInt(180);
		else if (y_position <= 0)
			orientationAngle = new Random().nextInt(180);
	}

	public Location getCurrentLocation() {
		return this.currentLocation;
	}
}
