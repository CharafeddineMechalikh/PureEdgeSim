package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Base class of the distributions that only produce whole numbers, such as the
 * number of tasks generated in a minute or the outcome of a coin toss.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public abstract class AbstractDiscreteDistribution implements Distribution {

	/** The generator used to draw values. */
	protected Random random;

	/**
	 * Creates a discrete distribution that draws from the shared simulation
	 * randomness.
	 *
	 * @see Randomness#getRandom()
	 */
	protected AbstractDiscreteDistribution() {
		this.random = Randomness.getRandom();
	}

	/**
	 * Creates a discrete distribution that draws from the given generator.
	 *
	 * @param random the generator to draw from
	 */
	protected AbstractDiscreteDistribution(Random random) {
		this.random = random;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the sampled integer, as a double
	 */
	@Override
	public double sample() {
		return sampleInt();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the exact discrete outcome, with no rounding applied afterwards
	 */
	@Override
	public abstract int sampleInt();

	/**
	 * Discrete distributions always return {@code true}.
	 *
	 * @return {@code true}
	 */
	@Override
	public boolean isDiscrete() {
		return true;
	}

	/**
	 * Replaces the generator of this distribution.
	 *
	 * @param random the new generator
	 */
	public void setRandom(Random random) {
		this.random = random;
	}

	/**
	 * Returns the generator used by this distribution.
	 *
	 * @return the random generator
	 */
	public Random getRandom() {
		return random;
	}

}
