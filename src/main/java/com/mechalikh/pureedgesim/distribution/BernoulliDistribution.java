package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Bernoulli distribution: one trial that succeeds with a given probability and
 * returns 1 or 0. It models binary outcomes such as a task being delayed or a
 * device being in range.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class BernoulliDistribution extends AbstractDiscreteDistribution {

	/** Probability of drawing a one. */
	private final double probability;

	/**
	 * Creates a Bernoulli distribution.
	 *
	 * @param probability the success probability, between 0 and 1
	 * @throws IllegalArgumentException when the probability is out of range
	 */
	public BernoulliDistribution(double probability) {
		this(Randomness.getRandom(), probability);
	}

	/**
	 * Creates a Bernoulli distribution drawing from the supplied generator.
	 *
	 * @param random      the generator to draw from
	 * @param probability the success probability, between 0 and 1
	 * @throws IllegalArgumentException when the probability is out of range
	 */
	public BernoulliDistribution(Random random, double probability) {
		super(random);
		if (probability < 0 || probability > 1)
			throw new IllegalArgumentException(
					"Distribution \"bernoulli\": the parameter \"probability\" must be between 0 and 1");
		this.probability = probability;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return 1 with the configured probability, 0 otherwise
	 */
	@Override
	public int sampleInt() {
		return random.nextDouble() < probability ? 1 : 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the success probability
	 */
	@Override
	public double mean() {
		return probability;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "bernoulli"}
	 */
	@Override
	public String getName() {
		return "bernoulli";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, the failure outcome
	 */
	@Override
	public double getMin() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return one, the success outcome
	 */
	@Override
	public double getMax() {
		return 1;
	}

	/**
	 * Returns the success probability.
	 *
	 * @return the probability of drawing a one
	 */
	public double getProbability() {
		return probability;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "bernoulli(probability=" + probability + ")";
	}

}
