/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 *     @since PureEdgeSim 6.0
 **/
package com.mechalikh.pureedgesim.taskgenerator;

import java.util.List;

import com.mechalikh.pureedgesim.distribution.Distribution;

/**
 * One node of the service graph of an application.
 * <p>
 * When an application declares subtasks, every request of a device generates
 * one instance of each subtask instead of a single independent task. The
 * subtasks whose dependency list is empty are offloaded at the request arrival
 * time; the others are offloaded once all their dependencies have returned
 * their results.
 * <p>
 * Each distribution is either the one of the application or the constant
 * override declared in the applications file, so a subtask samples its own
 * values for every request, exactly like an independent task does.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Application#getSubtasks()
 * @see com.mechalikh.pureedgesim.scenariomanager.ApplicationFileParser
 */
public class Subtask {

	/** Name of this subtask, unique within its application. */
	protected final String name;

	/** Names of the subtasks that must finish before this one is offloaded. */
	protected final List<String> dependencies;

	/** Distribution of the task length, in MI. */
	protected final Distribution taskLength;

	/** Distribution of the request size, in bits. */
	protected final Distribution requestSize;

	/** Distribution of the results size, in bits. */
	protected final Distribution resultsSize;

	/** Distribution of the container size, in bits. */
	protected final Distribution containerSize;

	/** Distribution of the latency requirement, in seconds. */
	protected final Distribution latency;

	/**
	 * Creates a subtask.
	 *
	 * @param name         name of the subtask, unique within its application
	 * @param dependencies names of the subtasks it depends on, empty for a root
	 * @param taskLength   distribution of the task length, in MI
	 * @param requestSize  distribution of the request size, in bits
	 * @param resultsSize  distribution of the results size, in bits
	 * @param containerSize distribution of the container size, in bits
	 * @param latency      distribution of the latency requirement, in seconds
	 */
	public Subtask(String name, List<String> dependencies, Distribution taskLength, Distribution requestSize,
			Distribution resultsSize, Distribution containerSize, Distribution latency) {
		this.name = name;
		this.dependencies = List.copyOf(dependencies);
		this.taskLength = taskLength;
		this.requestSize = requestSize;
		this.resultsSize = resultsSize;
		this.containerSize = containerSize;
		this.latency = latency;
	}

	/**
	 * Returns the name of this subtask.
	 *
	 * @return the name of this subtask
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the names of the subtasks this one depends on.
	 *
	 * @return the dependency names, empty when this is a root subtask
	 */
	public List<String> getDependencies() {
		return dependencies;
	}

	/**
	 * Returns whether this subtask can be offloaded as soon as a request arrives.
	 *
	 * @return {@code true} when this subtask has no dependencies
	 */
	public boolean isRoot() {
		return dependencies.isEmpty();
	}

	/**
	 * Draws the length of the next instance of this subtask.
	 *
	 * @return the task length in MI, at least one MI
	 */
	public long sampleTaskLength() {
		return Math.max(1, (long) taskLength.sample());
	}

	/**
	 * Draws the size of the next request.
	 *
	 * @return the request size in bits, at least one bit
	 */
	public long sampleRequestSize() {
		return Math.max(1, (long) requestSize.sample());
	}

	/**
	 * Draws the size of the next results message.
	 *
	 * @return the results size in bits, at least one bit
	 */
	public long sampleResultsSize() {
		return Math.max(1, (long) resultsSize.sample());
	}

	/**
	 * Draws the size of the container used for the next instance of this subtask.
	 *
	 * @return the container size in bits, at least one bit
	 */
	public long sampleContainerSize() {
		return Math.max(1, (long) containerSize.sample());
	}

	/**
	 * Draws the latency requirement of the next instance of this subtask.
	 *
	 * @return the maximum accepted latency, in seconds
	 */
	public double sampleLatency() {
		return Math.max(0.001, latency.sample());
	}

	/**
	 * Returns the distribution of the task length.
	 *
	 * @return the task length distribution, in MI
	 */
	public Distribution getTaskLengthDistribution() {
		return taskLength;
	}

	/**
	 * Returns the distribution of the request size.
	 *
	 * @return the request size distribution, in bits
	 */
	public Distribution getRequestSizeDistribution() {
		return requestSize;
	}

	/**
	 * Returns the distribution of the results size.
	 *
	 * @return the results size distribution, in bits
	 */
	public Distribution getResultsSizeDistribution() {
		return resultsSize;
	}

	/**
	 * Returns the distribution of the container size.
	 *
	 * @return the container size distribution, in bits
	 */
	public Distribution getContainerSizeDistribution() {
		return containerSize;
	}

	/**
	 * Returns the distribution of the latency requirement.
	 *
	 * @return the latency distribution, in seconds
	 */
	public Distribution getLatencyDistribution() {
		return latency;
	}

	/**
	 * Returns a string representation of this subtask.
	 *
	 * @return the name, the dependencies, and the distributions of this subtask
	 */
	@Override
	public String toString() {
		return "Subtask [name=" + name + ", dependencies=" + dependencies + ", taskLength=" + taskLength
				+ ", requestSize=" + requestSize + ", resultsSize=" + resultsSize + ", containerSize=" + containerSize
				+ ", latency=" + latency + "]";
	}
}
