package com.mechalikh.pureedgesim.distribution;

/**
 * A degenerate distribution that always returns the same value. It is the
 * representation of a scenario file that gives a plain number instead of a
 * distribution, so that the simulation can sample every value through the same
 * interface.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class ConstantDistribution implements Distribution {

	/** The value always returned. */
	private final double value;

	/**
	 * Creates a distribution that always returns the given value.
	 *
	 * @param value the constant value
	 */
	public ConstantDistribution(double value) {
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the constant value
	 */
	@Override
	public double sample() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the constant value truncated toward minus infinity
	 */
	@Override
	public int sampleInt() {
		return (int) Math.floor(value);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the constant value
	 */
	@Override
	public double mean() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "constant"}
	 */
	@Override
	public String getName() {
		return "constant";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the configured value
	 */
	@Override
	public double getMin() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the configured value
	 */
	@Override
	public double getMax() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code false}
	 */
	@Override
	public boolean isDiscrete() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the constant value
	 */
	@Override
	public String toString() {
		return "constant(" + value + ")";
	}

}
