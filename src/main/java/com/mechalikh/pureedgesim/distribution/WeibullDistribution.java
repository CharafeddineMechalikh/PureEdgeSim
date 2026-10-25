package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Weibull distribution with a shape and a scale parameter, drawn with the
 * inverse transform {@code scale * (-ln U)^(1/shape)}. A shape below one gives
 * decreasing failure rates, a shape of one is the exponential distribution, and a
 * shape above one gives increasing rates, which makes it the standard choice for
 * battery ageing and hardware lifetime.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class WeibullDistribution extends AbstractDistribution {

	/** Shape parameter, usually noted k. */
	private final double shape;

	/** Scale parameter, usually noted lambda. */
	private final double scale;

	/**
	 * Creates a Weibull distribution.
	 *
	 * @param shape the shape parameter, must be positive
	 * @param scale the scale parameter, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public WeibullDistribution(double shape, double scale) {
		this(Randomness.getRandom(), shape, scale);
	}

	/**
	 * Creates a Weibull distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param shape  the shape parameter, must be positive
	 * @param scale  the scale parameter, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public WeibullDistribution(Random random, double shape, double scale) {
		super(random);
		requirePositive("shape", shape, 0);
		requirePositive("scale", scale, 0);
		this.shape = shape;
		this.scale = scale;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a positive value
	 */
	@Override
	public double sample() {
		double uniform = random.nextDouble();
		while (uniform == 0.0)
			uniform = random.nextDouble();
		return scale * Math.pow(-Math.log(uniform), 1 / shape);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code scale * Gamma(1 + 1/shape)}
	 */
	@Override
	public double mean() {
		return scale * SpecialFunctions.gamma(1 + 1 / shape);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "weibull"}
	 */
	@Override
	public String getName() {
		return "weibull";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, Weibull values are never negative
	 */
	@Override
	public double getMin() {
		return 0;
	}

	/**
	 * Returns the shape parameter.
	 *
	 * @return the shape
	 */
	public double getShape() {
		return shape;
	}

	/**
	 * Returns the scale parameter.
	 *
	 * @return the scale
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "weibull(shape=" + shape + ", scale=" + scale + ")";
	}

}
