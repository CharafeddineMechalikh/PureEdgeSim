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

/**
 * In this example we show how to implement a Fuzzy Logic based orchestration
 * algorithm, we tried to implement this algorithm but with a little
 * modification in order to support mist computing (computing at the extreme
 * edge). The algorithm can be found in this paper here:
 * 
 * C. Sonmez, A. Ozgovde and C. Ersoy, "Fuzzy Workload Orchestration for Edge
 * Computing," in IEEE Transactions on Network and Service Management, vol. 16,
 * no. 2, pp. 769-782, June 2019.
 * 
 * We also started with stage 2 and then stage 1, as this decreases the
 * algorithm complexity. Hence, shorter simulation time.
 * 
 * To use it you must add JFuzzy_Logic jar file PureEdgeSim/Libs/ folder
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 3.0
 */
public class Example8 {

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/Example8_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/Example8_output/";

	public static void main(String[] args) {

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		/** if we want to change the path of all configuration files at once : */

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		// telling PureEdgeSim to use the custom orchestrator class
		sim.setCustomEdgeOrchestrator(Example8FuzzyLogicOrchestrator.class);

		// launching the simulation
		sim.launchSimulation();
	}

}
