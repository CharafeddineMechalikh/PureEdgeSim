package com.mechalikh.pureedgesim.distribution;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central source of randomness for the simulation.
 * <p>
 * By default the generator is a shared {@link java.security.SecureRandom}, which
 * makes every run different. Setting the {@code random_seed} parameter, or
 * calling {@link #setSeed(long)} programmatically before the simulation starts,
 * switches every module to a deterministic, per-thread stream built from that
 * seed, so that a given scenario can be reproduced exactly.
 * <p>
 * Each simulation thread owns its own {@link java.util.Random} instance, so
 * parallel runs do not contend on a single generator. Use
 * {@link #beginScenario(long)} to derive the thread stream from a scenario
 * identifier when reproducible results are required across parallel executions.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public final class Randomness {

	/** Bit-mixing constant used to combine the seed with a scenario salt. */
	private static final long MIXING_CONSTANT = 0x9E3779B97F4A7C15L;

	/** The configured seed, only meaningful when {@link #isSeeded()} is true. */
	private static volatile long seed = 0L;

	/** Whether a deterministic seed has been configured. */
	private static volatile boolean seeded = false;

	/** Shared non-blocking secure generator used when no seed is configured. */
	private static volatile java.util.Random secureInstance;

	/** Per-thread generator, re-created whenever the thread starts a new scenario. */
	private static final ThreadLocal<java.util.Random> THREAD_RANDOM = new ThreadLocal<>();

	/** Per-thread scenario salt, zero by default. */
	private static final ThreadLocal<Long> THREAD_SALT = ThreadLocal.withInitial(() -> 0L);

	/** Counter used to salt threads that do not call {@link #beginScenario(long)}. */
	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

	/** Number of {@link #getRandom()} calls, exposed for diagnostics and tests. */
	private static final AtomicLong DRAW_COUNT = new AtomicLong();

	private Randomness() {
	}

	/**
	 * Returns the generator that the calling thread should use.
	 * <p>
	 * When no seed is configured, the shared secure generator is returned. When a
	 * seed is configured, a thread-local {@link java.util.Random} derived from the
	 * seed and the thread's scenario salt is returned, so that repeated runs
	 * produce the same sequence of values.
	 *
	 * @return the random generator of the current thread
	 */
	public static java.util.Random getRandom() {
		DRAW_COUNT.incrementAndGet();
		if (!seeded) {
			return getSecureInstance();
		}
		java.util.Random random = THREAD_RANDOM.get();
		if (random == null) {
			long salt = THREAD_SALT.get();
			random = new java.util.Random(mix(seed, salt));
			THREAD_RANDOM.set(random);
		}
		return random;
	}

	/**
	 * Returns the next pseudo-random integer in the range {@code [0, bound)}.
	 * <p>
	 * Convenience wrapper used by the scenario generators.
	 *
	 * @param bound the exclusive upper bound, must be positive
	 * @return a random integer
	 */
	public static int nextInt(int bound) {
		return getRandom().nextInt(bound);
	}

	/**
	 * Returns the next pseudo-random double in the range {@code [0.0, 1.0)}.
	 *
	 * @return a random value uniformly distributed between zero (inclusive) and
	 *         one (exclusive)
	 */
	public static double nextDouble() {
		return getRandom().nextDouble();
	}

	/**
	 * Configures a deterministic seed for the whole simulation.
	 * <p>
	 * All modules pick the seed up automatically when they create their
	 * generators, so this method only needs to be called once, before the
	 * simulation is launched. Call {@link #reset()} to switch back to the
	 * non-deterministic behavior, or {@code setSeed(0)} to keep the default value.
	 *
	 * @param value the seed, any {@code long}
	 */
	public static void setSeed(long value) {
		seed = value;
		seeded = true;
		clearThreadState();
	}

	/**
	 * Returns the configured seed.
	 *
	 * @return the seed, or {@code 0} when none was configured
	 */
	public static long getSeed() {
		return seed;
	}

	/**
	 * Returns whether random values are reproducible.
	 *
	 * @return {@code true} when a seed has been configured
	 */
	public static boolean isSeeded() {
		return seeded;
	}

	/**
	 * Starts a new scenario on the calling thread and re-derives its generator
	 * from the configured seed and the given salt.
	 * <p>
	 * Different salts give different, yet individually reproducible, streams. Two
	 * threads that run the same scenario with the same salt draw the same values,
	 * so the salt should identify the scenario, not the thread.
	 *
	 * @param salt a scenario identifier, such as the iteration number
	 */
	public static void beginScenario(long salt) {
		THREAD_SALT.set(salt);
		THREAD_RANDOM.remove();
	}

	/**
	 * Assigns an automatic salt to the calling thread, based on the order in which
	 * threads first request a generator.
	 * <p>
	 * Used when no explicit scenario salt is available. The resulting stream is
	 * reproducible as long as the thread layout of the run does not change.
	 */
	public static void beginAutomaticScenario() {
		beginScenario(THREAD_COUNTER.incrementAndGet());
	}

	/**
	 * Returns the number of generator requests made since the JVM started.
	 *
	 * @return the total number of {@link #getRandom()} calls
	 */
	public static long getDrawCount() {
		return DRAW_COUNT.get();
	}

	/**
	 * Clears the seed and the per-thread generators, restoring the default
	 * non-deterministic behavior. Intended for tests and for repeated runs inside
	 * the same JVM.
	 */
	public static void reset() {
		seed = 0L;
		seeded = false;
		clearThreadState();
	}

	/**
	 * Mixes the seed with a scenario salt so that scenarios do not share their
	 * streams while remaining reproducible.
	 *
	 * @param baseSeed the configured seed
	 * @param salt     the scenario salt
	 * @return the value used to initialise the thread generator
	 */
	private static long mix(long baseSeed, long salt) {
		long mixed = baseSeed ^ (salt * MIXING_CONSTANT);
		mixed ^= (mixed >>> 33);
		mixed *= 0xff51afd7ed558ccdL;
		mixed ^= (mixed >>> 33);
		return mixed;
	}

	/**
	 * Returns the lazily created shared secure generator.
	 *
	 * @return the shared secure generator
	 */
	private static java.util.Random getSecureInstance() {
		java.util.Random local = secureInstance;
		if (local == null) {
			synchronized (Randomness.class) {
				local = secureInstance;
				if (local == null) {
					try {
						local = java.security.SecureRandom.getInstanceStrong();
					} catch (java.security.NoSuchAlgorithmException e) {
						local = new java.util.Random();
					}
					secureInstance = local;
				}
			}
		}
		return local;
	}

	/**
	 * Removes the generator of the calling thread so that the next request re-seeds
	 * it, and clears the salt counters used for automatic salting.
	 */
	private static void clearThreadState() {
		THREAD_RANDOM.remove();
		THREAD_SALT.set(0L);
	}

}
