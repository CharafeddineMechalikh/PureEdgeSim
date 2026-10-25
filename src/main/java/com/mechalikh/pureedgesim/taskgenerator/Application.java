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
 *     @since PureEdgeSim 5.0
 **/
package com.mechalikh.pureedgesim.taskgenerator;

import com.mechalikh.pureedgesim.distribution.ConstantDistribution;
import com.mechalikh.pureedgesim.distribution.Distribution;

/**
 * This class represents an application that can be executed on a computing
 * node.
 * <p>
 * Each numeric parameter may be a fixed value or a probability distribution
 * declared in the applications file. The scalar getters return the mean of the
 * distribution, so reports and charts keep a single representative value, while
 * the task generator uses the {@code sample} methods to draw a value for every
 * task.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 **/

public class Application {

	/**
	 * The rate at which requests are generated for this application
	 */
	protected int rate;

	/**
	 * The latency of the application, in seconds
	 */
	protected double latency;

	/**
	 * The size of the container that this application runs in, in bits
	 */
	protected long containerSize;

	/**
	 * The size of the request that is sent to the application, in bits
	 */
	protected long requestSize;

	/**
	 * The size of the results that the application returns, in bits
	 */
	protected long resultsSize;

	/**
	 * The length of time it takes for the application to execute, in MI
	 * (Mega-Instructions)
	 */
	protected double taskLength;

	/**
	 * The percentage of time that the application is being used by the user
	 */
	protected double usagePercentage;

	/**
	 * The type of application
	 */
	protected String type;

	/** Distribution of the request size, in bits. */
	protected Distribution requestSizeDistribution;

	/** Distribution of the results size, in bits. */
	protected Distribution resultsSizeDistribution;

	/** Distribution of the container size, in bits. */
	protected Distribution containerSizeDistribution;

	/** Distribution of the task length, in MI. */
	protected Distribution taskLengthDistribution;

	/** Distribution of the latency requirement, in seconds. */
	protected Distribution latencyDistribution;

	/** Distribution of the number of tasks generated each minute. */
	protected Distribution rateDistribution;

	/**
	 * Distribution of the time between two consecutive tasks, in seconds, or
	 * {@code null} when the tasks are evenly spaced.
	 */
	protected Distribution arrivalDistribution;

	/**
	 * The number of bits in one megabyte.
	 */
	private static final double BITS_IN_MB = 8000000.0;

	/**
	 * Constructs a new Application object whose parameters never vary.
	 *
	 * @param type            the type of the application
	 * @param rate            the rate at which requests are generated for this
	 *                        application
	 * @param usagePercentage the percentage of time that the application is being
	 *                        used by the user
	 * @param latency         the latency of the application, in seconds
	 * @param containerSize   the size of the container that this application runs
	 *                        in, in bits
	 * @param requestSize     the size of the request that is sent to the
	 *                        application, in bits
	 * @param resultsSize     the size of the results that the application returns,
	 *                        in bits
	 * @param taskLength      the length of time it takes for the application to
	 *                        execute, in MI (Mega-Instructions)
	 */
	public Application(String type, int rate, double usagePercentage, double latency, long containerSize,
			long requestSize, long resultsSize, double taskLength) {
		this(type, rate, usagePercentage, latency, containerSize, requestSize, resultsSize, taskLength, null, null,
				null, null, null, null, null);
	}

	/**
	 * Constructs a new Application object, describing some of its parameters with a
	 * distribution.
	 * <p>
	 * Every distribution may be {@code null}, in which case the corresponding scalar
	 * value is used for all the tasks.
	 *
	 * @param type                    the type of the application
	 * @param rate                    the mean rate at which requests are generated
	 *                                for this application
	 * @param usagePercentage         the percentage of devices using this
	 *                                application
	 * @param latency                 the mean latency of the application, in seconds
	 * @param containerSize           the mean container size, in bits
	 * @param requestSize             the mean request size, in bits
	 * @param resultsSize             the mean results size, in bits
	 * @param taskLength              the mean task length, in MI
	 * @param requestSizeDistribution the distribution of the request size, or
	 *                                {@code null}
	 * @param resultsSizeDistribution the distribution of the results size, or
	 *                                {@code null}
	 * @param containerSizeDistribution
	 *                                the distribution of the container size, or
	 *                                {@code null}
	 * @param taskLengthDistribution  the distribution of the task length, or
	 *                                {@code null}
	 * @param latencyDistribution     the distribution of the latency, or
	 *                                {@code null}
	 * @param rateDistribution        the distribution of the number of tasks
	 *                                generated each minute, or {@code null}
	 * @param arrivalDistribution     the distribution of the time between two
	 *                                tasks, or {@code null} for evenly spaced tasks
	 */
	public Application(String type, int rate, double usagePercentage, double latency, long containerSize,
			long requestSize, long resultsSize, double taskLength, Distribution requestSizeDistribution,
			Distribution resultsSizeDistribution, Distribution containerSizeDistribution,
			Distribution taskLengthDistribution, Distribution latencyDistribution, Distribution rateDistribution,
			Distribution arrivalDistribution) {
		setType(type);
		setRate(rate);
		setUsagePercentage(usagePercentage);
		setLatency(latency);
		setContainerSize(containerSize);
		setRequestSize(requestSize);
		setResultsSize(resultsSize);
		setTaskLength(taskLength);
		this.requestSizeDistribution = orConstant(requestSizeDistribution, requestSize);
		this.resultsSizeDistribution = orConstant(resultsSizeDistribution, resultsSize);
		this.containerSizeDistribution = orConstant(containerSizeDistribution, containerSize);
		this.taskLengthDistribution = orConstant(taskLengthDistribution, taskLength);
		this.latencyDistribution = orConstant(latencyDistribution, latency);
		this.rateDistribution = orConstant(rateDistribution, rate);
		this.arrivalDistribution = arrivalDistribution;
	}

	/**
	 * Returns the given distribution, or a constant one when it is {@code null}.
	 *
	 * @param distribution the configured distribution, may be {@code null}
	 * @param value        the value used when no distribution was configured
	 * @return a distribution that always produces a value
	 */
	protected static Distribution orConstant(Distribution distribution, double value) {
		return distribution == null ? new ConstantDistribution(value) : distribution;
	}

	/**
	 * Gets the rate at which requests are generated for this application.
	 *
	 * @return the rate at which requests are generated for this application
	 */
	public int getRate() {
		return rate;
	}

	/**
	 * Sets the rate at which requests are generated for this application. The
	 * distribution of the rate is replaced by a constant one, so the tasks stop
	 * varying with this parameter.
	 *
	 * @param rate the rate at which requests are generated for this application
	 */
	public void setRate(int rate) {
		this.rate = rate;
		this.rateDistribution = new ConstantDistribution(rate);
	}

	/**
	 * 
	 * Returns the size of the container in bits.
	 * 
	 * @return the size of the container in bits
	 */
	public long getContainerSizeInBits() {
		return containerSize;
	}

	/**
	 * 
	 * Sets the size of the container in bits. The distribution of the container size
	 * is replaced by a constant one.
	 * 
	 * @param containerSize the size of the container in bits
	 */
	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
		this.containerSizeDistribution = new ConstantDistribution(containerSize);
	}

	/**
	 * 
	 * Returns the size of the request in bits.
	 * 
	 * @return the size of the request in bits
	 */
	public long getRequestSize() {
		return requestSize;
	}

	/**
	 * 
	 * Sets the size of the request in bits. The distribution of the request size is
	 * replaced by a constant one.
	 * 
	 * @param requestSize the size of the request in bits
	 */
	public void setRequestSize(long requestSize) {
		this.requestSize = requestSize;
		this.requestSizeDistribution = new ConstantDistribution(requestSize);
	}

	/**
	 * 
	 * Returns the length of the task in MI.
	 * 
	 * @return the length of the task in MI
	 */
	public double getTaskLength() {
		return taskLength;
	}

	/**
	 * 
	 * Sets the length of the task in MI. The distribution of the task length is
	 * replaced by a constant one.
	 * 
	 * @param taskLength the length of the task in MI
	 */
	public void setTaskLength(double taskLength) {
		this.taskLength = taskLength;
		this.taskLengthDistribution = new ConstantDistribution(taskLength);
	}

	/**
	 * 
	 * Returns the size of the results in bits.
	 * 
	 * @return the size of the results in bits
	 */
	public long getResultsSize() {
		return resultsSize;
	}

	/**
	 * 
	 * Sets the size of the results in bits. The distribution of the results size is
	 * replaced by a constant one.
	 * 
	 * @param resultsSize the size of the results in bits
	 */
	public void setResultsSize(long resultsSize) {
		this.resultsSize = resultsSize;
		this.resultsSizeDistribution = new ConstantDistribution(resultsSize);
	}

	/**
	 * 
	 * Returns the usage percentage of the application.
	 * 
	 * @return the usage percentage of the application
	 */
	public double getUsagePercentage() {
		return usagePercentage;
	}

	/**
	 * 
	 * Sets the usage percentage of the application.
	 * 
	 * @param usagePercentage the usage percentage of the application
	 */
	public void setUsagePercentage(double usagePercentage) {
		this.usagePercentage = usagePercentage;
	}

	/**
	 * 
	 * Returns the latency of the application in seconds.
	 * 
	 * @return the latency of the application in seconds
	 */
	public double getLatency() {
		return latency;
	}

	/**
	 * 
	 * Sets the latency of the application in seconds. The distribution of the
	 * latency is replaced by a constant one.
	 * 
	 * @param latency the latency of the application in seconds
	 */
	public void setLatency(double latency) {
		this.latency = latency;
		this.latencyDistribution = new ConstantDistribution(latency);
	}

	/**
	 * 
	 * Returns the type of the application.
	 * 
	 * @return the type of the application
	 */
	public String getType() {
		return type;
	}

	/**
	 * 
	 * Sets the type of the application.
	 * 
	 * @param type the type of the application
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 
	 * Returns the size of the container in megabytes.
	 * 
	 * @return the size of the container in megabytes
	 */
	public double getContainerSizeInMBytes() {
		return containerSize / BITS_IN_MB;
	}

	/**
	 * Returns a string representation of the Application object.
	 * 
	 * @return a string representation of the Application object
	 */
	@Override
	public String toString() {
		return "Application [type=" + type + ", rate=" + rateDistribution + ", latency=" + latencyDistribution
				+ ", containerSize=" + containerSizeDistribution + ", requestSize=" + requestSizeDistribution
				+ ", resultsSize=" + resultsSizeDistribution + ", taskLength=" + taskLengthDistribution
				+ ", usagePercentage=" + usagePercentage + ", arrival=" + arrivalDistribution + "]";
	}

	/**
	 * Draws the size of the next request.
	 * 
	 * @return the request size in bits, at least one bit
	 */
	public long sampleRequestSize() {
		return Math.max(1, (long) requestSizeDistribution.sample());
	}

	/**
	 * Draws the size of the next results message.
	 * 
	 * @return the results size in bits, at least one bit
	 */
	public long sampleResultsSize() {
		return Math.max(1, (long) resultsSizeDistribution.sample());
	}

	/**
	 * Draws the size of the container used for the next task.
	 * 
	 * @return the container size in bits, at least one bit
	 */
	public long sampleContainerSize() {
		return Math.max(1, (long) containerSizeDistribution.sample());
	}

	/**
	 * Draws the length of the next task.
	 * 
	 * @return the task length in MI, at least one MI
	 */
	public long sampleTaskLength() {
		return Math.max(1, (long) taskLengthDistribution.sample());
	}

	/**
	 * Draws the latency requirement of the next task.
	 * 
	 * @return the maximum accepted latency, in seconds
	 */
	public double sampleLatency() {
		return Math.max(0.001, latencyDistribution.sample());
	}

	/**
	 * Draws the number of tasks generated by one device during one minute.
	 * 
	 * @return the number of tasks, at least zero
	 */
	public int sampleTasksPerMinute() {
		return Math.max(0, rateDistribution.sampleInt());
	}

	/**
	 * Returns the distribution of the time between two consecutive tasks.
	 * 
	 * @return the arrival distribution, or {@code null} when the tasks are evenly
	 *         spaced inside each minute
	 */
	public Distribution getArrivalDistribution() {
		return arrivalDistribution;
	}

	/**
	 * Returns whether the tasks of this application arrive randomly in time.
	 * 
	 * @return {@code true} when an arrival distribution was configured
	 */
	public boolean hasRandomArrival() {
		return arrivalDistribution != null;
	}

	/**
	 * Returns the distribution of the request size.
	 * 
	 * @return the request size distribution, in bits
	 */
	public Distribution getRequestSizeDistribution() {
		return requestSizeDistribution;
	}

	/**
	 * Returns the distribution of the results size.
	 * 
	 * @return the results size distribution, in bits
	 */
	public Distribution getResultsSizeDistribution() {
		return resultsSizeDistribution;
	}

	/**
	 * Returns the distribution of the container size.
	 * 
	 * @return the container size distribution, in bits
	 */
	public Distribution getContainerSizeDistribution() {
		return containerSizeDistribution;
	}

	/**
	 * Returns the distribution of the task length.
	 * 
	 * @return the task length distribution, in MI
	 */
	public Distribution getTaskLengthDistribution() {
		return taskLengthDistribution;
	}

	/**
	 * Returns the distribution of the latency requirement.
	 * 
	 * @return the latency distribution, in seconds
	 */
	public Distribution getLatencyDistribution() {
		return latencyDistribution;
	}

	/**
	 * Returns the distribution of the number of tasks generated each minute.
	 * 
	 * @return the rate distribution
	 */
	public Distribution getRateDistribution() {
		return rateDistribution;
	}

}
