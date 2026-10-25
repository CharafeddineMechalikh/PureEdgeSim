package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class GaussMarkovMobilityModelTest extends AbstractTest {

	/**
	 * Generator that replays scripted draws: every {@code nextDouble} returns the
	 * same value (so the initial mean direction is known), while {@code nextGaussian}
	 * replays the scripted values, cycling when exhausted.
	 */
	private static final class ScriptedRandom extends Random {
		private static final long serialVersionUID = 1L;
		private final double uniform;
		private final double[] gaussians;
		private int index;

		ScriptedRandom(double uniform, double... gaussians) {
			super(0L);
			this.uniform = uniform;
			this.gaussians = gaussians;
		}

		@Override
		public double nextDouble() {
			return uniform;
		}

		@Override
		public double nextGaussian() {
			return gaussians[index++ % gaussians.length];
		}
	}

	private SimulationManager simulationManager;
	private GaussMarkovMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		emptyEdgeList();
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
		model = new GaussMarkovMobilityModel(simulationManager, new Location(100, 100));
		model.random = new Random(42);
		model.setSpeed(10);
		model.setMobile(true);
	}

	private void emptyEdgeList() {
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		ComputingNodesGenerator generator = mock(ComputingNodesGenerator.class);
		when(simulationManager.getDataCentersManager()).thenReturn(dataCentersManager);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		when(generator.getEdgeOnlyList()).thenReturn(new ArrayList<ComputingNode>());
	}

	private static Map<String, String> options(String... keyValues) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < keyValues.length; i += 2)
			map.put(keyValues[i], keyValues[i + 1]);
		return map;
	}

	private static void assertInsideMap(Location location) {
		assertTrue(location.getXPos() >= 0 && location.getXPos() <= SimulationParameters.simulationMapWidth,
				"x=" + location.getXPos() + " left the simulation width");
		assertTrue(location.getYPos() >= 0 && location.getYPos() <= SimulationParameters.simulationMapLength,
				"y=" + location.getYPos() + " left the simulation length");
	}

	private static double displacement(Location from, Location to) {
		return Math.hypot(to.getXPos() - from.getXPos(), to.getYPos() - from.getYPos());
	}

	@Test
	void alphaOfOneIsRejected() {
		model.setParameters(options("alpha", "1"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(100, 100)));
		assertTrue(error.getMessage().contains("GaussMarkovMobilityModel - the \"alpha\" option must be >= 0 and < 1"),
				"unexpected message: " + error.getMessage());
		assertTrue(error.getMessage().contains("found \"1.0\""), "unexpected message: " + error.getMessage());
	}

	@Test
	void negativeAlphaIsRejected() {
		model.setParameters(options("alpha", "-0.5"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(100, 100)));
		assertTrue(error.getMessage().contains("must be >= 0 and < 1"), "unexpected message: " + error.getMessage());
		assertTrue(error.getMessage().contains("found \"-0.5\""), "unexpected message: " + error.getMessage());
	}

	@Test
	void alphaOfZeroIsAcceptedAndMovesTheDevice() {
		// alpha = 0 is the random walk limit of the process, it must not be rejected.
		model.setParameters(options("alpha", "0"));
		model.random = new Random(3);
		Location next = model.getNextLocation(new Location(100, 100));
		assertTrue(displacement(new Location(100, 100), next) > 1e-9, "the device did not move");
		assertTrue(model.initialised);
	}

	@Test
	void nonNumericAlphaIsRejected() {
		model.setParameters(options("alpha", "memoryless"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(100, 100)));
		assertTrue(error.getMessage().contains("the \"alpha\" option must be a number"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void withoutNoiseTheDeviceGoesStraightAtItsConfiguredSpeed() {
		// nextDouble = 0 means a mean direction of 0 degrees, nextGaussian = 0 means
		// no perturbation, so the device simply covers speed * updateInterval on X.
		model.random = new ScriptedRandom(0.0, 0.0);
		Location current = new Location(100, 100);
		for (int i = 1; i <= 10; i++) {
			current = model.getNextLocation(current);
			assertEquals(100.0 + 10 * i, current.getXPos(), 1e-9, "unexpected x at step " + i);
			assertEquals(100.0, current.getYPos(), 1e-9, "unexpected y at step " + i);
		}
		// The next step crosses the right border, so the base class reflects it.
		current = model.getNextLocation(current);
		assertEquals(190.0, current.getXPos(), 1e-9);
		assertEquals(100.0, current.getYPos(), 1e-9);
	}

	@Test
	void theDeviationRelaxesTowardTheMeanByAlpha() {
		// The direction is perturbed at the first update, then the noise is switched
		// off: the deviation from the mean direction must be multiplied by alpha.
		model.random = new ScriptedRandom(0.5, 1.0, 0.0, 0.0, 0.0);
		model.setParameters(options("alpha", "0.5", "std", "10"));
		model.getNextLocation(new Location(100, 100));
		double firstDeviation = Math.abs(model.direction - model.meanDirection);
		assertTrue(firstDeviation > 5 && firstDeviation < 15, "unexpected first deviation: " + firstDeviation);

		model.getNextLocation(new Location(100, 100));
		double secondDeviation = Math.abs(model.direction - model.meanDirection);
		assertEquals(0.5, secondDeviation / firstDeviation, 1e-9, "the memory of the process is not alpha");
		assertEquals(180.0, model.meanDirection, 1e-9, "the mean direction must come from the initial draw");
	}

	@Test
	void stdOptionIsTheOrientationDeviationInDegrees() {
		// With alpha = 0 the noise factor sqrt(1 - alpha^2) is exactly 1, and the
		// scripted Gaussian is 1, so the direction is mean + std degrees verbatim.
		GaussMarkovMobilityModel narrow = new GaussMarkovMobilityModel(simulationManager, new Location(100, 100));
		narrow.setSpeed(10);
		narrow.random = new ScriptedRandom(0.5, 1.0);
		narrow.setParameters(options("alpha", "0", "std", "5"));
		narrow.getNextLocation(new Location(100, 100));
		assertEquals(185.0, narrow.direction, 1e-9);

		GaussMarkovMobilityModel wide = new GaussMarkovMobilityModel(simulationManager, new Location(100, 100));
		wide.setSpeed(10);
		wide.random = new ScriptedRandom(0.5, 1.0);
		wide.setParameters(options("alpha", "0", "std", "100"));
		wide.getNextLocation(new Location(100, 100));
		assertEquals(280.0, wide.direction, 1e-9);
	}

	@Test
	void speedStdDefaultsToTwentyPercentOfTheDeviceSpeed() {
		// alpha = 0 and a scripted Gaussian of 1: the speed becomes mean + speed_std,
		// and the covered distance is that speed times the update interval.
		GaussMarkovMobilityModel standard = new GaussMarkovMobilityModel(simulationManager, new Location(100, 100));
		standard.random = new ScriptedRandom(0.0, 1.0);
		standard.setSpeed(10);
		standard.setParameters(options("alpha", "0"));
		Location standardStart = new Location(100, 100);
		Location standardNext = standard.getNextLocation(standardStart);
		assertEquals(12.0, standard.currentSpeed, 1e-9, "default speed_std must be 0.2 * speed");
		assertEquals(12.0, displacement(standardStart, standardNext), 1e-9);

		GaussMarkovMobilityModel amplified = new GaussMarkovMobilityModel(simulationManager, new Location(100, 100));
		amplified.random = new ScriptedRandom(0.0, 1.0);
		amplified.setSpeed(10);
		amplified.setParameters(options("alpha", "0", "speed_std", "8"));
		Location amplifiedStart = new Location(100, 100);
		Location amplifiedNext = amplified.getNextLocation(amplifiedStart);
		assertEquals(18.0, amplified.currentSpeed, 1e-9, "the speed_std option must be honoured");
		assertEquals(18.0, displacement(amplifiedStart, amplifiedNext), 1e-9);
	}

	@Test
	void negativeSpeedIsClampedToZeroInsteadOfMovingTheDeviceBackwards() {
		// The huge speed deviation makes the current speed negative: the device must
		// stop, not travel in the opposite direction.
		model.random = new ScriptedRandom(0.0, -1.0);
		model.setParameters(options("alpha", "0", "speed_std", "100"));
		Location start = new Location(100, 100);
		Location next = model.getNextLocation(start);
		assertEquals(-90.0, model.currentSpeed, 1e-9);
		assertEquals(100.0, next.getXPos(), 1e-9);
		assertEquals(100.0, next.getYPos(), 1e-9);
	}

	@Test
	void initDrawsTheMeanDirectionOnlyOnce() {
		model.random = new Random(42);
		model.getNextLocation(new Location(100, 100));
		double mean = model.meanDirection;
		assertTrue(mean >= 0 && mean < 360, "mean direction out of range: " + mean);
		model.getNextLocation(new Location(100, 100));
		model.getNextLocation(new Location(100, 100));
		assertEquals(mean, model.meanDirection, 1e-12, "the process was re-initialised");
		assertTrue(model.initialised);
	}

	@Test
	void deviceNeverLeavesTheMapAndStepStaysBounded() {
		model.random = new Random(42);
		Location current = new Location(100, 100);
		for (int i = 0; i < 400; i++) {
			Location next = model.getNextLocation(current);
			// The step can only shrink when the border folds the position back inside,
			// and the drawn speed is mean-reverting around 10 m/s.
			assertTrue(displacement(current, next) <= Math.max(0, model.currentSpeed) * 1 + 1e-9,
					"the device moved faster than its current speed");
			assertInsideMap(next);
			current = next;
		}
	}

	@Test
	void noMemoryTurnsMoreThanALongMemory() {
		// Same seed, same Gaussian stream: with alpha = 0 every update redraws the
		// direction, while alpha = 0.95 keeps 95 % of the previous one, so the total
		// orientation variation is several times smaller. The bound (factor 2) is far
		// below the expected factor 3 and the seed makes the run deterministic.
		double withoutMemory = totalOrientationVariation(0.0);
		double withMemory = totalOrientationVariation(0.95);
		assertTrue(withMemory > 0, "the long memory process never turned");
		assertTrue(withoutMemory > 2 * withMemory,
				"alpha did not smooth the trajectory: " + withoutMemory + " vs " + withMemory);
	}

	private double totalOrientationVariation(double alpha) {
		GaussMarkovMobilityModel candidate = new GaussMarkovMobilityModel(simulationManager, new Location(100, 100));
		candidate.random = new Random(1234);
		candidate.setSpeed(10);
		candidate.setParameters(options("alpha", String.valueOf(alpha), "std", "30"));
		Location current = new Location(100, 100);
		double variation = 0;
		double previous = 0;
		for (int i = 0; i < 150; i++) {
			current = candidate.getNextLocation(current);
			if (i > 0)
				variation += Math.abs(candidate.direction - previous);
			previous = candidate.direction;
		}
		return variation;
	}

	@Test
	void trajectoryIsCorrelatedRatherThanIndependent() {
		// Consecutive headings of a Gauss-Markov device stay close, unlike the random
		// walk where two consecutive directions are independent.
		model.random = new Random(42);
		model.setParameters(options("alpha", "0.9", "std", "10"));
		Location current = new Location(100, 100);
		model.getNextLocation(current);
		double previous = model.direction;
		for (int i = 0; i < 50; i++) {
			current = model.getNextLocation(current);
			assertTrue(Math.abs(model.direction - previous) < 90,
					"the orientation jumped by " + (model.direction - previous) + " degrees");
			previous = model.direction;
			assertInsideMap(current);
		}
	}

	@Test
	void generatePathPrecomputesOneLocationPerUpdate() {
		model.setParameters(options("alpha", "0.9", "std", "10"));
		model.generatePath();

		assertEquals(61, model.path.size());
		assertEquals(new Location(100, 100), model.path.get(0));
		for (int key = 0; key <= 60000; key += 1000) {
			assertInsideMap(model.path.get(key));
		}
		assertNotEquals(model.path.get(5000), model.path.get(0));
	}
}
