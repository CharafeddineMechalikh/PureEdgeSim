package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Circular mobility model. The node travels around a circle at its configured
 * speed, the simplest way to reproduce a device that covers a fixed area, such as
 * a robot sweeping a warehouse aisle or a drone patrolling a perimeter.
 * <p>
 * The tour starts from the angle defined by the initial location of the node, so
 * the device never jumps to another point of the circle.
 * <p>
 * Options, read from the attributes of the {@code <mobility>} element:
 * <ul>
 * <li>{@code radius}: radius of the circle, in meters. Required.</li>
 * <li>{@code center_x}: horizontal coordinate of the center, in meters. Default:
 * the center of the simulation area.</li>
 * <li>{@code center_y}: vertical coordinate of the center, in meters. Default:
 * the center of the simulation area.</li>
 * <li>{@code clockwise}: direction of the tour. Default {@code true}.</li>
 * </ul>
 * Select it with
 * {@code <mobility model="circular" radius="150" center_x="300" center_y="300">true</mobility>}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class CircularMobilityModel extends MobilityModel {

	/** Current angle on the circle, in radians. */
	protected double angle;
	/** Whether the angle has been taken from the initial location. */
	protected boolean initialised;

	/**
	 * Creates a circular model for a node at the given location.
	 *
	 * @param simulationManager the simulation manager
	 * @param currentLocation   the initial location
	 */
	public CircularMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	/**
	 * Moves the node along the circle by the distance covered during one update.
	 *
	 * @param location the current location
	 * @return the location at the next update
	 * @throws IllegalArgumentException when the {@code radius} option is missing or
	 *                                  not positive
	 */
	@Override
	protected Location getNextLocation(Location location) {
		double radius = getParameter("radius", -1);
		if (radius <= 0)
			throw new IllegalArgumentException(
					getName() + " - the \"radius\" option must be > 0, found \"" + radius + "\"");
		double centerX = getParameter("center_x", getMapWidth() / 2.0);
		double centerY = getParameter("center_y", getMapLength() / 2.0);
		if (!initialised) {
			angle = Math.atan2(location.getYPos() - centerY, location.getXPos() - centerX);
			initialised = true;
		}
		double angularStep = getStepDistance() / radius;
		angle += getParameterBoolean("clockwise", true) ? angularStep : -angularStep;
		return new Location(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);
	}

}
