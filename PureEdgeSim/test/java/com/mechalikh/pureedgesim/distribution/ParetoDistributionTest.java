package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParetoDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static ParetoDistribution distribution() {
		return new ParetoDistribution(new Random(1), 1, 3);
	}

	@Test
	void noDrawFallsBelowTheMinimum() {
		ParetoDistribution distribution = distribution();
		assertEquals(1.0, distribution.getMin(), 0.0);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		for (int draw = 0; draw < 400; draw++)
			assertTrue(distribution.sample() >= 1);
	}

	@Test
	void theTailIsHeavyEnoughToLeaveRoomForOutliers() {
		ParetoDistribution distribution = distribution();
		double largest = 0;
		for (int draw = 0; draw < 5000; draw++)
			largest = Math.max(largest, distribution.sample());
		assertTrue(largest > 10, "A tail index of 3 should still produce values far above the minimum, got " + largest);
	}

	@Test
	void theMeanMatchesTheEmpiricalAverage() {
		assertEquals(1.5, distribution().mean(), 1e-12);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution(), 0.1, 50000);
	}

	@Test
	void aTailIndexOfOneOrLessHasNoFiniteMean() {
		assertEquals(Double.POSITIVE_INFINITY, new ParetoDistribution(new Random(1), 5, 1).mean());
		assertEquals(Double.POSITIVE_INFINITY, new ParetoDistribution(new Random(1), 5, 0.4).mean());
	}

	@Test
	void eachParameterIsCheckedByName() {
		IllegalArgumentException scale = assertThrows(IllegalArgumentException.class,
				() -> new ParetoDistribution(new Random(1), 0, 2));
		IllegalArgumentException shape = assertThrows(IllegalArgumentException.class,
				() -> new ParetoDistribution(new Random(1), 10, 0));
		assertEquals("Distribution \"pareto\": the parameter \"scale\" must be > 0.0", scale.getMessage());
		assertEquals("Distribution \"pareto\": the parameter \"shape\" must be > 0.0", shape.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new ParetoDistribution(new Random(seed), 1, 3));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new ParetoDistribution(new Random(seed), 1, 3));
	}

	@Test
	void integerDrawsAreFloored() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new ParetoDistribution(new Random(seed), 1, 3));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(27);
		ParetoDistribution distribution = new ParetoDistribution(2, 1.5);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		ParetoDistribution distribution = distribution();
		assertEquals("pareto", distribution.getName());
		assertEquals("pareto(scale=1.0, shape=3.0)", distribution.toString());
		assertEquals(1.0, distribution.getScale(), 0.0);
		assertEquals(3.0, distribution.getShape(), 0.0);
	}

}
