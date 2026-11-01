/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General  License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General  License for more details.
 *
 *     You should have received a copy of the GNU General  License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 *     @since PureEdgeSim 6.1
 **/
package com.mechalikh.pureedgesim.simulationengine;

import java.util.Objects;

/**
 * Base implementation of the ordering that a {@link FutureQueue} relies on.
 * <p>
 * A queue element is placed in the queue's {@link java.util.TreeSet} by its
 * {@link Comparable} result, so two elements are ordered by a time key first and
 * by the insertion serial second, which keeps elements with the same time key in
 * arrival order. The time key is left abstract because each kind of element
 * chooses its own: an offloaded task orders itself by its offloading time, while
 * a queued request orders itself by its arrival time.
 * <p>
 * The generic parameter is the type this element is compared with, so a task
 * queue compares tasks ({@code TaskAbstract}) while a simulator holding its own
 * element class compares that class (for example
 * {@code AbstractQueueElement<MyRequest>}).
 *
 * @param <E> the type of queue elements this one is compared with, as declared
 *            by the queue that holds them
 * @author Charafeddine Mechalikh
 * @see QueueElement
 * @see FutureQueue
 */
public abstract class AbstractQueueElement<E extends QueueElement> implements QueueElement, Comparable<E> {

	/**
	 * The insertion serial that breaks time-key ties inside a
	 * {@link FutureQueue}.
	 */
	protected long serial;

	/**
	 * Returns the time key this element is ordered by inside a queue.
	 *
	 * @return the time key, in seconds
	 */
	public abstract double getTime();

	/**
	 * Returns the insertion serial of this element.
	 *
	 * @return the serial number, {@code 0} before the element has been queued
	 */
	@Override
	public long getSerial() {
		return this.serial;
	}

	/**
	 * Sets the insertion serial of this element, assigned by the queue when the
	 * element is added.
	 *
	 * @param l the serial number to set
	 */
	@Override
	public void setSerial(long l) {
		this.serial = l;
	}

	/**
	 * Compares this element with the specified one for order. Returns a negative
	 * integer, zero, or a positive integer as this element's time key is less
	 * than, equal to, or greater than the other one's, using the insertion serial
	 * to break ties.
	 *
	 * @param that the element to be compared
	 * @return a negative integer, zero, or a positive integer as this element is
	 *         less than, equal to, or greater than the specified element
	 */
	@Override
	public int compareTo(final E that) {
		if (that.equals(null)) {
			return 1;
		}

		if (this.equals(that)) {
			return 0;
		}

		int res = Double.compare(this.getTime(), timeOf(that));
		if (res != 0) {
			return res;
		}

		return Long.compare(serial, that.getSerial());
	}

	/**
	 * Reads the time key of another element. The {@link QueueElement} interface
	 * itself does not declare a time key, so an element that exposes
	 * {@code getTime()}, like a task or a request, is ordered by it, while a plain
	 * queue element without a time key falls back to its serial.
	 *
	 * @param that the element to read
	 * @return its time key, or {@code 0} when it exposes none
	 */
	private static double timeOf(QueueElement that) {
		if (that instanceof AbstractQueueElement)
			return ((AbstractQueueElement<?>) that).getTime();
		return 0;
	}

	/**
	 * Indicates whether some other object is "equal to" this one. Two elements are
	 * equal when they are of the same class and share the same time key and
	 * insertion serial.
	 *
	 * @param obj the object to compare to
	 * @return true if this object is the same as the obj argument; false otherwise
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final QueueElement that = (QueueElement) obj;
		return Double.compare(timeOf(that), getTime()) == 0 && getSerial() == that.getSerial();
	}

	/**
	 * Returns a hash code value for the object. The hash code is generated based
	 * on the time key and insertion serial of this element.
	 *
	 * @return the hash code value for the object.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getTime(), getSerial());
	}
}
