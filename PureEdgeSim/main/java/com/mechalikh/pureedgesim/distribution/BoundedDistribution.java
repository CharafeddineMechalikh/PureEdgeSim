package com.mechalikh.pureedgesim.distribution;

/**
 * Wraps another distribution and restricts its outcome to an interval.
 * <p>
 * Values outside the interval are rejected and drawn again, up to
 * {@value #MAX_ATTEMPTS} times. When every attempt falls outside the interval,
 * the last value is clamped, so that a badly parameterised scenario cannot
 * produce an infinite loop or an out-of-range value.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class BoundedDistribution implements Distribution {

	/** Maximum number of rejection attempts before the value is clamped. */
	public static final int MAX_ATTEMPTS = 100;

	/** The distribution actually sampled. */
	private final Distribution delegate;

	/** Lower bound of the accepted interval. */
	private final double min;

	/** Upper bound of the accepted interval. */
	private final double max;

	/**
	 * Creates a bounded view of a distribution.
	 *
	 * @param delegate the distribution to sample
	 * @param min      the inclusive lower bound
	 * @param max      the inclusive upper bound
	 * @throws IllegalArgumentException when the bounds are inconsistent
	 */
	public BoundedDistribution(Distribution delegate, double min, double max) {
		if (min > max)
			throw new IllegalArgumentException(
					"Distribution \"" + delegate.getName() + "\": the \"min\" attribute must be <= the \"max\" attribute");
		this.delegate = delegate;
		this.min = min;
		this.max = max;
	}

	/**
	 * Draws a value and keeps it only when it falls inside the interval.
	 *
	 * @return a value between {@code min} and {@code max}
	 */
	@Override
	public double sample() {
		double value = Double.NaN;
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			value = delegate.sample();
			if (value >= min && value <= max)
				return value;
		}
		return Math.min(max, Math.max(min, value));
	}

	/**
	 * Draws an integer value inside the interval.
	 *
	 * @return a value between {@code min} and {@code max}
	 */
	@Override
	public int sampleInt() {
		if (delegate.isDiscrete())
			return delegate.sampleInt();
		return (int) Math.floor(sample());
	}

	/**
	 * Returns the mean of the wrapped distribution, clamped to the interval. This
	 * is an approximation: the exact mean of a truncated distribution differs from
	 * the mean of the original one.
	 *
	 * @return the clamped mean of the wrapped distribution
	 */
	@Override
	public double mean() {
		return Math.min(max, Math.max(min, delegate.mean()));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the discrete flag of the wrapped distribution
	 */
	@Override
	public boolean isDiscrete() {
		return delegate.isDiscrete();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the name of the wrapped distribution
	 */
	@Override
	public String getName() {
		return delegate.getName();
	}

	/**
	 * Returns the inclusive lower bound.
	 *
	 * @return the minimum accepted value
	 */
	public double getMin() {
		return min;
	}

	/**
	 * Returns the inclusive upper bound.
	 *
	 * @return the maximum accepted value
	 */
	public double getMax() {
		return max;
	}

	/**
	 * Returns the distribution actually sampled.
	 *
	 * @return the wrapped distribution
	 */
	public Distribution getDelegate() {
		return delegate;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the name of the wrapped distribution, its parameters, and the bounds
	 */
	@Override
	public String toString() {
		return delegate.getName() + "(min=" + min + ", max=" + max + ")";
	}

}
