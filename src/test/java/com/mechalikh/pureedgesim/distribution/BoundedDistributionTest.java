package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import org.junit.jupiter.api.Test;

class BoundedDistributionTest {

	/**
	 * A delegate that replays a fixed script of values and counts how many times it
	 * was sampled, so the rejection loop can be observed deterministically.
	 */
	private static final class Scripted implements Distribution {

		private final Deque<Double> script = new ArrayDeque<>();
		private final boolean discrete;
		private double last;
		private int calls;

		Scripted(boolean discrete, double... values) {
			this.discrete = discrete;
			for (double value : values)
				script.add(value);
		}

		@Override
		public double sample() {
			calls++;
			if (!script.isEmpty())
				last = script.poll();
			// Once the script is exhausted the last value repeats, so a rejected
			// value keeps being rejected until the loop stops or one is accepted.
			return last;
		}

		@Override
		public int sampleInt() {
			return (int) Math.floor(sample());
		}

		@Override
		public double mean() {
			return 0.0;
		}

		@Override
		public String getName() {
			return "scripted";
		}

		@Override
		public boolean isDiscrete() {
			return discrete;
		}
	}

	private static final int MAX = BoundedDistribution.MAX_ATTEMPTS;

	@Test
	void anInSupportDrawIsAcceptedImmediately() {
		Scripted delegate = new Scripted(false, 5.0);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 0, 10);
		assertEquals(5.0, bounded.sample(), 0.0);
		assertEquals(1, delegate.calls);
	}

	@Test
	void anOutOfSupportDrawIsRejectedAndTheNextIsKept() {
		Scripted delegate = new Scripted(false, 100.0, -5.0, 6.0);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 0, 10);
		assertEquals(6.0, bounded.sample(), 0.0);
		assertEquals(3, delegate.calls, "two rejected draws then one accepted draw must be consumed");
	}

	@Test
	void everyAttemptFailingClampsTheLastValueIntoTheInterval() {
		Scripted delegate = new Scripted(false, 1000.0);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 0, 10);
		double value = bounded.sample();
		assertEquals(10.0, value, 0.0);
		assertEquals(MAX, delegate.calls, "the loop must stop after MAX_ATTEMPTS tries, never spin forever");
	}

	@Test
	void theClampLandsOnTheMinimumForLowOutliers() {
		Scripted delegate = new Scripted(false, -1000.0);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 0, 10);
		assertEquals(0.0, bounded.sample(), 0.0);
	}

	@Test
	void meanIsTheDelegateMeanClampedToTheInterval() {
		BoundedDistribution high = new BoundedDistribution(new ConstantDistribution(50), 0, 10);
		assertEquals(10.0, high.mean(), 0.0);
		BoundedDistribution inside = new BoundedDistribution(new ConstantDistribution(4), 0, 10);
		assertEquals(4.0, inside.mean(), 0.0);
	}

	@Test
	void boundsAndDelegateAreExposed() {
		ConstantDistribution delegate = new ConstantDistribution(3);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 1, 9);
		assertEquals(1.0, bounded.getMin(), 0.0);
		assertEquals(9.0, bounded.getMax(), 0.0);
		assertEquals(delegate, bounded.getDelegate());
		assertEquals("constant", bounded.getName());
	}

	@Test
	void aDiscreteDelegateForwardsItsIntegerDrawUnbounded() {
		BernoulliDistribution delegate = new BernoulliDistribution(new Random(1), 0.5);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 0, 1);
		assertTrue(bounded.isDiscrete());
		for (int draw = 0; draw < 100; draw++) {
			int value = bounded.sampleInt();
			assertTrue(value == 0 || value == 1, "discrete delegate outcome expected, got " + value);
		}
	}

	@Test
	void aContinuousDelegateFloorsTheBoundedSample() {
		UniformDistribution delegate = new UniformDistribution(new Random(1), 0, 10);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 2, 8);
		assertTrue(!bounded.isDiscrete());
		for (int draw = 0; draw < 200; draw++) {
			int value = bounded.sampleInt();
			assertTrue(value >= 2 && value <= 8, "sampleInt() outside the bound: " + value);
		}
	}

	@Test
	void realUniformKeepsEverySampleInsideTheBound() {
		UniformDistribution delegate = new UniformDistribution(new Random(1), 0, 20);
		BoundedDistribution bounded = new BoundedDistribution(delegate, 5, 15);
		DistributionTestSupport.samplesStayInsideTheSupport(bounded);
	}

	@Test
	void toStringCombinesTheNameAndTheBounds() {
		BoundedDistribution bounded = new BoundedDistribution(new UniformDistribution(new Random(1), 0, 1), 2, 8);
		assertEquals("uniform(min=2.0, max=8.0)", bounded.toString());
	}

	@Test
	void rejectsInconsistentBounds() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> new BoundedDistribution(new ConstantDistribution(1), 10, 5));
		assertTrue(error.getMessage().contains("constant"));
		assertTrue(error.getMessage().contains("the \"min\" attribute must be <= the \"max\" attribute"));
	}
}
