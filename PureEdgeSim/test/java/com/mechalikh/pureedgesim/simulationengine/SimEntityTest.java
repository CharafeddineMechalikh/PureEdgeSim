package com.mechalikh.pureedgesim.simulationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SimEntityTest {

	private static class TestEntity extends SimEntity {
		Event lastEvent;

		TestEntity(PureEdgeSim simulation) {
			super(simulation);
		}

		TestEntity() {
			super();
		}

		Event doubleSchedule(SimEntity target, double delay, int tag) {
			return schedule(target, delay, tag);
		}

		Event intSchedule(SimEntity target, int delay, int tag) {
			return schedule(target, delay, tag);
		}

		Event dataSchedule(SimEntity target, double delay, int tag, Object data) {
			return schedule(target, delay, tag, data);
		}

		Event now(SimEntity target, int tag) {
			return scheduleNow(target, tag);
		}

		Event nowWithData(SimEntity target, int tag, Object data) {
			return scheduleNow(target, tag, data);
		}

		void assignId(int value) {
			setId(value);
		}

		@Override
		protected void processEvent(Event e) {
			lastEvent = e;
		}
	}

	private static class OtherEntity extends TestEntity {
		OtherEntity(PureEdgeSim simulation) {
			super(simulation);
		}

		@Override
		protected void processEvent(Event e) {
			lastEvent = e;
		}
	}

	@Test
	void constructorRegistersEntityAndAssignsSequentialIdsPerClass() {
		PureEdgeSim simulation = new PureEdgeSim();
		TestEntity first = new TestEntity(simulation);
		TestEntity second = new TestEntity(simulation);
		OtherEntity other = new OtherEntity(simulation);

		assertEquals(0, first.getId());
		assertEquals(1, second.getId());
		assertEquals(0, other.getId());
		assertSame(simulation, first.getSimulation());
		assertEquals(3, simulation.entitiesList.size());
	}

	@Test
	void setSimulatorAndSetIdCanBeUsedDirectly() {
		PureEdgeSim simulation = new PureEdgeSim();
		TestEntity entity = new TestEntity();
		assertNull(entity.getSimulation());

		entity.setSimulator(simulation);
		entity.assignId(5);
		assertSame(simulation, entity.getSimulation());
		assertEquals(5, entity.getId());
	}

	@Test
	void everyScheduleOverloadUsesTheSimulationClock() {
		PureEdgeSim simulation = new PureEdgeSim();
		TestEntity entity = new TestEntity(simulation);
		Object payload = new Object();

		Event doubleEvent = entity.doubleSchedule(entity, 2.5, 11);
		Event intEvent = entity.intSchedule(entity, 3, 12);
		Event dataEvent = entity.dataSchedule(entity, 4.5, 13, payload);
		Event nowEvent = entity.now(entity, 14);
		Event nowDataEvent = entity.nowWithData(entity, 15, payload);

		assertEquals(2.5, doubleEvent.getTime());
		assertEquals(3.0, intEvent.getTime());
		assertEquals(4.5, dataEvent.getTime());
		assertSame(payload, dataEvent.getData());
		assertEquals(0.0, nowEvent.getTime());
		assertEquals(0.0, nowDataEvent.getTime());
		assertSame(payload, nowDataEvent.getData());
		assertEquals(11, doubleEvent.getTag());
		assertEquals(12, intEvent.getTag());
		assertEquals(13, dataEvent.getTag());
		assertEquals(14, nowEvent.getTag());
		assertEquals(15, nowDataEvent.getTag());
		assertEquals(5, simulation.getEventsQueue().size());
	}
}
