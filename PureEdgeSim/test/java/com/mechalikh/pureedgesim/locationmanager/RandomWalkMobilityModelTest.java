package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class RandomWalkMobilityModelTest extends AbstractTest {

	/** Generator that always returns the same uniform draw, hence a fixed angle. */
	private static final class FixedDirectionRandom extends Random {
		private static final long serialVersionUID = 1L;
		private final double uniform;

		FixedDirectionRandom(double uniform) {
			super(0L);
			this.uniform = uniform;
		}

		@Override
		public double nextDouble() {
			return uniform;
		}
	}

	private SimulationManager simulationManager;
	private RandomWalkMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		emptyEdgeList();
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
		model = new RandomWalkMobilityModel(simulationManager, new Location(100, 100));
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

	private static double displacement(Location from, Location to) {
		return Math.hypot(to.getXPos() - from.getXPos(), to.getYPos() - from.getYPos());
	}

	private static void assertInsideMap(Location location) {
		assertTrue(location.getXPos() >= 0 && location.getXPos() <= SimulationParameters.simulationMapWidth,
				"x=" + location.getXPos() + " left the simulation width [0,"
						+ SimulationParameters.simulationMapWidth + "]");
		assertTrue(location.getYPos() >= 0 && location.getYPos() <= SimulationParameters.simulationMapLength,
				"y=" + location.getYPos() + " left the simulation length [0,"
						+ SimulationParameters.simulationMapLength + "]");
	}

	@Test
	void everyStepCoversExactlyTheDistanceAllowedByTheSpeed() {
		Location current = model.getCurrentLocation();
		// The device starts in the middle of a 200x200 map with a 2 m step, so no
		// border reflection can happen during the 20 first ticks and the displacement
		// must be exactly speed * updateInterval every single time.
		for (int i = 0; i < 20; i++) {
			Location next = model.getNextLocation(current);
			assertEquals(2.0, displacement(current, next), 1e-9, "step " + i + " is not speed * updateInterval");
			current = next;
		}
	}

	@Test
	void stepLengthScalesWithSpeedAndWithTheUpdateInterval() {
		model.setSpeed(10);
		assertEquals(10.0, model.getStepDistance(), 1e-12);
		Location next = model.getNextLocation(new Location(100, 100));
		assertEquals(10.0, displacement(new Location(100, 100), next), 1e-9);

		// The same speed but two updates give twice the distance to cover.
		SimulationParameters.updateInterval = 2;
		assertEquals(20.0, model.getStepDistance(), 1e-12);
		assertEquals(20.0, displacement(new Location(100, 100), model.getNextLocation(new Location(100, 100))), 1e-9);
	}

	@Test
	void directionIsRedrawnAtEveryUpdate() {
		Location current = new Location(100, 100);
		Location previous = model.getNextLocation(current);
		double previousDx = previous.getXPos() - current.getXPos();
		double previousDy = previous.getYPos() - current.getYPos();
		for (int i = 0; i < 20; i++) {
			current = previous;
			previous = model.getNextLocation(current);
			double dx = previous.getXPos() - current.getXPos();
			double dy = previous.getYPos() - current.getYPos();
			// Two consecutive steps are collinear only when the two drawn angles are
			// equal modulo pi, which does not happen for the deterministic stream.
			double cross = previousDx * dy - previousDy * dx;
			assertTrue(Math.abs(cross) > 1e-6, "the direction did not change at iteration " + i);
			previousDx = dx;
			previousDy = dy;
		}
	}

	@Test
	void pauseDurationsOfTheScenarioAreIgnored() {
		// A random walk never stops: even with long pause durations configured, the
		// device moves at every update, unlike the default random waypoint model.
		model.setMinPauseDuration(100);
		model.setMaxPauseDuration(200);
		model.setMinMobilityDuration(100);
		model.setMaxMobilityDuration(300);
		Location current = new Location(100, 100);
		for (int i = 0; i < 10; i++) {
			Location next = model.getNextLocation(current);
			assertTrue(displacement(current, next) > 1e-9, "the device paused at iteration " + i);
			current = next;
		}
	}

	@Test
	void fastDeviceIsReflectedBackInsideTheMap() {
		model.setSpeed(60);
		Location current = new Location(10, 10);
		for (int i = 0; i < 300; i++) {
			current = model.getNextLocation(current);
			assertInsideMap(current);
		}
	}

	@Test
	void crossingTheRightBorderReversesTheHorizontalCoordinate() {
		// nextDouble() = 0 gives an angle of 0 degrees, i.e. a step toward +X.
		model.random = new FixedDirectionRandom(0.0);
		model.setSpeed(20);
		Location next = model.getNextLocation(new Location(195, 100));
		assertEquals(185.0, next.getXPos(), 1e-9);
		assertEquals(100.0, next.getYPos(), 1e-9);
	}

	@Test
	void crossingTheLeftBorderReversesTheHorizontalCoordinate() {
		// nextDouble() = 0.5 gives an angle of 180 degrees, i.e. a step toward -X.
		model.random = new FixedDirectionRandom(0.5);
		model.setSpeed(20);
		Location next = model.getNextLocation(new Location(10, 100));
		assertEquals(10.0, next.getXPos(), 1e-9);
		assertEquals(100.0, next.getYPos(), 1e-9);
	}

	@Test
	void sameSeedProducesTheSameTrajectory() {
		RandomWalkMobilityModel other = new RandomWalkMobilityModel(simulationManager, new Location(100, 100));
		other.random = new Random(42);
		other.setSpeed(2);
		Location first = new Location(100, 100);
		Location second = new Location(100, 100);
		for (int i = 0; i < 25; i++) {
			first = model.getNextLocation(first);
			second = other.getNextLocation(second);
			assertEquals(first.getXPos(), second.getXPos(), 1e-12, "x diverged at iteration " + i);
			assertEquals(first.getYPos(), second.getYPos(), 1e-12, "y diverged at iteration " + i);
		}
	}

	@Test
	void generatePathPrecomputesOneLocationPerUpdate() {
		model.generatePath();

		assertEquals(61, model.path.size());
		assertEquals(61, model.datacentersMap.size());
		assertEquals(new Location(100, 100), model.path.get(0));
		assertEquals(model.path.get(1000), model.updateLocation(1));
		assertFalse(model.path.get(1000).equals(model.path.get(0)), "the precomputed path never moved");
		for (int key = 0; key <= 60000; key += 1000) {
			assertInsideMap(model.path.get(key));
		}
	}
}
