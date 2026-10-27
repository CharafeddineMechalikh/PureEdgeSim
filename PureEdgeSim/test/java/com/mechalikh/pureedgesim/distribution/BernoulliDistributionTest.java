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

class BernoulliDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static BernoulliDistribution distribution() {
		return new BernoulliDistribution(new Random(1), 0.3);
	}

	@Test
	void onlyTheTwoOutcomesAppear() {
		BernoulliDistribution distribution = distribution();
		Set<Integer> outcomes = new HashSet<>();
		for (int draw = 0; draw < 400; draw++)
			outcomes.add(distribution.sampleInt());
		assertEquals(2, outcomes.size());
		assertTrue(outcomes.contains(0) && outcomes.contains(1));
	}

	@Test
	void theDrawsAreWholeNumbersInsideTheSupport() {
		BernoulliDistribution distribution = distribution();
		DistributionTestSupport.sampledValuesAreWholeNumbers(distribution);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		DistributionTestSupport.integerDrawsStayInsideTheSupport(distribution);
		assertEquals(0.0, distribution.getMin(), 0.0);
		assertEquals(1.0, distribution.getMax(), 0.0);
		assertTrue(distribution.isDiscrete());
	}

	@Test
	void theMeanIsTheSuccessProbability() {
		assertEquals(0.3, distribution().mean(), 0.0);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution());
	}

	@Test
	void theExtremeProbabilitiesNeverDraw() {
		BernoulliDistribution never = new BernoulliDistribution(new Random(1), 0);
		BernoulliDistribution always = new BernoulliDistribution(new Random(1), 1);
		for (int draw = 0; draw < 400; draw++) {
			assertEquals(0, never.sampleInt());
			assertEquals(1, always.sampleInt());
		}
	}

	@Test
	void aProbabilityOutsideTheUnitRangeIsNamed() {
		IllegalArgumentException below = assertThrows(IllegalArgumentException.class,
				() -> new BernoulliDistribution(new Random(1), -0.1));
		IllegalArgumentException above = assertThrows(IllegalArgumentException.class,
				() -> new BernoulliDistribution(new Random(1), 1.5));
		assertEquals("Distribution \"bernoulli\": the parameter \"probability\" must be between 0 and 1",
				below.getMessage());
		assertEquals(above.getMessage(), below.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new BernoulliDistribution(new Random(seed), 0.3));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new BernoulliDistribution(new Random(seed), 0.3));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(31);
		BernoulliDistribution distribution = new BernoulliDistribution(0.5);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		BernoulliDistribution distribution = distribution();
		assertEquals("bernoulli", distribution.getName());
		assertEquals("bernoulli(probability=0.3)", distribution.toString());
		assertEquals(0.3, distribution.getProbability(), 0.0);
	}

}
