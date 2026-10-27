package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class ManhattanMobilityModelTest extends AbstractTest {

	private SimulationManager simulationManager;
	private ManhattanMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		emptyEdgeList();
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
		model = new ManhattanMobilityModel(simulationManager, new Location(100, 100));
		model.random = new Random(42);
		model.setSpeed(17);
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
	void horizontalLegOnlyMovesTheXCoordinate() {
		model.target = new Location(200, 50);
		Location current = new Location(100, 50);
		// The step is 17 m, so the device advances on X until the remaining 15 m are
		// shorter than one step and it lands exactly on the intersection.
		double[] expected = new double[] { 117, 134, 151, 168, 185, 200 };
		for (int i = 0; i < expected.length; i++) {
			current = model.getNextLocation(current);
			assertEquals(expected[i], current.getXPos(), 1e-9, "unexpected x at step " + i);
			assertEquals(50.0, current.getYPos(), 1e-9, "the vertical axis moved during the horizontal leg");
		}
	}

	@Test
	void verticalLegCompletesTheHorizontalOneFirst() {
		model.target = new Location(200, 120);
		Location current = new Location(190, 50);
		// X is only 10 m away, i.e. less than one step: it is snapped to 200 while Y
		// starts moving, then the device climbs the street until it reaches 120.
		double[] expectedY = new double[] { 67, 84, 101, 118, 120 };
		for (int i = 0; i < expectedY.length; i++) {
			current = model.getNextLocation(current);
			assertEquals(200.0, current.getXPos(), 1e-9, "the device did not snap to the target column");
			assertEquals(expectedY[i], current.getYPos(), 1e-9, "unexpected y at step " + i);
		}
	}

	@Test
	void verticalLegTowardTheBottomOfTheMap() {
		model.target = new Location(200, 20);
		Location current = new Location(190, 50);
		current = model.getNextLocation(current);
		assertEquals(200.0, current.getXPos(), 1e-9);
		assertEquals(33.0, current.getYPos(), 1e-9, "the device must move toward smaller Y");
		current = model.getNextLocation(current);
		assertEquals(200.0, current.getXPos(), 1e-9);
		assertEquals(20.0, current.getYPos(), 1e-9, "the remaining gap is shorter than one step");
	}

	@Test
	void intersectionIsKeptUntilItHasBeenReached() {
		Location target = new Location(200, 50);
		model.target = target;
		Location next = model.getNextLocation(new Location(100, 50));
		assertSame(target, model.target, "the destination changed in the middle of a leg");
		assertEquals(117.0, next.getXPos(), 1e-9);

		// Half a step short of the intersection the destination is still the same.
		model.getNextLocation(new Location(190, 50));
		assertSame(target, model.target, "the destination must be kept until the intersection is reached");

		// Once the device stands on the intersection it turns toward a new one.
		model.getNextLocation(new Location(200, 50));
		assertNotSame(target, model.target, "a new intersection should be picked on arrival");
	}

	@Test
	void arrivalOnAnIntersectionPicksANewDestination() {
		Location previous = new Location(100, 100);
		model.target = previous;
		model.setParameters(options("grid_size", "50"));
		Location next = model.getNextLocation(new Location(100, 100));

		assertNotSame(previous, model.target, "standing on the intersection must select another one");
		assertEquals(0.0, model.target.getXPos() % 50, 1e-9, "the new destination is not a lattice point");
		assertEquals(0.0, model.target.getYPos() % 50, 1e-9, "the new destination is not a lattice point");
		assertTrue(Math.abs(next.getXPos() - 100) <= 17 + 1e-9 && Math.abs(next.getYPos() - 100) <= 17 + 1e-9,
				"the device covered more than one step");
		assertInsideMap(next);
	}

	@Test
	void movementIsAlwaysAlignedOnOneAxis() {
		model.setParameters(options("grid_size", "50"));
		double step = model.getStepDistance();
		Location current = new Location(10, 10);
		for (int i = 0; i < 400; i++) {
			Location next = model.getNextLocation(current);
			double dx = next.getXPos() - current.getXPos();
			double dy = next.getYPos() - current.getYPos();
			assertTrue(Math.abs(dx) <= step + 1e-9, "the device jumped on X at iteration " + i);
			assertTrue(Math.abs(dy) <= step + 1e-9, "the device jumped on Y at iteration " + i);
			// The node follows the streets: one axis is advanced while the other is held
			// still, except on the turn where the finished axis is snapped in place.
			assertTrue(dy == 0 || dx == 0 || next.getXPos() == model.target.getXPos()
					|| next.getYPos() == model.target.getYPos(),
					"the device moved diagonally at iteration " + i);
			assertInsideMap(next);
			current = next;
		}
	}

	@Test
	void intersectionsAreOnTheGridLattice() {
		model.setParameters(options("grid_size", "50"));
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 400; i++) {
			Location intersection = model.pickIntersection();
			assertTrue(intersection.getXPos() % 50 == 0, "x is not a multiple of the grid: " + intersection.getXPos());
			assertTrue(intersection.getYPos() % 50 == 0, "y is not a multiple of the grid: " + intersection.getYPos());
			assertTrue(intersection.getXPos() >= 50 && intersection.getXPos() <= 200,
					"x outside the grid: " + intersection.getXPos());
			assertTrue(intersection.getYPos() >= 50 && intersection.getYPos() <= 200,
					"y outside the grid: " + intersection.getYPos());
			seen.add(intersection.getXPos() + "," + intersection.getYPos());
		}
		// A 200x200 area with a 50 m grid has 4x4 intersections; 400 draws of the
		// deterministic stream must reach almost all of them.
		assertTrue(seen.size() >= 13, "the generator does not cover the grid: " + seen.size() + " intersections");
	}

	@Test
	void gridSizeOptionMovesTheLattice() {
		model.setParameters(options("grid_size", "25"));
		for (int i = 0; i < 200; i++) {
			Location intersection = model.pickIntersection();
			assertEquals(0.0, intersection.getXPos() % 25, 1e-9);
			assertEquals(0.0, intersection.getYPos() % 25, 1e-9);
			assertTrue(intersection.getXPos() >= 25 && intersection.getXPos() <= 200,
					"x outside the grid: " + intersection.getXPos());
		}

		model.setParameters(options("grid_size", "100"));
		for (int i = 0; i < 200; i++) {
			Location intersection = model.pickIntersection();
			assertTrue(intersection.getXPos() == 100 || intersection.getXPos() == 200,
					"a 100 m grid only has two columns, found " + intersection.getXPos());
			assertTrue(intersection.getYPos() == 100 || intersection.getYPos() == 200,
					"a 100 m grid only has two rows, found " + intersection.getYPos());
		}
	}

	@Test
	void defaultGridIsOneHundredMeters() {
		for (int i = 0; i < 200; i++) {
			Location intersection = model.pickIntersection();
			assertEquals(0.0, intersection.getXPos() % 100, 1e-9);
			assertEquals(0.0, intersection.getYPos() % 100, 1e-9);
		}
	}

	@Test
	void gridSmallerThanOneMeterIsClamped() {
		// Math.max(1, grid_size) keeps a zero grid from dividing by zero.
		model.setParameters(options("grid_size", "0"));
		for (int i = 0; i < 100; i++) {
			Location intersection = model.pickIntersection();
			assertEquals(Math.floor(intersection.getXPos()), intersection.getXPos(), 1e-9);
			assertTrue(intersection.getXPos() >= 1 && intersection.getXPos() <= 200,
					"x outside the grid: " + intersection.getXPos());
			assertTrue(intersection.getYPos() >= 1 && intersection.getYPos() <= 200,
					"y outside the grid: " + intersection.getYPos());
		}
	}

	@Test
	void aGridWiderThanTheMapStillProducesExactlyOneColumn() {
		// Math.max(1, width / grid_size): the intersection is one grid step from the
		// origin, which the scenario generator must keep inside the simulation area.
		model.setParameters(options("grid_size", "220"));
		Location intersection = model.pickIntersection();
		assertEquals(220.0, intersection.getXPos(), 1e-9);
		assertEquals(220.0, intersection.getYPos(), 1e-9);
	}

	@Test
	void withoutSpeedTheDeviceStaysOnTheStreet() {
		model.setSpeed(0);
		Location current = new Location(100, 100);
		assertSame(current, model.getNextLocation(current));
	}

	@Test
	void deviceNeverLeavesTheSimulationArea() {
		model.setParameters(options("grid_size", "50"));
		model.setSpeed(23);
		Location current = new Location(3, 7);
		for (int i = 0; i < 600; i++) {
			current = model.getNextLocation(current);
			assertInsideMap(current);
		}
	}

	@Test
	void generatePathPrecomputesOneLocationPerUpdate() {
		model.setParameters(options("grid_size", "50"));
		model.generatePath();

		assertEquals(61, model.path.size());
		assertEquals(new Location(100, 100), model.path.get(0));
		for (int key = 0; key <= 60000; key += 1000) {
			assertInsideMap(model.path.get(key));
		}
		assertEquals(model.path.get(1000), model.updateLocation(1));
	}

	@Test
	void nonNumericGridSizeIsRejected() {
		model.setParameters(options("grid_size", "wide"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> model.pickIntersection());
		assertTrue(error.getMessage().contains("the \"grid_size\" option must be a number"),
				"unexpected message: " + error.getMessage());
	}
}
