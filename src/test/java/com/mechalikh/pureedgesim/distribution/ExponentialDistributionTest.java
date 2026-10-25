package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExponentialDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static ExponentialDistribution distribution() {
		return new ExponentialDistribution(new Random(1), 20);
	}

	@Test
	void samplesStayPositive() {
		ExponentialDistribution distribution = distribution();
		assertEquals(0.0, distribution.getMin(), 0.0);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		for (int draw = 0; draw < 400; draw++)
			assertTrue(distribution.sample() > 0, "A gap between two events cannot be exactly zero");
	}

	@Test
	void theMeanMatchesTheEmpiricalAverage() {
		assertEquals(20.0, distribution().mean(), 0.0);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution());
	}

	@Test
	void theLawIsMemorylessEnoughToBeSkewedRight() {
		ExponentialDistribution distribution = distribution();
		int belowMean = 0;
		for (int draw = 0; draw < 20000; draw++)
			if (distribution.sample() < 20)
				belowMean++;
		// 1 - exp(-1) of the mass of an exponential law sits below its mean.
		double share = belowMean / 20000.0;
		assertTrue(share > 0.61 && share < 0.66, share + " of the draws fell below the mean");
	}

	@Test
	void aMeanNotStrictlyPositiveIsRejected() {
		IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
				() -> new ExponentialDistribution(new Random(1), 0));
		IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
				() -> new ExponentialDistribution(new Random(1), -4));
		assertEquals("Distribution \"exponential\": the parameter \"mean\" must be > 0.0", zero.getMessage());
		assertEquals(negative.getMessage(), zero.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new ExponentialDistribution(new Random(seed), 20));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new ExponentialDistribution(new Random(seed), 20));
	}

	@Test
	void integerDrawsAreFloored() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new ExponentialDistribution(new Random(seed), 20));
		DistributionTestSupport.integerDrawsStayInsideTheSupport(new ExponentialDistribution(new Random(1), 0.5));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(23);
		ExponentialDistribution distribution = new ExponentialDistribution(5);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		ExponentialDistribution distribution = distribution();
		assertEquals("exponential", distribution.getName());
		assertEquals("exponential(mean=20.0)", distribution.toString());
	}

}
