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

/**
 * This class represents an application that can be executed on a computing
 * node.
 * 
 * @author Charafeddine Mechalikh
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

	/**
	 * The number of bits in one megabyte.
	 */
	private static final double BITS_IN_MB = 8000000.0;

	/**
	 * Constructs a new Application object.
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
		setType(type);
		setRate(rate);
		setUsagePercentage(usagePercentage);
		setLatency(latency);
		setContainerSize(containerSize);
		setRequestSize(requestSize);
		setResultsSize(resultsSize);
		setTaskLength(taskLength);
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
	 * Sets the rate at which requests are generated for this application.
	 *
	 * @param rate the rate at which requests are generated for this application
	 */
	public void setRate(int rate) {
		this.rate = rate;
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
	 * Sets the size of the container in bits.
	 * 
	 * @param containerSize the size of the container in bits
	 */
	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
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
	 * Sets the size of the request in bits.
	 * 
	 * @param requestSize the size of the request in bits
	 */
	public void setRequestSize(long requestSize) {
		this.requestSize = requestSize;
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
	 * Sets the length of the task in MI.
	 * 
	 * @param taskLength the length of the task in MI
	 */
	public void setTaskLength(double taskLength) {
		this.taskLength = taskLength;
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
	 * Sets the size of the results in bits.
	 * 
	 * @param resultsSize the size of the results in bits
	 */
	public void setResultsSize(long resultsSize) {
		this.resultsSize = resultsSize;
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
	 * Sets the latency of the application in seconds.
	 * 
	 * @param latency the latency of the application in seconds
	 */
	public void setLatency(double latency) {
		this.latency = latency;
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
	 * 
	 * Returns a string representation of the Application object.
	 * 
	 * @return a string representation of the Application object
	 */
	@Override
	public String toString() {
		return "Application [type=" + type + ", rate=" + rate + ", latency=" + latency + ", containerSize="
				+ containerSize + ", requestSize=" + requestSize + ", resultsSize=" + resultsSize + ", taskLength="
				+ taskLength + ", usagePercentage=" + usagePercentage + "]";
	}

}
