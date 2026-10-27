package com.mechalikh.pureedgesim.taskgenerator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;

/** Shared fixtures for the task generator tests. */
final class TaskTestSupport {

	private TaskTestSupport() {
	}

	static DefaultSimulationManager newManager() {
		return new DefaultSimulationManager(new SimLog("test", false), new PureEdgeSim(), 0, 0, new Scenario(0, 0, 0));
	}

	static void installGenerator(DefaultSimulationManager manager, ComputingNodesGenerator generator) {
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		manager.setDataCentersManager(dataCentersManager);
	}

	static ComputingNode device(boolean generatingTasks) {
		ComputingNode device = mock(ComputingNode.class);
		when(device.isGeneratingTasks()).thenReturn(generatingTasks);
		return device;
	}
}
