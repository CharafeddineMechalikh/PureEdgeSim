package com.mechalikh.pureedgesim.locationmanager;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;

public class MobilityModelNull extends MobilityModel {

	public Location getCurrentLocation() {
		return new Location(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public Location getNextLocation(Location location) {
		return new Location(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public double distanceTo(ComputingNode device2) {
		return Double.POSITIVE_INFINITY;
	}
}