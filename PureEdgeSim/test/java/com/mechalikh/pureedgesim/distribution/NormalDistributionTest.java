package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class NormalDistributionTest {

	private NormalDistribution sample() {
		return new NormalDistribution(new Random(1), 10, 2);
	}

	@Test
	void meanIsTheConfiguredCenter() {
		assertEquals(10.0, sample().mean(), 0.0);
	}

	@Test
	void standardDeviationIsExposed() {
		assertEquals(2.0, sample().getStandardDeviation(), 0.0);
	}

	@Test
	// A normal distribution has an unbounded support, so the reported bounds are
	// the infinities and every finite draw trivially lies inside them.
	void theSupportIsTheWholeNumberLine() {
		NormalDistribution distribution = sample();
		assertEquals(Double.NEGATIVE_INFINITY, distribution.getMin(), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, distribution.getMax(), 0.0);
	}

	@Test
	// Over 400 draws of a standard normal, both signs appear with probability
	// 1 - 2^-400, so this only fails if the generator is broken.
	void drawsScatterOnBothSidesOfTheMean() {
		NormalDistribution distribution = new NormalDistribution(new Random(2), 0, 1);
		boolean below = false;
		boolean above = false;
		for (int draw = 0; draw < 400; draw++) {
			double value = distribution.sample();
			below |= value < 0;
			above |= value > 0;
		}
		assertTrue(below, "expected some draws below the mean");
		assertTrue(above, "expected some draws above the mean");
	}

	@Test
	// The standard error of the mean over 50000 N(10,2) draws is 0.009, far below
	// the 5% band; the sampled spread is also within a few percent of 2.
	void empiricalAverageAndSpreadMatchTheParameters() {
		NormalDistribution forMean = sample();
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(forMean);
		DistributionTestSupport.empiricalSpreadMatchesTheConfiguredStandardDeviation(
				new NormalDistribution(new Random(3), 10, 2), 10, 2, 0.05);
	}

	@Test
	void nameIsNormal() {
		assertEquals("normal", sample().getName());
	}

	@Test
	void toStringShowsTheParameters() {
		assertEquals("normal(mean=10.0, std=2.0)", sample().toString());
	}

	@Test
	void rejectsANonPositiveDeviation() {
		IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
				() -> new NormalDistribution(new Random(1), 0, 0));
		IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
				() -> new NormalDistribution(new Random(1), 0, -1));
		assertTrue(zero.getMessage().contains("the parameter \"std\" must be > 0.0"));
		assertTrue(negative.getMessage().contains("the parameter \"std\" must be > 0.0"));
	}

	@Test
	void aFixedSeedPinsTheWholeSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new NormalDistribution(new Random(seed), 0, 1));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new NormalDistribution(new Random(seed), 0, 1));
	}

	@Test
	void theIntegerDrawIsTheFloorOfTheDoubleDraw() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new NormalDistribution(new Random(seed), 0, 1));
	}
}
