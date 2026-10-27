package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Gamma distribution with a shape and a scale parameter, sampled with the
 * Marsaglia and Tsang method. It covers the exponential distribution (shape 1)
 * and the Erlang distribution (integer shape), and models durations whose
 * variability grows with their length.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class GammaDistribution extends AbstractDistribution {

	/** Shape parameter, usually noted k. */
	protected final double shape;

	/** Scale parameter, usually noted theta. */
	protected final double scale;

	/**
	 * Creates a gamma distribution.
	 *
	 * @param shape the shape parameter, must be positive
	 * @param scale the scale parameter, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public GammaDistribution(double shape, double scale) {
		this(Randomness.getRandom(), shape, scale);
	}

	/**
	 * Creates a gamma distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param shape  the shape parameter, must be positive
	 * @param scale  the scale parameter, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public GammaDistribution(Random random, double shape, double scale) {
		super(random);
		requirePositive("shape", shape, 0);
		requirePositive("scale", scale, 0);
		this.shape = shape;
		this.scale = scale;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Shapes below one are boosted with the identity
	 * {@code Gamma(k) = Gamma(k + 1) * U^(1/k)}.
	 *
	 * @return a positive value
	 */
	@Override
	public double sample() {
		if (shape < 1) {
			double uniform = random.nextDouble();
			while (uniform == 0.0)
				uniform = random.nextDouble();
			return draw(shape + 1) * Math.pow(uniform, 1 / shape);
		}
		return draw(shape);
	}

	/**
	 * Marsaglia and Tsang's rejection sampling of a gamma variable of the given
	 * shape, scaled by {@link #scale}.
	 *
	 * @param effectiveShape the shape used for the draw, at least one
	 * @return a positive scaled value
	 */
	protected double draw(double effectiveShape) {
		double d = effectiveShape - 1.0 / 3.0;
		double c = 1.0 / Math.sqrt(9.0 * d);
		while (true) {
			double x = random.nextGaussian();
			double v = 1.0 + c * x;
			if (v <= 0)
				continue;
			v = v * v * v;
			double u = random.nextDouble();
			if (u < 1 - 0.0331 * x * x * x * x)
				return d * v * scale;
			if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v)))
				return d * v * scale;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the product of the shape and the scale
	 */
	@Override
	public double mean() {
		return shape * scale;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "gamma"}
	 */
	@Override
	public String getName() {
		return "gamma";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, gamma values are never negative
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
		return "gamma(shape=" + shape + ", scale=" + scale + ")";
	}

}
