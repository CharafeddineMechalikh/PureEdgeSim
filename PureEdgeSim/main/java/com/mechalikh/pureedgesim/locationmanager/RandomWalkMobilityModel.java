package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Random walk mobility model. At every update the node picks a new direction and
 * moves in a straight line for one update interval, without ever pausing. When it
 * reaches the border of the simulation area it is reflected back inside.
 * <p>
 * Compared to the default random waypoint model, the node never stops, which
 * makes this the cheapest model to generate and a good fit for devices that are
 * continuously on the move.
 * <p>
 * It takes no additional option. Select it with
 * {@code <mobility model="random-walk">true</mobility>}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class RandomWalkMobilityModel extends MobilityModel {

	/**
	 * Creates a random walk model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager
	 * @param currentLocation   the initial location
	 */
	public RandomWalkMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	/**
	 * Moves the node one step in a uniformly drawn direction.
	 *
	 * @param location the current location
	 * @return the location at the next update
	 */
	@Override
	protected Location getNextLocation(Location location) {
		double angle = random.nextDouble() * 360.0;
		double distance = getStepDistance();
		return reflectLocation(new Location(location.getXPos() + Math.cos(Math.toRadians(angle)) * distance,
				location.getYPos() + Math.sin(Math.toRadians(angle)) * distance));
	}

}
