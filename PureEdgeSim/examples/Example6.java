package examples;

import com.mechalikh.pureedgesim.MainApplication;

public class Example6 extends MainApplication {
	/**
	 * This is a simple example showing how to launch simulation using a custom
	 * network model. The CustomNetworkModel.java is located under the
	 * examples/CustomNetworkModel/ folder. As you can see, this class extends the
	 * MainApplication class provided by PureEdgeSim, which is required for this
	 * example to work.
	 * 
	 * In this example, we will implement the cooperative caching algorithm
	 * presented in the following paper: Mechalikh, C., Taktak, H., Moussa, F.:
	 * Towards a Scalable and QoS-Aware Load Balancing Platform for Edge Computing
	 * Environments. The 2019 International Conference on High Performance Computing
	 * & Simulation (2019) 684-691
	 */

	/**
	 * Before running this example you need to 1/ enable the registry in the
	 * simualtion parameters file and set enable_registry=false registry_mode=CACHE
	 * 
	 * 2/ enable orchestrators in the simualtion parameters file and set
	 * enable_orchestrators=true deploy_orchestrator=CLUSTER
	 * 
	 * you can than compare between registry_mode=CLOUD in which the containers are
	 * downloaded from the cloud everytime and registry_mode=CACHE in which the
	 * frequently needed containers are cached in edge devices. Same for
	 * deploy_orchestrator=CLUSTER and deploy_orchestrator=CLOUD. where the
	 * orchestrators are deployed on the cluster heads or on the cloud
	 */

	public Example6(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		/*
		 * Before implementing the cooperative caching algorithm (which will require a
		 * custom network model) we need to implement a clustering algorithm in order to
		 * group edge devices in clusters. The clustering algorithm is implemented in the
		 * CustomEdgeDevice.java. We extended the DefaultEdgeDataCenter class in this
		 * case.To use it we need to execute the following line.
		 */

		setCustomEdgeDataCenters(CustomEdgeDevice.class);

		/*
		 * After adding the clustering algorithm we can now implement the cooperative
		 * caching algorithm in the CustomNetworkModel class. This custom class can be
		 * used using the following line. However, in this example instead of extending
		 * the NetworkModel, we extended the DefaultNetworkModel, because we only want
		 * to add the cooperative caching algorithm and the DefaultNetworkModel is
		 * realistic enough, so need to change it with another one.
		 */

		setCustomNetworkModel(CustomNetworkModel.class);

		/*
		 * To use the PureEdgeSim default network model you can also uncomment this:
		 */
		// setCustomNetworkModel(DefaultNetworkModel.class);

		// Start the simulation
		launchSimulation();
	}

}
