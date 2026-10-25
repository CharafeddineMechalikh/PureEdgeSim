package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Gauss-Markov mobility model. The direction and the speed of the node follow a
 * mean-reverting random process, so that consecutive movements are correlated
 * instead of independent. This produces the smooth, curved trajectories of
 * vehicles or pedestrians, which neither the random waypoint nor the random walk
 * model can reproduce.
 * <p>
 * At every update the direction becomes
 * {@code mean + alpha * (direction - mean) + gaussian * std * sqrt(1 - alpha^2)},
 * and the same formula is applied to the speed, whose long term mean is the
 * {@code speed} of the device.
 * <p>
 * Options, read from the attributes of the {@code <mobility>} element:
 * <ul>
 * <li>{@code alpha}: memory of the previous movement, between 0 (no memory, a
 * random walk) and 1 (no noise, a straight line). Default 0.7.</li>
 * <li>{@code std}: standard deviation of the direction change, in degrees.
 * Default 30.</li>
 * <li>{@code speed_std}: standard deviation of the speed change, in meters per
 * second. Default 20% of the device speed.</li>
 * </ul>
 * Select it with
 * {@code <mobility model="gauss-markov" alpha="0.8" std="15">true</mobility>}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class GaussMarkovMobilityModel extends MobilityModel {

	/** Memory of the previous movement, between 0 and 1. */
	protected double alpha;
	/** Standard deviation of the direction noise, in degrees. */
	protected double directionStd;
	/** Standard deviation of the speed noise, in meters per second. */
	protected double speedStd;
	/** Long term mean of the direction, in degrees. */
	protected double meanDirection;
	/** Long term mean of the speed, in meters per second. */
	protected double meanSpeed;
	/** Current direction, in degrees. */
	protected double direction;
	/** Current speed, in meters per second. */
	protected double currentSpeed;
	/** Whether the process has been initialised from the scenario options. */
	protected boolean initialised;

	/**
	 * Creates a Gauss-Markov model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager
	 * @param currentLocation   the initial location
	 */
	public GaussMarkovMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	/**
	 * Applies one step of the mean-reverting process to the direction and the speed,
	 * then moves the node along the new direction.
	 *
	 * @param location the current location
	 * @return the location at the next update
	 */
	@Override
	protected Location getNextLocation(Location location) {
		if (!initialised)
			init();
		double noise = Math.sqrt(1 - alpha * alpha);
		direction = meanDirection + alpha * (direction - meanDirection) + random.nextGaussian() * directionStd * noise;
		currentSpeed = meanSpeed + alpha * (currentSpeed - meanSpeed) + random.nextGaussian() * speedStd * noise;
		double distance = Math.max(0, currentSpeed) * SimulationParameters.updateInterval;
		return reflectLocation(new Location(location.getXPos() + Math.cos(Math.toRadians(direction)) * distance,
				location.getYPos() + Math.sin(Math.toRadians(direction)) * distance));
	}

	/**
	 * Reads the model options and seeds the process with a random initial direction.
	 *
	 * @throws IllegalArgumentException when {@code alpha} is not between 0 and 1
	 */
	protected void init() {
		alpha = getParameter("alpha", 0.7);
		if (alpha < 0 || alpha >= 1)
			throw new IllegalArgumentException(
					getName() + " - the \"alpha\" option must be >= 0 and < 1, found \"" + alpha + "\"");
		directionStd = getParameter("std", 30);
		meanSpeed = getSpeed();
		speedStd = getParameter("speed_std", meanSpeed * 0.2);
		meanDirection = random.nextDouble() * 360.0;
		direction = meanDirection;
		currentSpeed = meanSpeed;
		initialised = true;
	}

}
