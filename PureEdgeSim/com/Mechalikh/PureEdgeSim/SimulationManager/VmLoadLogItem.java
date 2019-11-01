package com.mechalikh.pureedgesim.SimulationManager;

class VmLoadLogItem {
	private double time;
	private double vmLoad;
	private int vmId;

	VmLoadLogItem(double time, double vmLoad, int vmId) {
		this.time = time;
		this.vmLoad = vmLoad;
		this.vmId = vmId;
	}

	public double getLoad() {
		return vmLoad;
	}

	public int getVmId() {
		return vmId;
	}

	public String toString() {
		return time + " is " + vmLoad;
	}
}
