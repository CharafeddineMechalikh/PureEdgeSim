package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AbstractDiscreteDistributionTest {

	@Test
	void theDoubleDrawIsTheExactIntegerOutcome() {
		Die first = new Die(new Random(1));
		Die second = new Die(new Random(1));
		for (int draw = 0; draw < 200; draw++)
			assertEquals(second.sampleInt(), (int) first.sample(),
					"sample() must return the outcome of sampleInt(), not a rounded continuous value");
	}

	@Test
	void discreteDistributionsAdvertiseBeingDiscrete() {
		assertTrue(new Die(new Random(1)).isDiscrete());
		assertTrue(new BernoulliDistribution(new Random(1), 0.5).isDiscrete());
		assertFalse(new UniformDistribution(new Random(1), 0, 1).isDiscrete());
	}

	@Test
	void everyOutcomeOfAFairDieAppears() {
		Die die = new Die(new Random(1));
		int[] counts = new int[7];
		for (int draw = 0; draw < 1200; draw++)
			counts[die.sampleInt()]++;
		for (int face = 1; face <= 6; face++)
			assertTrue(counts[face] > 100, "The face " + face + " appeared " + counts[face] + " times over 1200 draws");
	}

	@Test
	void theGeneratorIsStoredAndReplaceable() {
		Random first = new Random(1);
		Die die = new Die(first);
		assertSame(first, die.getRandom());
		Random replaced = new Random(9);
		die.setRandom(replaced);
		assertSame(replaced, die.getRandom());
		// A die built on a generator that replayed the same stream must draw the same
		// values, which proves the draws come from the generator that was set.
		assertEquals(DistributionTestSupport.draws(new Die(new Random(9))),
				DistributionTestSupport.draws(die));
	}

	@Test
	void theSharedRandomnessSuppliesTheGeneratorWhenNoneIsGiven() {
		Randomness.setSeed(6);
		Die die = new Die();
		assertSame(Randomness.getRandom(), die.getRandom());
	}

	/** Concrete discrete subclass used to exercise the base class. */
	private static class Die extends AbstractDiscreteDistribution {

		Die(Random random) {
			super(random);
		}

		Die() {
			super();
		}

		@Override
		public int sampleInt() {
			return random.nextInt(6) + 1;
		}

		@Override
		public double mean() {
			return 3.5;
		}

		@Override
		public String getName() {
			return "die";
		}
	}

	@AfterEach
	void restoreGlobalRandomness() {
		Randomness.reset();
	}

}
