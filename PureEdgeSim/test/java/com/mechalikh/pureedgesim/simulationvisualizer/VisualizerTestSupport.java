package com.mechalikh.pureedgesim.simulationvisualizer;

import java.io.File;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Shared fixture for the simulation visualizer tests. It runs a single fast
 * bootstrap simulation to obtain a fully wired {@link BootstrapSimulationManager}
 * (with a real data-centers manager and computing nodes generator). The manager
 * can then be reconfigured by each test (clock, scenario, network model, nodes)
 * without running another simulation.
 */
final class VisualizerTestSupport {

	static final String BOOTSTRAP_OUTPUT = AbstractTest.OUTPUT_FOLDER + "visualizer-bootstrap/";

	private VisualizerTestSupport() {
	}

	/**
	 * Boots a fast simulation once per JVM and returns the captured manager.
	 */
	static synchronized BootstrapSimulationManager manager() {
		if (BootstrapSimulationManager.last == null) {
			bootstrap();
		}
		BootstrapSimulationManager captured = BootstrapSimulationManager.last;
		if (captured == null || captured.getDataCentersManager() == null
				|| captured.getDataCentersManager().getComputingNodesGenerator() == null) {
			throw new IllegalStateException("The bootstrap simulation did not initialize a data-centers manager");
		}
		return captured;
	}

	private static void bootstrap() {
		String settingsFolder = AbstractTest.SETTINGS_FOLDER;
		SimulationParameters.simulationParametersFile = settingsFolder + "fast.properties";
		SimulationParameters.applicationFile = settingsFolder + "applications.xml";
		SimulationParameters.edgeDataCentersFile = settingsFolder + "edge_datacenters.xml";
		SimulationParameters.edgeDevicesFile = settingsFolder + "edge_devices.xml";
		SimulationParameters.cloudDataCentersFile = settingsFolder + "cloud.xml";
		SimulationParameters.outputFolder = BOOTSTRAP_OUTPUT;

		SimLogCapture.mute();
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomSettingsFolder(settingsFolder);
			simulation.setCustomOutputFolder(BOOTSTRAP_OUTPUT);
			simulation.setCustomSimulationManager(BootstrapSimulationManager.class);
			SimulationParameters.simulationParametersFile = settingsFolder + "fast.properties";
			simulation.launchSimulation();
		} finally {
			SimLogCapture.unmute();
		}
	}

	/**
	 * Reconfigures the captured manager and empties the computing node lists so
	 * each test starts from a clean, controlled state.
	 */
	static BootstrapSimulationManager configure(String architecture, String algorithm, double clock,
			double failureRate, double wanUp, double wanDown) {
		BootstrapSimulationManager manager = manager();

		SimulationParameters.orchestrationArchitectures = new String[] { architecture };
		SimulationParameters.orchestrationAlgorithms = new String[] { algorithm };

		TestSimulation simulation = new TestSimulation();
		simulation.setTime(clock);
		manager.overrideSimulation(simulation);
		manager.overrideScenario(new Scenario(20, 0, 0));
		manager.overrideFailureRate(failureRate);

		// The fake network model registers itself on the manager when constructed.
		FakeNetworkModel networkModel = new FakeNetworkModel(manager, wanUp, wanDown);
		manager.overrideNetworkModel(networkModel);

		ComputingNodesGenerator generator = generator(manager);
		generator.getMistOnlyList().clear();
		generator.getMistOnlyListSensorsExcluded().clear();
		generator.getEdgeOnlyList().clear();
		generator.getCloudOnlyList().clear();
		return manager;
	}

	static ComputingNodesGenerator generator(BootstrapSimulationManager manager) {
		return manager.getDataCentersManager().getComputingNodesGenerator();
	}

	static ComputingNodesGenerator generator() {
		return generator(manager());
	}

	static void setClock(double clock) {
		((TestSimulation) manager().getSimulation()).setTime(clock);
	}

	static TestNode newNode(double x, double y) {
		return new TestNode(manager(), x, y);
	}

	static TestNode newNode(double avgCpuUtilization) {
		return new TestNode(manager(), 0, 0).cpu(avgCpuUtilization);
	}

	/** A simulation engine whose clock can be set directly by the tests. */
	static final class TestSimulation extends PureEdgeSim {
		void setTime(double time) {
			this.time = time;
		}
	}

	/** A mobility model pinned to a fixed location. */
	static final class FixedMobilityModel extends MobilityModel {
		FixedMobilityModel(double x, double y) {
			super();
			currentLocation = new Location(x, y);
		}

		@Override
		protected Location getNextLocation(Location location) {
			return location;
		}
	}

	/** A computing node whose state (alive/idle/cpu usage) is fully test-driven. */
	static final class TestNode extends DefaultComputingNode {
		private double avgCpuUtilization;

		TestNode(SimulationManager simulationManager, double x, double y) {
			super(simulationManager, 100, 1, 1000, 1000);
			setMobilityModel(new FixedMobilityModel(x, y));
		}

		TestNode cpu(double value) {
			this.avgCpuUtilization = value;
			return this;
		}

		TestNode dead() {
			setDeath(true, 0);
			return this;
		}

		@Override
		public double getAvgCpuUtilization() {
			return avgCpuUtilization;
		}
	}

	/** A network model that reports fixed WAN utilizations. */
	static final class FakeNetworkModel extends NetworkModel {
		private final double wanUp;
		private final double wanDown;

		FakeNetworkModel(SimulationManager simulationManager, double wanUp, double wanDown) {
			super(simulationManager);
			this.wanUp = wanUp;
			this.wanDown = wanDown;
		}

		@Override
		public void processEvent(Event e) {
			// Nothing to do.
		}

		@Override
		protected void updateEdgeDevicesRemainingEnergy(TransferProgress transfer, ComputingNode origin,
				ComputingNode destination) {
			// Nothing to do.
		}

		@Override
		protected void transferFinished(TransferProgress transfer) {
			// Nothing to do.
		}

		@Override
		public double getWanUpUtilization() {
			return wanUp;
		}

		@Override
		public double getWanDownUtilization() {
			return wanDown;
		}
	}

	/** Deletes a directory tree if it exists. */
	static void deleteRecursively(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				deleteRecursively(child);
			}
		}
		file.delete();
	}
}
