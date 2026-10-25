package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Normal (Gaussian) distribution, described by its mean and standard deviation.
 * It models quantities that cluster around a typical value with symmetric
 * variability, such as the speed of a pedestrian or the noise added to a node
 * position.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class NormalDistribution extends AbstractDistribution {

	/** Center of the distribution. */
	private final double mean;

	/** Spread around the center. */
	private final double standardDeviation;

	/**
	 * Creates a normal distribution.
	 *
	 * @param mean                the center of the distribution
	 * @param standardDeviation   the spread, must be positive
	 * @throws IllegalArgumentException when the standard deviation is not positive
	 */
	public NormalDistribution(double mean, double standardDeviation) {
		this(Randomness.getRandom(), mean, standardDeviation);
	}

	/**
	 * Creates a normal distribution drawing from the supplied generator.
	 *
	 * @param random              the generator to draw from
	 * @param mean                the center of the distribution
	 * @param standardDeviation   the spread, must be positive
	 * @throws IllegalArgumentException when the standard deviation is not positive
	 */
	public NormalDistribution(Random random, double mean, double standardDeviation) {
		super(random);
		requirePositive("std", standardDeviation, 0);
		this.mean = mean;
		this.standardDeviation = standardDeviation;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a value drawn from the normal distribution
	 */
	@Override
	public double sample() {
		return mean + standardDeviation * random.nextGaussian();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the configured mean
	 */
	@Override
	public double mean() {
		return mean;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "normal"}
	 */
	@Override
	public String getName() {
		return "normal";
	}

	/**
	 * Returns the standard deviation.
	 *
	 * @return the spread of the distribution
	 */
	public double getStandardDeviation() {
		return standardDeviation;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "normal(mean=" + mean + ", std=" + standardDeviation + ")";
	}

}
