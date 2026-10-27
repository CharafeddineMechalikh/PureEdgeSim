package com.mechalikh.pureedgesim.distribution;

/**
 * Lanczos approximation of the Gamma function, needed by the Weibull and Beta
 * distributions.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public final class SpecialFunctions {

	/** Lanczos coefficients for g = 7, accurate to about 15 decimal digits. */
	private static final double[] LANCZOS_COEFFICIENTS = { 0.99999999999980993, 676.5203681218851,
			-1259.1392167224028, 771.32342877765313, -176.61502916214059, 12.507343278686905,
			-0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7 };

	private SpecialFunctions() {
	}

	/**
	 * Returns the natural logarithm of the Gamma function of {@code x}.
	 * <p>
	 * The reflection formula is applied when {@code x} is below 0.5, so fractional
	 * and negative arguments are supported.
	 *
	 * @param x the argument, not a non-positive integer
	 * @return ln(Gamma(x))
	 */
	public static double lnGamma(double x) {
		if (x < 0.5)
			return Math.log(Math.PI / Math.sin(Math.PI * x)) - lnGamma(1 - x);

		x -= 1;
		double sum = LANCZOS_COEFFICIENTS[0];
		for (int i = 1; i < LANCZOS_COEFFICIENTS.length; i++)
			sum += LANCZOS_COEFFICIENTS[i] / (x + i);

		double t = x + LANCZOS_COEFFICIENTS.length - 1.5;
		return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(sum);
	}

	/**
	 * Returns the Gamma function of {@code x}.
	 *
	 * @param x the argument, not a non-positive integer
	 * @return Gamma(x)
	 */
	public static double gamma(double x) {
		return Math.exp(lnGamma(x));
	}

}
