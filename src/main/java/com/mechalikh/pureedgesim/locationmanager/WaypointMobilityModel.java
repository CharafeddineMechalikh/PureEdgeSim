package com.mechalikh.pureedgesim.locationmanager;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Waypoint (route) mobility model. The node follows a fixed list of coordinates,
 * moving from one to the next at its configured speed, optionally stopping for a
 * pause at every waypoint, and optionally looping back to the first one when the
 * route ends.
 * <p>
 * This is the model to use when devices follow a known trajectory, such as a bus
 * line, a delivery route or the tour of a building.
 * <p>
 * Options, read from the attributes of the {@code <mobility>} element:
 * <ul>
 * <li>{@code waypoints}: the list of coordinates, as
 * {@code "x1,y1;x2,y2;..."} in meters. Required, at least two points.</li>
 * <li>{@code loop}: whether the route is repeated once the last waypoint has been
 * reached. Default {@code true}.</li>
 * </ul>
 * The dwell time at each waypoint is taken from the {@code minPauseDuration}
 * element of the device, so set it to {@code 0} for a continuous route.
 * <p>
 * Select it with
 * {@code <mobility model="waypoint" waypoints="100,100;400,100;400,400">true</mobility>}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class WaypointMobilityModel extends MobilityModel {

	/** Coordinates of the route, in the order they must be visited. */
	protected List<Location> waypoints;
	/** Index of the waypoint the node is currently heading for. */
	protected int targetIndex;
	/** Remaining dwell time at the current waypoint, in seconds. */
	protected double pauseRemaining;
	/** Whether the node has reached the end of a route that is not repeated. */
	protected boolean finished;

	/**
	 * Creates a waypoint model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager
	 * @param currentLocation   the initial location
	 */
	public WaypointMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	/**
	 * Moves the node toward the next waypoint, waiting at each waypoint for the
	 * configured pause duration.
	 *
	 * @param location the current location
	 * @return the location at the next update
	 * @throws IllegalArgumentException when the {@code waypoints} attribute is
	 *                                  missing or malformed
	 */
	@Override
	protected Location getNextLocation(Location location) {
		if (waypoints == null)
			parseWaypoints();
		if (finished)
			return waypoints.get(waypoints.size() - 1);
		if (pauseRemaining > 0) {
			pauseRemaining -= SimulationParameters.updateInterval;
			return location;
		}
		Location target = waypoints.get(targetIndex);
		if (reached(location, target, getStepDistance()))
			return advance(location);
		return moveTowards(location, target, getStepDistance());
	}

	/**
	 * Records the arrival at a waypoint and selects the next one.
	 *
	 * @param location the location of the waypoint just reached
	 * @return the same location, after the dwell time has been scheduled
	 */
	protected Location advance(Location location) {
		if (targetIndex >= waypoints.size() - 1) {
			if (getParameterBoolean("loop", true))
				targetIndex = 0;
			else
				finished = true;
		} else {
			targetIndex++;
		}
		pauseRemaining = getMinPauseDuration();
		return location;
	}

	/**
	 * Reads and validates the {@code waypoints} attribute.
	 *
	 * @throws IllegalArgumentException when the attribute is absent or does not
	 *                                  contain at least two coordinates
	 */
	protected void parseWaypoints() {
		String value = getRequiredParameter("waypoints");
		List<Location> parsed = new ArrayList<>();
		for (String point : value.split(";")) {
			if (point.trim().isEmpty())
				continue;
			String[] coordinates = point.split(",");
			if (coordinates.length != 2)
				throw new IllegalArgumentException(getName() + " - the \"waypoints\" option must be written as "
						+ "\"x1,y1;x2,y2\", found \"" + value + "\"");
			try {
				parsed.add(new Location(Double.parseDouble(coordinates[0].trim()),
						Double.parseDouble(coordinates[1].trim())));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(getName() + " - the \"waypoints\" option must only contain numbers, "
						+ "found \"" + point.trim() + "\"");
			}
		}
		if (parsed.size() < 2)
			throw new IllegalArgumentException(
					getName() + " - the \"waypoints\" option must contain at least two points, found \"" + value + "\"");
		waypoints = parsed;
	}

}
