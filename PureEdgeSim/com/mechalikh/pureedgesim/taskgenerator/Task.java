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
 **/
package com.mechalikh.pureedgesim.taskgenerator;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationengine.QueueElement;

public interface Task extends QueueElement {


    enum FailureReason {
		FAILED_DUE_TO_LATENCY, FAILED_BECAUSE_DEVICE_DEAD, FAILED_DUE_TO_DEVICE_MOBILITY,
		NOT_GENERATED_BECAUSE_DEVICE_DEAD, NO_OFFLOADING_DESTINATIONS, INSUFFICIENT_RESOURCES, INSUFFICIENT_POWER
	}

	enum Status {
		SUCCESS, FAILED
	}
	
	double getMaxLatency();

	void setMaxLatency(double maxLatency);

	double getActualNetworkTime();

	void addActualNetworkTime(double actualNetworkTime);

	double getActualCpuTime();

	double getExecStartTime();

	double getWatingTime();

	void setArrivalTime(double clock);

	void setExecutionStartTime(double clock);

	void setExecutionFinishTime(double clock);

	void setId(int id);

	int getId();

	void setTime(double time);

	double getTime();

	ComputingNode getEdgeDevice();

	void setEdgeDevice(ComputingNode device);

	void setContainerSizeInBits(long containerSize);

	long getContainerSizeInBits();

	double getContainerSizeInMBytes();

	ComputingNode getOrchestrator();

	ComputingNode getRegistry();

	void setRegistry(ComputingNode registry);

	int getApplicationID();

	void setApplicationID(int applicationID);

	FailureReason getFailureReason();

	void setFailureReason(FailureReason reason);

	ComputingNode getOffloadingDestination();

	void setOffloadingDestination(ComputingNode applicationPlacementLocation);

	Task setFileSizeInBits(long requestSize);

	Task setOutputSizeInBits(long outputSize);

	double getLength();

	double getFileSizeInBits();

	double getOutputSizeInBits();

	void setStatus(Status status);

	Status getStatus();

	String getType();

	void setType(String type);

	void setLength(double length);

	void setOrchestrator(ComputingNode orchestrator);

	double getTotalDelay();
	
	void setSerial(long l);

	long getSerial();

}
