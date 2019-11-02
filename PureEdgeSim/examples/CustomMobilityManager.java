package examples;

import com.mechalikh.pureedgesim.LocationManager.Location;
import com.mechalikh.pureedgesim.LocationManager.Mobility;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;

/* A simple custom mobility model that increase the X and Y coordinates by 1 (meter) every time.
 * This model uses the current Location of the device (from the Mobility.class) in order to calculate the next position
 * this model is over simplified , it doesn't use any speed or whatever, it is just used to give an example of how it works
 * you can create amore realistic model, or use the "MobilityManager" model provided by pureEdgeSim.  
 */

public class CustomMobilityManager extends Mobility {

	public CustomMobilityManager(Location location) {
		super(location);
	}

	public Location getNextLocation() {
		// add 1 to the previous X_position
		Double x_position = this.currentLocation.getXPos() + 1;
		// add 1 to the previous Y_osition
		Double y_position = this.currentLocation.getYPos() + 1;

		// if x position is bigger then the simulation area length, start from 0
		if (x_position > simulationParameters.AREA_LENGTH)
			x_position = x_position % simulationParameters.AREA_LENGTH;

		// if y position is bigger then the simulation area length, start from 0
		if (y_position > simulationParameters.AREA_WIDTH)
			y_position = y_position % simulationParameters.AREA_WIDTH;

		// update the location 
		currentLocation = new Location(x_position, y_position);
		
		// return the new location
		return currentLocation;
	}
}
