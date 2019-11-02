package examples;

import com.mechalikh.pureedgesim.MainApplication;
import com.mechalikh.pureedgesim.DataCentersManager.EdgeDataCenter;
import com.mechalikh.pureedgesim.DataCentersManager.DefaultEnergyModel;
import com.mechalikh.pureedgesim.LocationManager.MobilityManager;
import com.mechalikh.pureedgesim.TasksGenerator.DefaultTasksGenerator;
import com.mechalikh.pureedgesim.TasksOrchestration.EdgeOrchestrator;

public class example1 extends MainApplication {
	/*
	 * This is a simple example showing how to launch simulation using custom
	 * mobility model, energy model, custom edge orchestrator, custom tasks
	 * generator, and custom edge devices. 
	 * By removing them, you will use the
	 * default models provided by ¨PureEdgeSim. As you can see, this class extends
	 * the Main class provided by PureEdgeSim, which is required for this example to
	 * work.
	 */
	public example1(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		setCustomMobilityModel(MobilityManager.class);
		setCustomEdgeOrchestrator(EdgeOrchestrator.class);
		setCustomTasksGenerator(DefaultTasksGenerator.class);
		setCustomEdgeDataCenters(EdgeDataCenter.class);
		setCustomEnergyModel(DefaultEnergyModel.class);
		launchSimulation();
	}

}
