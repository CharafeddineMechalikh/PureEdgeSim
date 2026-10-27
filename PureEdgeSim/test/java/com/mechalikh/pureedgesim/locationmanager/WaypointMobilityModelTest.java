package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class WaypointMobilityModelTest extends AbstractTest {

	private SimulationManager simulationManager;
	private WaypointMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		emptyEdgeList();
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
		model = new WaypointMobilityModel(simulationManager, new Location(0, 0));
		model.setSpeed(30);
		model.setMinPauseDuration(0);
		model.setMaxPauseDuration(0);
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

	private static double displacement(Location from, Location to) {
		return Math.hypot(to.getXPos() - from.getXPos(), to.getYPos() - from.getYPos());
	}

	@Test
	void waypointsAreParsedLazilyFromTheOption() {
		model.setParameters(options("waypoints", "50,50;150,50;150,150"));
		assertNull(model.waypoints, "the route must be read on the first update");

		model.getNextLocation(model.getCurrentLocation());

		assertNotNull(model.waypoints);
		assertEquals(3, model.waypoints.size());
		assertEquals(50.0, model.waypoints.get(0).getXPos(), 1e-12);
		assertEquals(50.0, model.waypoints.get(0).getYPos(), 1e-12);
		assertEquals(150.0, model.waypoints.get(1).getXPos(), 1e-12);
		assertEquals(150.0, model.waypoints.get(2).getYPos(), 1e-12);
	}

	@Test
	void spacesAndEmptySegmentsAreTolerated() {
		model.setParameters(options("waypoints", " 50 , 50 ;; 150 , 50 "));
		model.parseWaypoints();

		assertEquals(2, model.waypoints.size());
		assertEquals(50.0, model.waypoints.get(0).getXPos(), 1e-12);
		assertEquals(150.0, model.waypoints.get(1).getXPos(), 1e-12);
	}

	@Test
	void missingWaypointsOptionIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(0, 0)));
		assertTrue(
				error.getMessage().contains(
						"WaypointMobilityModel - the \"waypoints\" option is required, check the \"mobility\" element"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void singleWaypointIsRejected() {
		model.setParameters(options("waypoints", "50,50"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, model::parseWaypoints);
		assertTrue(error.getMessage().contains("the \"waypoints\" option must contain at least two points"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void waypointWithoutItsSecondCoordinateIsRejected() {
		model.setParameters(options("waypoints", "50,50;150"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, model::parseWaypoints);
		assertTrue(error.getMessage().contains("the \"waypoints\" option must be written as \"x1,y1;x2,y2\""),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void nonNumericWaypointIsRejected() {
		model.setParameters(options("waypoints", "50,50;a,b"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, model::parseWaypoints);
		assertTrue(error.getMessage().contains("must only contain numbers, found \"a,b\""),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void routeIsFollowedInOrderWithoutTeleporting() {
		model.setParameters(options("waypoints", "100,0;200,0"));
		// The route is a straight line of 100 m legs covered 30 m at a time, so the
		// device advances, stops one step short of each waypoint to turn around it,
		// and loops back to the first waypoint at the end of the route.
		double[] expectedX = new double[] { 30, 60, 90, 90, 120, 150, 180, 180, 150 };
		int[] expectedIndex = new int[] { 0, 0, 0, 1, 1, 1, 1, 0, 0 };
		Location current = new Location(0, 0);
		for (int i = 0; i < expectedX.length; i++) {
			Location next = model.getNextLocation(current);
			assertEquals(expectedX[i], next.getXPos(), 1e-9, "unexpected x at update " + i);
			assertEquals(0.0, next.getYPos(), 1e-9, "unexpected y at update " + i);
			assertEquals(expectedIndex[i], model.targetIndex, "unexpected waypoint at update " + i);
			assertTrue(displacement(current, next) <= model.getStepDistance() + 1e-9,
					"the device jumped between two waypoints at update " + i);
			current = next;
		}
	}

	@Test
	void movementIsInterpolatedOnDiagonalLegs() {
		model.setParameters(options("waypoints", "100,100;100,190"));
		Location current = new Location(0, 0);
		for (int i = 0; i < 4; i++) {
			Location next = model.getNextLocation(current);
			assertEquals(30.0, displacement(current, next), 1e-9, "the leg is not covered one step at a time");
			assertTrue(next.getXPos() <= 100 && next.getYPos() <= 100, "the device left the segment");
			current = next;
		}
	}

	@Test
	void deviceDwellsAtEachWaypointForThePauseDuration() {
		model.setMinPauseDuration(3);
		model.setParameters(options("waypoints", "30,0;60,0"));
		Location start = new Location(0, 0);

		// The first waypoint is already within one step, so the device arrives and
		// stays three updates (the pause) before resuming.
		Location arrived = model.getNextLocation(start);
		assertEquals(0.0, arrived.getXPos(), 1e-9);
		assertEquals(3.0, model.pauseRemaining, 1e-12, "the dwell must be the configured pause");

		Location current = arrived;
		for (int i = 2; i <= 4; i++) {
			Location next = model.getNextLocation(current);
			assertEquals(0.0, next.getXPos(), 1e-9, "the device moved during the dwell at update " + i);
			assertEquals(0.0, next.getYPos(), 1e-9);
			assertEquals(3.0 - (i - 1), model.pauseRemaining, 1e-12);
			current = next;
		}
		current = model.getNextLocation(current);
		assertEquals(30.0, current.getXPos(), 1e-9, "the device must resume after the dwell");
		assertEquals(0.0, model.pauseRemaining, 1e-12);
	}

	@Test
	void loopFalseStopsAtTheLastWaypoint() {
		model.setParameters(options("waypoints", "30,0;60,0", "loop", "false"));
		Location current = new Location(0, 0);
		double[] expectedX = new double[] { 0, 30, 30, 60, 60 };
		for (int i = 0; i < expectedX.length; i++) {
			current = model.getNextLocation(current);
			assertEquals(expectedX[i], current.getXPos(), 1e-9, "unexpected x at update " + i);
			assertEquals(0.0, current.getYPos(), 1e-9, "the route lies on the X axis");
		}
		assertTrue(model.finished, "a non repeated route must be marked as finished");
		assertEquals(1, model.targetIndex, "the device must stay on the last waypoint");
		// Once finished the device remains parked on the last waypoint.
		Location parked = model.getNextLocation(current);
		assertEquals(60.0, parked.getXPos(), 1e-9);
		assertEquals(0.0, parked.getYPos(), 1e-9);
	}

	@Test
	void loopTrueRestartsTheRouteAfterTheLastWaypoint() {
		model.setParameters(options("waypoints", "30,0;60,0"));
		Location current = new Location(0, 0);
		List<Integer> targeted = new ArrayList<>();
		List<Double> xs = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			current = model.getNextLocation(current);
			targeted.add(model.targetIndex);
			xs.add(current.getXPos());
		}
		// The node arrives one step short of each waypoint and turns around it, so the
		// route wraps back to the first waypoint instead of stopping.
		assertEquals(Arrays.asList(1, 1, 0, 1, 0, 1), targeted, "unexpected waypoint order: " + targeted);
		assertFalse(model.finished, "a repeated route is never finished");
		for (double x : xs) {
			assertTrue(x >= -1e-9 && x <= 60 + 1e-9, "the device left the route: " + x);
		}
	}

	@Test
	void generatePathFollowsTheRouteInsideTheMap() {
		model.setParameters(options("waypoints", "50,50;150,50;150,150;50,150"));
		model.setMobile(true);

		model.generatePath();

		assertEquals(61, model.path.size());
		assertEquals(new Location(0, 0), model.path.get(0));
		List<Location> route = model.waypoints;
		for (int key = 0; key <= 60000; key += 1000) {
			Location location = model.path.get(key);
			assertTrue(location.getXPos() >= 0 && location.getXPos() <= 200, "x left the map at " + key);
			assertTrue(location.getYPos() >= 0 && location.getYPos() <= 200, "y left the map at " + key);
		}
		// Every waypoint of the route is visited: the device comes within one step of
		// each of them, in the order of the list.
		for (Location waypoint : route) {
			boolean reachedIt = false;
			for (int key = 0; key <= 60000; key += 1000) {
				if (displacement(waypoint, model.path.get(key)) <= model.getStepDistance() + 1e-9) {
					reachedIt = true;
					break;
				}
			}
			assertTrue(reachedIt, "the waypoint " + waypoint.getXPos() + "," + waypoint.getYPos() + " was never visited");
		}
	}

	@Test
	void nonNumericCoordinateNamesTheModelInError() {
		model.setParameters(options("waypoints", "x,0;2,0"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, model::parseWaypoints);
		assertTrue(error.getMessage().startsWith("WaypointMobilityModel"),
				"the error should name the model: " + error.getMessage());
	}

	@Test
	void immobileDeviceKeepsItsInitialLocation() {
		model.setParameters(options("waypoints", "50,50;150,50"));
		model.setMobile(false);

		model.generatePath();

		assertTrue(model.path.isEmpty(), "an immobile device must not precompute a route");
		assertEquals(new Location(0, 0), model.getCurrentLocation());
	}
}
