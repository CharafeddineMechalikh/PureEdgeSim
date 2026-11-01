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
package com.mechalikh.pureedgesim.network;

/**
 * The raw figures of a communication link: a capacity in bits per second and a
 * latency in seconds per hop.
 * <p>
 * This is the profile of a link, not the link itself. {@link NetworkLink} is a
 * simulation entity that routes transfers, splits its capacity among the
 * transfers it carries and charges them hop by hop; this class only holds what
 * an entity needs to describe the bearer, so a scenario or another simulator
 * can quote a link without instantiating the event engine. The values are the
 * ones a link is advertised with: they do not account for the sharing of the
 * medium, for retransmissions or for protocol overheads.
 *
 * @author Charafeddine Mechalikh
 * @see NetworkLink
 */
public class LinkProfile {

	/** Link capacity, in bits per second. */
	protected double bandwidth = 0;
	/** Link latency, in seconds per hop. */
	protected double latency = 0;

	/**
	 * Returns the capacity of the link.
	 *
	 * @return the capacity, in bits per second.
	 */
	public double getBandwidth() {
		return bandwidth;
	}

	/**
	 * Sets the capacity of the link.
	 *
	 * @param bandwidth the capacity, in bits per second.
	 * @return this profile, for chaining.
	 */
	public LinkProfile setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
		return this;
	}

	/**
	 * Returns the latency of the link.
	 *
	 * @return the latency, in seconds per hop.
	 */
	public double getLatency() {
		return latency;
	}

	/**
	 * Sets the latency of the link.
	 *
	 * @param latency the latency, in seconds per hop.
	 * @return this profile, for chaining.
	 */
	public LinkProfile setLatency(double latency) {
		this.latency = latency;
		return this;
	}

	/**
	 * Returns a one-line description naming the capacity and the latency.
	 *
	 * @return the summary line.
	 */
	@Override
	public String toString() {
		return "LinkProfile[" + bandwidth + " bits/s, " + latency + " s/hop]";
	}
}
