package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpecialFunctionsTest {

	@Test
	// Gamma(1) = Gamma(2) = 1, so their logarithms are zero.
	void lnGammaOfTheUnitArgumentIsZero() {
		assertEquals(0.0, SpecialFunctions.lnGamma(1.0), 1e-9);
		assertEquals(0.0, SpecialFunctions.lnGamma(2.0), 1e-9);
	}

	@Test
	// Gamma(5) = 4! = 24.
	void lnGammaMatchesAFactorial() {
		assertEquals(Math.log(24.0), SpecialFunctions.lnGamma(5.0), 1e-9);
	}

	@Test
	// Gamma(1/2) = sqrt(pi), handled by the Lanczos branch (x >= 0.5).
	void lnGammaOfAHalfIsTheLogOfSquareRootPi() {
		assertEquals(0.5 * Math.log(Math.PI), SpecialFunctions.lnGamma(0.5), 1e-9);
	}

	@Test
	// Gamma(0.25) ~ 3.62561, reached through the reflection formula branch x < 0.5.
	void lnGammaUsesTheReflectionFormulaBelowAHalf() {
		assertEquals(Math.log(3.6256099082219083), SpecialFunctions.lnGamma(0.25), 1e-6);
	}

	@Test
	// Gamma(-1.5) = (4/3)*sqrt(pi) ~ 2.36327, also via the reflection branch for a
	// negative argument.
	void lnGammaSupportsNegativeFractionalArguments() {
		assertEquals(Math.log((4.0 / 3.0) * Math.sqrt(Math.PI)), SpecialFunctions.lnGamma(-1.5), 1e-4);
	}

	@Test
	// The reflection identity Gamma(x)*Gamma(1-x) = pi/sin(pi x) holds for a value
	// that exercises the x < 0.5 branch against the x >= 0.5 branch.
	void theReflectionIdentityHolds() {
		double x = 0.3;
		double sum = SpecialFunctions.lnGamma(x) + SpecialFunctions.lnGamma(1 - x);
		assertEquals(Math.log(Math.PI / Math.sin(Math.PI * x)), sum, 1e-6);
	}

	@Test
	// gamma() is exp(lnGamma()), checked on integer and half-integer arguments.
	void gammaExponentiatesTheLogGamma() {
		assertEquals(24.0, SpecialFunctions.gamma(5.0), 1e-6);
		assertEquals(1.0, SpecialFunctions.gamma(1.0), 1e-9);
		assertEquals(Math.sqrt(Math.PI), SpecialFunctions.gamma(0.5), 1e-6);
		assertEquals(2.0, SpecialFunctions.gamma(3.0), 1e-6);
	}

	@Test
	// Gamma(1 + 1/2) is the factor the Weibull mean relies on.
	void gammaOfThreeHalvesMatchesTheKnownConstant() {
		assertEquals(0.5 * Math.sqrt(Math.PI), SpecialFunctions.gamma(1.5), 1e-6);
	}
}
