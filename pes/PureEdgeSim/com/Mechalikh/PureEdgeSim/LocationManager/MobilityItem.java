package com.Mechalikh.PureEdgeSim.LocationManager;

import org.cloudbus.cloudsim.hosts.Host;


public class MobilityItem {
private Location location;
private double time;
private Host fogHost;
	public MobilityItem(double time, Location location,Host fogHost) {
		this.time=time;
		this.location=location;
		this.setFogHost(fogHost);
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
	public Host getFogHost() {
		return fogHost;
	}
	public void setFogHost(Host fogHost) {
		this.fogHost = fogHost;
	}

}
