package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class CategoricalDistributionTest {

	private CategoricalDistribution uniform3() {
		return new CategoricalDistribution(new Random(1), new double[] { 1, 2, 3 }, null);
	}

	private CategoricalDistribution weighted() {
		return new CategoricalDistribution(new Random(2), new double[] { 1, 2, 3 }, new double[] { 1, 1, 2 });
	}

	@Test
	void drawsOnlyReturnConfiguredValues() {
		CategoricalDistribution distribution = uniform3();
		for (int draw = 0; draw < 400; draw++) {
			double value = distribution.sample();
			assertTrue(value == 1 || value == 2 || value == 3, "categorical drew " + value);
		}
	}

	@Test
	void equalWeightsGiveTheArithmeticAverage() {
		assertEquals(2.0, uniform3().mean(), 1e-12);
	}

	@Test
	void meanIsTheWeightedAverage() {
		// (1*1 + 2*1 + 3*2) / 4 = 9/4 = 2.25
		assertEquals(2.25, weighted().mean(), 1e-12);
	}

	@Test
	void boundsAreTheExtremeValues() {
		CategoricalDistribution distribution = weighted();
		assertEquals(1.0, distribution.getMin(), 0.0);
		assertEquals(3.0, distribution.getMax(), 0.0);
	}

	@Test
	// std of the weighted categorical is ~0.83; the standard error over 50000 draws
	// is ~0.004, far inside the 5% band around the mean 2.25.
	void empiricalAverageMatchesTheWeightedMean() {
		DistributionTestSupport.meanMatchesTheEmpiricalAverage(new CategoricalDistribution(new Random(3),
				new double[] { 1, 2, 3 }, new double[] { 1, 1, 2 }));
	}

	@Test
	void getValuesReturnsACopyOfTheConfiguredValues() {
		double[] source = { 4, 5, 6 };
		CategoricalDistribution distribution = new CategoricalDistribution(new Random(1), source, null);
		assertArrayEquals(new double[] { 4, 5, 6 }, distribution.getValues(), 0.0);
		distribution.getValues()[0] = 99;
		assertEquals(4.0, distribution.getValues()[0], 0.0);
		source[1] = -1;
		assertEquals(5.0, distribution.getValues()[1], 0.0);
	}

	@Test
	// A single value with the integer draw must be floored toward minus infinity.
	void theIntegerDrawIsTheFloorOfTheChosenValue() {
		CategoricalDistribution distribution = new CategoricalDistribution(new Random(1), new double[] { 2.7 }, null);
		assertEquals(2, distribution.sampleInt());
		assertEquals(2.7, distribution.sample(), 0.0);
	}

	@Test
	void nameIsCategorical() {
		assertEquals("categorical", uniform3().getName());
	}

	@Test
	void toStringListsValuesAndTotalWeight() {
		assertEquals("categorical(values=1.0, 2.0, 3.0, totalWeight=3.0)", uniform3().toString());
	}

	@Test
	void rejectsAnEmptyValueList() {
		IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
				() -> new CategoricalDistribution(new Random(1), new double[0], null));
		IllegalArgumentException nullValues = assertThrows(IllegalArgumentException.class,
				() -> new CategoricalDistribution(new Random(1), null, null));
		IllegalArgumentException convenience = assertThrows(IllegalArgumentException.class,
				() -> new CategoricalDistribution(new double[0]));
		assertTrue(empty.getMessage().contains("at least one value is required"));
		assertTrue(nullValues.getMessage().contains("at least one value is required"));
		assertTrue(convenience.getMessage().contains("at least one value is required"));
	}

	@Test
	void rejectsAMismatchedWeightLength() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new CategoricalDistribution(new Random(1), new double[] { 1, 2, 3 }, new double[] { 1, 2 }));
		assertTrue(error.getMessage().contains("the number of weights must match the number of values"));
	}

	@Test
	void rejectsANegativeWeight() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new CategoricalDistribution(new Random(1), new double[] { 1, 2 }, new double[] { 1, -1 }));
		assertTrue(error.getMessage().contains("the weight of \"2.0\" must be >= 0"));
	}

	@Test
	void rejectsANonPositiveWeightTotal() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new CategoricalDistribution(new Random(1), new double[] { 1, 2 }, new double[] { 0, 0 }));
		assertTrue(error.getMessage().contains("the sum of the weights must be > 0"));
	}

	@Test
	void aFixedSeedPinsTheWholeSequence() {
		DistributionTestSupport.sameSeedGivesTheSameSequence(
				seed -> new CategoricalDistribution(new Random(seed), new double[] { 1, 2, 3, 4 }, null));
		DistributionTestSupport.differentSeedGivesADifferentSequence(
				seed -> new CategoricalDistribution(new Random(seed), new double[] { 1, 2, 3, 4 }, null));
	}
}
