package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class CircularMobilityModelTest extends AbstractTest {

	private SimulationManager simulationManager;
	private CircularMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		emptyEdgeList();
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
		// The tour starts on the circle of radius 90 centred on the 200x200 area, so
		// the whole path stays inside the simulation map.
		model = new CircularMobilityModel(simulationManager, new Location(190, 100));
		model.setSpeed(30);
		model.setMobile(true);
		model.setParameters(options("radius", "90"));
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

	private static double distanceTo(double x, double y, double centerX, double centerY) {
		return Math.hypot(x - centerX, y - centerY);
	}

	@Test
	void missingRadiusIsRejected() {
		model.setParameters(options());
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(190, 100)));
		assertTrue(
				error.getMessage().contains("CircularMobilityModel - the \"radius\" option must be > 0, found \"-1.0\""),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void nonPositiveRadiusIsRejected() {
		model.setParameters(options("radius", "0"));
		IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(190, 100)));
		assertTrue(zero.getMessage().contains("the \"radius\" option must be > 0, found \"0.0\""),
				"unexpected message: " + zero.getMessage());

		model.setParameters(options("radius", "-5"));
		IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(190, 100)));
		assertTrue(negative.getMessage().contains("found \"-5.0\""), "unexpected message: " + negative.getMessage());
	}

	@Test
	void nonNumericRadiusIsRejected() {
		model.setParameters(options("radius", "wide"));
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> model.getNextLocation(new Location(190, 100)));
		assertTrue(error.getMessage().contains("the \"radius\" option must be a number"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void distanceToTheCenterStaysEqualToRadius() {
		Location current = model.getCurrentLocation();
		for (int i = 0; i < 200; i++) {
			Location next = model.getNextLocation(current);
			assertEquals(90.0, distanceTo(next.getXPos(), next.getYPos(), 100, 100), 1e-6,
					"the device left the circle at iteration " + i);
			assertTrue(next.getXPos() >= 0 && next.getXPos() <= 200, "x left the map: " + next.getXPos());
			assertTrue(next.getYPos() >= 0 && next.getYPos() <= 200, "y left the map: " + next.getYPos());
			current = next;
		}
	}

	@Test
	void angularStepIsTheArcCoveredDuringOneUpdate() {
		// The device starts at angle 0 (east of the center) and covers 30 m per update
		// on a circle of radius 90, i.e. 30 / 90 radians at each update.
		Location current = new Location(190, 100);
		for (int i = 1; i <= 5; i++) {
			current = model.getNextLocation(current);
			assertEquals(i * (30.0 / 90.0), model.angle, 1e-12, "unexpected angle after " + i + " updates");
		}
	}

	@Test
	void halvingTheRadiusDoublesTheAngularSpeed() {
		Location current = new Location(190, 100);
		for (int i = 0; i < 5; i++) {
			current = model.getNextLocation(current);
		}
		double wideAngle = model.angle;

		CircularMobilityModel tight = new CircularMobilityModel(simulationManager, new Location(190, 100));
		tight.setSpeed(30);
		tight.setParameters(options("radius", "45", "center_x", "145"));
		Location tightCurrent = new Location(190, 100);
		for (int i = 0; i < 5; i++) {
			tightCurrent = tight.getNextLocation(tightCurrent);
		}
		assertEquals(2.0, tight.angle / wideAngle, 1e-9, "a smaller circle must turn faster for the same speed");
	}

	@Test
	void angleIsDerivedFromTheInitialLocationOnlyOnce() {
		// The tour starts from the angle of the device, here atan2(90, 0) = pi / 2,
		// and keeps advancing by one arc per update even if the location handed over
		// to the model is a different point of the circle.
		CircularMobilityModel north = new CircularMobilityModel(simulationManager, new Location(100, 190));
		north.setSpeed(30);
		north.setParameters(options("radius", "90"));
		Location current = new Location(100, 190);
		current = north.getNextLocation(current);
		assertEquals(Math.PI / 2 + 30.0 / 90.0, north.angle, 1e-12, "the tour did not start at the device angle");
		current = north.getNextLocation(new Location(190, 100));
		double expectedAngle = Math.PI / 2 + 2 * (30.0 / 90.0);
		assertEquals(expectedAngle, north.angle, 1e-12, "the angle was re-initialised");
		assertEquals(100 + Math.cos(expectedAngle) * 90, current.getXPos(), 1e-9,
				"the device jumped back to the point it was handed");
		assertEquals(100 + Math.sin(expectedAngle) * 90, current.getYPos(), 1e-9,
				"the device jumped back to the point it was handed");
	}

	@Test
	void clockwiseFalseReversesTheTour() {
		Location forward = new Location(190, 100);
		for (int i = 0; i < 4; i++) {
			forward = model.getNextLocation(forward);
		}
		assertTrue(forward.getYPos() > 100, "the default tour turns toward the top of the map");

		CircularMobilityModel backward = new CircularMobilityModel(simulationManager, new Location(190, 100));
		backward.setSpeed(30);
		backward.setParameters(options("radius", "90", "clockwise", "false"));
		Location current = new Location(190, 100);
		for (int i = 0; i < 4; i++) {
			current = backward.getNextLocation(current);
		}
		assertTrue(current.getYPos() < 100, "clockwise=false must turn toward the bottom of the map");
		assertEquals(forward.getXPos(), current.getXPos(), 1e-9, "the mirrored tour must keep the same X");
		assertEquals(100.0 - current.getYPos(), forward.getYPos() - 100.0, 1e-9,
				"clockwise=false must mirror the tour on the horizontal axis");
		assertEquals(-model.angle, backward.angle, 1e-12, "the angles must be opposite");
	}

	@Test
	void clockwiseOptionDefaultsToTrue() {
		assertEquals(90.0, model.getParameter("radius", -1), 1e-12);
		assertTrue(model.getParameterBoolean("clockwise", true));
		Location next = model.getNextLocation(new Location(190, 100));
		assertTrue(next.getYPos() > 100, "the tour must start clockwise: " + next.getYPos());
	}

	@Test
	void centerOptionsMoveTheCircle() {
		CircularMobilityModel orbit = new CircularMobilityModel(simulationManager, new Location(40, 40));
		orbit.setSpeed(30);
		orbit.setMobile(true);
		orbit.setParameters(options("radius", "10", "center_x", "30", "center_y", "40"));
		Location current = new Location(40, 40);
		for (int i = 0; i < 20; i++) {
			current = orbit.getNextLocation(current);
			assertEquals(10.0, distanceTo(current.getXPos(), current.getYPos(), 30, 40), 1e-6,
					"the configured center is not used at iteration " + i);
			assertTrue(current.getXPos() >= 20 && current.getXPos() <= 40, "x left the circle: " + current.getXPos());
			assertTrue(current.getYPos() >= 30 && current.getYPos() <= 50, "y left the circle: " + current.getYPos());
		}
		assertEquals(30.0, orbit.getParameter("center_x", -1), 1e-12, "center_x must be read as a number");
	}

	@Test
	void initialLocationOutsideTheCircleIsSnappedOntoIt() {
		// The device starts 127 m from the center but the radius is 90: the first
		// update puts it back on the tour, keeping its bearing.
		Location next = model.getNextLocation(new Location(190, 190));
		assertEquals(90.0, distanceTo(next.getXPos(), next.getYPos(), 100, 100), 1e-9);
		assertEquals(Math.PI / 4 + 30.0 / 90.0, model.angle, 1e-12, "the bearing of the device must be kept");
	}

	@Test
	void generatePathFollowsTheCircleForEveryUpdate() {
		model.generatePath();

		assertEquals(61, model.path.size());
		assertEquals(new Location(190, 100), model.path.get(0));
		for (int key = 1000; key <= 60000; key += 1000) {
			Location location = model.path.get(key);
			assertTrue(location.getXPos() >= 0 && location.getXPos() <= 200, "x left the map at " + key);
			assertTrue(location.getYPos() >= 0 && location.getYPos() <= 200, "y left the map at " + key);
			assertEquals(90.0, distanceTo(location.getXPos(), location.getYPos(), 100, 100), 1e-6,
					"the precomputed path left the circle at " + key);
		}
		assertNotEquals(model.path.get(1000), model.path.get(0));
	}
}
