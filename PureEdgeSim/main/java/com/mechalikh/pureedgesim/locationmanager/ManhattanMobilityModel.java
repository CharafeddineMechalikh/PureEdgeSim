package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Manhattan (city-block) mobility model. The node only moves along the axes,
 * following the streets of an imaginary grid, and turns at the intersections.
 * This is the standard model for pedestrians in a city and for vehicles on a
 * road grid, where the straight-line movement of the random waypoint model would
 * mean crossing buildings.
 * <p>
 * The node heads for a randomly chosen intersection, moving horizontally first
 * and vertically second, and picks a new intersection once it arrives.
 * <p>
 * Options, read from the attributes of the {@code <mobility>} element:
 * <ul>
 * <li>{@code grid_size}: distance between two intersections, in meters. Default
 * 100.</li>
 * </ul>
 * Select it with {@code <mobility model="manhattan" grid_size="200">true</mobility>}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class ManhattanMobilityModel extends MobilityModel {

	/** Intersection the node is currently heading for. */
	protected Location target;

	/**
	 * Creates a Manhattan model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager
	 * @param currentLocation   the initial location
	 */
	public ManhattanMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	/**
	 * Moves the node one step along the current street, and picks a new destination
	 * once the current intersection has been reached.
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
			target = pickIntersection();
		return moveOneAxis(location, target, step);
	}

	/**
	 * Moves one coordinate at a time: the horizontal offset is cancelled first, then
	 * the vertical one, so that the path follows the streets.
	 *
	 * @param location    the current location
	 * @param destination the intersection being approached
	 * @param step        the distance covered during this update
	 * @return the next location, still on the way to the destination
	 */
	protected Location moveOneAxis(Location location, Location destination, double step) {
		double x = location.getXPos();
		double y = location.getYPos();
		if (!withinStep(x, destination.getXPos(), step))
			return new Location(x + Math.signum(destination.getXPos() - x) * step, y);
		if (!withinStep(y, destination.getYPos(), step))
			return new Location(destination.getXPos(), y + Math.signum(destination.getYPos() - y) * step);
		return new Location(destination.getXPos(), destination.getYPos());
	}

	/**
	 * Picks a random intersection of the grid, different from the current position.
	 *
	 * @return the location of the next intersection
	 */
	protected Location pickIntersection() {
		double grid = Math.max(1, getParameter("grid_size", 100));
		int columns = Math.max(1, (int) (getMapWidth() / grid));
		int rows = Math.max(1, (int) (getMapLength() / grid));
		return new Location(grid * (1 + random.nextInt(columns)), grid * (1 + random.nextInt(rows)));
	}

	/**
	 * Checks whether a coordinate is one step away from its destination.
	 *
	 * @param current     the current coordinate
	 * @param destination the destination coordinate
	 * @param step        the distance covered during this update
	 * @return {@code true} when the destination is within reach
	 */
	private boolean withinStep(double current, double destination, double step) {
		return Math.abs(destination - current) <= step;
	}

}
