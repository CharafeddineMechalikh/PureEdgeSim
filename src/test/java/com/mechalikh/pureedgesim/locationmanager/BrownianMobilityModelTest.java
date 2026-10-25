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

class BrownianMobilityModelTest extends AbstractTest {

	/** Generator replaying scripted Gaussian draws, cycling when exhausted. */
	private static final class ScriptedRandom extends Random {
		private static final long serialVersionUID = 1L;
		private final double[] gaussians;
		private int index;

		ScriptedRandom(double... gaussians) {
			super(0L);
			this.gaussians = gaussians;
		}

		@Override
		public double nextGaussian() {
			return gaussians[index++ % gaussians.length];
		}
	}

	private SimulationManager simulationManager;
	private BrownianMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		emptyEdgeList();
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
		model = new BrownianMobilityModel(simulationManager, new Location(100, 100));
		model.random = new Random(42);
		model.setSpeed(2);
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

	@Test
	void defaultDisplacementIsTheDistanceCoveredInOneUpdate() {
		model.random = new ScriptedRandom(1.0);
		model.setSpeed(7);
		// No "std" option: each coordinate is displaced by speed * updateInterval = 7 m.
		Location next = model.getNextLocation(new Location(100, 100));
		assertEquals(107.0, next.getXPos(), 1e-9);
		assertEquals(107.0, next.getYPos(), 1e-9);

		// Doubling the speed doubles the default standard deviation.
		model.setSpeed(14);
		next = model.getNextLocation(new Location(100, 100));
		assertEquals(114.0, next.getXPos(), 1e-9);
		assertEquals(114.0, next.getYPos(), 1e-9);
	}

	@Test
	void stdOptionScalesEveryCoordinateOfTheStep() {
		BrownianMobilityModel narrow = new BrownianMobilityModel(simulationManager, new Location(100, 100));
		BrownianMobilityModel wide = new BrownianMobilityModel(simulationManager, new Location(100, 100));
		narrow.random = new Random(42);
		wide.random = new Random(42);
		narrow.setParameters(options("std", "2"));
		wide.setParameters(options("std", "8"));

		// Both models draw the very same Gaussian values, so the two displacements
		// differ by exactly the ratio of the configured standard deviations.
		Location narrowStep = narrow.getNextLocation(new Location(100, 100));
		Location wideStep = wide.getNextLocation(new Location(100, 100));
		double narrowDx = narrowStep.getXPos() - 100;
		double wideDx = wideStep.getXPos() - 100;
		assertTrue(Math.abs(narrowDx) > 1e-9, "the scripted stream produced a zero increment");
		assertEquals(4.0, wideDx / narrowDx, 1e-9);
		double narrowDy = narrowStep.getYPos() - 100;
		double wideDy = wideStep.getYPos() - 100;
		assertEquals(4.0, wideDy / narrowDy, 1e-9);
	}

	@Test
	void stdOptionMakesTheDeviceSpeedIrrelevant() {
		BrownianMobilityModel slow = new BrownianMobilityModel(simulationManager, new Location(100, 100));
		BrownianMobilityModel fast = new BrownianMobilityModel(simulationManager, new Location(100, 100));
		slow.random = new Random(42);
		fast.random = new Random(42);
		slow.setParameters(options("std", "5"));
		fast.setParameters(options("std", "5"));
		slow.setSpeed(1);
		fast.setSpeed(200);

		Location slowStep = slow.getNextLocation(new Location(100, 100));
		Location fastStep = fast.getNextLocation(new Location(100, 100));
		assertEquals(slowStep.getXPos(), fastStep.getXPos(), 1e-9);
		assertEquals(slowStep.getYPos(), fastStep.getYPos(), 1e-9);
	}

	@Test
	void eachCoordinateDrawsItsOwnIncrement() {
		// The first draw feeds X and the second feeds Y: they move by different
		// amounts and in opposite directions, so the increments are independent.
		model.random = new ScriptedRandom(3.0, -2.0);
		model.setParameters(options("std", "10"));
		Location next = model.getNextLocation(new Location(100, 100));
		assertEquals(130.0, next.getXPos(), 1e-9);
		assertEquals(80.0, next.getYPos(), 1e-9);
	}

	@Test
	void incrementsAreGaussianAroundZero() {
		// std = 10 with 2000 samples: the standard error of the mean is 0.22, so the
		// 3 m bound is more than 13 sigma away, and the sample deviation bound [6,14]
		// is far outside the sampling noise. The seed makes the run deterministic.
		model.random = new Random(2024);
		model.setParameters(options("std", "10"));
		Location start = new Location(100, 100);
		List<Double> xs = new ArrayList<>();
		List<Double> ys = new ArrayList<>();
		int identicalIncrements = 0;
		for (int i = 0; i < 2000; i++) {
			Location next = model.getNextLocation(start);
			double dx = next.getXPos() - start.getXPos();
			double dy = next.getYPos() - start.getYPos();
			xs.add(dx);
			ys.add(dy);
			if (dx == dy)
				identicalIncrements++;
			assertInsideMap(next);
		}
		assertEquals(0, identicalIncrements, "X and Y drew the same increment, so they are not independent");
		assertTrue(Math.abs(mean(xs)) < 3.0, "the X increments are not centred on zero: " + mean(xs));
		assertTrue(Math.abs(mean(ys)) < 3.0, "the Y increments are not centred on zero: " + mean(ys));
		double deviation = sampleDeviation(xs);
		assertTrue(deviation > 6 && deviation < 14, "the X deviation is not the configured std: " + deviation);
		deviation = sampleDeviation(ys);
		assertTrue(deviation > 6 && deviation < 14, "the Y deviation is not the configured std: " + deviation);
	}

	private static double mean(List<Double> values) {
		double sum = 0;
		for (double value : values)
			sum += value;
		return sum / values.size();
	}

	private static double sampleDeviation(List<Double> values) {
		double mean = mean(values);
		double sum = 0;
		for (double value : values)
			sum += (value - mean) * (value - mean);
		return Math.sqrt(sum / (values.size() - 1));
	}

	@Test
	void crossingTheFarBordersFoldsTheDeviceBackInside() {
		model.random = new ScriptedRandom(1.0, 2.0);
		model.setParameters(options("std", "50"));
		// X: 170 + 50 = 220 is reflected to 200 - 20 = 180.
		// Y: 190 + 100 = 290 is reflected to 200 - 90 = 110.
		Location next = model.getNextLocation(new Location(170, 190));
		assertEquals(180.0, next.getXPos(), 1e-9);
		assertEquals(110.0, next.getYPos(), 1e-9);
	}

	@Test
	void crossingTheNearBordersFoldsTheDeviceBackInside() {
		model.random = new ScriptedRandom(-1.0, -0.5);
		model.setParameters(options("std", "50"));
		// X: 10 - 50 = -40 is reflected to +40, Y: 20 - 25 = -5 is reflected to +5.
		Location next = model.getNextLocation(new Location(10, 20));
		assertEquals(40.0, next.getXPos(), 1e-9);
		assertEquals(5.0, next.getYPos(), 1e-9);
	}

	@Test
	void diffusionNeverLeavesTheSimulationArea() {
		model.random = new Random(7);
		model.setParameters(options("std", "40"));
		Location current = new Location(100, 100);
		for (int i = 0; i < 3000; i++) {
			current = model.getNextLocation(current);
			assertInsideMap(current);
		}
		// A diffusion with std = 40 does not sit still.
		assertTrue(Math.abs(current.getXPos() - 100) + Math.abs(current.getYPos() - 100) > 1,
				"the device did not wander away from its start");
	}

	@Test
	void nonNumericStdIsRejected() {
		model.setParameters(options("std", "wide"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(100, 100)));
		assertTrue(error.getMessage().contains("the \"std\" option must be a number"),
				"unexpected message: " + error.getMessage());
		assertTrue(error.getMessage().startsWith("BrownianMobilityModel"),
				"the error should name the model: " + error.getMessage());
	}

	@Test
	void generatePathPrecomputesOneLocationPerUpdate() {
		model.generatePath();

		assertEquals(61, model.path.size());
		assertEquals(new Location(100, 100), model.path.get(0));
		for (int key = 0; key <= 60000; key += 1000) {
			assertInsideMap(model.path.get(key));
		}
		assertEquals(model.path.get(30000), model.updateLocation(30));
	}
}
