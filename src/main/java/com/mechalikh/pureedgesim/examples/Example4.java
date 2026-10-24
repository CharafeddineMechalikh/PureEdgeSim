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

public class Example4 {
	/**
	 * This is a simple example showing how to launch simulation using a custom Edge
	 * device/ data center class . by removing it, PureEdgeSim will use the default
	 * one.
	 * 
	 * @author Charafeddine Mechalikh
	 * @since PureEdgeSim 2.2
	 */
	public Example4() {

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		/*
		 * To use your custom class to update the computing nodes classes, do this: The
		 * custom nodes status updater class can be found in the examples folder as
		 * well. by removing this line, PureEdgeSim will use its default class.
		 */
		sim.setCustomComputingNode(Example4CustomComputingNode.class);

		// To use the PureEdgeSim default edge data centers class you can also uncomment
		// this:
		// setCustomEdgeDataCenters(DefaultEdgeDataCenter.class);

		// Start the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example4();
	}

}
