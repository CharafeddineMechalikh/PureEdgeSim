package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class UniformDistributionTest {

	private UniformDistribution sample() {
		return new UniformDistribution(new Random(1), 1, 5);
	}

	@Test
	void drawsStayInsideTheInterval() {
		DistributionTestSupport.samplesStayInsideTheSupport(sample());
	}

	@Test
	void meanIsTheMidpoint() {
		assertEquals(3.0, sample().mean(), 0.0);
	}

	@Test
	void boundsAreTheConfiguredInterval() {
		UniformDistribution distribution = sample();
		assertEquals(1.0, distribution.getMin(), 0.0);
		assertEquals(5.0, distribution.getMax(), 0.0);
	}

	@Test
	// Over 50000 draws the standard error of a uniform [1,5] is ~0.5% of the mean,
	// so the 5% band used by the helper only fails on a real defect.
	void empiricalAverageMatchesTheMidpoint() {
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(sample());
	}

	@Test
	void nameIsUniform() {
		assertEquals("uniform", sample().getName());
	}

	@Test
	void toStringShowsTheBounds() {
		assertEquals("uniform(min=1.0, max=5.0)", sample().toString());
	}

	@Test
	void rejectsAnEmptyInterval() {
		IllegalArgumentException equal = assertThrows(IllegalArgumentException.class,
				() -> new UniformDistribution(new Random(1), 5, 5));
		IllegalArgumentException inverted = assertThrows(IllegalArgumentException.class,
				() -> new UniformDistribution(new Random(1), 5, 1));
		assertTrue(equal.getMessage().contains("\"max\" must be > \"min\""));
		assertTrue(inverted.getMessage().contains("\"max\" must be > \"min\""));
	}

	@Test
	void aFixedSeedPinsTheWholeSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new UniformDistribution(new Random(seed), 0, 10));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new UniformDistribution(new Random(seed), 0, 10));
	}

	@Test
	void theIntegerDrawIsTheFloorOfTheDoubleDraw() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new UniformDistribution(new Random(seed), 0, 10));
	}
}
