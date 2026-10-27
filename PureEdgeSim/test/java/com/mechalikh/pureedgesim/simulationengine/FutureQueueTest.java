package com.mechalikh.pureedgesim.simulationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class FutureQueueTest {

	private static Event event(double time, int tag) {
		return new Event(null, time, tag);
	}

	@Test
	void addKeepsElementsOrderedByTime() {
		FutureQueue<Event> queue = new FutureQueue<>();
		assertTrue(queue.isEmpty());
		assertEquals(0, queue.size());

		Event late = event(3.0, 30);
		Event early = event(1.0, 10);
		Event middle = event(2.0, 20);
		queue.add(late);
		queue.add(early);
		queue.add(middle);

		assertFalse(queue.isEmpty());
		assertEquals(3, queue.size());
		assertSame(early, queue.first());

		List<Integer> tags = new ArrayList<>();
		Iterator<Event> iterator = queue.iterator();
		while (iterator.hasNext()) {
			tags.add(iterator.next().getTag());
		}
		assertEquals(List.of(10, 20, 30), tags);
	}

	@Test
	void addFirstPutsElementsAtTheHead() {
		FutureQueue<Event> queue = new FutureQueue<>();
		Event normal = event(1.0, 1);
		queue.add(normal);

		Event first = event(1.0, 2);
		queue.addFirst(first);
		assertSame(first, queue.first());

		Event evenEarlier = event(1.0, 3);
		queue.addFirst(evenEarlier);
		assertSame(evenEarlier, queue.first());
		assertEquals(3, queue.size());

		assertEquals(-2L, evenEarlier.getSerial());
		assertEquals(-1L, first.getSerial());
		assertEquals(0L, normal.getSerial());
	}

	@Test
	void removeReportsWhetherAnElementWasRemoved() {
		FutureQueue<Event> queue = new FutureQueue<>();
		Event first = event(1.0, 1);
		Event second = event(2.0, 2);
		queue.add(first);
		queue.add(second);

		assertTrue(queue.remove(first));
		assertEquals(1, queue.size());
		assertSame(second, queue.first());
		assertFalse(queue.remove(first));
	}

	@Test
	void firstOnEmptyQueueThrowsNoSuchElementException() {
		FutureQueue<Event> queue = new FutureQueue<>();
		assertThrows(NoSuchElementException.class, queue::first);
	}

	@Test
	void streamAndIteratorExposeTheSortedElements() {
		FutureQueue<Event> queue = new FutureQueue<>();
		queue.add(event(2.0, 2));
		queue.add(event(1.0, 1));

		assertEquals(List.of(1, 2), queue.stream().map(Event::getTag).collect(java.util.stream.Collectors.toList()));

		Iterator<Event> iterator = queue.iterator();
		assertTrue(iterator.hasNext());
		assertEquals(1, iterator.next().getTag());
		assertEquals(2, iterator.next().getTag());
		assertFalse(iterator.hasNext());
	}

	@Test
	void maximumQueueSizeIsTracked() {
		ExposedQueue queue = new ExposedQueue();
		queue.add(event(1.0, 1));
		queue.add(event(2.0, 2));
		assertEquals(2, queue.maximumSize());
		queue.remove(queue.first());
		queue.add(event(3.0, 3));
		assertEquals(2, queue.maximumSize());
	}

	private static final class ExposedQueue extends FutureQueue<Event> {
		long maximumSize() {
			return maxTsNumber;
		}
	}
}
