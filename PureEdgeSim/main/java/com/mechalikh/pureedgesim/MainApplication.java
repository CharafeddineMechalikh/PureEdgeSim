/**
 * PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 * 
 * This file is part of the PureEdgeSim Project.
 * 
 * PureEdgeSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * PureEdgeSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.mechalikh.pureedgesim;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * The MainApplication class is the entry point for launching simulations with
 * default settings. The Simulation class is responsible for managing the
 * simulation process and can be customized by the user.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class MainApplication {

	/**
	 * Entry point for launching simulations with default settings. Creates a new
	 * Simulation object and launches it using the launchSimulation method.
	 * 
	 * @param args The command line arguments, if any.
	 */
	public static void main(final String[] args) {
		new Simulation().launchSimulation();
	}

}
