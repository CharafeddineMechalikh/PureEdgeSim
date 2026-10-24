/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package com.mechalikh.pureedgesim.simulationengine;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * An interface to be implemented by Task and Event queues.
 *
 * @author Marcos Dias de Assuncao
 * @author Manoel Campos da Silva Filho
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public interface Queue<T> {
    /**
     * Adds a new queue element to the queue. Adding a new element to the queue preserves the temporal order of
     * the elements in the queue.
     *
     * @param queueElement The queueElement to be put in the queue.
     */
    void add(T queueElement);

    /**
     * Returns an iterator to the elements into the queue.
     *
     * @return the iterator
     */
    Iterator<T> iterator();

    /**
     * Returns a stream to the elements into the queue.
     *
     * @return the stream
     */
    Stream<T> stream();

    /**
     * Returns the size of this queue.
     *
     * @return the size
     */
    int size();

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Gets the first element of the queue.
     *
     * @return the first element
     * @throws NoSuchElementException when the queue is empty
     */
    T first() throws NoSuchElementException;
}
