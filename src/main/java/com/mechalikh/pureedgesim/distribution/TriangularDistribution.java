package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Triangular distribution over {@code [min, max]} with a most likely value. When
 * only expert estimates are available, its three parameters are easier to state
 * than a mean and a deviation, which is why it is popular for durations and
 * message sizes.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class TriangularDistribution extends AbstractDistribution {

	/** Lower bound of the interval. */
	private final double min;

	/** Most likely value. */
	private final double mode;

	/** Upper bound of the interval. */
	private final double max;

	/** Cumulative probability reached at the mode. */
	private final double modeProbability;

	/**
	 * Creates a triangular distribution.
	 *
	 * @param min  the lower bound
	 * @param mode the most likely value
	 * @param max  the upper bound
	 * @throws IllegalArgumentException when the parameters are not ordered
	 */
	public TriangularDistribution(double min, double mode, double max) {
		this(Randomness.getRandom(), min, mode, max);
	}

	/**
	 * Creates a triangular distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param min    the lower bound
	 * @param mode   the most likely value
	 * @param max    the upper bound
	 * @throws IllegalArgumentException when the parameters are not ordered
	 */
	public TriangularDistribution(Random random, double min, double mode, double max) {
		super(random);
		if (max <= min)
			throw new IllegalArgumentException("Distribution \"triangular\": \"max\" must be > \"min\"");
		if (mode < min || mode > max)
			throw new IllegalArgumentException("Distribution \"triangular\": \"mode\" must be between \"min\" and \"max\"");
		this.min = min;
		this.mode = mode;
		this.max = max;
		this.modeProbability = (mode - min) / (max - min);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a value between {@code min} and {@code max}
	 */
	@Override
	public double sample() {
		double uniform = random.nextDouble();
		if (uniform < modeProbability)
			return min + Math.sqrt(uniform * (max - min) * (mode - min));
		return max - Math.sqrt((1 - uniform) * (max - min) * (max - mode));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the average of the three parameters
	 */
	@Override
	public double mean() {
		return (min + mode + max) / 3;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "triangular"}
	 */
	@Override
	public String getName() {
		return "triangular";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the configured minimum
	 */
	@Override
	public double getMin() {
		return min;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the configured maximum
	 */
	@Override
	public double getMax() {
		return max;
	}

	/**
	 * Returns the most likely value.
	 *
	 * @return the mode
	 */
	public double getMode() {
		return mode;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "triangular(min=" + min + ", mode=" + mode + ", max=" + max + ")";
	}

}
