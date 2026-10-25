package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class GeometricDistributionTest {

	private GeometricDistribution sample() {
		return new GeometricDistribution(new Random(1), 0.3);
	}

	@Test
	void valuesAreNeverBelowOne() {
		DistributionTestSupport.sampledValuesAreWholeNumbers(sample());
		// getMin() is 1 and getMax() is +infinity, so every integer draw passes.
		DistributionTestSupport.samplesStayInsideTheSupport(sample());
	}

	@Test
	void lowerBoundIsOne() {
		assertEquals(1.0, sample().getMin(), 0.0);
	}

	@Test
	void meanIsOneOverProbability() {
		assertEquals(1 / 0.3, sample().mean(), 1e-12);
	}

	@Test
	// Over 50000 draws the standard error of a geometric (p=0.3, std ~2.79) is ~0.01,
	// far inside the 5% band around the mean 3.33.
	void empiricalAverageMatchesTheMean() {
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(new GeometricDistribution(new Random(2), 0.3));
	}

	@Test
	// With probability one the first trial succeeds, so the outcome is always one.
	void certainSuccessAlwaysReturnsOne() {
		GeometricDistribution certain = new GeometricDistribution(new Random(1), 1.0);
		assertEquals(1.0, certain.mean(), 1e-12);
		for (int draw = 0; draw < 100; draw++)
			assertEquals(1, certain.sampleInt());
	}

	@Test
	void nameIsGeometric() {
		assertEquals("geometric", sample().getName());
	}

	@Test
	void toStringShowsTheProbability() {
		assertEquals("geometric(probability=0.3)", sample().toString());
	}

	@Test
	void rejectsANonPositiveProbability() {
		IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
				() -> new GeometricDistribution(new Random(1), 0));
		IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
				() -> new GeometricDistribution(new Random(1), -0.5));
		assertTrue(zero.getMessage().contains("the parameter \"probability\" must be > 0 and <= 1"));
		assertTrue(negative.getMessage().contains("the parameter \"probability\" must be > 0 and <= 1"));
	}

	@Test
	void rejectsAProbabilityAboveOne() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new GeometricDistribution(new Random(1), 1.5));
		assertTrue(error.getMessage().contains("the parameter \"probability\" must be > 0 and <= 1"));
	}

	@Test
	void aFixedSeedPinsTheWholeSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new GeometricDistribution(new Random(seed), 0.4));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new GeometricDistribution(new Random(seed), 0.4));
	}
}
