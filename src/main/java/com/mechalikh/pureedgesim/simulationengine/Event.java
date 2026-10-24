package com.mechalikh.pureedgesim.simulationengine;

import java.util.Objects;

/**
 * A message delivered to a {@link SimEntity} at a given simulation time.
 * Events are ordered by time, then by insertion serial, so events scheduled for
 * the same instant are processed in the order they were created (or in reverse
 * creation order when inserted with
 * {@link PureEdgeSim#insertFirst(Event)}).
 */
public class Event implements Comparable<Event>, QueueElement {
	/** Simulation time at which the event must be processed, in seconds. */
	double time;
	/** Entity that will process the event. */
	protected SimEntity simEntity;
	/** Caller-defined event type. */
	protected int tag;
	/** Optional payload, usually the task or transfer concerned. */
	protected Object data;
	/** Insertion order, used to break ties between events at the same time. */
	protected long serial;

	/**
	 * Creates an event without payload.
	 *
	 * @param simEntity the entity that will process the event.
	 * @param time      the simulation time, in seconds.
	 * @param tag       the event type.
	 */
	public Event(SimEntity simEntity, Double time, int tag) {
		this.simEntity = simEntity;
		this.time = time;
		this.tag = tag;
	}

	/**
	 * Creates an event with a payload.
	 *
	 * @param simEntity the entity that will process the event.
	 * @param time      the simulation time, in seconds.
	 * @param tag       the event type.
	 * @param data      the payload.
	 */
	public Event(SimEntity simEntity, Double time, int tag, Object data) {
		this.simEntity = simEntity;
		this.time = time;
		this.tag = tag;
		this.data = data;
	}

	/**
	 * Returns the event type.
	 *
	 * @return the tag.
	 */
	public int getTag() {
		return tag;
	}

	/**
	 * Returns the simulation time of this event.
	 *
	 * @return the time in seconds.
	 */
	public double getTime() {
		return time;
	}

	/**
	 * Returns the entity that will process this event.
	 *
	 * @return the target entity.
	 */
	public SimEntity getSimEntity() {
		return simEntity;
	}

	/**
	 * Returns the event payload.
	 *
	 * @return the payload, or null for events without data.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Sets the insertion serial used for tie-breaking.
	 *
	 * @param l the serial.
	 */
	public void setSerial(long l) {
		this.serial = l;
	}

	@Override
	public int compareTo(final Event that) {
		if (that == null) {
			return 1;
		}

		if (this.equals(that)) {
			return 0;
		}

		int res = Double.compare(time, that.getTime());
		if (res != 0) {
			return res;
		}

		return Long.compare(serial, that.getSerial());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final Event that = (Event) obj;
		return Double.compare(that.getTime(), getTime()) == 0 && getTag() == that.getTag()
				&& getSerial() == that.getSerial();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTime(), getTag(), getSerial());
	}

	/**
	 * Returns the insertion serial.
	 *
	 * @return the serial.
	 */
	public long getSerial() {
		return this.serial;
	}

}
