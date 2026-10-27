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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * The main class of the Mobility Manager module, that generates the mobility
 * path for different edge devices. It implements the Null Object Design Pattern
 * in order to start avoiding {@link NullPointerException} when using the NULL
 * object instead of attributing null to EnergyModelNetworkLink variables.
 * <p>
 * Subclasses only have to implement {@link #getNextLocation(Location)}: the
 * path is precomputed once, at simulation start, by calling that method at
 * every update interval. Model-specific options are read from
 * {@link #getParameters()}, which is filled from the attributes of the
 * {@code <mobility>} element of the device.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */

public abstract class MobilityModel {

	/** The current position of the node holding this mobility model. */
	protected Location currentLocation;
	/** Whether the node moves during the simulation. */
	protected boolean isMobile = false;
	/** Minimum pause duration between two movements, in seconds. */
	protected double minPauseDuration;
	/** Maximum pause duration between two movements, in seconds. */
	protected double maxPauseDuration;
	/** Maximum duration of a single movement, in seconds. */
	protected double maxMobilityDuration;
	/** Minimum duration of a single movement, in seconds. */
	protected double minMobilityDuration;
	/** Movement speed in meters per second. */
	protected double speed;
	/** Generator used for the random choices of the model. */
	protected Random random;
	/** Model-specific options, as read from the scenario file. */
	protected Map<String, String> parameters = new HashMap<>();
	/** The simulation manager used to query time, nodes, and the topology. */
	protected SimulationManager simulationManager;
	/** The closest peripheral edge data center at simulation start. */
	protected ComputingNode closestEdgeDataCenter = ComputingNode.NULL;
	/** Precomputed locations, keyed by simulation time in milliseconds. */
	Map<Integer, Location> path = new LinkedHashMap<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));
	/** Precomputed closest edge data center, keyed by simulation time in milliseconds. */
	Map<Integer, ComputingNode> datacentersMap = new LinkedHashMap<>(
			(int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval));

	/**
	 * An attribute that implements the Null Object Design Pattern to avoid
	 * NullPointerException when using the NULL object instead of attributing null
	 * to MobilityModel variables.
	 */
	public static final MobilityModel NULL = new MobilityModelNull();

	/**
	 * Creates a mobility model for a node at the given initial location.
	 *
	 * @param simulationManager the simulation manager
	 * @param location          the initial location
	 */
	protected MobilityModel(SimulationManager simulationManager, Location location) {
		currentLocation = location;
		random = Randomness.getRandom();
		setSimulationManager(simulationManager);
	}

	/**
	 * Returns the name used to select this model in the scenario file.
	 *
	 * @return the simple class name of the model
	 */
	public String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * Replaces the model-specific options, normally read from the attributes of the
	 * {@code <mobility>} element. Attribute names are matched
	 * case-insensitively.
	 *
	 * @param parameters the new options, {@code null} is ignored
	 * @return this model, for method chaining
	 */
	public MobilityModel setParameters(Map<String, String> parameters) {
		this.parameters = new HashMap<>();
		if (parameters != null)
			for (Map.Entry<String, String> entry : parameters.entrySet())
				this.parameters.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
		return this;
	}

	/**
	 * Returns the model-specific options.
	 *
	 * @return a map of option name to value, never {@code null}
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Returns a model-specific option.
	 *
	 * @param name         the option name, case insensitive
	 * @param defaultValue the value returned when the option is not set
	 * @return the option value
	 * @throws IllegalArgumentException when the option is set but not a number
	 */
	public double getParameter(String name, double defaultValue) {
		String value = parameters.get(name.toLowerCase(Locale.ROOT));
		if (value == null || value.trim().isEmpty())
			return defaultValue;
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(getName() + " - the \"" + name + "\" option must be a number, found \""
					+ value.trim() + "\"");
		}
	}

	/**
	 * Returns a model-specific option read as a text value.
	 *
	 * @param name         the option name, case insensitive
	 * @param defaultValue the value returned when the option is not set
	 * @return the option value
	 */
	public String getParameter(String name, String defaultValue) {
		String value = parameters.get(name.toLowerCase(Locale.ROOT));
		if (value == null || value.trim().isEmpty())
			return defaultValue;
		return value.trim();
	}

	/**
	 * Returns a model-specific option read as an integer.
	 *
	 * @param name         the option name, case insensitive
	 * @param defaultValue the value returned when the option is not set
	 * @return the option value
	 * @throws IllegalArgumentException when the option is set but not a whole number
	 */
	public int getParameterInt(String name, int defaultValue) {
		double value = getParameter(name, defaultValue);
		if (value != Math.floor(value))
			throw new IllegalArgumentException(
					getName() + " - the \"" + name + "\" option must be an integer, found \"" + value + "\"");
		return (int) value;
	}

	/**
	 * Returns a model-specific option read as a boolean.
	 *
	 * @param name         the option name, case insensitive
	 * @param defaultValue the value returned when the option is not set
	 * @return the option value
	 */
	public boolean getParameterBoolean(String name, boolean defaultValue) {
		String value = parameters.get(name.toLowerCase(Locale.ROOT));
		if (value == null || value.trim().isEmpty())
			return defaultValue;
		return Boolean.parseBoolean(value.trim());
	}

	/**
	 * Returns a required model-specific option.
	 *
	 * @param name the option name, case insensitive
	 * @return the trimmed option value
	 * @throws IllegalArgumentException when the option is not set
	 */
	public String getRequiredParameter(String name) {
		String value = parameters.get(name.toLowerCase(Locale.ROOT));
		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException(
					getName() + " - the \"" + name + "\" option is required, check the \"mobility\" element");
		return value.trim();
	}

	/**
	 * Returns the horizontal size of the simulation area.
	 *
	 * @return the map width in meters
	 */
	protected double getMapWidth() {
		return SimulationParameters.simulationMapWidth;
	}

	/**
	 * Returns the vertical size of the simulation area.
	 *
	 * @return the map length in meters
	 */
	protected double getMapLength() {
		return SimulationParameters.simulationMapLength;
	}

	/**
	 * Returns the distance covered between two location updates.
	 *
	 * @return the speed multiplied by the update interval, in meters
	 */
	protected double getStepDistance() {
		return speed * SimulationParameters.updateInterval;
	}

	/**
	 * Returns the number of location updates between the start and the end of the
	 * simulation, including the initial one.
	 *
	 * @return the number of update ticks
	 */
	protected int getTicksCount() {
		return (int) (SimulationParameters.simulationDuration / SimulationParameters.updateInterval) + 1;
	}

	/**
	 * Draws a value between two bounds. Returns the lower bound when the interval is
	 * empty, so that a scenario with equal minimum and maximum durations is valid
	 * instead of failing.
	 *
	 * @param min the inclusive lower bound
	 * @param max the exclusive upper bound
	 * @return a random value inside the interval
	 */
	protected double randomInRange(double min, double max) {
		if (max <= min)
			return min;
		return min + random.nextDouble() * (max - min);
	}

	/**
	 * Moves a location closer to a target, stopping at the target when the distance
	 * to cover is larger than the remaining gap.
	 *
	 * @param from     the current location
	 * @param target   the location to move to
	 * @param distance the distance to cover, in meters
	 * @return the next location
	 */
	protected Location moveTowards(Location from, Location target, double distance) {
		double dx = target.getXPos() - from.getXPos();
		double dy = target.getYPos() - from.getYPos();
		double gap = Math.sqrt(dx * dx + dy * dy);
		if (gap == 0 || gap <= distance)
			return new Location(target.getXPos(), target.getYPos());
		double ratio = distance / gap;
		return new Location(from.getXPos() + dx * ratio, from.getYPos() + dy * ratio);
	}

	/**
	 * Checks whether a location has reached its target.
	 *
	 * @param from      the current location
	 * @param target    the location that should have been reached
	 * @param tolerance the accepted error, in meters
	 * @return {@code true} when both locations are closer than the tolerance
	 */
	protected boolean reached(Location from, Location target, double tolerance) {
		double dx = target.getXPos() - from.getXPos();
		double dy = target.getYPos() - from.getYPos();
		return Math.sqrt(dx * dx + dy * dy) <= tolerance;
	}

	/**
	 * Bounces a location off the borders of the simulation area, so that a node that
	 * left the map comes back in at the same distance from the border it crossed.
	 *
	 * @param location the location to bring back into the map
	 * @return the reflected location, always inside the simulation area
	 */
	protected Location reflectLocation(Location location) {
		double width = getMapWidth();
		double length = getMapLength();
		double x = location.getXPos();
		double y = location.getYPos();
		if (x < 0)
			x = -x;
		if (y < 0)
			y = -y;
		if (width > 0 && x > width)
			x = width - (x - width);
		if (length > 0 && y > length)
			y = length - (y - length);
		return new Location(Math.min(Math.max(x, 0), Math.max(width, 0)),
				Math.min(Math.max(y, 0), Math.max(length, 0)));
	}

	/**
	 * Creates a mobility model without an initial location or simulation manager.
	 * Used by the {@link #NULL} object.
	 */
	protected MobilityModel() {
	}

	/**
	 * Computes the next location of the node. Called once per update interval
	 * while the path table is generated.
	 *
	 * @param location the current location.
	 * @return the next location.
	 */
	protected abstract Location getNextLocation(Location location);

	/**
	 * Updates and returns the current location from the precomputed path for the
	 * given simulation time. Times beyond the simulation duration leave the
	 * location unchanged.
	 *
	 * @param time the simulation time in seconds
	 * @return the current location after the update
	 */
	public Location updateLocation(double time) {
		if (time <= SimulationParameters.simulationDuration)
			currentLocation = path.get((int) time * 1000);
		return currentLocation;
	}

	/**
	 * Returns the current location of the node.
	 *
	 * @return the current location
	 */
	public Location getCurrentLocation() {
		return currentLocation;
	}

	/**
	 * Returns whether the node moves during the simulation.
	 *
	 * @return {@code true} if the node is mobile
	 */
	public boolean isMobile() {
		return isMobile;
	}

	/**
	 * Sets whether the node moves during the simulation.
	 *
	 * @param mobile {@code true} to make the node mobile
	 * @return this model, for method chaining
	 */
	public MobilityModel setMobile(boolean mobile) {
		isMobile = mobile;
		return this;
	}

	/**
	 * Returns the minimum pause duration between two movements.
	 *
	 * @return the minimum pause duration in seconds
	 */
	public double getMinPauseDuration() {
		return minPauseDuration;
	}

	/**
	 * Sets the minimum pause duration between two movements.
	 *
	 * @param minPauseDuration the minimum pause duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMinPauseDuration(double minPauseDuration) {
		this.minPauseDuration = minPauseDuration;
		return this;
	}

	/**
	 * Returns the maximum pause duration between two movements.
	 *
	 * @return the maximum pause duration in seconds
	 */
	public double getMaxPauseDuration() {
		return maxPauseDuration;
	}

	/**
	 * Sets the maximum pause duration between two movements.
	 *
	 * @param maxPauseDuration the maximum pause duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMaxPauseDuration(double maxPauseDuration) {
		this.maxPauseDuration = maxPauseDuration;
		return this;
	}

	/**
	 * Returns the minimum duration of a single movement.
	 *
	 * @return the minimum mobility duration in seconds
	 */
	public double getMinMobilityDuration() {
		return minMobilityDuration;
	}

	/**
	 * Sets the minimum duration of a single movement.
	 *
	 * @param minMobilityDuration the minimum mobility duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMinMobilityDuration(double minMobilityDuration) {
		this.minMobilityDuration = minMobilityDuration;
		return this;
	}

	/**
	 * Returns the maximum duration of a single movement.
	 *
	 * @return the maximum mobility duration in seconds
	 */
	public double getMaxMobilityDuration() {
		return maxMobilityDuration;
	}

	/**
	 * Sets the maximum duration of a single movement.
	 *
	 * @param maxMobilityDuration the maximum mobility duration in seconds
	 * @return this model, for method chaining
	 */
	public MobilityModel setMaxMobilityDuration(double maxMobilityDuration) {
		this.maxMobilityDuration = maxMobilityDuration;
		return this;
	}

	/**
	 * Returns the movement speed.
	 *
	 * @return the speed in meters per second
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Sets the movement speed.
	 *
	 * @param speed the speed in meters per second
	 * @return this model, for method chaining
	 */
	public MobilityModel setSpeed(double speed) {
		this.speed = speed;
		return this;
	}

	/**
	 * Returns the Euclidean distance between this node and another node, based on
	 * their current locations.
	 *
	 * @param device2 the other node
	 * @return the distance in meters
	 */
	public double distanceTo(ComputingNode device2) {
		return Math.abs(Math.sqrt(Math
				.pow((getCurrentLocation().getXPos() - device2.getMobilityModel().getCurrentLocation().getXPos()), 2)
				+ Math.pow((getCurrentLocation().getYPos() - device2.getMobilityModel().getCurrentLocation().getYPos()),
						2)));
	}

	/**
	 * Returns the simulation manager.
	 *
	 * @return the simulation manager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Sets the simulation manager.
	 *
	 * @param simulationManager the simulation manager
	 */
	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	/**
	 * Generates the location table for the whole simulation at update-interval
	 * granularity. Immobile nodes keep their initial location. For mobile nodes,
	 * the closest edge data center is also recorded at each interval so that
	 * handovers can be resolved instantly later.
	 */
	public void generatePath() {

		closestEdgeDataCenter = getDataCenter();

		if (!isMobile())
			return;
		Location newLocation = getCurrentLocation();

		// Working around the double imprecision
		int interval = (int) (SimulationParameters.updateInterval * 1000);
		int simulationTime = (int) (SimulationParameters.simulationDuration * 1000);

		for (int i = 0; i <= simulationTime; i = i + interval) {
			path.put(i, newLocation);
			newLocation = getNextLocation(newLocation);
			datacentersMap.put(i, getDataCenter());
		}

	}

	/**
	 * Returns the closest peripheral edge data center within
	 * {@link SimulationParameters#edgeDataCentersRange}.
	 *
	 * @return the closest edge data center, or {@link ComputingNode#NULL} when
	 *         none is in range
	 */
	protected ComputingNode getDataCenter() {
		List<ComputingNode> list = getSimulationManager().getDataCentersManager().getComputingNodesGenerator()
				.getEdgeOnlyList();
		double range = SimulationParameters.edgeDataCentersRange;
		ComputingNode closestDC = ComputingNode.NULL;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isPeripheral() && distanceTo(list.get(i)) <= range) {
				range = distanceTo(list.get(i));
				closestDC = list.get(i);
			}
		}
		return closestDC;
	}

	/**
	 * Returns the edge data center closest to the node at the current simulation
	 * time. Mobile nodes within the simulation duration use the value recorded in
	 * the precomputed table; otherwise the value computed at simulation start is
	 * returned.
	 *
	 * @return the closest edge data center, or {@link ComputingNode#NULL} when none
	 *         is in range
	 */
	public ComputingNode getClosestEdgeDataCenter() {
		return (isMobile && getSimulationManager().getSimulation().clock() <= SimulationParameters.simulationDuration)
				? datacentersMap.get((int) (getSimulationManager().getSimulation().clock() * 1000))
				: closestEdgeDataCenter;
	}

}
