package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocationTest {

	@Test
	void coordinatesAreExposed() {
		Location location = new Location(1.5, -2.5);
		assertEquals(1.5, location.getXPos());
		assertEquals(-2.5, location.getYPos());
	}

	@Test
	void equalityUsesCoordinatesAndClass() {
		Location location = new Location(1, 2);

		assertTrue(location.equals(location));
		assertTrue(location.equals(new Location(1, 2)));
		assertFalse(location.equals(null));
		assertFalse(location.equals("not a location"));
		assertFalse(location.equals(new Location(2, 2)));
		assertFalse(location.equals(new Location(1, 3)));
	}

	@Test
	void subclassesAreNotEqualToTheBaseClass() {
		Location base = new Location(1, 2);
		Location subclass = new Location(1, 2) {
			private static final long serialVersionUID = 1L;
		};
		assertFalse(base.equals(subclass));
		assertFalse(subclass.equals(base));
	}

	@Test
	void hashCodeIsConsistentWithEquals() {
		Location first = new Location(10.5, 20.25);
		Location second = new Location(10.5, 20.25);
		assertEquals(first.hashCode(), second.hashCode());
		assertNotEquals(first.hashCode(), new Location(10.5, 21.25).hashCode());
	}

	@Test
	void hashHelpersProduceStableValues() {
		Location location = new Location(3, 4);
		int hash = location.hash(7, location.toBits(3.5));
		assertEquals(7 * 89 + location.toBits(3.5), hash);

		assertEquals(location.toBits(1.0), location.toBits(1.0));
		assertNotEquals(location.toBits(1.0), location.toBits(2.0));
	}

	@Test
	void specialFloatingPointValuesAreSupported() {
		Location positiveInfinity = new Location(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		Location anotherPositiveInfinity = new Location(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		assertEquals(positiveInfinity, anotherPositiveInfinity);
		assertEquals(positiveInfinity.hashCode(), anotherPositiveInfinity.hashCode());

		Location notANumber = new Location(Double.NaN, Double.NaN);
		Location anotherNaN = new Location(Double.NaN, Double.NaN);
		assertFalse(notANumber.equals(anotherNaN));
		assertEquals(notANumber.hashCode(), anotherNaN.hashCode());
	}
}
