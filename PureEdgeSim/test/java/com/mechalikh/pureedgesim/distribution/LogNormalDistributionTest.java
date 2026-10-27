package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LogNormalDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static LogNormalDistribution distribution() {
		return new LogNormalDistribution(new Random(1), 25, 15);
	}

	@Test
	void samplesStayStrictlyPositive() {
		LogNormalDistribution distribution = distribution();
		assertEquals(0.0, distribution.getMin(), 0.0);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		for (int draw = 0; draw < 400; draw++)
			assertTrue(distribution.sample() > 0);
	}

	@Test
	void theConfiguredMeanAndSpreadAreTheOnesOfTheValues() {
		// The parameters describe the generated values, not the underlying normal.
		DistributionTestSupport.empiricalSpreadMatchesTheConfiguredStandardDeviation(distribution(), 25, 15, 0.05);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution());
	}

	@Test
	void theUnderlyingNormalIsDerivedFromTheRequestedMoments() {
		LogNormalDistribution distribution = distribution();
		double sigma = Math.sqrt(Math.log(1 + 15.0 * 15 / (25 * 25)));
		double mu = Math.log(25) - sigma * sigma / 2;
		assertEquals(sigma, distribution.getSigma(), 1e-12);
		assertEquals(mu, distribution.getMu(), 1e-12);
	}

	@Test
	void theLawIsSkewedToTheRight() {
		LogNormalDistribution distribution = distribution();
		int belowMean = 0;
		double largest = 0;
		for (int draw = 0; draw < 20000; draw++) {
			double value = distribution.sample();
			if (value < 25)
				belowMean++;
			largest = Math.max(largest, value);
		}
		assertTrue(belowMean > 11000, belowMean + " of 20000 draws should sit below the mean of a skewed law");
		assertTrue(largest > 100, "The right tail should reach far above the mean, got " + largest);
	}

	@Test
	void eachParameterIsCheckedByName() {
		IllegalArgumentException mean = assertThrows(IllegalArgumentException.class,
				() -> new LogNormalDistribution(new Random(1), 0, 5));
		IllegalArgumentException std = assertThrows(IllegalArgumentException.class,
				() -> new LogNormalDistribution(new Random(1), 10, 0));
		assertEquals("Distribution \"lognormal\": the parameter \"mean\" must be > 0.0", mean.getMessage());
		assertEquals("Distribution \"lognormal\": the parameter \"std\" must be > 0.0", std.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(
				seed -> new LogNormalDistribution(new Random(seed), 25, 15));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new LogNormalDistribution(new Random(seed), 25, 15));
	}

	@Test
	void integerDrawsAreFloored() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new LogNormalDistribution(new Random(seed), 25, 15));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(29);
		LogNormalDistribution distribution = new LogNormalDistribution(10, 2);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		LogNormalDistribution distribution = distribution();
		assertEquals("lognormal", distribution.getName());
		assertEquals("lognormal(mean=25.0, std=15.0)", distribution.toString());
	}

}
