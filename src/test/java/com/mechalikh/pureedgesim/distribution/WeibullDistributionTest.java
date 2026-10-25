package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WeibullDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static WeibullDistribution distribution() {
		return new WeibullDistribution(new Random(1), 1.5, 10);
	}

	@Test
	void samplesStayPositive() {
		WeibullDistribution distribution = distribution();
		assertEquals(0.0, distribution.getMin(), 0.0);
		DistributionTestSupport.samplesStayInsideTheSupport(distribution);
		for (int draw = 0; draw < 400; draw++)
			assertTrue(distribution.sample() > 0);
	}

	@Test
	void theMeanUsesTheGammaFunction() {
		WeibullDistribution distribution = distribution();
		assertEquals(10 * SpecialFunctions.gamma(1 + 1 / 1.5), distribution.mean(), 1e-12);
		// Gamma(1 + 2/3) is 0.9027, so a Weibull of scale 10 averages 9.027.
		assertEquals(9.02745293, distribution.mean(), 1e-6);
	}

	@Test
	void theMeanMatchesTheEmpiricalAverage() {
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution());
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(new WeibullDistribution(new Random(1), 0.6, 3), 0.1,
				50000);
	}

	@Test
	void aShapeOfOneIsTheExponentialLaw() {
		WeibullDistribution distribution = new WeibullDistribution(new Random(1), 1, 6);
		assertEquals(6.0, distribution.mean(), 1e-12, "Gamma(2) is 1, so the mean is the scale");
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution);
	}

	@Test
	void eachParameterIsCheckedByName() {
		IllegalArgumentException shape = assertThrows(IllegalArgumentException.class,
				() -> new WeibullDistribution(new Random(1), 0, 5));
		IllegalArgumentException scale = assertThrows(IllegalArgumentException.class,
				() -> new WeibullDistribution(new Random(1), 2, -1));
		assertEquals("Distribution \"weibull\": the parameter \"shape\" must be > 0.0", shape.getMessage());
		assertEquals("Distribution \"weibull\": the parameter \"scale\" must be > 0.0", scale.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new WeibullDistribution(new Random(seed), 1.5, 10));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new WeibullDistribution(new Random(seed), 1.5, 10));
	}

	@Test
	void integerDrawsAreFloored() {
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(
				seed -> new WeibullDistribution(new Random(seed), 1.5, 10));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(26);
		WeibullDistribution distribution = new WeibullDistribution(1.2, 3);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		WeibullDistribution distribution = distribution();
		assertEquals("weibull", distribution.getName());
		assertEquals("weibull(shape=1.5, scale=10.0)", distribution.toString());
		assertEquals(1.5, distribution.getShape(), 0.0);
		assertEquals(10.0, distribution.getScale(), 0.0);
	}

}
