package com.mechalikh.pureedgesim.simulationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventTest {

	@Test
	void constructorsExposeTheirValues() {
		SimEntity entity = null;
		Event plain = new Event(entity, 2.5, 7);
		assertEquals(2.5, plain.getTime());
		assertEquals(7, plain.getTag());
		assertEquals(entity, plain.getSimEntity());
		assertEquals(null, plain.getData());
		assertEquals(0L, plain.getSerial());

		Object data = "payload";
		Event withData = new Event(entity, 3.5, 8, data);
		assertEquals(3.5, withData.getTime());
		assertEquals(8, withData.getTag());
		assertEquals(data, withData.getData());

		plain.setSerial(42L);
		assertEquals(42L, plain.getSerial());
	}

	@Test
	void compareToHandlesNullEqualityAndOrdering() {
		Event first = new Event(null, 1.0, 1);
		first.setSerial(0);
		Event same = new Event(null, 1.0, 1);
		same.setSerial(0);

		assertEquals(1, first.compareTo(null));
		assertEquals(0, first.compareTo(first));
		assertEquals(0, first.compareTo(same));

		Event later = new Event(null, 2.0, 1);
		later.setSerial(0);
		Event earlier = new Event(null, 0.0, 1);
		earlier.setSerial(0);
		assertTrue(first.compareTo(later) < 0);
		assertTrue(first.compareTo(earlier) > 0);

		Event sameTimeLowerSerial = new Event(null, 1.0, 1);
		sameTimeLowerSerial.setSerial(-1);
		Event sameTimeHigherSerial = new Event(null, 1.0, 1);
		sameTimeHigherSerial.setSerial(1);
		assertTrue(first.compareTo(sameTimeLowerSerial) > 0);
		assertTrue(first.compareTo(sameTimeHigherSerial) < 0);

		// Same time and same serial but a different tag is not equal, although it
		// compares equal because only the time and serial take part in ordering.
		Event otherTag = new Event(null, 1.0, 2);
		otherTag.setSerial(0);
		assertFalse(first.equals(otherTag));
		assertEquals(0, first.compareTo(otherTag));
	}

	@Test
	void equalsAndHashCodeFollowTimeTagAndSerial() {
		Event event = new Event(null, 1.0, 3, "x");
		event.setSerial(9);

		assertTrue(event.equals(event));
		assertFalse(event.equals(null));
		assertFalse(event.equals("not an event"));

		Event equal = new Event(null, 1.0, 3, "y");
		equal.setSerial(9);
		assertTrue(event.equals(equal));
		assertEquals(event.hashCode(), equal.hashCode());

		Event differentTime = new Event(null, 2.0, 3);
		differentTime.setSerial(9);
		assertFalse(event.equals(differentTime));
		assertNotEquals(event.hashCode(), differentTime.hashCode());

		Event differentTag = new Event(null, 1.0, 4);
		differentTag.setSerial(9);
		assertFalse(event.equals(differentTag));

		Event differentSerial = new Event(null, 1.0, 3);
		differentSerial.setSerial(10);
		assertFalse(event.equals(differentSerial));
	}
}
