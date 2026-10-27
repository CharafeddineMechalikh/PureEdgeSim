package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ScaledDistributionTest {

	@Test
	void everyDrawIsMultipliedByTheFactor() {
		ScaledDistribution scaled = new ScaledDistribution(new UniformDistribution(new Random(1), 1, 5), 3);
		for (int draw = 0; draw < 200; draw++) {
			double value = scaled.sample();
			assertTrue(value >= 3 && value <= 15, "scaled sample out of [3,15]: " + value);
		}
	}

	@Test
	void theMeanScalesWithTheFactor() {
		ScaledDistribution scaled = new ScaledDistribution(new ConstantDistribution(5), 2);
		assertEquals(10.0, scaled.mean(), 1e-12);
	}

	@Test
	void theBoundsScaleWithTheFactor() {
		ScaledDistribution scaled = new ScaledDistribution(new UniformDistribution(new Random(1), 0, 10), 3);
		assertEquals(0.0, scaled.getMin(), 0.0);
		assertEquals(30.0, scaled.getMax(), 0.0);
	}

	@Test
	// Scaling a uniform [1,5] by 3 keeps the mean at the scaled midpoint.
	void empiricalAverageIsTheScaledMean() {
		ScaledDistribution scaled = new ScaledDistribution(new UniformDistribution(new Random(2), 1, 5), 3);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(scaled, 0.05, DistributionTestSupport.MEAN_DRAWS);
	}

	@Test
	void nameAndDelegateAndFactorAreExposed() {
		ConstantDistribution delegate = new ConstantDistribution(2);
		ScaledDistribution scaled = new ScaledDistribution(delegate, 4);
		assertEquals("constant", scaled.getName());
		assertEquals(delegate, scaled.getDelegate());
		assertEquals(4.0, scaled.getFactor(), 0.0);
	}

	@Test
	void aDiscreteDelegateStaysDiscreteForAnIntegralFactor() {
		BernoulliDistribution delegate = new BernoulliDistribution(new Random(1), 0.5);
		ScaledDistribution integral = new ScaledDistribution(delegate, 2);
		assertTrue(integral.isDiscrete());
		for (int draw = 0; draw < 200; draw++) {
			int value = integral.sampleInt();
			assertTrue(value == 0 || value == 2, "Bernoulli*2 must give 0 or 2, got " + value);
		}
	}

	@Test
	// A fractional factor breaks integrality, so the wrapper is no longer discrete.
	void aDiscreteDelegateBecomesContinuousForAFractionalFactor() {
		BernoulliDistribution delegate = new BernoulliDistribution(new Random(1), 0.5);
		ScaledDistribution fractional = new ScaledDistribution(delegate, 1.5);
		assertFalse(fractional.isDiscrete());
	}

	@Test
	void aContinuousDelegateFloorsTheScaledSample() {
		ScaledDistribution scaled = new ScaledDistribution(new UniformDistribution(new Random(1), 0, 1), 10);
		for (int draw = 0; draw < 200; draw++) {
			int value = scaled.sampleInt();
			assertTrue(value >= 0 && value < 10, "scaled sampleInt out of [0,10): " + value);
		}
	}

	@Test
	void toStringCombinesTheDelegateAndTheFactor() {
		ScaledDistribution scaled = new ScaledDistribution(new ConstantDistribution(5), 2);
		assertEquals("constant(5.0) x 2.0", scaled.toString());
	}

	@Test
	void rejectsANonPositiveFactor() {
		IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
				() -> new ScaledDistribution(new ConstantDistribution(1), 0));
		IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
				() -> new ScaledDistribution(new ConstantDistribution(1), -2));
		assertTrue(zero.getMessage().contains("the scaling factor must be > 0, found \"0.0\""));
		assertTrue(zero.getMessage().contains("constant"));
		assertTrue(negative.getMessage().contains("the scaling factor must be > 0, found \"-2.0\""));
	}
}
