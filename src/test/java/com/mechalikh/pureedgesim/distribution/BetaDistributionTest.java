package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BetaDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static BetaDistribution distribution() {
		return new BetaDistribution(new Random(1), 2, 5);
	}

	@Test
	void samplesStayBetweenZeroAndOne() {
		BetaDistribution distribution = distribution();
		assertEquals(0.0, distribution.getMin(), 0.0);
		assertEquals(1.0, distribution.getMax(), 0.0);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
	}

	@Test
	void theMeanIsAlphaOverTheSumOfTheShapes() {
		assertEquals(2.0 / 7.0, distribution().mean(), 1e-12);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution());
	}

	@Test
	void equalShapesCenterTheLawOnOneHalf() {
		BetaDistribution symmetric = new BetaDistribution(new Random(1), 3, 3);
		assertEquals(0.5, symmetric.mean(), 1e-12);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(symmetric);
	}

	@Test
	void shapesOfOneGiveAUniformSpreadOfFractions() {
		BetaDistribution uniform = new BetaDistribution(new Random(1), 1, 1);
		assertEquals(0.5, uniform.mean(), 1e-12);
		int[] quarters = new int[4];
		for (int draw = 0; draw < 20000; draw++)
			quarters[(int) (uniform.sample() * 4)]++;
		for (int quarter = 0; quarter < 4; quarter++)
			assertTrue(quarters[quarter] > 4500 && quarters[quarter] < 5500,
					"The quarter " + quarter + " got " + quarters[quarter] + " of 20000 draws");
	}

	@Test
	void eachShapeIsCheckedByName() {
		IllegalArgumentException alpha = assertThrows(IllegalArgumentException.class,
				() -> new BetaDistribution(new Random(1), 0, 2));
		IllegalArgumentException beta = assertThrows(IllegalArgumentException.class,
				() -> new BetaDistribution(new Random(1), 2, -1));
		assertEquals("Distribution \"beta\": the parameter \"alpha\" must be > 0.0", alpha.getMessage());
		assertEquals("Distribution \"beta\": the parameter \"beta\" must be > 0.0", beta.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new BetaDistribution(new Random(seed), 2, 5));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new BetaDistribution(new Random(seed), 2, 5));
	}

	@Test
	void integerDrawsAreFlooredToOneOrZero() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new BetaDistribution(new Random(seed), 2, 5));
		BetaDistribution distribution = distribution();
		for (int draw = 0; draw < 400; draw++)
			assertEquals(0, distribution.sampleInt());
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(28);
		BetaDistribution distribution = new BetaDistribution(2, 3);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		BetaDistribution distribution = distribution();
		assertEquals("beta", distribution.getName());
		assertEquals("beta(alpha=2.0, beta=5.0)", distribution.toString());
	}

}
