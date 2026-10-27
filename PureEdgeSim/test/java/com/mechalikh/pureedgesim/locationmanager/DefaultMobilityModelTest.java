package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class DefaultMobilityModelTest extends AbstractTest {

	private SimulationManager simulationManager;
	private DefaultMobilityModel model;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		model = new DefaultMobilityModel(simulationManager, new Location(50, 50));
		model.random = new Random(42);
		model.setSpeed(2);
		model.setMinPauseDuration(50);
		model.setMaxPauseDuration(200);
		model.setMinMobilityDuration(100);
		model.setMaxMobilityDuration(300);
	}

	@Test
	void constructorInitialisesRandomGeneratorAndOrientation() {
		DefaultMobilityModel fresh = new DefaultMobilityModel(simulationManager, new Location(1, 1));
		assertNotNull(fresh.random);
		assertTrue(fresh.orientationAngle >= 0 && fresh.orientationAngle < 359);
		assertEquals(1, fresh.getCurrentLocation().getXPos());
	}

	@Test
	void pausedDeviceKeepsItsLocationAndDecrementsPauseDuration() {
		Location location = new Location(10, 20);
		model.pause = true;
		model.pauseDuration = 5;

		assertSame(location, model.getNextLocation(location));
		assertEquals(4, model.pauseDuration);
	}

	@Test
	void movingDeviceUpdatesItsPositionAlongOrientation() {
		model.pause = false;
		model.pauseDuration = 5;
		model.mobilityDuration = 10;
		model.orientationAngle = 0;
		model.setSpeed(2);

		Location next = model.getNextLocation(new Location(50, 50));

		assertEquals(52, next.getXPos(), 1e-9);
		assertEquals(50, next.getYPos(), 1e-9);
	}

	@Test
	void devicePausesWhenMobilityDurationIsExhausted() {
		model.pause = false;
		model.pauseDuration = -1;
		model.mobilityDuration = 0;

		Location next = model.getNextLocation(new Location(50, 50));

		assertTrue(model.pause);
		assertTrue(model.pauseDuration >= 50 && model.pauseDuration < 200);
		assertTrue(model.mobilityDuration >= 100 && model.mobilityDuration < 300);
		assertTrue(model.orientationAngle >= 0 && model.orientationAngle < 359);
		assertNotNull(next);
	}

	@Test
	void deviceResumesWhenPauseDurationExpired() {
		model.pause = false;
		model.pauseDuration = 0;
		model.mobilityDuration = 10;

		model.getNextLocation(new Location(50, 50));

		assertEquals(false, model.pause);
		assertEquals(9, model.mobilityDuration);
	}

	@Test
	void pausePicksDurationsAndOrientationWithinConfiguredBounds() {
		model.pause();

		assertTrue(model.pause);
		assertTrue(model.pauseDuration >= 50 && model.pauseDuration < 200);
		assertTrue(model.mobilityDuration >= 100 && model.mobilityDuration < 300);
		assertTrue(model.orientationAngle >= 0 && model.orientationAngle < 359);
	}

	@Test
	void resumeClearsPauseAndDecrementsMobilityDuration() {
		model.pause = true;
		model.mobilityDuration = 10;

		model.resume();

		assertEquals(false, model.pause);
		assertEquals(9, model.mobilityDuration);
	}

	@Test
	void reorientateTurnsAroundAtTheRightEdge() {
		model.orientationAngle = 45;
		model.reoriontate(200, 50);
		assertTrue(model.orientationAngle <= -90);
		assertTrue(model.orientationAngle >= -269);
	}

	@Test
	void reorientateTurnsAroundAtTheLeftEdge() {
		model.orientationAngle = 45;
		model.reoriontate(0, 50);
		assertTrue(model.orientationAngle >= -90);
		assertTrue(model.orientationAngle < 90);
	}

	@Test
	void reorientateTurnsAroundAtTheTopEdge() {
		model.orientationAngle = 45;
		model.reoriontate(50, 200);
		assertTrue(model.orientationAngle <= 0);
		assertTrue(model.orientationAngle >= -179);
	}

	@Test
	void reorientateTurnsAroundAtTheBottomEdge() {
		model.orientationAngle = 45;
		model.reoriontate(50, 0);
		assertTrue(model.orientationAngle >= 0);
		assertTrue(model.orientationAngle <= 179);
	}

	@Test
	void reorientateKeepsDirectionInsideTheMap() {
		model.orientationAngle = 45;
		model.reoriontate(50, 50);
		assertEquals(45, model.orientationAngle);
	}

	@Test
	void updateLocationMovesAlongTheOrientationAngle() {
		model.orientationAngle = 90;
		model.setSpeed(2);

		Location next = model.updateLocation(50, 50);

		assertEquals(50, next.getXPos(), 1e-9);
		assertEquals(52, next.getYPos(), 1e-9);
	}

	@Test
	void movingFromTheRightEdgeRedirectsTheDeviceInsideTheMap() {
		model.random = new Random() {
			private static final long serialVersionUID = 1L;

			@Override
			public int nextInt(int bound) {
				return 60;
			}
		};
		model.pause = false;
		model.pauseDuration = 5;
		model.mobilityDuration = 10;

		Location next = model.getNextLocation(new Location(200, 50));

		assertTrue(next.getXPos() < 200);
	}
}
