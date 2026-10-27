package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.ApplicationFileParser;
import com.mechalikh.pureedgesim.scenariomanager.DatacentersParser;
import com.mechalikh.pureedgesim.scenariomanager.EdgeDevicesParser;
import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Unit-level test that wires the real models together (parsers, computing
 * nodes, topology, network, task generator and orchestrator) and runs a short
 * simulation without going through Simulation.launchSimulation().
 */
class ModelWiringTest extends AbstractTest {

	private static class ExposedManager extends DefaultSimulationManager {
		ExposedManager(SimLog simLog, PureEdgeSim simulation, int id, int iteration, Scenario scenario) {
			super(simLog, simulation, id, iteration, scenario);
		}

		FutureQueue<Task> taskList() {
			return taskList;
		}
	}

	private void parseTestSettings() {
		assertTrue(new EdgeDevicesParser(SimulationParameters.edgeDevicesFile).parse());
		assertTrue(new DatacentersParser(SimulationParameters.edgeDataCentersFile, TYPES.EDGE_DATACENTER).parse());
		assertTrue(new DatacentersParser(SimulationParameters.cloudDataCentersFile, TYPES.CLOUD).parse());
		useSettings(FAST_SETTINGS);
		assertTrue(new ParametersParser(SimulationParameters.simulationParametersFile).parse());
		assertTrue(new ApplicationFileParser(SimulationParameters.applicationFile).parse());
	}

	@Test
	void realModelsAreWiredAndSimulationRuns() throws Exception {
		parseTestSettings();
		assertEquals(1, SimulationParameters.numberOfEdgeDataCenters);
		assertEquals(1, SimulationParameters.numberOfCloudDataCenters);
		assertFalse(SimulationParameters.applicationList.isEmpty());

		Simulation simulation = new Simulation();
		Scenario scenario = new Scenario(SimulationParameters.minNumberOfEdgeDevices, 0, 0);
		SimLog simLog = new SimLog("wiring-test", true);
		PureEdgeSim engine = new PureEdgeSim();
		ExposedManager manager = new ExposedManager(simLog, engine, 1, 1, scenario);
		simLog.initialize(manager, scenario.getDevicesCount(), 0, 0);

		SimulationThread thread = new SimulationThread(simulation, 0, 1);
		SimLogCapture.mute();
		try {
			thread.loadModels(manager);

			assertNotNull(manager.getDataCentersManager());
			DataCentersManager dataCentersManager = manager.getDataCentersManager();
			assertTrue(dataCentersManager.getComputingNodesGenerator().getMistOnlyList().size() > 0);
			assertTrue(dataCentersManager.getComputingNodesGenerator().getMistOnlyListSensorsExcluded().size() > 0);
			assertEquals(1, dataCentersManager.getComputingNodesGenerator().getEdgeOnlyList().size());
			assertEquals(1, dataCentersManager.getComputingNodesGenerator().getCloudOnlyList().size());
			assertEquals(dataCentersManager.getComputingNodesGenerator().getMistOnlyList().size() + 2,
					dataCentersManager.getComputingNodesGenerator().getAllNodesList().size());
			assertNotNull(dataCentersManager.getTopology());
			assertNotNull(manager.getNetworkModel());

			FutureQueue<Task> tasks = manager.taskList();
			assertNotNull(tasks);
			assertTrue(tasks.size() > 0, "tasks should be generated");

			manager.startSimulation();

			assertTrue(new File(OUTPUT_FOLDER).exists());
			assertTrue(engine.clock() > 0);
			assertFalse(manager.getFinishedTaskList().isEmpty(), "some tasks should finish execution");
		} finally {
			SimLogCapture.unmute();
		}
	}
}
