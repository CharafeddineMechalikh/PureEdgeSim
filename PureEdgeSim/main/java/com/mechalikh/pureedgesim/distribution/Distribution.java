package com.mechalikh.pureedgesim.distribution;

/**
 * A probability distribution used to draw the values that the scenario files do
 * not fix, such as task lengths, message sizes, arrival gaps, speeds, or node
 * positions.
 * <p>
 * Implementations are stateless apart from their parameters and the random
 * generator they were built with, so a single instance can be shared by every
 * task or node that samples the same quantity. Callers that need integer values
 * use {@link #sampleInt()}, which truncates continuous distributions and returns
 * the exact outcome of discrete ones.
 * <p>
 * Distributions are created from the scenario files with
 * {@link DistributionFactory}, or programmatically with the constructors of the
 * concrete classes.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public interface Distribution {

	/**
	 * Draws one value from the distribution.
	 *
	 * @return the sampled value
	 */
	double sample();

	/**
	 * Draws one value and truncates it toward minus infinity.
	 * <p>
	 * Discrete distributions override this method and return their exact outcome.
	 *
	 * @return the sampled value as an integer
	 */
	int sampleInt();

	/**
	 * Returns the theoretical mean of the distribution, independent of any sample.
	 * <p>
	 * Used for logging and to keep the reported averages of a scenario meaningful
	 * when a value is described by a distribution rather than by a constant.
	 *
	 * @return the expected value
	 */
	double mean();

	/**
	 * Returns the name under which the distribution is configured in the scenario
	 * files.
	 *
	 * @return the distribution name, in lower case
	 */
	String getName();

	/**
	 * Returns whether the distribution only produces integer values.
	 *
	 * @return {@code true} for discrete distributions such as Poisson or Bernoulli
	 */
	boolean isDiscrete();

	/**
	 * Returns the smallest value the distribution can produce.
	 * <p>
	 * Distributions with an unbounded support, such as the normal one, return
	 * {@link Double#NEGATIVE_INFINITY}. The validation of the scenario files uses
	 * this bound to reject values that cannot fit.
	 *
	 * @return the lower bound of the support, or minus infinity
	 */
	default double getMin() {
		return Double.NEGATIVE_INFINITY;
	}

	/**
	 * Returns the largest value the distribution can produce.
	 *
	 * @return the upper bound of the support, or plus infinity
	 * @see #getMin()
	 */
	default double getMax() {
		return Double.POSITIVE_INFINITY;
	}

}
