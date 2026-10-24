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

public class Example7 {

	/** You must read this to understand 
	 * This is a simple example showing how to launch simulation using a custom
	 * network model. The CustomNetworkModel.java is located under the examples/f
	 * older. As you can see, this class extends the MainApplication class provided
	 * by PureEdgeSim, which is required for this example to work.
	 * 
	 * In this example, we will implement the cooperative caching algorithm
	 * presented in the following paper: Mechalikh, C., Taktak, H., Moussa, F.:
	 * Towards a Scalable and QoS-Aware Load Balancing Platform for Edge Computing
	 * Environments. The 2019 International Conference on High Performance Computing
	 * & Simulation (2019) 684-691
	 *
	 * Before running this example you need to
	 * 
	 * 1/ enable the registry in the simulation parameters file by setting
	 * enable_registry=true registry_mode=CACHE
	 * 
	 * 2/ enable orchestrators in the simulation parameters file by setting
	 * enable_orchestrators=true deploy_orchestrator=CLUSTER
	 * 
	 * you can then compare between registry_mode=CLOUD in which the containers are
	 * downloaded from the cloud everytime and registry_mode=CACHE in which the
	 * frequently needed containers are cached in edge devices. Same for
	 * deploy_orchestrator=CLUSTER and deploy_orchestrator=CLOUD. where the
	 * orchestrators are deployed on the cluster heads or on the cloud.
	 * 
	 * Try to use the MIST_ONLY architecture, in order to see clearly the difference
	 * in WAN usage (no tasks offloading to the cloud, so the wan will only be used
	 * by containers). To see the effect, try with 60 minutes simulation time.
	 * 
	 * You will see that the cooperative caching algorithm decreases the WAN usage
	 * remarkably.
	 * 
	 * @author Charafeddine Mechalikh
	 * @since PureEdgeSim 2.3
	 */

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/Example7_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/Example7_output/";

	public Example7() {
		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		/**
		 * Before implementing the cooperative caching algorithm (which will require a
		 * custom network model) we need to implement a clustering algorithm in order to
		 * group edge devices in clusters. The clustering algorithm is implemented in
		 * the CustomEdgeDevice.java. We extended the DefaultEdgeDataCenter class in
		 * this case.To use it we need to execute the following line.
		 **/

		sim.setCustomComputingNode(Example7CachingDevice.class);

		/**
		 * After adding the clustering algorithm we can now implement the cooperative
		 * caching algorithm in the CustomNetworkModel class. This custom class can be
		 * used using the following line. However, in this example instead of extending
		 * the NetworkModel, we extended the DefaultNetworkModel, because we only want
		 * to add the cooperative caching algorithm and the DefaultNetworkModel is
		 * realistic enough, so need to change it with another one.
		 **/

		sim.setCustomNetworkModel(Example7CustomNetworkModel.class);

		/**
		 * To use the PureEdgeSim default network model you can also uncomment this:
		 **/
		// setCustomNetworkModel(DefaultNetworkModel.class);

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		/** if we want to change the path of all configuration files at once : */

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		// Start the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example7();

	}

}
