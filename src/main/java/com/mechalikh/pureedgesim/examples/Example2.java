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

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

public class Example2 {
	/**
	 * This is a simple example showing how to launch simulation using a custom
	 * mobility model. by removing it, pureEdgeSim will use the default model.
	 * 
	 * @author Charafeddine Mechalikh
	 * @since PureEdgeSim 2.2
	 */
	public Example2() {
		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		/*
		 * To use your custom mobility model, do this: The custom mobility manager class
		 * can be found in the examples folder as well. by removing this line,
		 * pureEdgeSim will use the default mobility model.
		 */
		sim.setCustomMobilityModel(Example2CustomMobilityModel.class);

		// To use the PureEdgeSim default Mobility Manager you can also uncomment this:
		// setCustomMobilityModel(MobilityManager.class);

		// Start the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example2();
	}

}
