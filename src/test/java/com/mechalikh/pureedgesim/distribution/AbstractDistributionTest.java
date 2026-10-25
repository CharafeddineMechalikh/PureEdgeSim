package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AbstractDistributionTest {

	@Test
	void sampleIntFloorsInsteadOfTruncatingTowardZero() {
		assertEquals(-1, new Fixed(new Random(1), -0.5).sampleInt());
		assertEquals(-3, new Fixed(new Random(1), -2.5).sampleInt());
		assertEquals(2, new Fixed(new Random(1), 2.9).sampleInt());
		assertEquals(2, new Fixed(new Random(1), 2.0).sampleInt());
	}

	@Test
	void theFloorOfANegativeIntervalIsNotACast() {
		UniformDistribution distribution = new UniformDistribution(new Random(1), -3.5, -2.5);
		List<Integer> seen = new ArrayList<>();
		for (int draw = 0; draw < 400; draw++) {
			int value = distribution.sampleInt();
			assertTrue(value == -4 || value == -3, "sampleInt() returned " + value + " outside [-3.5, -2.5]");
			if (!seen.contains(value))
				seen.add(value);
		}
		assertEquals(2, seen.size(), "Both -4 and -3 must appear, a cast would never give -4");
	}

	@Test
	void continuousDistributionsAreNeverDiscrete() {
		assertFalse(new Fixed(new Random(1), 1).isDiscrete());
		assertFalse(new UniformDistribution(new Random(1), 0, 1).isDiscrete());
	}

	@Test
	void clampConfinesAValueToTheInterval() {
		Fixed fixed = new Fixed(new Random(1), 0);
		assertEquals(3, fixed.confine(5, 0, 3), 0.0);
		assertEquals(0, fixed.confine(-1, 0, 3), 0.0);
		assertEquals(2, fixed.confine(2, 0, 3), 0.0);
		assertEquals(0, fixed.confine(0, 0, 3), 0.0);
		assertEquals(3, fixed.confine(3, 0, 3), 0.0);
	}

	@Test
	void requirePositiveNamesTheOffendingParameter() {
		Fixed fixed = new Fixed(new Random(1), 0);
		IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
				() -> fixed.checkPositive("alpha", 0, 0));
		IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
				() -> fixed.checkPositive("alpha", -1, 0));
		assertEquals("Distribution \"fixed\": the parameter \"alpha\" must be > 0.0", zero.getMessage());
		assertEquals(negative.getMessage(), zero.getMessage());
		fixed.checkPositive("alpha", 0.5, 0);
	}

	@Test
	void theGeneratorCanBeReplacedAndReadBack() {
		Random first = new Random(1);
		Fixed fixed = new Fixed(first, 3);
		assertSame(first, fixed.getRandom());
		Random second = new Random(2);
		fixed.setRandom(second);
		assertSame(second, fixed.getRandom());
	}

	@Test
	void theSharedRandomnessSuppliesTheGeneratorWhenNoneIsGiven() {
		Randomness.setSeed(4);
		Fixed fixed = new Fixed();
		assertSame(Randomness.getRandom(), fixed.getRandom());
	}

	@Test
	void aReplacedGeneratorChangesTheStream() {
		NormalDistribution distribution = new NormalDistribution(new Random(1), 0, 1);
		List<Double> before = DistributionTestSupport.draws(distribution);
		distribution.setRandom(new Random(2));
		assertFalse(before.equals(DistributionTestSupport.draws(distribution)));
	}

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

	/** Concrete subclass used to exercise the helpers of the base class. */
	private static class Fixed extends AbstractDistribution {

		private final double value;

		Fixed(Random random, double value) {
			super(random);
			this.value = value;
		}

		Fixed() {
			super();
			this.value = 0;
		}

		@Override
		public double sample() {
			return value;
		}

		@Override
		public double mean() {
			return value;
		}

		@Override
		public String getName() {
			return "fixed";
		}

		double confine(double candidate, double min, double max) {
			return clamp(candidate, min, max);
		}

		void checkPositive(String name, double parameter, double min) {
			requirePositive(name, parameter, min);
		}
	}

}
