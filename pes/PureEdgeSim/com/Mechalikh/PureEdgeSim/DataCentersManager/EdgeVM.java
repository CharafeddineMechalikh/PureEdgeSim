package com.Mechalikh.PureEdgeSim.DataCentersManager;

import org.cloudbus.cloudsim.vms.VmSimple;

import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;

public class EdgeVM extends VmSimple {
	
 public EdgeVM(int id, double mipsCapacity, long numberOfPes) {
		super(id, mipsCapacity, numberOfPes); 
	} 
	// Type of device / server( edge device, fog server, or cloud) 
	public SimulationParameters.TYPES getType() {
		return ((EdgeDataCenter)this.getHost().getDatacenter()).getType();
	} 
	
	public void setType(SimulationParameters.TYPES type) {
		((EdgeDataCenter)this.getHost().getDatacenter()).setType(type);
	}
}
