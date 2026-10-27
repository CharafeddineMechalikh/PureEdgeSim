package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GammaDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static GammaDistribution shapeAboveOne() {
		return new GammaDistribution(new Random(1), 2, 3);
	}

	private static GammaDistribution shapeBelowOne() {
		return new GammaDistribution(new Random(1), 0.4, 2);
	}

	@Test
	void samplesStayPositiveForBothSamplingBranches() {
		DistributionTestSupport.samplesStayInsideTheSupport(shapeAboveOne());
		DistributionTestSupport.samplesStayInsideTheSupport(shapeBelowOne());
		assertEquals(0.0, shapeAboveOne().getMin(), 0.0);
	}

	@Test
	void theMeanIsShapeTimesScale() {
		assertEquals(6.0, shapeAboveOne().mean(), 0.0);
		assertEquals(0.8, shapeBelowOne().mean(), 1e-12);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(shapeAboveOne());
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(shapeBelowOne(), 0.1, 50000);
	}

	@Test
	void aShapeOfOneReproducesTheExponentialLaw() {
		GammaDistribution exponentialLike = new GammaDistribution(new Random(1), 1, 7);
		assertEquals(7.0, exponentialLike.mean(), 0.0);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(exponentialLike);
	}

	@Test
	void aLargeShapeGathersAroundItsMean() {
		GammaDistribution concentrated = new GammaDistribution(new Random(1), 60, 2);
		int inside = 0;
		for (int draw = 0; draw < 4000; draw++) {
			double value = concentrated.sample();
			if (value >= 110 && value <= 130)
				inside++;
		}
		// Shape 60 and scale 2 give a mean of 120 and a deviation of 15.5, so a
		// +-10 band holds about half of the draws and never the whole mass.
		assertTrue(inside > 1700 && inside < 2200, inside + " of 4000 draws fell inside the band around the mean");
	}

	@Test
	void eachParameterIsCheckedByName() {
		IllegalArgumentException shape = assertThrows(IllegalArgumentException.class,
				() -> new GammaDistribution(new Random(1), 0, 5));
		IllegalArgumentException scale = assertThrows(IllegalArgumentException.class,
				() -> new GammaDistribution(new Random(1), 5, -2));
		assertEquals("Distribution \"gamma\": the parameter \"shape\" must be > 0.0", shape.getMessage());
		assertEquals("Distribution \"gamma\": the parameter \"scale\" must be > 0.0", scale.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new GammaDistribution(new Random(seed), 2, 3));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new GammaDistribution(new Random(seed), 2, 3));
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new GammaDistribution(new Random(seed), 0.4, 2));
	}

	@Test
	void integerDrawsAreFlooredAndTheLawStaysContinuous() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new GammaDistribution(new Random(seed), 2, 3));
		assertFalse(shapeAboveOne().isDiscrete());
	}

	@Test
	void theParametersAreReadableBack() {
		GammaDistribution distribution = shapeAboveOne();
		assertEquals(2.0, distribution.getShape(), 0.0);
		assertEquals(3.0, distribution.getScale(), 0.0);
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(24);
		GammaDistribution distribution = new GammaDistribution(3, 1);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		GammaDistribution distribution = shapeAboveOne();
		assertEquals("gamma", distribution.getName());
		assertEquals("gamma(shape=2.0, scale=3.0)", distribution.toString());
	}

}
