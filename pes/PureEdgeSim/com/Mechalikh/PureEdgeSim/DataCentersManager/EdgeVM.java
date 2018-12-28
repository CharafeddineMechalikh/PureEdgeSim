package com.Mechalikh.PureEdgeSim.DataCentersManager;

import org.cloudbus.cloudsim.vms.VmSimple;

import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;

public class EdgeVM extends VmSimple {
 public EdgeVM(int id, double mipsCapacity, long numberOfPes) {
		super(id, mipsCapacity, numberOfPes);
		// TODO Auto-generated constructor stub
	}
private SimulationParameters.TYPES type;
	 
	public SimulationParameters.TYPES getType() {
		return type;
	}
	public void setType(SimulationParameters.TYPES type) {
		this.type = type;
	}
}
