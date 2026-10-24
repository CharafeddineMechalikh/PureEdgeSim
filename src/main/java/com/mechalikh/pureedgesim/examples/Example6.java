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
import com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract.Files;

public class Example6 {
	/**
	 * This example shows how to use other simulation parameters files. The files
	 * used in this example are located in the examples/Example6_settings/ folder.
	 * By default, if the user doesn't specify the simulation settings folder,
	 * PureEdgeSim will use the default ones located in settings/ folder.
	 * 
	 * in the simulation parameters file that is used in this example
	 * ("PureEdgeSim/examples/Example6_settings/"), the simulation time is set to 60
	 * minutes and the update intervals to 0.1 seconds.
	 * 
	 * @author Charafeddine Mechalikh
	 * @since PureEdgeSim 2.4
	 */
	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/Example6_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/Example6_output/";

	// If we want only to use files one by one
	private static String simConfigfile = settingsPath + "simulation_parameters.properties";
	private static String applicationsFile = settingsPath + "applications.xml";
	private static String edgeDataCentersFile = settingsPath + "edge_datacenters.xml";
	private static String edgeDevicesFile = settingsPath + "edge_devices.xml";
	private static String cloudFile = settingsPath + "cloud.xml";

	public Example6() {

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		/** if we want to change the path of all configuration files at once : */

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		/**
		 * if we want to change the path of only one file, while keeping the default one
		 * for the others :
		 */

		// To change the simulation_parameters.properties path only
		sim.setCustomFilePath(simConfigfile, Files.SIMULATION_PARAMETERS);
		// To change the applications.xml path only
		sim.setCustomFilePath(applicationsFile, Files.APPLICATIONS_FILE);
		// To change the edge_datacenters.xml path only
		sim.setCustomFilePath(edgeDataCentersFile, Files.EDGE_DATACENTERS_FILE);
		// To change the edge_devices.xml path only
		sim.setCustomFilePath(edgeDevicesFile, Files.EDGE_DEVICES_FILE);
		// To change the cloud.xml path only
		sim.setCustomFilePath(cloudFile, Files.CLOUD_FILE);

		/**
		 * In addition to showcasing how to set custom file paths, the energy
		 * consumption rate has been increased to show the death of devices on runtime
		 **/

		// Start the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example6();
	}

}
