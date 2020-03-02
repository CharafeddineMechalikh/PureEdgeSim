package examples;

import com.mechalikh.pureedgesim.MainApplication;

public class Example5 extends MainApplication {
	/**
	 * This is a simple example showing how to launch simulation using a custom task
	 * orchesrator. The CustomEdgeOrchestrator.java is located under the examples/
	 * folder. As you can see, this class extends the Main class provided by
	 * PureEdgeSim, which is required for this example to work.
	 */
	public Example5(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		/*
		 * To use your custom Edge orchestrator class, do this: The custom orchestrator
		 * class can be found in the examples folder. by removing this line, pureEdgeSim
		 * will use the default orchestrator class.
		 */
		setCustomEdgeOrchestrator(CustomEdgeOrchestrator.class);

		/*
		 * This custom class uses another orchestrator algorithm called
		 * Increase_Lifetime, that avoids offloading the tasks to battery-powered
		 * devices. This algorithm wotks better when you use the ALL architecture you
		 * can compare its performance to the Round-Robin and Trade-off algorithms used
		 * by the default orchestrator class, as this algorihtm relies more on the cloud
		 * and the edge data centers (cloud and edge computing). You can use your own
		 * algorithm by adding it to your custom class. After adding it to the
		 * orchestrator class,to use it you need to add it to the simulation parameters
		 * file (under the settings/ folder). To use the PureEdgeSim default edge
		 * orchestrator class you can also uncomment this:
		 */
		// setCustomEdgeOrchestrator(DefaultEdgeOrchestrator.class);

		// Start the simulation
		launchSimulation();
	}

}
