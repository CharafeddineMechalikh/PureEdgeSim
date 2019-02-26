package com.Mechalikh.PureEdgeSim.TasksGenerator;

import org.cloudbus.cloudsim.cloudlets.CloudletSimple;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter; 

public class Task extends CloudletSimple {
	private Status failureReason= Status.NULL;

	public static enum Status {
		FAILED_DUE_TO_LATENCY, FAILED_BECAUSE_DEVICE_DEAD, FAILED_DUE_TO_DEVICE_MOBILITY, NOT_GENERATED_BECAUSE_DEVICE_DEAD,
		FAILED_NO_RESSOURCES, NULL
	} 
  
	private double time;
	private double maxLatency;
	private double lanDownloadNetworkUsageTime = 0;  
	private double wanDownloadNetworkUsageTime = 0;  
	private EdgeDataCenter dev;
	private double wanUploadNetworkUsageTime=0;
	private double lanUploadNetworkUsageTime=0;
	private long containerSize;
	private EdgeDataCenter orchestrator; 

	public Task(int id, long cloudletLength, long pesNumber) {
		super(id, cloudletLength, pesNumber); 
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getTime() {
		return time;
	}

	public double getMaxLatency() {
		return maxLatency;
	}

	public void setMaxLatency(double maxLatency) {
		this.maxLatency = maxLatency;
	}

	public Status getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(Status status) {
		this.failureReason = status;
	}

	public double getDownloadLanNetworkUsage() {
		return lanDownloadNetworkUsageTime; 
	}

	public void addDownloadLanNetworkUsage(double lanDownloadNetworkUsageTime) {
		this.lanDownloadNetworkUsageTime += lanDownloadNetworkUsageTime;
}
	public double getDownloadWanNetworkUsage() {
		return wanDownloadNetworkUsageTime; 
	}

	public void addDownloadWanNetworkUsage(double wanDownloadNetworkUsageTime) {
		this.wanDownloadNetworkUsageTime += wanDownloadNetworkUsageTime;
		this.lanDownloadNetworkUsageTime += lanDownloadNetworkUsageTime;
}
	
	public double getUploadLanNetworkUsage() {
		return lanUploadNetworkUsageTime; 
	}

	public void addUploadLanNetworkUsage(double lanUploadNetworkUsageTime) {
		this.lanUploadNetworkUsageTime += lanUploadNetworkUsageTime;
}
	public double getUploadWanNetworkUsage() {
		return wanUploadNetworkUsageTime; 
	}

	public void addUploadWanNetworkUsage(double wanUploadNetworkUsageTime) {
		this.wanUploadNetworkUsageTime += wanUploadNetworkUsageTime;
		this.lanUploadNetworkUsageTime+=lanUploadNetworkUsageTime;
}
 
	public EdgeDataCenter getEdgeDevice() {
		return dev;
	}

	public void setEdgeDevice(EdgeDataCenter dev) {
		this.dev = dev;
	}

	public void setContainerSize(long containerSize) {
	  this.containerSize=containerSize;
	}
	public long getContainerSize() {
		  return containerSize;
		}

	public void setOrchestrator(EdgeDataCenter orch) {
		this.orchestrator=orch;
		
	}
	public EdgeDataCenter getOrchestrator() {
		return orchestrator;
		
	}

	 
 
	 
}
