package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.distribution.ParetoDistribution;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Lévy walk mobility model. The node moves in a sequence of straight flights:
 * each flight has a random direction and a length drawn from a Pareto
 * (power-law) distribution, so that most flights are short while very long ones
 * happen occasionally.
 * <p>
 * This is the model of choice for foraging animals, and more generally for users
 * whose movement is not limited to a fixed area, because it produces the
 * clustering and the occasional long jumps that are visible in human mobility
 * traces.
 * <p>
 * Options, read from the attributes of the {@code <mobility>} element:
 * <ul>
 * <li>{@code shape}: exponent of the Pareto distribution. Default 1.5; values
 * close to 1 give far more long flights.</li>
 * <li>{@code min_step}: shortest flight, in meters. Default: the distance covered
 * by the device speed in one update interval.</li>
 * <li>{@code max_step}: longest accepted flight, in meters. Default: the diagonal
 * of the simulation area.</li>
 * </ul>
 * Select it with
 * {@code <mobility model="levy-walk" shape="1.8" min_step="20">true</mobility>}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class LevyWalkMobilityModel extends MobilityModel {

	/** End of the flight currently being flown. */
	protected Location target;

	/**
	 * Creates a Lévy walk model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager
	 * @param currentLocation   the initial location
	 */
	public LevyWalkMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	/**
	 * Continues the current flight, or starts a new one when the previous target has
	 * been reached.
	 *
	 * @param location the current location
	 * @return the location at the next update
	 */
	@Override
	protected Location getNextLocation(Location location) {
		double step = getStepDistance();
		if (step <= 0)
			return location;
		if (target == null || reached(location, target, 1e-6))
			target = pickFlightEnd(location);
		return moveTowards(location, target, step);
	}

	/**
	 * Draws a flight length from the Pareto distribution and projects its end point
	 * from the current location.
	 *
	 * @param location the location where the flight starts
	 * @return the end of the flight, inside the simulation area
	 */
	protected Location pickFlightEnd(Location location) {
		double minStep = getParameter("min_step", getStepDistance());
		double maxStep = getParameter("max_step", Math.hypot(getMapWidth(), getMapLength()));
		double shape = getParameter("shape", 1.5);
		if (shape <= 0)
			throw new IllegalArgumentException(
					getName() + " - the \"shape\" option must be > 0, found \"" + shape + "\"");
		double length = Math.min(maxStep, new ParetoDistribution(random, minStep, shape).sample());
		double angle = random.nextDouble() * 360.0;
		Location end = new Location(location.getXPos() + Math.cos(Math.toRadians(angle)) * length,
				location.getYPos() + Math.sin(Math.toRadians(angle)) * length);
		return clampToMap(end);
	}

	/**
	 * Brings a location back inside the simulation area without changing the
	 * direction of the flight.
	 *
	 * @param location the location to clamp
	 * @return the closest location inside the map
	 */
	protected Location clampToMap(Location location) {
		return new Location(Math.min(Math.max(location.getXPos(), 0), getMapWidth()),
				Math.min(Math.max(location.getYPos(), 0), getMapLength()));
	}

}
