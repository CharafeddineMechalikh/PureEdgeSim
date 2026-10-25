package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Erlang distribution, the gamma distribution restricted to an integer shape. It
 * models the time needed to complete a fixed number of sequential exponential
 * phases, which is a common description of service times in queueing models.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class ErlangDistribution extends GammaDistribution {

	/**
	 * Creates an Erlang distribution.
	 *
	 * @param shape the number of phases, a positive integer
	 * @param scale the mean duration of one phase, must be positive
	 * @throws IllegalArgumentException when the shape is not an integer or a
	 *                                  parameter is not positive
	 */
	public ErlangDistribution(double shape, double scale) {
		this(Randomness.getRandom(), shape, scale);
	}

	/**
	 * Creates an Erlang distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param shape  the number of phases, a positive integer
	 * @param scale  the mean duration of one phase, must be positive
	 * @throws IllegalArgumentException when the shape is not an integer or a
	 *                                  parameter is not positive
	 */
	public ErlangDistribution(Random random, double shape, double scale) {
		super(random, shape, scale);
		if (Math.abs(shape - Math.round(shape)) > 1e-9)
			throw new IllegalArgumentException(
					"Distribution \"erlang\": the parameter \"shape\" must be a whole number of phases");
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "erlang"}
	 */
	@Override
	public String getName() {
		return "erlang";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "erlang(shape=" + getShape() + ", scale=" + getScale() + ")";
	}

}
