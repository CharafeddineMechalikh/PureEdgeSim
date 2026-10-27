package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
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

class LevyWalkMobilityModelTest extends AbstractTest {

	/** Generator whose every draw is the same value, to reproduce the Pareto law. */
	private static final class ScriptedRandom extends Random {
		private static final long serialVersionUID = 1L;
		private final double uniform;

		ScriptedRandom(double uniform) {
			super(0L);
			this.uniform = uniform;
		}

		@Override
		public double nextDouble() {
			return uniform;
		}
	}

	private SimulationManager simulationManager;
	private LevyWalkMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		emptyEdgeList();
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
		model = new LevyWalkMobilityModel(simulationManager, new Location(100, 100));
		model.random = new Random(42);
		model.setSpeed(20);
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

	private static double distance(Location from, Location to) {
		return Math.hypot(to.getXPos() - from.getXPos(), to.getYPos() - from.getYPos());
	}

	/**
	 * Draws the end of a flight from the centre of a large map, so that the border
	 * clamping never shortens the flight and the measured length is the Pareto
	 * sample itself.
	 */
	private double[] drawFlightLengths(LevyWalkMobilityModel candidate, Location center, int draws) {
		double[] lengths = new double[draws];
		for (int i = 0; i < draws; i++) {
			Location end = candidate.pickFlightEnd(center);
		 lengths[i] = distance(center, end);
		}
		return lengths;
	}

	private LevyWalkMobilityModel wideMapModel(String... keyValues) {
		SimulationParameters.simulationMapWidth = 2000;
		SimulationParameters.simulationMapLength = 2000;
		LevyWalkMobilityModel candidate = new LevyWalkMobilityModel(simulationManager, new Location(1000, 1000));
		candidate.random = new Random(42);
		candidate.setSpeed(20);
		candidate.setMobile(true);
		candidate.setParameters(options(keyValues));
		return candidate;
	}

	@Test
	void flightEndIsClampedToTheSimulationArea() {
		assertEquals(0.0, model.clampToMap(new Location(-30, 500)).getXPos(), 1e-12);
		assertEquals(200.0, model.clampToMap(new Location(-30, 500)).getYPos(), 1e-12);
		assertEquals(50.0, model.clampToMap(new Location(50, -10)).getXPos(), 1e-12);
		assertEquals(0.0, model.clampToMap(new Location(50, -10)).getYPos(), 1e-12);
		assertEquals(50.0, model.clampToMap(new Location(50, 60)).getXPos(), 1e-12);
		assertEquals(60.0, model.clampToMap(new Location(50, 60)).getYPos(), 1e-12);
	}

	@Test
	void projectedFlightEndsStayInsideTheMap() {
		Location center = new Location(100, 100);
		for (int i = 0; i < 300; i++) {
			Location end = model.pickFlightEnd(center);
			assertInsideMap(end);
			// The default maximum is the diagonal of the simulation area.
			assertTrue(distance(center, end) <= Math.hypot(200, 200) + 1e-6, "flight longer than the diagonal");
		}
	}

	@Test
	void flightsRespectTheConfiguredMinimumAndMaximum() {
		// Pareto draws are at least min_step and are capped at max_step; the heavy tail
		// of a shape of 0.5 makes 25 % of the flights hit the cap, so with 200 draws of
		// the deterministic stream both the cap and the long tail are observable.
		LevyWalkMobilityModel candidate = wideMapModel("min_step", "50", "max_step", "800", "shape", "0.5");
		double[] lengths = drawFlightLengths(candidate, new Location(1000, 1000), 200);

		double shortest = Double.MAX_VALUE;
		double longest = 0;
		int capped = 0;
		for (double length : lengths) {
			assertTrue(length >= 50 - 1e-6, "a flight is shorter than min_step: " + length);
			assertTrue(length <= 800 + 1e-6, "a flight is longer than max_step: " + length);
			if (Math.abs(length - 800) < 1e-6)
				capped++;
			shortest = Math.min(shortest, length);
			longest = Math.max(longest, length);
		}
		assertTrue(capped > 0, "the max_step cap was never reached");
		assertTrue(longest > 250, "no long flight was drawn, the tail is not heavy: " + longest);
		assertTrue(longest > 3 * shortest, "flights are not spread over an order of magnitude");
	}

	@Test
	void maxStepTruncatesTheParetoTail() {
		LevyWalkMobilityModel candidate = wideMapModel("min_step", "50", "max_step", "60", "shape", "0.5");
		double[] lengths = drawFlightLengths(candidate, new Location(1000, 1000), 300);

		int truncated = 0;
		int untouched = 0;
		for (double length : lengths) {
			assertTrue(length <= 60 + 1e-6, "a flight escapes max_step: " + length);
			assertTrue(length >= 50 - 1e-6, "a flight is shorter than min_step: " + length);
			if (Math.abs(length - 60) < 1e-6)
				truncated++;
			if (length < 55)
				untouched++;
		}
		assertTrue(truncated > 0, "samples above max_step must be truncated");
		assertTrue(untouched > 0, "samples below max_step must be kept");
	}

	@Test
	void defaultMinimumFlightIsTheDistanceCoveredInOneUpdate() {
		// No min_step option: the shortest flight is the one the device can cover
		// during a single update, i.e. speed * updateInterval = 20 m.
		LevyWalkMobilityModel candidate = wideMapModel();
		double[] lengths = drawFlightLengths(candidate, new Location(1000, 1000), 200);
		double shortest = Double.MAX_VALUE;
		double longest = 0;
		for (double length : lengths) {
			assertTrue(length >= 20 - 1e-6, "a flight is shorter than one update: " + length);
			shortest = Math.min(shortest, length);
			longest = Math.max(longest, length);
		}
		assertEquals(20.0, shortest, 2.0, "the default min_step must be the step distance");
		assertTrue(longest > 20, "every flight has the very same length");
	}

	@Test
	void flightLengthFollowsTheParetoFormula() {
		// A scripted uniform of 0.5 and a Pareto drawn as scale / U^(1/shape) give a
		// flight of exactly 20 m for a 10 m minimum and a shape of 1.
		LevyWalkMobilityModel candidate = wideMapModel("min_step", "10", "shape", "1");
		candidate.random = new ScriptedRandom(0.5);
		Location center = new Location(1000, 1000);
		Location end = candidate.pickFlightEnd(center);
		// The same scripted draw also fixes the direction at 0.5 * 360 = 180 degrees.
		assertEquals(980.0, end.getXPos(), 1e-6);
		assertEquals(1000.0, end.getYPos(), 1e-6);

		candidate.setParameters(options("min_step", "10", "shape", "2"));
		end = candidate.pickFlightEnd(center);
		assertEquals(10 / Math.sqrt(0.5), distance(center, end), 1e-6, "the shape exponent is not used");

		candidate.setParameters(options("min_step", "10", "shape", "1", "max_step", "15"));
		end = candidate.pickFlightEnd(center);
		assertEquals(15.0, distance(center, end), 1e-6, "the flight must be capped by max_step");
	}

	@Test
	void nonPositiveShapeIsRejected() {
		model.setParameters(options("shape", "0"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.pickFlightEnd(new Location(100, 100)));
		assertTrue(error.getMessage().contains("LevyWalkMobilityModel - the \"shape\" option must be > 0"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void minStepMustBePositiveForTheParetoLaw() {
		model.setParameters(options("min_step", "0"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.pickFlightEnd(new Location(100, 100)));
		assertTrue(error.getMessage().contains("the parameter \"scale\" must be > 0"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void nonNumericShapeIsRejected() {
		model.setParameters(options("shape", "heavy"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.pickFlightEnd(new Location(100, 100)));
		assertTrue(error.getMessage().contains("the \"shape\" option must be a number"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void withoutSpeedTheDeviceStaysWhereItIs() {
		model.setSpeed(0);
		Location current = new Location(100, 100);
		assertSame(current, model.getNextLocation(current));
	}

	@Test
	void deviceCoversAtMostOneStepPerUpdateAndNeverLeavesTheMap() {
		Location current = new Location(100, 100);
		for (int i = 0; i < 500; i++) {
			Location next = model.getNextLocation(current);
			assertTrue(distance(current, next) <= 20 + 1e-9, "the device teleported at iteration " + i);
			assertInsideMap(next);
			current = next;
		}
	}

	@Test
	void eachFlightIsAStraightLine() {
		Location current = new Location(100, 100);
		Location previousTarget = model.target;
		double baseDx = 0;
		double baseDy = 0;
		int flights = 0;
		for (int i = 0; i < 400; i++) {
			Location next = model.getNextLocation(current);
			double dx = next.getXPos() - current.getXPos();
			double dy = next.getYPos() - current.getYPos();
			if (model.target != previousTarget) {
				flights++;
				previousTarget = model.target;
				baseDx = dx;
				baseDy = dy;
			} else if (Math.hypot(dx, dy) > 1e-9 && Math.hypot(baseDx, baseDy) > 1e-9) {
				double cross = Math.abs(baseDx * dy - baseDy * dx);
				double product = Math.hypot(baseDx, baseDy) * Math.hypot(dx, dy);
				assertTrue(cross <= 1e-9 * product, "the device curved inside its flight at iteration " + i);
				assertTrue(baseDx * dx + baseDy * dy > 0, "the flight reversed its direction at iteration " + i);
			}
			current = next;
		}
		assertTrue(flights > 4, "too few flights were drawn: " + flights);
	}

	@Test
	void generatePathPrecomputesOneLocationPerUpdate() {
		model.generatePath();

		assertEquals(61, model.path.size());
		assertEquals(new Location(100, 100), model.path.get(0));
		for (int key = 0; key <= 60000; key += 1000) {
			assertInsideMap(model.path.get(key));
		}
		assertEquals(model.path.get(1000), model.updateLocation(1));
	}
}
