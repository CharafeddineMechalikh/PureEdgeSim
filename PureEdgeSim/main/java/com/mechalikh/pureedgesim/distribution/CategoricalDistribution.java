package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Discrete distribution over a list of values with optional weights. It lets a
 * scenario file mix a few known profiles, such as three task lengths observed in
 * a trace, instead of fitting them with a continuous family.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class CategoricalDistribution extends AbstractDiscreteDistribution {

	/** The values that can be drawn. */
	private final double[] values;

	/** Cumulative weights, of the same length as {@link #values}. */
	private final double[] cumulativeWeights;

	/** Sum of the weights, used to draw and to compute the mean. */
	private final double totalWeight;

	/**
	 * Creates a categorical distribution with equal weights.
	 *
	 * @param values the values that can be drawn, at least one
	 * @throws IllegalArgumentException when no value is given
	 */
	public CategoricalDistribution(double[] values) {
		this(Randomness.getRandom(), values, null);
	}

	/**
	 * Creates a categorical distribution.
	 *
	 * @param random the generator to draw from
	 * @param values the values that can be drawn, at least one
	 * @param weights the weight of each value, or {@code null} for equal weights
	 * @throws IllegalArgumentException when the lists are inconsistent
	 */
	public CategoricalDistribution(Random random, double[] values, double[] weights) {
		super(random);
		if (values == null || values.length == 0)
			throw new IllegalArgumentException("Distribution \"categorical\": at least one value is required");
		if (weights != null && weights.length != values.length)
			throw new IllegalArgumentException(
					"Distribution \"categorical\": the number of weights must match the number of values");

		this.values = values.clone();
		this.cumulativeWeights = new double[this.values.length];
		double runningTotal = 0;
		for (int i = 0; i < this.values.length; i++) {
			double weight = weights == null ? 1 : weights[i];
			if (weight < 0)
				throw new IllegalArgumentException(
						"Distribution \"categorical\": the weight of \"" + this.values[i] + "\" must be >= 0");
			runningTotal += weight;
			cumulativeWeights[i] = runningTotal;
		}
		if (runningTotal <= 0)
			throw new IllegalArgumentException("Distribution \"categorical\": the sum of the weights must be > 0");
		this.totalWeight = runningTotal;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return one of the configured values
	 */
	@Override
	public double sample() {
		return values[pickIndex()];
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return one of the configured values, truncated toward minus infinity
	 */
	@Override
	public int sampleInt() {
		return (int) Math.floor(values[pickIndex()]);
	}

	/**
	 * Selects an entry with a probability proportional to its weight.
	 *
	 * @return the index of the drawn value
	 */
	private int pickIndex() {
		double draw = random.nextDouble() * totalWeight;
		for (int i = 0; i < cumulativeWeights.length; i++)
			if (draw < cumulativeWeights[i])
				return i;
		return cumulativeWeights.length - 1;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the weighted average of the configured values
	 */
	@Override
	public double mean() {
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			double weight = i == 0 ? cumulativeWeights[0] : cumulativeWeights[i] - cumulativeWeights[i - 1];
			sum += weight * values[i];
		}
		return sum / totalWeight;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "categorical"}
	 */
	@Override
	public String getName() {
		return "categorical";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the smallest configured value
	 */
	@Override
	public double getMin() {
		return extremeValue(true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the largest configured value
	 */
	@Override
	public double getMax() {
		return extremeValue(false);
	}

	/**
	 * Scans the configured values to find one of the two extremes.
	 *
	 * @param smallest {@code true} for the minimum, {@code false} for the maximum
	 * @return the extreme value
	 */
	private double extremeValue(boolean smallest) {
		double result = values[0];
		for (double value : values)
			if (smallest ? value < result : value > result)
				result = value;
		return result;
	}

	/**
	 * Returns the values that can be drawn.
	 *
	 * @return a copy of the configured values
	 */
	public double[] getValues() {
		return values.clone();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the number of configured values
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("categorical(values=");
		for (int i = 0; i < values.length; i++)
			builder.append(i == 0 ? "" : ", ").append(values[i]);
		return builder.append(", totalWeight=").append(totalWeight).append(')').toString();
	}

}
