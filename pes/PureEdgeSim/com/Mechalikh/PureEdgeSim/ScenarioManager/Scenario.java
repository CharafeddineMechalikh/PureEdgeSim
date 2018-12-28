package com.Mechalikh.PureEdgeSim.ScenarioManager;
 
 

public class Scenario {
private int devicesCount;
private int orchPolicy;
private int orchCriteria;
	public Scenario(int devicesCount, int orchPolicy, int orchCriteria) {
		this.orchCriteria=orchCriteria;
		this.devicesCount=devicesCount;
		this.orchPolicy=orchPolicy; 
	
	} 
	public int getOrchCriteria() {
		return orchCriteria;
	}
	public void setorchCriteria(int orchCriteria) {
		this.orchCriteria = orchCriteria;
	}
	public int getOrchPolicy() {
		return orchPolicy;
	}
	public void setOrchPolicy(int orchPolicy) {
		this.orchPolicy = orchPolicy;
	}
	public int getDevicesCount() {
		return devicesCount;
	}
	public void setDevicesCount(int devicesCount) {
		this.devicesCount = devicesCount;
	}
	public String getStringOrchPolicy() {
		
		return SimulationParameters.ORCHESTRATOR_POLICIES[orchPolicy].trim();
	}
	public String getStringOrchCriteria() {

		return SimulationParameters.ORCHESTRATOR_CRITERIA[orchCriteria].trim();
	}
	 
}
