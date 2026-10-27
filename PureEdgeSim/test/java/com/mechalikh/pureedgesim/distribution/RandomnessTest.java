package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RandomnessTest {

	// Randomness holds static state, so the previous configuration is captured
	// before each test and restored afterwards to keep the rest of the suite
	// order-independent.
	private long savedSeed;
	private boolean savedSeeded;

	@BeforeEach
	void captureGlobalState() {
		savedSeed = Randomness.getSeed();
		savedSeeded = Randomness.isSeeded();
	}

	@AfterEach
	void restoreGlobalState() {
		if (savedSeeded)
			Randomness.setSeed(savedSeed);
		else
			Randomness.reset();
	}

	/** Draws ten doubles after re-deriving this thread's generator from {@code salt}. */
	private static List<Double> sequence(long salt) {
		Randomness.beginScenario(salt);
		Random random = Randomness.getRandom();
		List<Double> values = new ArrayList<>(10);
		for (int i = 0; i < 10; i++)
			values.add(random.nextDouble());
		return values;
	}

	@Test
	void theDefaultStateIsNotSeeded() {
		Randomness.reset();
		assertFalse(Randomness.isSeeded());
		assertEquals(0L, Randomness.getSeed());
	}

	@Test
	void theUnseededGeneratorIsTheSharedSecureInstance() {
		Randomness.reset();
		Random first = Randomness.getRandom();
		assertNotNull(first);
		assertSame(first, Randomness.getRandom(), "the secure generator is a lazily created singleton");
	}

	@Test
	void settingASeedSwitchesToADeterministicPlainGenerator() {
		Randomness.setSeed(42);
		assertTrue(Randomness.isSeeded());
		assertEquals(42L, Randomness.getSeed());
		Random random = Randomness.getRandom();
		assertFalse(random instanceof SecureRandom, "a seeded stream must not use the secure generator");
		assertSame(random, Randomness.getRandom(), "the thread generator is cached");
	}

	@Test
	void theNextHelpersStayInsideTheirRanges() {
		Randomness.setSeed(1);
		for (int i = 0; i < 500; i++) {
			double value = Randomness.nextDouble();
			assertTrue(value >= 0.0 && value < 1.0, "nextDouble out of range: " + value);
			int integer = Randomness.nextInt(10);
			assertTrue(integer >= 0 && integer < 10, "nextInt out of range: " + integer);
		}
	}

	@Test
	void everyGeneratorRequestBumpsTheDrawCount() {
		Randomness.setSeed(3);
		long before = Randomness.getDrawCount();
		Randomness.getRandom();
		assertEquals(before + 1, Randomness.getDrawCount());
		Randomness.nextInt(5);
		assertEquals(before + 2, Randomness.getDrawCount());
		Randomness.nextDouble();
		assertEquals(before + 3, Randomness.getDrawCount());
	}

	@Test
	void aScenarioSaltMakesTheStreamReproducible() {
		// java.util.Random is fully specified, so the same seed and salt always give
		// the identical sequence; a different salt moves to a different stream.
		Randomness.setSeed(123);
		assertEquals(sequence(7), sequence(7));
		assertNotEquals(sequence(7), sequence(9));
	}

	@Test
	void twoThreadsSharingASeedAndSaltDrawTheSameValues() throws InterruptedException {
		Randomness.setSeed(99);
		List<Double> expected = sequence(5);
		List<Double> actual = Collections.synchronizedList(new ArrayList<Double>());
		Thread worker = new Thread(() -> actual.addAll(sequence(5)));
		worker.start();
		worker.join();
		assertEquals(expected, actual);
	}

	@Test
	void beginAutomaticScenarioYieldsADeterministicPlainGenerator() {
		Randomness.reset();
		Randomness.setSeed(5);
		Randomness.beginAutomaticScenario();
		Random random = Randomness.getRandom();
		assertFalse(random instanceof SecureRandom);
		assertSame(random, Randomness.getRandom());
		for (int i = 0; i < 50; i++) {
			double value = random.nextDouble();
			assertTrue(value >= 0.0 && value < 1.0);
		}
	}

	@Test
	void resettingRestoresTheUnseededState() {
		Randomness.setSeed(7);
		Randomness.reset();
		assertFalse(Randomness.isSeeded());
		assertEquals(0L, Randomness.getSeed());
		assertNotNull(Randomness.getRandom());
	}
}
