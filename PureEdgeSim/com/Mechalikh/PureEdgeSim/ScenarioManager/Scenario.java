package com.mechalikh.pureedgesim.ScenarioManager;

public class Scenario {
	private int devicesCount; // The number of edge devices in this scenario
	private int orchArchitecture; // The used architecture/ computing paradigms
	private int orchAlgorithm; // The tasks orchestration algorithm that will be used in this scenario

	public Scenario(int devicesCount,  int orchAlgorithm, int orchArchitecture) {
		this.orchAlgorithm = orchAlgorithm;
		this.devicesCount = devicesCount;
		this.orchArchitecture = orchArchitecture; 
	}

	public int getOrchAlgorithm() {
		return orchAlgorithm;
	}

	public void setOrchAlgorithm(int orchAlgorithm) {
		this.orchAlgorithm = orchAlgorithm;
	}

	public int getOrchArchitecture() {
		return orchArchitecture;
	}

	public void setOrchArchitecture(int orchArchitecture) {
		this.orchArchitecture = orchArchitecture;
	}

	public int getDevicesCount() {
		return devicesCount;
	}

	public void setDevicesCount(int devicesCount) {
		this.devicesCount = devicesCount;
	}

	public String getStringOrchArchitecture() {

		return simulationParameters.ORCHESTRATION_ARCHITECTURES[orchArchitecture].trim();
	}

	public String getStringOrchAlgorithm() {

		return simulationParameters.ORCHESTRATION_AlGORITHMS[orchAlgorithm].trim();
	}
	
	public String toString() {
		return getStringOrchAlgorithm()+"-"+getStringOrchArchitecture()+ "-"+getDevicesCount();
	}
	

}
