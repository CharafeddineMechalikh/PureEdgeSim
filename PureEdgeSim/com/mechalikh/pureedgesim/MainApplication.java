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
package com.mechalikh.pureedgesim;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * The main class of the PureEdgeSim that launches simulations with default settings.
 * The user can follow the provided examples to see how he or she can integrate customs models.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class MainApplication {

	public static void main(String[] args) {
		
		// Launch a simulation with default settings
		new Simulation().launchSimulation();
	}

}