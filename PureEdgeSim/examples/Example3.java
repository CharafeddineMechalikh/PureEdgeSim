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

public class Example3 {
	/**
	 * This example we propose a simulation scenario where a Edge data
	 * centers/servers work cooperatively.
	 * 
	 * To do this, first, we defined the Edge data centers characteristics in the
	 * edge_datacenters.xml file (in PureEdgeSim\examples\Example3_settings\)
	 * 
	 * Then, in the simulation_parameters.properties file, we used the
	 * Edge_and_Cloud architecture, which means the tasks will be offloaded to the
	 * Edge data centers or to the Cloud. Edge data centers are linked, the topology
	 * is given in edge_datacenters.xml file.
	 * 
	 * We also enabled the deployment of orchestrators on physical devices by
	 * setting: enable_orchestrators=true
	 * 
	 * An orchestrator will be deployed on each Edge dat cetner:
	 * deploy_orchestrator=EDGE
	 * 
	 * By doing so, the tasks will be sent to the nearest orchestrator (which is the
	 * nearest edge data center). This orchestrator will decide whether to offlaod the
	 * tasks to the cloud , to another edge data center, or to execute them locally,
	 * depending on the orchestration algorithm
	 * 
	 * @author Charafeddine Mechalikh
	 * @since PureEdgeSim 3.1
	 */
	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/Example3_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/Example3_output/";

	public Example3() {
		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		/** if we want to change the path of all configuration files at once : */

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		// Start the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example3();
	}

}
