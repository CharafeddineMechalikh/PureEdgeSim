package com.mechalikh.pureedgesim.distribution;

import java.util.Random;

/**
 * Beta distribution over {@code [0, 1]}, sampled as the ratio of two gamma
 * variables. It describes proportions and probabilities, such as the share of
 * tasks that a device dedicates to a workload or the failure probability of a
 * link when that probability itself varies.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public class BetaDistribution extends AbstractDistribution {

	/** First shape parameter. */
	private final double alpha;

	/** Second shape parameter. */
	private final double beta;

	/** Gamma generator of the numerator. */
	private final GammaDistribution alphaGamma;

	/** Gamma generator of the denominator part. */
	private final GammaDistribution betaGamma;

	/**
	 * Creates a beta distribution.
	 *
	 * @param alpha the first shape parameter, must be positive
	 * @param beta  the second shape parameter, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public BetaDistribution(double alpha, double beta) {
		this(Randomness.getRandom(), alpha, beta);
	}

	/**
	 * Creates a beta distribution drawing from the supplied generator.
	 *
	 * @param random the generator to draw from
	 * @param alpha  the first shape parameter, must be positive
	 * @param beta   the second shape parameter, must be positive
	 * @throws IllegalArgumentException when a parameter is not positive
	 */
	public BetaDistribution(Random random, double alpha, double beta) {
		super(random);
		requirePositive("alpha", alpha, 0);
		requirePositive("beta", beta, 0);
		this.alpha = alpha;
		this.beta = beta;
		this.alphaGamma = new GammaDistribution(random, alpha, 1);
		this.betaGamma = new GammaDistribution(random, beta, 1);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return a value between 0 and 1
	 */
	@Override
	public double sample() {
		double first = alphaGamma.sample();
		double second = betaGamma.sample();
		if (first + second == 0)
			return 0;
		return first / (first + second);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code alpha / (alpha + beta)}
	 */
	@Override
	public double mean() {
		return alpha / (alpha + beta);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@code "beta"}
	 */
	@Override
	public String getName() {
		return "beta";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return zero, the support of the beta distribution
	 */
	@Override
	public double getMin() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return one, the support of the beta distribution
	 */
	@Override
	public double getMax() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the distribution parameters
	 */
	@Override
	public String toString() {
		return "beta(alpha=" + alpha + ", beta=" + beta + ")";
	}

}
