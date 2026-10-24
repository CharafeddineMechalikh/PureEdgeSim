package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;

class MobilityModelNullTest {

	@Test
	void currentLocationIsAlwaysTheInfiniteLocation() {
		Location location = MobilityModel.NULL.getCurrentLocation();
		assertEquals(Double.POSITIVE_INFINITY, location.getXPos());
		assertEquals(Double.POSITIVE_INFINITY, location.getYPos());
		assertEquals(location, MobilityModel.NULL.getCurrentLocation());
	}

	@Test
	void nextLocationIsAlwaysTheInfiniteLocation() {
		Location next = MobilityModel.NULL.getNextLocation(new Location(1, 2));
		assertEquals(Double.POSITIVE_INFINITY, next.getXPos());
		assertEquals(Double.POSITIVE_INFINITY, next.getYPos());
	}

	@Test
	void distanceToIsAlwaysInfinite() {
		assertEquals(Double.POSITIVE_INFINITY, MobilityModel.NULL.distanceTo(ComputingNode.NULL));
	}

	@Test
	void generatePathIsANoOp() {
		MobilityModel.NULL.generatePath();
		assertFalse(MobilityModel.NULL.isMobile());
		assertSame(ComputingNode.NULL, MobilityModel.NULL.getClosestEdgeDataCenter());
	}
}
