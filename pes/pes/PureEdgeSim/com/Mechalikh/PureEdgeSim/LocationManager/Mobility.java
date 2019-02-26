package com.Mechalikh.PureEdgeSim.LocationManager;
 
import java.util.List;  

public abstract class Mobility {

protected Location initialLocation;
	public Mobility(Location location) { 
		this.initialLocation= location;
	}
	public abstract List<MobilityItem> generateLocationChanges();
}
