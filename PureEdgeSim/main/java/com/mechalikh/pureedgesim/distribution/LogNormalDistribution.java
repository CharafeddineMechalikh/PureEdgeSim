package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Log-normal distribution, parameterised by the mean and the standard deviation
 * of the values it produces, not of the underlying normal variable. The
 * parameters are converted with
 * 
 * <pre>
 * sigma = sqrt(ln(1 + std^2 / mean^2))
 * mu    = ln(mean) - sigma^2 / 2
 * </pre>
 * 
 * so that a scenario file can state the average task length and its spread
 * directly. The result is always positive, which makes this distribution the
 * usual choice for file sizes, container sizes, and service durations.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class LogNormalDistribution extends AbstractDistribution {

	/** Mean of the produced values. */
	private final double mean;

	/** Standard deviation of the produced values. */
	private final double standardDeviation;

	/** Mean of the underlying normal variable. */
	private final double mu;

	/** Standard deviation of the underlying normal variable. */
	private final double sigma;

	/**
	 * Creates a log-normal distribution.
	 *
	 * @param mean              the wanted mean of the values, must be positive
	 * @param standardDeviation the wanted standard deviation, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public LogNormalDistribution(double mean, double standardDeviation) {
		this(Randomness.getRandom(), mean, standardDeviation);
	}

	/**
	 * Creates a log-normal distribution drawing from the supplied generator.
	 *
	 * @param random            the generator to draw from
	 * @param mean              the wanted mean of the values, must be positive
	 * @param standardDeviation the wanted standard deviation, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public LogNormalDistribution(Random random, double mean, double standardDeviation) {
		super(random);
		requirePositive("mean", mean, 0);
		requirePositive("std", standardDeviation, 0);
		this.mean = mean;
		this.standardDeviation = standardDeviation;
		this.sigma = Math.sqrt(Math.log(1 + (standardDeviation * standardDeviation) / (mean * mean)));
		this.mu = Math.log(mean) - (sigma * sigma) / 2;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a positive value whose average and spread match the configured
	 *         parameters
	 */
	@Override
	public double sample() {
		return Math.exp(mu + sigma * random.nextGaussian());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the configured mean
	 */
	@Override
	public double mean() {
		return mean;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "lognormal"}
	 */
	@Override
	public String getName() {
		return "lognormal";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, log-normal values are always positive
	 */
	@Override
	public double getMin() {
		return 0;
	}

	/**
	 * Returns the mean of the underlying normal variable.
	 *
	 * @return mu
	 */
	public double getMu() {
		return mu;
	}

	/**
	 * Returns the standard deviation of the underlying normal variable.
	 *
	 * @return sigma
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "lognormal(mean=" + mean + ", std=" + standardDeviation + ")";
	}

}
