/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package com.mechalikh.pureedgesim.simulationengine;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * An {@link Queue} that stores future simulation Events and Tasks. It uses a {@link TreeSet}
 * in order ensure the Ts are stored ordered.
 *
 * @author Marcos Dias de Assuncao
 * @author Manoel Campos da Silva Filho
 * @author Charafeddine Mechalikh
 * @see java.util.TreeSet
 * @since PureEdgeSim 5.0
 */
public class FutureQueue<T extends QueueElement> implements Queue<T> {

	/**
	 * The sorted set of QueueElements.
	 */
	protected final SortedSet<T> sortedSet = new TreeSet<>();

	protected long serial;

	protected long lowestSerial;

	/** @see #getMaxTsNumber() */
	protected long maxTsNumber;

	@Override
	public void add(final T item) {
		item.setSerial(serial++);
		sortedSet.add(item);
		maxTsNumber = Math.max(maxTsNumber, sortedSet.size());
	}

	/**
	 * Adds a new item to the head of the queue.
	 *
	 * @param item The element to be put in the queue.
	 */
	public void addFirst(final T item) {
		item.setSerial(--lowestSerial);
		sortedSet.add(item);
	}

	@Override
	public Iterator<T> iterator() {
		return sortedSet.iterator();
	}

	@Override
	public Stream<T> stream() {
		return sortedSet.stream();
	}

	@Override
	public int size() {
		return sortedSet.size();
	}

	@Override
	public boolean isEmpty() {
		return sortedSet.isEmpty();
	}

	/**
	 * Removes an item from the queue.
	 *
	 * @param queueElement the element to remove
	 * @return true if successful; false if not queueElement was removed
	 */
	public boolean remove(final T queueElement) {
		return sortedSet.remove(queueElement);
	}

	@Override
	public T first() throws NoSuchElementException {
		return sortedSet.first();
	}

}
