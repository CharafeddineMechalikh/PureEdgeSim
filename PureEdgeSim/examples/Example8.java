package examples;

import com.mechalikh.pureedgesim.MainApplication; 

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
 * To use it you must add JFuzzy_Logic jar file  PureEdgeSim/Libs/ folder
 */
public class Example8 extends MainApplication {

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/Example8_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/Example8_output/";

	public Example8(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		// changing the default output folder
		setCustomOutputFolder(outputPath);

		/** if we want to change the path of all configuration files at once : */

		// changing the simulation settings folder
		setCustomSettingsFolder(settingsPath);
		
		// telling PureEdgeSim to use the custom orchestrator class
		Example8.setCustomEdgeOrchestrator(FuzzyLogicOrchestrator.class);
		
		//launching the simulation
		Example8.launchSimulation();
	}

}
