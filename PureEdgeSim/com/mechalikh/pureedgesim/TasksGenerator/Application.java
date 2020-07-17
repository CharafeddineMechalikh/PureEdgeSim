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
 *     @author Mechalikh
 **/
package com.mechalikh.pureedgesim.TasksGenerator;

public class Application {
	private int id;
	private int rate;
	private double latency;
	private long containerSize;
	private long requestSize;
	private long resultsSize;
	private double taskLength;
	private int numberOfCores;

	public Application(int id, int rate, double latency, long containerSize, long requestSize, long resultsSize,
			double taskLength, int numberOfCores) {
		this.setId(id);
		this.rate = rate;
		this.latency = latency;
		this.containerSize = containerSize;
		this.requestSize = requestSize;
		this.resultsSize = resultsSize;
		this.taskLength = taskLength;
		this.numberOfCores = numberOfCores;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

	public double getLatency() {
		return latency;
	}

	public void setLatency(double latency) {
		this.latency = latency;
	}

	public long getContainerSize() {
		return containerSize;
	}

	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
	}

	public long getRequestSize() {
		return requestSize;
	}

	public void setRequestSize(long requestSize) {
		this.requestSize = requestSize;
	}

	public double getTaskLength() {
		return taskLength;
	}

	public void setTaskLength(double taskLength) {
		this.taskLength = taskLength;
	}

	public long getResultsSize() {
		return resultsSize;
	}

	public void setResultsSize(long resultsSize) {
		this.resultsSize = resultsSize;
	}

	public int getNumberOfCores() {
		return numberOfCores;
	}

	public void setNumberOfCores(int numberOfCores) {
		this.numberOfCores = numberOfCores;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
