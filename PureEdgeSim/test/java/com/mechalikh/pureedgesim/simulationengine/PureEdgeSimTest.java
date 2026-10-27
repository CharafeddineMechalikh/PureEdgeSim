package com.mechalikh.pureedgesim.simulationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class PureEdgeSimTest {

	private static class RecordingEntity extends SimEntity
			implements OnSimulationStartListener, OnSimulationEndListener {

		final List<String> processed = new ArrayList<>();
		final String name;
		int starts;
		int ends;
		Consumer<Event> onSimulationStartAction;
		Consumer<Event> onEventAction;
		boolean terminateOnFirstEvent;

		RecordingEntity(PureEdgeSim simulation, String name) {
			super(simulation);
			this.name = name;
		}

		@Override
		public void onSimulationStart() {
			starts++;
			if (onSimulationStartAction != null) {
				onSimulationStartAction.accept(null);
			}
		}

		@Override
		public void onSimulationEnd() {
			ends++;
		}

		@Override
		protected void processEvent(Event e) {
			processed.add(name + ":" + e.getTag() + "@" + e.getTime());
			if (onEventAction != null) {
				onEventAction.accept(e);
			}
			if (terminateOnFirstEvent) {
				simulation.terminate();
			}
		}
	}

	@Test
	void startNotifiesListenersProcessesSameTimeEventsAndClearsEntities() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity first = new RecordingEntity(simulation, "first");
		RecordingEntity second = new RecordingEntity(simulation, "second");

		first.onSimulationStartAction = ignored -> {
			first.schedule(first, 1.0, 10);
			first.schedule(first, 1.0, 11);
			first.scheduleNow(first, 5);
		};
		second.onSimulationStartAction = ignored -> second.schedule(second, 1.0, 20);

		simulation.start();

		assertEquals(1, first.starts);
		assertEquals(1, second.starts);
		assertEquals(1, first.ends);
		assertEquals(1, second.ends);
		assertEquals(3, first.processed.size());
		assertEquals("first:5@0.0", first.processed.get(0));
		assertEquals("first:10@1.0", first.processed.get(1));
		assertEquals("first:11@1.0", first.processed.get(2));
		assertEquals(1.0, simulation.clock());
		assertEquals(0, simulation.clockInMinutes());
		assertTrue(simulation.getEventsQueue().isEmpty());
		assertTrue(simulation.entitiesList.isEmpty());
	}

	@Test
	void clockInMinutesConvertsSeconds() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity entity = new RecordingEntity(simulation, "e");
		simulation.insert(new Event(entity, 125.0, 1));

		simulation.start();

		assertEquals(125.0, simulation.clock());
		assertEquals(2, simulation.clockInMinutes());
	}

	@Test
	void terminateStopsTheEventLoopButStillNotifiesEndListeners() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity entity = new RecordingEntity(simulation, "e");
		entity.terminateOnFirstEvent = true;
		entity.onSimulationStartAction = ignored -> {
			entity.schedule(entity, 1.0, 1);
			entity.schedule(entity, 10.0, 2);
		};

		simulation.start();

		assertEquals(List.of("e:1@1.0"), entity.processed);
		assertEquals(1, entity.ends);
		assertEquals(1.0, simulation.clock());
		assertEquals(1, simulation.getEventsQueue().size());
	}

	@Test
	void scheduleNowIsProcessedBeforeAlreadyQueuedEventsAtTheSameTime() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity entity = new RecordingEntity(simulation, "e");
		entity.onSimulationStartAction = ignored -> {
			entity.schedule(entity, 1.0, 1);
			entity.scheduleNow(entity, 2);
		};

		simulation.start();

		assertEquals(List.of("e:2@0.0", "e:1@1.0"), entity.processed);
	}

	@Test
	void runClockTickReturnsFalseWhenQueueIsEmptyOrNextEventIsBeyondLimit() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity entity = new RecordingEntity(simulation, "e");

		assertFalse(simulation.runClockTickAndProcessFutureEvents(10));

		simulation.insert(new Event(entity, 10.0, 1));
		assertFalse(simulation.runClockTickAndProcessFutureEvents(5));
		assertEquals(1, simulation.getEventsQueue().size());

		assertTrue(simulation.runClockTickAndProcessFutureEvents(10));
		assertEquals(List.of("e:1@10.0"), entity.processed);
		assertTrue(simulation.getEventsQueue().isEmpty());
	}

	@Test
	void processingAnEventInThePastThrowsIllegalArgumentException() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity entity = new RecordingEntity(simulation, "e");
		Event future = new Event(entity, 10.0, 1);
		simulation.insert(future);
		simulation.processEvent(future);
		assertEquals(10.0, simulation.clock());

		simulation.insert(new Event(entity, 3.0, 2));
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> simulation.runClockTickAndProcessFutureEvents(Double.MAX_VALUE));
		assertTrue(exception.getMessage().contains("Past event detected"));
	}

	@Test
	void insertFirstPlacesTheEventAtTheHeadOfTheQueue() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity entity = new RecordingEntity(simulation, "e");
		Event normal = new Event(entity, 4.0, 1);
		Event first = new Event(entity, 2.0, 2);
		simulation.getEventsQueue().add(normal);
		simulation.insertFirst(first);

		assertSame(first, simulation.getEventsQueue().first());
	}

	@Test
	void addEntityRegistersAnEntityWithoutStartingTheSimulation() {
		PureEdgeSim simulation = new PureEdgeSim();
		RecordingEntity entity = new RecordingEntity(simulation, "e");
		assertEquals(1, simulation.entitiesList.size());
		assertSame(entity, simulation.entitiesList.get(0));
	}
}
