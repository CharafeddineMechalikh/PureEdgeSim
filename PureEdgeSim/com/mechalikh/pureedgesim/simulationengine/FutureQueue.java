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
 * An {@link EventQueue} that stores future simulation events.
 * It uses a {@link TreeSet} in order ensure the events
 * are stored ordered. 
 *
 * @author Marcos Dias de Assuncao
 * @author Manoel Campos da Silva Filho
 * @see java.util.TreeSet
 * @since CloudSim Toolkit 1.0
 */
public class FutureQueue implements EventQueue {

    /**
     * The sorted set of events.
     */
    private final SortedSet<Event> sortedSet = new TreeSet<>();

    /** @see #getSerial() */
    private long serial;

    private long lowestSerial;

    /** @see #getMaxEventsNumber() */
    private long maxEventsNumber;

    @Override
    public void addEvent(final Event newEvent) {
        newEvent.setSerial(serial++);
        sortedSet.add(newEvent);
        maxEventsNumber = Math.max(maxEventsNumber, sortedSet.size());
    }

    /**
     * Adds a new event to the head of the queue.
     *
     * @param newEvent The event to be put in the queue.
     */
    public void addEventFirst(final Event newEvent) {
        newEvent.setSerial(--lowestSerial);
        sortedSet.add(newEvent);
    }

    @Override
    public Iterator<Event> iterator() {
        return sortedSet.iterator();
    }

    @Override
    public Stream<Event> stream() {
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
     * Removes the event from the queue.
     *
     * @param event the event
     * @return true if successful; false if not event was removed
     */
    public boolean remove(final Event event) {
        return sortedSet.remove(event);
    }

    @Override
    public Event first() throws NoSuchElementException {
        return sortedSet.first();
    }

    /** Gets an incremental number used for {@link Event#getSerial()} event attribute. */
    public long getSerial() {
        return serial;
    }
}
