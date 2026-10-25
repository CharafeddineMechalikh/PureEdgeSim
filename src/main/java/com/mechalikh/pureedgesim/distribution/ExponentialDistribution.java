package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Exponential distribution with the given mean, the standard model of the gap
 * between independent events. Drawing the inter-arrival time of tasks from it
 * produces a Poisson arrival process, which is what most workload studies assume.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class ExponentialDistribution extends AbstractDistribution {

	/** Mean of the distribution, also called 1/lambda. */
	private final double mean;

	/**
	 * Creates an exponential distribution.
	 *
	 * @param mean the average value, must be positive
	 * @throws IllegalArgumentException when the mean is not positive
	 */
	public ExponentialDistribution(double mean) {
		this(Randomness.getRandom(), mean);
	}

	/**
	 * Creates an exponential distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param mean   the average value, must be positive
	 * @throws IllegalArgumentException when the mean is not positive
	 */
	public ExponentialDistribution(Random random, double mean) {
		super(random);
		requirePositive("mean", mean, 0);
		this.mean = mean;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a positive value whose average is the configured mean
	 */
	@Override
	public double sample() {
		double uniform = random.nextDouble();
		while (uniform == 0.0)
			uniform = random.nextDouble();
		return -mean * Math.log(uniform);
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
	 * @return {@code "exponential"}
	 */
	@Override
	public String getName() {
		return "exponential";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, the gaps are never negative
	 */
	@Override
	public double getMin() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "exponential(mean=" + mean + ")";
	}

}
