package examples;

import com.mechalikh.pureedgesim.MainApplication;

public class example4 extends MainApplication {
	/*
	 * This is a simple example showing how to launch simulation using a custom
	 * energy model. by removing it, pureEdgeSim will use the default models
	 * provided by PureEdgeSim. As you can see, this class extends the Main class
	 * provided by PureEdgeSim, which is required for this example to work
	 */
	public example4(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		// To use your custom Edge datacenters/ devices class, do this:
		// The custom edge data center class can be found in the examples folder as
		// well.
		// by removing this line, pureEdgeSim will use the default datacenters/devices
		// class.
		setCustomEdgeDataCenters(CustomEdgeDataCenter.class);

		// To use the PureEdgeSim default edge data centers class you can also uncomment
		// this:
		// setCustomEdgeDataCenters(DefaultEdgeDataCenter.class);

		// Start the simulation
		launchSimulation();
	}

}
