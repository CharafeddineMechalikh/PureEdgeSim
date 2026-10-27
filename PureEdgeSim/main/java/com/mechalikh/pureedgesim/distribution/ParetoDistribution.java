package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Pareto type I distribution with a minimum value and a shape parameter, drawn
 * with the inverse transform {@code scale / U^(1/shape)}. It produces the heavy
 * tails observed in real workloads, where a few requests or tasks dominate the
 * total load.
 * <p>
 * The {@code scale} attribute is the minimum value of the distribution, often
 * noted {@code xm}, and {@code shape} is the tail index, often noted alpha.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class ParetoDistribution extends AbstractDistribution {

	/** Minimum value of the distribution. */
	private final double scale;

	/** Tail index, usually noted alpha. */
	private final double shape;

	/**
	 * Creates a Pareto distribution.
	 *
	 * @param scale the minimum value, must be positive
	 * @param shape the tail index, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public ParetoDistribution(double scale, double shape) {
		this(Randomness.getRandom(), scale, shape);
	}

	/**
	 * Creates a Pareto distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param scale  the minimum value, must be positive
	 * @param shape  the tail index, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public ParetoDistribution(Random random, double scale, double shape) {
		super(random);
		requirePositive("scale", scale, 0);
		requirePositive("shape", shape, 0);
		this.scale = scale;
		this.shape = shape;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The tail is heavy, so very large values can appear. Use the {@code max}
	 * attribute of the scenario file to cap them.
	 *
	 * @return a value greater than or equal to the configured minimum
	 */
	@Override
	public double sample() {
		double uniform = random.nextDouble();
		while (uniform == 0.0)
			uniform = random.nextDouble();
		return scale / Math.pow(uniform, 1 / shape);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the mean when the tail index is above one, or positive infinity when
	 *         the distribution has no finite mean
	 */
	@Override
	public double mean() {
		if (shape <= 1)
			return Double.POSITIVE_INFINITY;
		return shape * scale / (shape - 1);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "pareto"}
	 */
	@Override
	public String getName() {
		return "pareto";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the scale, the smallest value the distribution produces
	 */
	@Override
	public double getMin() {
		return scale;
	}

	/**
	 * Returns the minimum value of the distribution.
	 *
	 * @return the minimum value
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Returns the tail index.
	 *
	 * @return the shape parameter
	 */
	public double getShape() {
		return shape;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "pareto(scale=" + scale + ", shape=" + shape + ")";
	}

}
