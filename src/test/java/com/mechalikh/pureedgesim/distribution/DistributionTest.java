package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DistributionTest {

	@Test
	void theDefaultSupportCoversTheWholeNumberLine() {
		Distribution unbounded = new FixedDistribution(2.5);
		assertEquals(Double.NEGATIVE_INFINITY, unbounded.getMin());
		assertEquals(Double.POSITIVE_INFINITY, unbounded.getMax());
	}

	@Test
	void anImplementationAdvertisesItsOwnBounds() {
		Distribution bounded = new FixedDistribution(2.5) {
			@Override
			public double getMin() {
				return 0;
			}

			@Override
			public double getMax() {
				return 3;
			}
		};
		assertEquals(0, bounded.getMin(), 0.0);
		assertEquals(3, bounded.getMax(), 0.0);
	}

	@Test
	void theContractOfTheIntegerDrawIsAFloor() {
		FixedDistribution distribution = new FixedDistribution(-2.5);
		assertEquals(-3, distribution.sampleInt());
		assertTrue(distribution.sample() == -2.5);
	}

	@Test
	void theDiscreteFlagIsPartOfTheInterface() {
		assertFalse(new FixedDistribution(1).isDiscrete());
		assertTrue(new FixedDistribution(1) {
			@Override
			public boolean isDiscrete() {
				return true;
			}
		}.isDiscrete());
	}

	/** Minimal implementation used to read the default behaviour of the interface. */
	private static class FixedDistribution implements Distribution {

		private final double value;

		FixedDistribution(double value) {
			this.value = value;
		}

		@Override
		public double sample() {
			return value;
		}

		@Override
		public int sampleInt() {
			return (int) Math.floor(value);
		}

		@Override
		public double mean() {
			return value;
		}

		@Override
		public String getName() {
			return "fixed";
		}

		@Override
		public boolean isDiscrete() {
			return false;
		}
	}

}
