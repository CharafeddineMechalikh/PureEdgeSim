package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Brownian motion (random diffusion) mobility model. Each coordinate of the node
 * is displaced by an independent Gaussian increment at every update, so the
 * trajectory is continuous but nowhere differentiable, like a particle suspended
 * in a fluid or a slowly drifting sensor.
 * <p>
 * The mean squared displacement after {@code t} seconds is
 * {@code 2 * std^2 * t / update_interval}, which means that {@code std} controls
 * how fast the node wanders away from where it started.
 * <p>
 * Options, read from the attributes of the {@code <mobility>} element:
 * <ul>
 * <li>{@code std}: standard deviation of the displacement of each coordinate, in
 * meters per update. Default: the distance covered by the device speed in one
 * update interval.</li>
 * </ul>
 * Select it with {@code <mobility model="brownian" std="5">true</mobility>}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class BrownianMobilityModel extends MobilityModel {

	/**
	 * Creates a Brownian motion model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager
	 * @param currentLocation   the initial location
	 */
	public BrownianMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	/**
	 * Displaces both coordinates by an independent Gaussian sample.
	 *
	 * @param location the current location
	 * @return the location at the next update
	 */
	@Override
	protected Location getNextLocation(Location location) {
		double std = getParameter("std", getStepDistance());
		return reflectLocation(new Location(location.getXPos() + random.nextGaussian() * std,
				location.getYPos() + random.nextGaussian() * std));
	}

}
