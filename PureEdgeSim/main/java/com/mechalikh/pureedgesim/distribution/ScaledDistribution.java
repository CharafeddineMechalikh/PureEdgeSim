package com.mechalikh.pureedgesim.distribution;

/**
 * Multiplies every value of another distribution by a positive constant.
 * <p>
 * Used to convert a distribution written in the units of a scenario file into the
 * units used by the simulator, for example the sizes written in kilobytes in
 * {@code applications.xml} and handled in bits everywhere else. The mean and the
 * bounds are scaled the same way, so the wrapped distribution stays consistent.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class ScaledDistribution implements Distribution {

	/** The distribution whose values are scaled. */
	private final Distribution delegate;

	/** The positive factor applied to every value. */
	private final double factor;

	/**
	 * Creates a scaled view of a distribution.
	 *
	 * @param delegate the distribution to scale
	 * @param factor   the multiplication factor, must be positive
	 * @throws IllegalArgumentException when the factor is not positive
	 */
	public ScaledDistribution(Distribution delegate, double factor) {
		if (factor <= 0)
			throw new IllegalArgumentException(
					"Distribution \"" + delegate.getName() + "\": the scaling factor must be > 0, found \"" + factor + "\"");
		this.delegate = delegate;
		this.factor = factor;
	}

	/**
	 * Draws a value and scales it.
	 *
	 * @return the scaled value
	 */
	@Override
	public double sample() {
		return delegate.sample() * factor;
	}

	/**
	 * Draws a value, scales it and truncates it toward minus infinity.
	 *
	 * @return the scaled value as an integer
	 */
	@Override
	public int sampleInt() {
		if (delegate.isDiscrete())
			return (int) Math.floor(delegate.sampleInt() * factor);
		return (int) Math.floor(sample());
	}

	/**
	 * Returns the scaled mean of the wrapped distribution.
	 *
	 * @return the mean multiplied by the factor
	 */
	@Override
	public double mean() {
		return delegate.mean() * factor;
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
	 * {@inheritDoc}
	 *
	 * @return the discrete flag of the wrapped distribution
	 */
	@Override
	public boolean isDiscrete() {
		return delegate.isDiscrete() && factor == Math.floor(factor);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the scaled lower bound of the wrapped distribution
	 */
	@Override
	public double getMin() {
		return delegate.getMin() * factor;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the scaled upper bound of the wrapped distribution
	 */
	@Override
	public double getMax() {
		return delegate.getMax() * factor;
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
	 * Returns the multiplication factor.
	 *
	 * @return the factor applied to every value
	 */
	public double getFactor() {
		return factor;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the wrapped distribution and the factor
	 */
	@Override
	public String toString() {
		return delegate + " x " + factor;
	}

}
