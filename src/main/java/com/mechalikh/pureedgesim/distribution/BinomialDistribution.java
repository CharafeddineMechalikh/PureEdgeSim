package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Binomial distribution: the number of successes among a fixed number of
 * independent trials with the same success probability. It counts things such as
 * how many of the tasks generated in a minute exceed their latency bound.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class BinomialDistribution extends AbstractDiscreteDistribution {

	/** Number of trials. */
	private final int trials;

	/** Probability of success of each trial. */
	private final double probability;

	/** Above this number of trials, the normal approximation is used. */
	private static final int APPROXIMATION_THRESHOLD = 64;

	/**
	 * Creates a binomial distribution.
	 *
	 * @param trials      the number of trials, at least one
	 * @param probability the success probability, between 0 and 1
	 * @throws IllegalArgumentException when a parameter is out of range
	 */
	public BinomialDistribution(int trials, double probability) {
		this(Randomness.getRandom(), trials, probability);
	}

	/**
	 * Creates a binomial distribution drawing from the supplied generator.
	 *
	 * @param random      the generator to draw from
	 * @param trials      the number of trials, at least one
	 * @param probability the success probability, between 0 and 1
	 * @throws IllegalArgumentException when a parameter is out of range
	 */
	public BinomialDistribution(Random random, int trials, double probability) {
		super(random);
		if (trials < 1)
			throw new IllegalArgumentException("Distribution \"binomial\": the parameter \"trials\" must be >= 1");
		if (probability < 0 || probability > 1)
			throw new IllegalArgumentException(
					"Distribution \"binomial\": the parameter \"probability\" must be between 0 and 1");
		this.trials = trials;
		this.probability = probability;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the number of successes, between 0 and the number of trials
	 */
	@Override
	public int sampleInt() {
		if (trials > APPROXIMATION_THRESHOLD) {
			double deviation = Math.sqrt(trials * probability * (1 - probability));
			double approximation = trials * probability + deviation * random.nextGaussian();
			return (int) Math.min(trials, Math.max(0, Math.floor(approximation + 0.5)));
		}
		int successes = 0;
		for (int trial = 0; trial < trials; trial++)
			if (random.nextDouble() < probability)
				successes++;
		return successes;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code trials * probability}
	 */
	@Override
	public double mean() {
		return trials * probability;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "binomial"}
	 */
	@Override
	public String getName() {
		return "binomial";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, the smallest number of successes
	 */
	@Override
	public double getMin() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the number of trials, the largest number of successes
	 */
	@Override
	public double getMax() {
		return trials;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "binomial(trials=" + trials + ", probability=" + probability + ")";
	}

}
