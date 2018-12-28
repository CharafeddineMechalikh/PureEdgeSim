package com.Mechalikh.PureEdgeSim.LocationManager;
 
import java.util.List; 

import org.cloudbus.cloudsim.hosts.Host;
 

public abstract class Mobility {

protected List<Host> fogHostsList;
	public Mobility(List<Host> fogHostsList) { 
		this.fogHostsList= fogHostsList;
	}
	public abstract List<MobilityItem> generateLocationChanges();
}
