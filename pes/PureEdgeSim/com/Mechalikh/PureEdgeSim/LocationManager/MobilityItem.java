package com.Mechalikh.PureEdgeSim.LocationManager;

public class MobilityItem {
	private Location location;
	private double time;

	public MobilityItem(double time, Location location) {
		this.time = time;
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

}
