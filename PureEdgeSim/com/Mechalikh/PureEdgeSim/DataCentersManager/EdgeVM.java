package com.mechalikh.pureedgesim.DataCentersManager;

import org.cloudbus.cloudsim.vms.VmSimple;

import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;

public class EdgeVM extends VmSimple {
	
 public EdgeVM(int id, double mipsCapacity, long numberOfPes) {
		super(id, mipsCapacity, numberOfPes); 
	} 
	// Type of device / server( edge device, fog server, or cloud) 
	public simulationParameters.TYPES getType() {
		return ((EdgeDataCenter)this.getHost().getDatacenter()).getType();
	} 
	
	public void setType(simulationParameters.TYPES type) {
		((EdgeDataCenter)this.getHost().getDatacenter()).setType(type);
	}
}
