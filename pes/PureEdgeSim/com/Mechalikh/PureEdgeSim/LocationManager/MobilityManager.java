package com.Mechalikh.PureEdgeSim.LocationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.hosts.Host;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;

public class MobilityManager extends Mobility{
	public MobilityManager(List<Host> fogHostsList) { 
		//must check if the device is mobile before calling the constructor
		super(fogHostsList);
	} 
@Override
	public List<MobilityItem> generateLocationChanges() {
		List<MobilityItem> locations=new ArrayList<MobilityItem>();
		if(SimulationParameters.MAX_TIME_FOR_CHANGING_LOCATION>0) {
			int interval=SimulationParameters.MAX_TIME_FOR_CHANGING_LOCATION-SimulationParameters.MIN_TIME_FOR_CHANGING_LOCATION;
			if (interval<0) interval=interval*-1; // if them min was > max, invert values
			int time=SimulationParameters.MIN_TIME_FOR_CHANGING_LOCATION+ new Random().nextInt(interval);
			while(time<SimulationParameters.SIMULATION_TIME) {
				int position=new Random().nextInt(fogHostsList.size());
				locations.add(new MobilityItem(time,((EdgeDataCenter)(fogHostsList.get(position)).getDatacenter()).getLocation(),fogHostsList.get(position)));
				time+=SimulationParameters.MIN_TIME_FOR_CHANGING_LOCATION+ new Random().nextInt(interval);
			}
		}
		return locations;
		
	}
}
