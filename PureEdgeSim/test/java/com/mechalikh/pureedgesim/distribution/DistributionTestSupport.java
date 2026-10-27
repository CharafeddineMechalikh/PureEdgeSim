package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

/**
 * Assertions shared by the distribution tests. They state the properties every
 * distribution promises (support, integrality, agreement between the theoretical
 * mean and the sampled average) so that each test class only adds the rules that
 * belong to its own law.
 */
final class DistributionTestSupport {

	/** Draws used to look for a value that leaves the support. */
	static final int SUPPORT_DRAWS = 400;

	/** Draws used to compare the theoretical mean with the empirical average. */
	static final int MEAN_DRAWS = 50000;

	/** Draws used to check that a seed pins the whole sequence. */
	static final int SEQUENCE_DRAWS = 40;

	private DistributionTestSupport() {
	}

	/** Builds the very same distribution over a caller supplied generator. */
	interface FromSeed {
		Distribution build(long seed);
	}

	static void samplesStayInsideTheSupport(Distribution distribution) {
		double min = distribution.getMin();
		double max = distribution.getMax();
		for (int draw = 0; draw < SUPPORT_DRAWS; draw++) {
			double value = distribution.sample();
			if (!(value >= min && value <= max))
				fail("sample() returned " + value + " outside the support [" + min + ", " + max + "] of "
						+ distribution);
		}
	}

	static void sampledValuesAreWholeNumbers(Distribution distribution) {
		for (int draw = 0; draw < SUPPORT_DRAWS; draw++) {
			double value = distribution.sample();
			if (value != Math.floor(value))
				fail("sample() returned " + value + " which is not a whole number for " + distribution);
		}
	}

	static void integerDrawsStayInsideTheSupport(Distribution distribution) {
		double min = Math.floor(distribution.getMin());
		double max = Math.floor(distribution.getMax());
		for (int draw = 0; draw < SUPPORT_DRAWS; draw++) {
			int value = distribution.sampleInt();
			if (!(value >= min && value <= max))
				fail("sampleInt() returned " + value + " outside the support [" + min + ", " + max + "] of "
						+ distribution);
		}
	}

	static double empiricalMean(Distribution distribution, int draws) {
		double total = 0;
		for (int draw = 0; draw < draws; draw++)
			total += distribution.sample();
		return total / draws;
	}

	/**
	 * With 50000 draws the standard error is far below one percent of the mean for
	 * every law tested here, so a five percent band only fails on a real bug.
	 */
	static void meanMatchesTheEmpiricalAverage(Distribution distribution) {
		meanMatchesTheEmpiricalAverage(distribution, 0.05, MEAN_DRAWS);
	}

	static void meanMatchesTheEmpiricalAverage(Distribution distribution, double relativeTolerance, int draws) {
		double theoretical = distribution.mean();
		double empirical = empiricalMean(distribution, draws);
		assertTrue(Math.abs(empirical - theoretical) <= relativeTolerance * Math.abs(theoretical),
				"The empirical average " + empirical + " of " + distribution + " is more than "
						+ 100 * relativeTolerance + " percent away from the theoretical mean " + theoretical);
	}

	static void empiricalSpreadMatchesTheConfiguredStandardDeviation(Distribution distribution, double expectedMean,
			double expectedDeviation, double relativeTolerance) {
		List<Double> values = new ArrayList<>(MEAN_DRAWS);
		for (int draw = 0; draw < MEAN_DRAWS; draw++)
			values.add(distribution.sample());
		double total = 0;
		for (double value : values)
			total += value;
		double mean = total / values.size();
		double variance = 0;
		for (double value : values)
			variance += (value - mean) * (value - mean);
		double deviation = Math.sqrt(variance / (values.size() - 1));
		assertTrue(Math.abs(mean - expectedMean) <= relativeTolerance * Math.abs(expectedMean),
				"The sampled average " + mean + " of " + distribution + " does not match the " + expectedMean
						+ " configured for it");
		assertTrue(Math.abs(deviation - expectedDeviation) <= relativeTolerance * expectedDeviation,
				"The sampled standard deviation " + deviation + " of " + distribution + " does not match the "
						+ expectedDeviation + " configured for it");
	}

	static void sameSeedGivesTheSameSequence(FromSeed builder) {
		List<Double> first = draws(builder.build(1));
		List<Double> second = draws(builder.build(1));
		assertEquals(first, second, "Two distributions built on the same seed must draw the same values");
	}

	static void differentSeedGivesADifferentSequence(FromSeed builder) {
		List<Double> first = draws(builder.build(1));
		List<Double> second = draws(builder.build(2));
		assertNotEquals(first, second, "A different seed must move the stream");
	}

	static void integerDrawIsTheFloorOfTheDoubleDraw(FromSeed builder) {
		Distribution doubles = builder.build(7);
		Distribution integers = builder.build(7);
		for (int draw = 0; draw < SEQUENCE_DRAWS; draw++)
			assertEquals((int) Math.floor(doubles.sample()), integers.sampleInt(),
					"sampleInt() must truncate the draw of sample() toward minus infinity");
	}

	static List<Double> draws(Distribution distribution) {
		List<Double> values = new ArrayList<>(SEQUENCE_DRAWS);
		for (int draw = 0; draw < SEQUENCE_DRAWS; draw++)
			values.add(distribution.sample());
		return values;
	}

}
