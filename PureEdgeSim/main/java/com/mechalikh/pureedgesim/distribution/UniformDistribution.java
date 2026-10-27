package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Continuous uniform distribution over the interval {@code [min, max]}. Every
 * value of the interval is equally likely, which makes it the default choice for
 * quantities whose variability is unknown.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class UniformDistribution extends AbstractDistribution {

	/** Lower bound of the interval. */
	private final double min;

	/** Upper bound of the interval. */
	private final double max;

	/**
	 * Creates a uniform distribution over the given interval.
	 *
	 * @param min the inclusive lower bound
	 * @param max the inclusive upper bound
	 * @throws IllegalArgumentException when {@code max} is not greater than
	 *                                  {@code min}
	 */
	public UniformDistribution(double min, double max) {
		this(Randomness.getRandom(), min, max);
	}

	/**
	 * Creates a uniform distribution over the given interval, drawing from the
	 * supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param min    the inclusive lower bound
	 * @param max    the inclusive upper bound
	 * @throws IllegalArgumentException when {@code max} is not greater than
	 *                                  {@code min}
	 */
	public UniformDistribution(Random random, double min, double max) {
		super(random);
		if (max <= min)
			throw new IllegalArgumentException("Distribution \"uniform\": \"max\" must be > \"min\"");
		this.min = min;
		this.max = max;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a value uniformly drawn between {@code min} and {@code max}
	 */
	@Override
	public double sample() {
		return min + (max - min) * random.nextDouble();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the midpoint of the interval
	 */
	@Override
	public double mean() {
		return (min + max) / 2;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "uniform"}
	 */
	@Override
	public String getName() {
		return "uniform";
	}

	/**
	 * Returns the inclusive lower bound.
	 *
	 * @return the minimum value
	 */
	public double getMin() {
		return min;
	}

	/**
	 * Returns the inclusive upper bound.
	 *
	 * @return the maximum value
	 */
	public double getMax() {
		return max;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the interval bounds
	 */
	@Override
	public String toString() {
		return "uniform(min=" + min + ", max=" + max + ")";
	}

}
