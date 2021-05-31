/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Mechalikh
 **/
package examples;

import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

/* A simple custom mobility model that increase the X and Y coordinates by 1 (meter) every time.
 * This model uses the current Location of the device (from the Mobility.class) in order to calculate the next position
 * this model is over simplified , it doesn't use any speed or whatever, it is just used to give an example of how it works
 * you can create amore realistic model, or use the "MobilityManager" model provided by pureEdgeSim.  
 */

public class CustomMobilityManager extends MobilityModel {

	public CustomMobilityManager(Location currentLocation, boolean mobile, double speed, double minPauseDuration,
			double maxPauseDuration, double minMobilityDuration, double maxMobilityDuration) {
		super(currentLocation, mobile, speed, minPauseDuration, maxPauseDuration, minMobilityDuration,
				maxMobilityDuration);
	}
	
	public Location getNextLocation() {
		// add 1 to the previous X_position
		Double x_position = this.currentLocation.getXPos() + 1;
		// add 1 to the previous Y_osition
		Double y_position = this.currentLocation.getYPos() + 1;

		// if x position is bigger then the simulation area length, start from 0
		if (x_position > SimulationParameters.AREA_LENGTH)
			x_position = x_position % SimulationParameters.AREA_LENGTH;

		// if y position is bigger then the simulation area length, start from 0
		if (y_position > SimulationParameters.AREA_WIDTH)
			y_position = y_position % SimulationParameters.AREA_WIDTH;

		// update the location 
		currentLocation = new Location(x_position, y_position);
		
		// return the new location
		return currentLocation;
	}
}
