package com.mechalikh.pureedgesim.taskgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;

class TaskAbstractTest {

	private DefaultTask task;

	/** A task that claims to be equal to null, used to exercise compareTo's guard. */
	static class NullEqualsTask extends DefaultTask {
		NullEqualsTask(int id) {
			super(id);
		}

		@Override
		public boolean equals(Object obj) {
			return obj == null || super.equals(obj);
		}
	}

	/** Another concrete task class, used for getClass based equality checks. */
	static class OtherTask extends DefaultTask {
		OtherTask(int id) {
			super(id);
		}
	}

	@BeforeEach
	void setUp() {
		task = new DefaultTask(1);
	}

	@Test
	void constructorAndBasicAccessors() {
		assertEquals(1, task.getId());
		task.setId(9);
		assertEquals(9, task.getId());
	}

	@Test
	void latencyAndLengthUseFluentSetters() {
		assertSame(task, task.setMaxLatency(2.5));
		assertEquals(2.5, task.getMaxLatency(), 0.0001);
		assertSame(task, task.setLength(123.0));
		assertEquals(123.0, task.getLength(), 0.0001);
	}

	@Test
	void networkTimeIsAccumulated() {
		assertEquals(0.0, task.getActualNetworkTime(), 0.0001);
		task.addActualNetworkTime(1.5);
		task.addActualNetworkTime(2.5);
		assertEquals(4.0, task.getActualNetworkTime(), 0.0001);
	}

	@Test
	void arrivalTimeInitializesTheExecutionStartTime() {
		task.setArrivalTime(10.0);
		assertEquals(10.0, task.getExecStartTime(), 0.0001);
		assertEquals(0.0, task.getWatingTime(), 0.0001);
	}

	@Test
	void waitingTimeIsTheDifferenceBetweenStartAndArrival() {
		task.setArrivalTime(10.0);
		task.setExecutionStartTime(15.0);
		assertEquals(5.0, task.getWatingTime(), 0.0001);
	}

	@Test
	void executionStartTimeAlsoResetsTheFinishTime() {
		task.setExecutionFinishTime(99.0);
		task.setExecutionStartTime(20.0);
		assertEquals(20.0, task.getExecStartTime(), 0.0001);
		assertEquals(0.0, task.getActualCpuTime(), 0.0001);
	}

	@Test
	void actualCpuTimeUsesTheFinishTime() {
		task.setExecutionStartTime(20.0);
		task.setExecutionFinishTime(23.5);
		assertEquals(3.5, task.getActualCpuTime(), 0.0001);
	}

	@Test
	void totalDelaySumsNetworkWaitingAndCpuTimes() {
		task.setArrivalTime(10.0);
		task.addActualNetworkTime(2.0);
		task.setExecutionStartTime(15.0);
		task.setExecutionFinishTime(20.0);
		assertEquals(12.0, task.getTotalDelay(), 0.0001);
	}

	@Test
	void serialCanBeSetAndRead() {
		task.setSerial(42L);
		assertEquals(42L, task.getSerial());
	}

	@Test
	void equalTasksCompareToZero() {
		DefaultTask other = new DefaultTask(2);
		task.setTime(5.0);
		other.setTime(5.0);
		task.setSerial(1L);
		other.setSerial(1L);

		assertEquals(0, task.compareTo(other));
	}

	@Test
	void tasksAreOrderedByTimeThenSerial() {
		DefaultTask other = new DefaultTask(2);
		task.setTime(5.0);
		other.setTime(7.0);
		assertTrue(task.compareTo(other) < 0);
		assertTrue(other.compareTo(task) > 0);

		other.setTime(5.0);
		task.setSerial(1L);
		other.setSerial(2L);
		assertTrue(task.compareTo(other) < 0);
		assertTrue(other.compareTo(task) > 0);
	}

	@Test
	void compareToReturnsOneWhenTheOtherTaskEqualsNull() {
		NullEqualsTask other = new NullEqualsTask(2);
		task.setTime(5.0);
		other.setTime(7.0);
		assertEquals(1, task.compareTo(other));
	}

	@Test
	void compareToThrowsForANullReference() {
		assertThrows(NullPointerException.class, () -> task.compareTo(null));
	}

	@Test
	void equalsUsesTimeAndSerial() {
		DefaultTask other = new DefaultTask(2);
		assertTrue(task.equals(task));
		assertFalse(task.equals(null));
		assertFalse(task.equals("not a task"));

		task.setTime(3.0);
		other.setTime(3.0);
		task.setSerial(4L);
		other.setSerial(4L);
		assertTrue(task.equals(other));
		assertEquals(task.hashCode(), other.hashCode());

		other.setSerial(5L);
		assertFalse(task.equals(other));

		other.setSerial(4L);
		other.setTime(6.0);
		assertFalse(task.equals(other));
	}

	@Test
	void equalsRejectsDifferentConcreteClasses() {
		OtherTask other = new OtherTask(2);
		task.setTime(3.0);
		other.setTime(3.0);
		task.setSerial(1L);
		other.setSerial(1L);

		assertFalse(task.equals(other));
		assertNotEquals(task, other);
	}

	@Test
	void hashCodesDifferWhenTimeOrSerialDiffer() {
		DefaultTask other = new DefaultTask(2);
		task.setTime(1.0);
		task.setSerial(1L);
		other.setTime(2.0);
		other.setSerial(1L);
		assertNotEquals(task.hashCode(), other.hashCode());
	}

	@Test
	void nullObjectCanBeUsedAsAnEdgeDevice() {
		assertSame(ComputingNode.NULL, task.getEdgeDevice());
	}
}
