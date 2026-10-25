package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PoissonDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static PoissonDistribution smallRate() {
		return new PoissonDistribution(new Random(1), 4);
	}

	private static PoissonDistribution largeRate() {
		return new PoissonDistribution(new Random(1), 80);
	}

	@Test
	void theKnuthBranchOnlyDrawsWholeCounts() {
		PoissonDistribution distribution = smallRate();
		DistributionTestSupport.sampledValuesAreWholeNumbers(distribution);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		Set<Integer> outcomes = new HashSet<>();
		for (int draw = 0; draw < 400; draw++)
			outcomes.add(distribution.sampleInt());
		assertTrue(outcomes.size() > 4, "Only " + outcomes.size() + " different counts appeared");
		assertTrue(distribution.isDiscrete());
	}

	@Test
	void theApproximationBranchOnlyDrawsWholeCounts() {
		// Above a mean of 30 the class switches to the normal approximation.
		PoissonDistribution distribution = largeRate();
		DistributionTestSupport.sampledValuesAreWholeNumbers(distribution);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		for (int draw = 0; draw < 400; draw++)
			assertTrue(distribution.sampleInt() >= 0);
	}

	@Test
	void theMeanIsTheRateInBothBranches() {
		assertEquals(4.0, smallRate().mean(), 0.0);
		assertEquals(80.0, largeRate().mean(), 0.0);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(smallRate());
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(largeRate());
	}

	@Test
	void theSupportStartsAtZeroAndIsOpenAbove() {
		PoissonDistribution distribution = smallRate();
		assertEquals(0.0, distribution.getMin(), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, distribution.getMax());
	}

	@Test
	void aRateOfZeroWouldNeverFinishCounting() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new PoissonDistribution(new Random(1), 0));
		assertEquals("Distribution \"poisson\": the parameter \"mean\" must be > 0", exception.getMessage());
		IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
				() -> new PoissonDistribution(new Random(1), -2));
		assertEquals(exception.getMessage(), negative.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new PoissonDistribution(new Random(seed), 4));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new PoissonDistribution(new Random(seed), 4));
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new PoissonDistribution(new Random(seed), 80));
	}

	@Test
	void theDoubleDrawIsTheExactCount() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(seed -> new PoissonDistribution(new Random(seed), 4));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(33);
		PoissonDistribution distribution = new PoissonDistribution(12);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		PoissonDistribution distribution = smallRate();
		assertEquals("poisson", distribution.getName());
		assertEquals("poisson(mean=4.0)", distribution.toString());
	}

}
