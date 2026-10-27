package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Base class of the continuous distributions. It holds the random generator and
 * the common conversion to integer values.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public abstract class AbstractDistribution implements Distribution {

	/** The generator used to draw values. */
	protected Random random;

	/**
	 * Creates a distribution that draws from the shared simulation randomness.
	 *
	 * @see Randomness#getRandom()
	 */
	protected AbstractDistribution() {
		this.random = Randomness.getRandom();
	}

	/**
	 * Creates a distribution that draws from the given generator.
	 * <p>
	 * Tests and custom models use this constructor to obtain reproducible values.
	 *
	 * @param random the generator to draw from
	 */
	protected AbstractDistribution(Random random) {
		this.random = random;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Continuous values are truncated toward minus infinity.
	 */
	@Override
	public int sampleInt() {
		return (int) Math.floor(sample());
	}

	/**
	 * Continuous distributions are never discrete.
	 *
	 * @return {@code false}
	 */
	@Override
	public boolean isDiscrete() {
		return false;
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

	/**
	 * Confines a value to the interval {@code [min, max]}.
	 *
	 * @param value the value to clamp
	 * @param min   the lower bound
	 * @param max   the upper bound
	 * @return the clamped value
	 */
	protected double clamp(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}

	/**
	 * Checks that a distribution parameter is neither null nor out of range.
	 *
	 * @param name  the parameter name, used in the error message
	 * @param value the parameter value
	 * @param min   the smallest accepted value, exclusive
	 * @throws IllegalArgumentException when the value is not strictly greater than
	 *                                  {@code min}
	 */
	protected void requirePositive(String name, double value, double min) {
		if (value <= min)
			throw new IllegalArgumentException(
					"Distribution \"" + getName() + "\": the parameter \"" + name + "\" must be > " + min);
	}

}
