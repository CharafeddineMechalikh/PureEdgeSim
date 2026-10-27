package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Geometric distribution: the number of trials needed to obtain the first
 * success, counted from one. It models waiting times in number of attempts, for
 * example how many retries a device performs before a transmission succeeds.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class GeometricDistribution extends AbstractDiscreteDistribution {

	/** Probability of success of each trial. */
	private final double probability;

	/**
	 * Creates a geometric distribution.
	 *
	 * @param probability the success probability, between 0 (exclusive) and 1
	 * @throws IllegalArgumentException when the probability is out of range
	 */
	public GeometricDistribution(double probability) {
		this(Randomness.getRandom(), probability);
	}

	/**
	 * Creates a geometric distribution drawing from the supplied generator.
	 *
	 * @param random      the generator to draw from
	 * @param probability the success probability, between 0 (exclusive) and 1
	 * @throws IllegalArgumentException when the probability is out of range
	 */
	public GeometricDistribution(Random random, double probability) {
		super(random);
		if (probability <= 0 || probability > 1)
			throw new IllegalArgumentException(
					"Distribution \"geometric\": the parameter \"probability\" must be > 0 and <= 1");
		this.probability = probability;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Drawn with the inverse transform {@code floor(ln U / ln(1 - p)) + 1}, so the
	 * cost does not grow with the outcome.
	 *
	 * @return the number of trials of the first success, at least one
	 */
	@Override
	public int sampleInt() {
		if (probability == 1)
			return 1;
		double uniform = random.nextDouble();
		while (uniform == 0.0)
			uniform = random.nextDouble();
		return (int) Math.floor(Math.log(uniform) / Math.log(1 - probability)) + 1;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code 1 / probability}
	 */
	@Override
	public double mean() {
		return 1 / probability;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "geometric"}
	 */
	@Override
	public String getName() {
		return "geometric";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return one, at least one trial is always needed
	 */
	@Override
	public double getMin() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "geometric(probability=" + probability + ")";
	}

}
