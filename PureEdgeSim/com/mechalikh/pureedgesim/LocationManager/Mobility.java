package com.mechalikh.pureedgesim.LocationManager;

public abstract class Mobility {

	protected Location currentLocation;
	protected boolean isMobile = false;
	
	public Mobility(Location location) {
		this.currentLocation = location;
	}

	public Mobility() { 
	}

	public abstract Location getNextLocation();

	public Location getCurrentLocation() { 
		return currentLocation;
	}
	
	public boolean isMobile() {
		return isMobile;
	}

	public void setMobile(boolean mobile) {
		isMobile = mobile;
	}
}
