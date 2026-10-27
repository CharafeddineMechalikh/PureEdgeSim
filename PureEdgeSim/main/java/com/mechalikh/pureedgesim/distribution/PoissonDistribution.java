package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Poisson distribution of the number of events observed in a fixed interval when
 * they happen independently at a known average rate. It is the natural model of
 * the number of tasks a device generates per minute.
 * <p>
 * Small means are sampled with Knuth's product method. Large means use the normal
 * approximation, which keeps the draw cheap without changing the average.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class PoissonDistribution extends AbstractDiscreteDistribution {

	/** Mean number of events per interval, also the variance. */
	private final double mean;

	/** Above this mean, the normal approximation is used. */
	private static final double APPROXIMATION_THRESHOLD = 30;

	/**
	 * Creates a Poisson distribution.
	 *
	 * @param mean the average number of events, must be positive
	 * @throws IllegalArgumentException when the mean is not positive
	 */
	public PoissonDistribution(double mean) {
		this(Randomness.getRandom(), mean);
	}

	/**
	 * Creates a Poisson distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param mean   the average number of events, must be positive
	 * @throws IllegalArgumentException when the mean is not positive
	 */
	public PoissonDistribution(Random random, double mean) {
		super(random);
		if (mean <= 0)
			throw new IllegalArgumentException("Distribution \"poisson\": the parameter \"mean\" must be > 0");
		this.mean = mean;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the number of events, at least zero
	 */
	@Override
	public int sampleInt() {
		if (mean >= APPROXIMATION_THRESHOLD) {
			double approximation = mean + Math.sqrt(mean) * random.nextGaussian();
			return (int) Math.max(0, Math.floor(approximation + 0.5));
		}
		// Knuth's multiplicative method.
		double limit = Math.exp(-mean);
		int count = 0;
		double product = 1.0;
		while (true) {
			product *= random.nextDouble();
			if (product <= limit)
				return count;
			count++;
		}
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
	 * @return {@code "poisson"}
	 */
	@Override
	public String getName() {
		return "poisson";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, counts are never negative
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
		return "poisson(mean=" + mean + ")";
	}

}
