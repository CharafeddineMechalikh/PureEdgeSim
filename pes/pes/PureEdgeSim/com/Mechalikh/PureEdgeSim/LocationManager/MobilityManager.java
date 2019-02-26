package com.Mechalikh.PureEdgeSim.LocationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random; 
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;

public class MobilityManager extends Mobility{
	public MobilityManager(Location location) { 
		//must check if the device is mobile before calling the constructor
		super(location);
	} 
@Override
	public List<MobilityItem> generateLocationChanges() {
	int X_position= this.initialLocation.getXPos();
	int Y_position=this.initialLocation.getYPos();
		List<MobilityItem> locations=new ArrayList<MobilityItem>();
		if(SimulationParameters.MAX_TIME_FOR_CHANGING_LOCATION>0) {
			int interval=SimulationParameters.MAX_TIME_FOR_CHANGING_LOCATION-SimulationParameters.MIN_TIME_FOR_CHANGING_LOCATION;
			if (interval<0) interval=interval*-1; // if them min was > max, invert values
			int time=SimulationParameters.MIN_TIME_FOR_CHANGING_LOCATION+ new Random().nextInt(interval);
			while(time<SimulationParameters.SIMULATION_TIME) {
				 X_position+= 1-new Random().nextInt(2);//decrement or increment x position
				 X_position= X_position % SimulationParameters.AREA_LENGTH; // ensure that the device stay in the simulation area
				 
				 Y_position+= 1-new Random().nextInt(2);// decrement or increment y position
				 Y_position= Y_position % SimulationParameters.AREA_WIDTH;
               
				locations.add(new MobilityItem(time,new Location(X_position, Y_position),null));
				time+=SimulationParameters.MIN_TIME_FOR_CHANGING_LOCATION+ new Random().nextInt(interval);
			}
		}
		return locations;
		
	}
}
