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

class BinomialDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static BinomialDistribution smallCount() {
		return new BinomialDistribution(new Random(1), 10, 0.3);
	}

	private static BinomialDistribution largeCount() {
		return new BinomialDistribution(new Random(1), 200, 0.3);
	}

	@Test
	void theTrialByTrialBranchStaysBetweenZeroAndTheNumberOfTrials() {
		BinomialDistribution distribution = smallCount();
		DistributionTestSupport.sampledValuesAreWholeNumbers(distribution);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		DistributionTestSupport.integerDrawsStayInsideTheSupport(distribution);
		Set<Integer> outcomes = new HashSet<>();
		for (int draw = 0; draw < 400; draw++)
			outcomes.add(distribution.sampleInt());
		assertTrue(outcomes.size() > 5, "Only " + outcomes.size() + " different counts appeared");
		assertTrue(distribution.isDiscrete());
	}

	@Test
	void theNormalApproximationBranchKeepsTheSupport() {
		// Above 64 trials the class switches to the normal approximation.
		BinomialDistribution distribution = largeCount();
		DistributionTestSupport.sampledValuesAreWholeNumbers(distribution);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		DistributionTestSupport.integerDrawsStayInsideTheSupport(distribution);
	}

	@Test
	void theMeanIsTrialsTimesProbabilityInBothBranches() {
		assertEquals(3.0, smallCount().mean(), 1e-12);
		assertEquals(60.0, largeCount().mean(), 1e-12);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(smallCount());
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(largeCount());
	}

	@Test
	void theBoundsArePublishedAsTheSupport() {
		BinomialDistribution distribution = smallCount();
		assertEquals(0.0, distribution.getMin(), 0.0);
		assertEquals(10.0, distribution.getMax(), 0.0);
	}

	@Test
	void aSingleTrialBehavesLikeACoinToss() {
		BinomialDistribution distribution = new BinomialDistribution(new Random(1), 1, 0.5);
		int ones = 0;
		for (int draw = 0; draw < 20000; draw++)
			ones += distribution.sampleInt();
		assertTrue(ones > 9500 && ones < 10500, ones + " successes out of 20000 single trials");
	}

	@Test
	void aZeroOrImpossibleProbabilityIsAcceptedAtTheEdges() {
		BinomialDistribution never = new BinomialDistribution(new Random(1), 10, 0);
		BinomialDistribution always = new BinomialDistribution(new Random(1), 10, 1);
		BinomialDistribution neverBig = new BinomialDistribution(new Random(1), 200, 0);
		for (int draw = 0; draw < 400; draw++) {
			assertEquals(0, never.sampleInt());
			assertEquals(10, always.sampleInt());
			assertEquals(0, neverBig.sampleInt());
		}
	}

	@Test
	void anImpossibleNumberOfTrialsIsNamed() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new BinomialDistribution(new Random(1), 0, 0.5));
		assertEquals("Distribution \"binomial\": the parameter \"trials\" must be >= 1", exception.getMessage());
	}

	@Test
	void anImpossibleProbabilityIsNamed() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new BinomialDistribution(new Random(1), 10, 1.4));
		assertEquals("Distribution \"binomial\": the parameter \"probability\" must be between 0 and 1",
				exception.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new BinomialDistribution(new Random(seed), 10, 0.3));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new BinomialDistribution(new Random(seed), 10, 0.3));
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new BinomialDistribution(new Random(seed), 200, 0.3));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(32);
		BinomialDistribution distribution = new BinomialDistribution(20, 0.5);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		BinomialDistribution distribution = smallCount();
		assertEquals("binomial", distribution.getName());
		assertEquals("binomial(trials=10, probability=0.3)", distribution.toString());
	}

}
