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
 *     @author Charafeddine Mechalikh
 **/
package examples;

import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters; 
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * A simple custom mobility model that increase the X and Y coordinates by 1
 * (meter) every time. This model uses the current Location of the device (from
 * the Mobility.class) in order to calculate the next position this model is
 * over simplified , it doesn't use any speed or whatever, it is just used to
 * give an example of how it works. you can create a more realistic model, or
 * use the "DefaultMobilityModel" provided by pureEdgeSim.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 2.2
 */

public class Example2CustomMobilityModel extends MobilityModel {

	public Example2CustomMobilityModel(SimulationManager simulationManager, Location currentLocation) {
		super(simulationManager, currentLocation);
	}

	@Override
	protected Location getNextLocation(Location location) {

		// Add 1 to the previous X_position
		Double x_position = location.getXPos() + 1;

		// Add 1 to the previous Y_osition
		Double y_position = location.getYPos() + 1;

		// If x position is bigger then the simulation area length, start from 0.
		if (x_position > SimulationParameters.simulationMapLength)
			x_position = x_position % SimulationParameters.simulationMapLength;

		// If y position is bigger then the simulation area length, start from 0.
		if (y_position > SimulationParameters.simulationMapWidth)
			y_position = y_position % SimulationParameters.simulationMapWidth;

		// Return the new Location.
		return new Location(x_position, y_position);

	}

}
