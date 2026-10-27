package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ErlangDistributionTest {

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	private static ErlangDistribution distribution() {
		return new ErlangDistribution(new Random(1), 3, 2);
	}

	@Test
	void samplesStayPositive() {
		DistributionTestSupport.samplesStayInsideTheSupport(distribution());
		assertEquals(0.0, distribution().getMin(), 0.0);
	}

	@Test
	void theMeanIsTheNumberOfPhasesTimesTheirDuration() {
		assertEquals(6.0, distribution().mean(), 0.0);
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(distribution());
	}

	@Test
	void itStaysAContinuousLawEvenWithAWholeShape() {
		ErlangDistribution distribution = distribution();
		assertFalse(distribution.isDiscrete());
		DistributionTestSupport.integerDrawIsTheFloorOfTheDoubleDraw(seed -> new ErlangDistribution(new Random(seed), 3, 2));
	}

	@Test
	void aShapeThatIsNotAWholeNumberOfPhasesIsRejected() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ErlangDistribution(new Random(1), 2.5, 1));
		assertEquals("Distribution \"erlang\": the parameter \"shape\" must be a whole number of phases",
				exception.getMessage());
	}

	@Test
	void aShapeVeryCloseToAnIntegerIsAccepted() {
		ErlangDistribution distribution = new ErlangDistribution(new Random(1), 3 + 1e-12, 2);
		assertEquals(3.0, distribution.getShape(), 1e-9);
	}

	@Test
	void theGammaChecksRunUnderTheErlangName() {
		IllegalArgumentException shape = assertThrows(IllegalArgumentException.class,
				() -> new ErlangDistribution(new Random(1), 0, 1));
		IllegalArgumentException scale = assertThrows(IllegalArgumentException.class,
				() -> new ErlangDistribution(new Random(1), 3, 0));
		assertEquals("Distribution \"erlang\": the parameter \"shape\" must be > 0.0", shape.getMessage());
		assertEquals("Distribution \"erlang\": the parameter \"scale\" must be > 0.0", scale.getMessage());
	}

	@Test
	void aSeedFixesTheSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(seed -> new ErlangDistribution(new Random(seed), 3, 2));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new ErlangDistribution(new Random(seed), 3, 2));
	}

	@Test
	void withoutAGeneratorItUsesTheSharedRandomness() {
		Randomness.setSeed(25);
		ErlangDistribution distribution = new ErlangDistribution(2, 4);
		assertSame(Randomness.getRandom(), distribution.getRandom());
	}

	@Test
	void itIsNamedTheWayTheScenarioFilesSpellIt() {
		ErlangDistribution distribution = distribution();
		assertEquals("erlang", distribution.getName());
		assertEquals("erlang(shape=3.0, scale=2.0)", distribution.toString());
		assertEquals(2.0, distribution.getScale(), 0.0);
	}

}
