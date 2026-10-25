package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class TriangularDistributionTest {

	private TriangularDistribution sample() {
		return new TriangularDistribution(new Random(1), 1, 2, 6);
	}

	@Test
	void drawsStayInsideTheInterval() {
		DistributionTestSupport.samplesStayInsideTheSupport(sample());
	}

	@Test
	void meanIsTheAverageOfTheThreeParameters() {
		assertEquals(3.0, sample().mean(), 0.0);
	}

	@Test
	void boundsAndModeAreExposed() {
		TriangularDistribution distribution = sample();
		assertEquals(1.0, distribution.getMin(), 0.0);
		assertEquals(6.0, distribution.getMax(), 0.0);
		assertEquals(2.0, distribution.getMode(), 0.0);
	}

	@Test
	// A triangular [1,2,6] has a small variance, so the 5% band over 50000 draws
	// sits many standard errors away from the true mean.
	void empiricalAverageMatchesTheMean() {
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(sample());
	}

	@Test
	void nameIsTriangular() {
		assertEquals("triangular", sample().getName());
	}

	@Test
	void toStringShowsTheParameters() {
		assertEquals("triangular(min=1.0, mode=2.0, max=6.0)", sample().toString());
	}

	@Test
	void rejectsAnEmptyInterval() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new TriangularDistribution(new Random(1), 5, 5, 5));
		assertTrue(error.getMessage().contains("\"max\" must be > \"min\""));
	}

	@Test
	void rejectsAModeBelowTheMinimum() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new TriangularDistribution(new Random(1), 2, 1, 6));
		assertTrue(error.getMessage().contains("\"mode\" must be between \"min\" and \"max\""));
	}

	@Test
	void rejectsAModeAboveTheMaximum() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new TriangularDistribution(new Random(1), 1, 7, 6));
		assertTrue(error.getMessage().contains("\"mode\" must be between \"min\" and \"max\""));
	}

	@Test
	void aFixedSeedPinsTheWholeSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new TriangularDistribution(new Random(seed), 0, 3, 9));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new TriangularDistribution(new Random(seed), 0, 3, 9));
	}
}
