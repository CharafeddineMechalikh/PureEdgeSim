package com.mechalikh.pureedgesim.LocationManager;

public abstract class Mobility {

	protected Location currentLocation;

	public Mobility(Location location) {
		this.currentLocation = location;
	}

	public Mobility() { 
	}

	public abstract Location getNextLocation();

	public Location getCurrentLocation() {
		return currentLocation;
	}
}
