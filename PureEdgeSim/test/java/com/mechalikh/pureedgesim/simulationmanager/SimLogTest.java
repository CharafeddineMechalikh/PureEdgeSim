package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class SimLogTest extends AbstractTest {

	private SimulationManager manager;
	private PureEdgeSim engine;
	private DataCentersManager dataCentersManager;
	private ComputingNodesGenerator generator;
	private InfrastructureGraph topology;

	@BeforeEach
	void setUpCollaborators() {
		SimulationParameters.numberOfCloudDataCenters = 1;
		SimulationParameters.numberOfEdgeDataCenters = 1;

		engine = mock(PureEdgeSim.class);
		when(engine.clock()).thenReturn(12.5);
		manager = mock(SimulationManager.class);
		when(manager.getSimulation()).thenReturn(engine);
		when(manager.getSimulationId()).thenReturn(2);
		when(manager.getScenario()).thenReturn(new Scenario(20, 0, 0));

		dataCentersManager = mock(DataCentersManager.class);
		generator = mock(ComputingNodesGenerator.class);
		topology = mock(InfrastructureGraph.class);
		when(manager.getDataCentersManager()).thenReturn(dataCentersManager);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		when(dataCentersManager.getTopology()).thenReturn(topology);
		when(topology.getWanLinks()).thenReturn(Collections.emptyList());
		when(topology.getManLinks()).thenReturn(Collections.emptyList());
		when(topology.getLanLinks()).thenReturn(Collections.emptyList());
		when(topology.getCellularLinks()).thenReturn(Collections.emptyList());
		when(topology.getEthernetLinks()).thenReturn(Collections.emptyList());
		when(topology.getWifiLinks()).thenReturn(Collections.emptyList());
	}

	private static ComputingNode node(TYPES type, double cpuUtilization, boolean dead, boolean battery,
			double deathTime, double energy) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(type);
		when(node.getAvgCpuUtilization()).thenReturn(cpuUtilization);
		when(node.isDead()).thenReturn(dead);
		when(node.getDeathTime()).thenReturn(deathTime);
		EnergyModelComputingNode energyModel = mock(EnergyModelComputingNode.class);
		when(energyModel.getTotalEnergyConsumption()).thenReturn(energy);
		when(energyModel.isBatteryPowered()).thenReturn(battery);
		when(energyModel.getBatteryLevelWattHour()).thenReturn(3.0);
		when(energyModel.getBatteryLevelPercentage()).thenReturn(50.0);
		when(node.getEnergyModel()).thenReturn(energyModel);
		return node;
	}

	private void wireNodes(List<ComputingNode> cloudNodes, List<ComputingNode> edgeNodes,
			List<ComputingNode> mistNodes) {
		List<ComputingNode> all = new ArrayList<>();
		all.addAll(cloudNodes);
		all.addAll(edgeNodes);
		all.addAll(mistNodes);
		when(generator.getAllNodesList()).thenReturn(all);
		when(generator.getCloudOnlyList()).thenReturn(cloudNodes);
		when(generator.getEdgeOnlyList()).thenReturn(edgeNodes);
		when(generator.getMistOnlyList()).thenReturn(mistNodes);
		when(generator.getMistOnlyListSensorsExcluded()).thenReturn(mistNodes);
	}

	private void wireMixedNodes() {
		ComputingNode cloud = node(TYPES.CLOUD, 5.0, false, false, 0, 10);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 7.0, false, false, 0, 4);
		ComputingNode deadLate = node(TYPES.EDGE_DEVICE, 2.0, true, false, 5, 2);
		ComputingNode deadEarly = node(TYPES.EDGE_DEVICE, 2.0, true, false, 3, 2);
		ComputingNode deadLast = node(TYPES.EDGE_DEVICE, 2.0, true, false, 8, 2);
		ComputingNode aliveBattery = node(TYPES.EDGE_DEVICE, 3.0, false, true, 0, 1);
		ComputingNode aliveNoBattery = node(TYPES.EDGE_DEVICE, 4.0, false, false, 0, 1);
		wireNodes(List.of(cloud), List.of(edge),
				List.of(deadLate, deadEarly, deadLast, aliveBattery, aliveNoBattery));
	}

	private static Task taskWithDestination(ComputingNode destination) {
		Task task = mock(Task.class);
		when(task.getOffloadingDestination()).thenReturn(destination);
		return task;
	}

	private static Task finishedTask(ComputingNode destination) {
		Task task = mock(Task.class);
		when(task.getOffloadingDestination()).thenReturn(destination);
		when(task.getActualCpuTime()).thenReturn(1.5);
		when(task.getWatingTime()).thenReturn(0.5);
		return task;
	}

	@Test
	void showIterationResultsPrintsEverythingAndWritesBothFiles() throws Exception {
		SimulationParameters.saveLog = true;
		SimulationParameters.outputFolder = OUTPUT_FOLDER + "simlog-show";
		deleteRecursively(Path.of(OUTPUT_FOLDER + "simlog-show"));
		wireMixedNodes();

		ComputingNode destination = node(TYPES.CLOUD, 0, false, false, 0, 0);
		Task finished = finishedTask(destination);
		List<Task> finishedTasks = List.of(finished);

		SimLog log = new SimLog("sim-start", true);
		log.initialize(manager, 20, 0, 0);
		assertEquals(1, log.getResultsList().size(), "the csv header should have been added");

		log.setGeneratedTasks(100);
		log.incrementTasksSent();
		log.incrementTasksSent();
		log.getTasksExecutionInfos(finished);
		log.taskSentFromOrchToDest(finished);
		log.incrementTasksFailedLatency(finished);
		log.incrementTasksFailedDependency();

		TransferProgress container = new TransferProgress(finished, 8_000_000, TransferProgress.Type.CONTAINER);
		container.setLanNetworkUsage(2.0);
		container.setManNetworkUsage(1.0);
		container.setWanNetworkUsage(3.0);
		container.setCurrentBandwidth(10_000_000);
		log.updateNetworkUsage(container);

		TransferProgress result = new TransferProgress(finished, 8_000_000, TransferProgress.Type.RESULTS_TO_DEV);
		result.setLanNetworkUsage(0.5);
		result.setCurrentBandwidth(20_000_000);
		log.updateNetworkUsage(result);

		SimLogCapture.mute();
		try {
			log.showIterationResults(finishedTasks);
		} finally {
			SimLogCapture.unmute();
		}

		String base = OUTPUT_FOLDER + "simlog-show/sim-start";
		assertTrue(new File(base + "/Sequential_simulation.csv").exists());
		assertTrue(new File(base + "/Sequential_simulation.txt").exists());
		List<String> csv = Files.readAllLines(Path.of(base + "/Sequential_simulation.csv"));
		assertEquals(2, csv.size(), "one header and one result row");
		assertTrue(csv.get(1).startsWith("ALL,TRADE_OFF,20,"));
		assertTrue(csv.get(0).contains("Tasks not executed because a dependency failed"),
				"the header must describe the dependency failures column");
		assertTrue(csv.get(0).contains("Cellular energy consumption (Wh)"),
				"the cellular column must not name a radio technology");
		assertFalse(csv.get(0).contains("LTE energy consumption"),
				"the radio technology is set by the parameters, not by the column name");
		assertEquals("1", csv.get(1).split(",", -1)[14], "the dependency failures column follows the dead-device one");

		assertEquals(1.5, log.totalExecutionTime, 1e-9);
		assertEquals(0.5, log.totalWaitingTime, 1e-9);
		assertEquals(1, log.executedTasksCount);
		assertEquals(2.5, log.totalLanUsage, 1e-9);
		assertEquals(1.0, log.totalManUsage, 1e-9);
		assertEquals(3.0, log.totalWanUsage, 1e-9);
		assertEquals(2.0, log.containersLanUsage, 1e-9);
		assertEquals(3.0, log.containersWanUsage, 1e-9);
		assertEquals(1.0, log.containersManUsage, 1e-9);
		assertEquals(30.0, log.totalBandwidth, 1e-9);
		assertEquals(2.0, log.totalTraffic, 1e-9);
		assertEquals(2, log.transfersCount);
	}

	@Test
	void saveLogWritesOnlyTheCsvWhenLogSavingIsDisabled() {
		SimulationParameters.saveLog = false;
		SimulationParameters.outputFolder = OUTPUT_FOLDER + "simlog-nosave";

		SimLog log = new SimLog("no-log", true);
		SimLogCapture.mute();
		try {
			log.saveLog();
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(new File(OUTPUT_FOLDER + "simlog-nosave/no-log/Sequential_simulation.csv").exists());
		assertFalse(new File(OUTPUT_FOLDER + "simlog-nosave/no-log/Sequential_simulation.txt").exists());
	}

	@Test
	void getFileNameUsesSequentialOrParallelNames() {
		SimulationParameters.outputFolder = OUTPUT_FOLDER + "simlog-names";
		SimLog sequential = new SimLog("seq", false);
		assertTrue(sequential.getFileName(".csv").endsWith("/Sequential_simulation.csv"));

		SimulationParameters.parallelism_enabled = true;
		SimLog parallel = new SimLog("par", false);
		parallel.initialize(manager, 20, 0, 0);
		assertTrue(parallel.getFileName(".txt").endsWith("/Parallel_simulation_2.txt"));
		assertTrue(new File(OUTPUT_FOLDER + "simlog-names/par").exists());
	}

	@Test
	void printSupportsAllFlagsAndFallsBackToConsoleWithoutAManager() {
		wireMixedNodes();
		SimLog log = new SimLog("print", false);
		log.initialize(manager, 20, 0, 0);

		SimLogCapture.mute();
		try {
			log.print("a formatted %s", "line");
			log.printWithoutTime("no time line");
			log.print(SimLog.SAME_LINE, " appended");
			log.print(99, "ignored flag");

			SimulationParameters.deepLoggingEnabled = true;
			log.deepLog("deep %s", "line");
			log.deepLog(SimLog.NO_TIME, "deep no time");
			SimulationParameters.deepLoggingEnabled = false;
			log.deepLog("disabled deep line");
			log.deepLog(SimLog.NO_TIME, "disabled deep no time");

			log.printSameLine("red text", "red");
			log.printSameLine("blue text", "blue");
			log.printSameLine("plain text");
			SimLog.println("static %s", "line");

			new SimLog("console", false).print("without manager %s", "ok");
			new SimLog("console", false).printWithoutTime("without manager no time");
		} finally {
			SimLogCapture.unmute();
		}

		assertEquals(4, log.log.size());
		assertTrue(log.log.get(0).contains("a formatted line"));
		assertEquals("no time line appended", log.log.get(1));
		assertTrue(log.log.get(2).contains("deep line"));
		assertEquals("deep no time", log.log.get(3));
	}

	@Test
	void cleanOutputFolderDeletesTheTreeOnlyOnTheFirstIteration() throws Exception {
		Path output = Path.of(OUTPUT_FOLDER, "simlog-clean");
		deleteRecursively(output);
		Files.createDirectories(output.resolve("nested"));
		Files.write(output.resolve("nested/file.txt"), new byte[] { 1 });
		SimulationParameters.outputFolder = output.toString();

		SimLog log = new SimLog("clean", true);
		assertTrue(log.isFirstIteration);
		SimLogCapture.mute();
		try {
			log.cleanOutputFolder();
		} finally {
			SimLogCapture.unmute();
		}
		assertFalse(Files.exists(output));
		assertFalse(log.isFirstIteration);

		Files.createDirectories(output);
		SimLogCapture.mute();
		try {
			log.cleanOutputFolder();
		} finally {
			SimLogCapture.unmute();
		}
		assertTrue(Files.exists(output), "the second call must not clean the folder again");

		SimLogCapture.mute();
		try {
			log.deleteDirectory(output.resolve("does-not-exist"));
		} finally {
			SimLogCapture.unmute();
		}
	}

	private static void deleteRecursively(Path path) throws Exception {
		if (!Files.exists(path)) {
			return;
		}
		try (java.util.stream.Stream<Path> walk = Files.walk(path)) {
			List<Path> paths = walk.sorted(Collections.reverseOrder()).collect(java.util.stream.Collectors.toList());
			for (Path entry : paths) {
				Files.delete(entry);
			}
		}
	}

	@Test
	void writeFileWritesAndClearsTheListAndSwallowsIoErrors() throws Exception {
		Path folder = Path.of(OUTPUT_FOLDER, "simlog-write");
		Files.createDirectories(folder);
		Path file = folder.resolve("out.txt");
		Files.deleteIfExists(file);

		SimLog log = new SimLog("write", false);
		List<String> lines = new ArrayList<>(Arrays.asList("one", "two"));
		SimLogCapture.mute();
		try {
			log.writeFile(file.toString(), lines);
		} finally {
			SimLogCapture.unmute();
		}
		assertTrue(lines.isEmpty());
		assertEquals(List.of("one", "two"), Files.readAllLines(file));

		List<String> notWritten = new ArrayList<>(List.of("three"));
		SimLogCapture.mute();
		try {
			log.writeFile(folder.toString(), notWritten);
		} finally {
			SimLogCapture.unmute();
		}
		assertFalse(notWritten.isEmpty(), "the list is only cleared after a successful write");
	}

	@Test
	void incrementMethodsUpdateEveryCounter() {
		SimLog log = new SimLog("counters", false);
		ComputingNode cloud = node(TYPES.CLOUD, 0, false, false, 0, 0);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 0, false, false, 0, 0);
		ComputingNode mist = node(TYPES.EDGE_DEVICE, 0, false, false, 0, 0);
		ComputingNode other = node(TYPES.NULL, 0, false, false, 0, 0);

		log.incrementTasksFailed(mock(Task.class));
		log.incrementTasksFailed(taskWithDestination(cloud));
		log.incrementTasksFailed(taskWithDestination(edge));
		log.incrementTasksFailed(taskWithDestination(mist));
		log.incrementTasksFailed(taskWithDestination(other));
		assertEquals(5, log.tasksFailed);
		assertEquals(1, log.tasksFailedCloud);
		assertEquals(1, log.tasksFailedEdge);
		assertEquals(1, log.tasksFailedMist);

		log.incrementFailedBeacauseDeviceDead(taskWithDestination(cloud));
		assertEquals(1, log.tasksFailedBeacauseDeviceDead);
		assertEquals(6, log.tasksFailed);

		log.incrementNotGeneratedBeacuseDeviceDead();
		assertEquals(1, log.notGeneratedBecDeviceDead);

		log.incrementTasksFailedLatency(taskWithDestination(edge));
		log.incrementTasksFailedMobility(taskWithDestination(mist));
		log.incrementTasksFailedLackOfRessources(taskWithDestination(cloud));
		assertEquals(1, log.tasksFailedLatency);
		assertEquals(1, log.tasksFailedMobility);
		assertEquals(1, log.tasksFailedRessourcesUnavailable);
		assertEquals(9, log.tasksFailed);
	}

	@Test
	void executionInfoAndDestinationCountersAreUpdated() {
		SimLog log = new SimLog("exec", false);
		Task task = finishedTask(node(TYPES.CLOUD, 0, false, false, 0, 0));
		log.getTasksExecutionInfos(task);
		assertEquals(1.5, log.totalExecutionTime, 1e-9);
		assertEquals(0.5, log.totalWaitingTime, 1e-9);
		assertEquals(1, log.executedTasksCount);

		log.taskSentFromOrchToDest(taskWithDestination(node(TYPES.CLOUD, 0, false, false, 0, 0)));
		log.taskSentFromOrchToDest(taskWithDestination(node(TYPES.EDGE_DATACENTER, 0, false, false, 0, 0)));
		log.taskSentFromOrchToDest(taskWithDestination(node(TYPES.EDGE_DEVICE, 0, false, false, 0, 0)));
		log.taskSentFromOrchToDest(taskWithDestination(node(TYPES.NULL, 0, false, false, 0, 0)));
		assertEquals(1, log.tasksExecutedOnCloud);
		assertEquals(1, log.tasksExecutedOnEdge);
		assertEquals(1, log.tasksExecutedOnMist);
	}

	@Test
	void networkUsageAccumulatesContainerAndRegularTransfers() {
		SimLog log = new SimLog("network", false);
		Task task = mock(Task.class);

		TransferProgress container = new TransferProgress(task, 8_000_000, TransferProgress.Type.CONTAINER);
		container.setLanNetworkUsage(1.0);
		container.setManNetworkUsage(2.0);
		container.setWanNetworkUsage(3.0);
		container.setCurrentBandwidth(10_000_000);
		log.updateNetworkUsage(container);

		TransferProgress regular = new TransferProgress(task, 16_000_000, TransferProgress.Type.REQUEST);
		regular.setLanNetworkUsage(0.5);
		regular.setManNetworkUsage(0.25);
		regular.setWanNetworkUsage(0.75);
		regular.setCurrentBandwidth(30_000_000);
		log.updateNetworkUsage(regular);

		assertEquals(1.5, log.totalLanUsage, 1e-9);
		assertEquals(2.25, log.totalManUsage, 1e-9);
		assertEquals(3.75, log.totalWanUsage, 1e-9);
		assertEquals(1.0, log.containersLanUsage, 1e-9);
		assertEquals(2.0, log.containersManUsage, 1e-9);
		assertEquals(3.0, log.containersWanUsage, 1e-9);
		assertEquals(40.0, log.totalBandwidth, 1e-9);
		assertEquals(3.0, log.totalTraffic, 1e-9);
		assertEquals(2, log.transfersCount);
	}

	@Test
	void nodeSelectionAndCpuUtilizationAreComputedFromTheGenerator() {
		wireMixedNodes();
		SimLog log = new SimLog("nodes", false);
		log.initialize(manager, 20, 0, 0);

		assertEquals(1, log.getNodesByType(TYPES.CLOUD).size());
		assertEquals(1, log.getNodesByType(TYPES.EDGE_DATACENTER).size());
		assertEquals(5, log.getNodesByType(TYPES.EDGE_DEVICE).size());
		assertEquals(0, log.getNodesByType(TYPES.NULL).size());
		assertEquals(5.0, log.getCpuUtilization(log.getNodesByType(TYPES.CLOUD)), 1e-9);
	}

	@Test
	void networkResultsCoverTheZeroContainerUsageBranches() {
		wireMixedNodes();
		ComputingNode cloud = node(TYPES.CLOUD, 0, false, false, 0, 0);
		Task task = finishedTask(cloud);
		SimLog log = new SimLog("zero-network", true);
		log.initialize(manager, 20, 0, 0);
		log.setGeneratedTasks(10);
		log.incrementTasksSent();
		log.getTasksExecutionInfos(task);
		log.totalLanUsage = 1.0;
		log.transfersCount = 1;

		SimLogCapture.mute();
		try {
			log.printTasksRelatedResults();
			log.printNetworkRelatedResults();
		} finally {
			SimLogCapture.unmute();
		}

		assertEquals(2, log.getResultsList().size());
	}

	@Test
	void powerResultsHandleAliveDevicesWithoutBatteriesAndNoDeaths() {
		ComputingNode cloud = node(TYPES.CLOUD, 0, false, false, 0, 10);
		ComputingNode edge = node(TYPES.EDGE_DATACENTER, 0, false, false, 0, 4);
		ComputingNode aliveNoBattery = node(TYPES.EDGE_DEVICE, 0, false, false, 0, 1);
		wireNodes(List.of(cloud), List.of(edge), List.of(aliveNoBattery));

		SimLog log = new SimLog("power", true);
		log.initialize(manager, 20, 0, 0);
		Task task = finishedTask(cloud);

		SimLogCapture.mute();
		try {
			log.printPowerConsumptionResults(List.of(task));
		} finally {
			SimLogCapture.unmute();
		}

		assertEquals(1, log.getResultsList().size());
	}

	@Test
	void initializeAndSimpleAccessorsExposeTheirValues() {
		wireMixedNodes();
		SimLog log = new SimLog("start", false);
		log.initialize(manager, 15, 0, 0);

		assertEquals(15, log.currentEdgeDevicesCount);
		assertEquals("TRADE_OFF", log.currentOrchAlgorithm);
		assertEquals("ALL", log.currentOrchArchitecture);
		assertEquals("start", log.getSimStartTime());

		log.setSimStartTime("other");
		log.setGeneratedTasks(9);
		log.setCurrentOrchPolicy("MIST");
		assertEquals("other", log.getSimStartTime());
		assertEquals(9, log.getGeneratedTasks());
		assertEquals("MIST", log.currentOrchArchitecture);
		assertEquals("   x", log.padLeftSpaces("x", 4));
	}

	@Test
	void constructorsOnlyAddTheHeaderOnTheFirstIteration() {
		SimLog first = new SimLog("a", true);
		SimLog subsequent = new SimLog("b", false);
		assertEquals(1, first.getResultsList().size());
		assertTrue(subsequent.getResultsList().isEmpty());
		assertNotEquals(first.getSimStartTime(), subsequent.getSimStartTime());
	}
}
