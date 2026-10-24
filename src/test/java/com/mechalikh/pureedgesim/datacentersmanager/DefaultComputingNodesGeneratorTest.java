package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Tests the generation of cloud/edge data centers and edge devices from the XML
 * configuration files, including the different orchestration deployment modes
 * and configuration error handling.
 */
class DefaultComputingNodesGeneratorTest extends DatacentersTestSupport {

	@TempDir
	Path tempDir;

	/**
	 * A computing node whose constructor always fails, used to exercise the
	 * reflective instantiation error path.
	 */
	public static class BrokenComputingNode extends DefaultComputingNode {
		public BrokenComputingNode(SimulationManager simulationManager, double mipsPerCore, int numberOfCPUCores,
				double storage, double ram) {
			super(simulationManager, mipsPerCore, numberOfCPUCores, storage, ram);
			throw new IllegalStateException("broken node");
		}
	}

	private static String deviceXml(int percentage) {
		return "<device arch=\"x86\" os=\"linux\" vmm=\"xen\">"
				+ "<connectivity>wifi</connectivity>"
				+ "<mobility>false</mobility><speed>0</speed>"
				+ "<minPauseDuration>0</minPauseDuration><maxPauseDuration>0</maxPauseDuration>"
				+ "<minMobilityDuration>0</minMobilityDuration><maxMobilityDuration>0</maxMobilityDuration>"
				+ "<battery>false</battery><percentage>" + percentage + "</percentage>"
				+ "<batteryCapacity>0</batteryCapacity><initialBatteryLevel>0</initialBatteryLevel>"
				+ "<idleConsumption>0.4</idleConsumption><maxConsumption>0.5</maxConsumption>"
				+ "<isOrchestrator>false</isOrchestrator><generateTasks>true</generateTasks>"
				+ "<cores>4</cores><mips>1000</mips><ram>100</ram><storage>100</storage>"
				+ "</device>";
	}

	@Test
	void generatesAllDataCentersAndDevicesWithTheExpectedDistribution() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();

		generator.generateDatacentersAndDevices();

		assertEquals(20, generator.getAllNodesList().size());
		assertEquals(9, generator.getAllNodesListSensorsExcluded().size());
		assertEquals(18, generator.getMistOnlyList().size());
		assertEquals(7, generator.getMistOnlyListSensorsExcluded().size());
		assertEquals(8, generator.getMistAndCloudListSensorsExcluded().size());
		assertEquals(8, generator.getMistAndEdgeListSensorsExcluded().size());
		assertEquals(1, generator.getCloudOnlyList().size());
		assertEquals(1, generator.getEdgeOnlyList().size());
		assertEquals(2, generator.getEdgeAndCloudList().size());
		assertTrue(generator.getOrchestratorsList().isEmpty());

		long sensors = generator.getMistOnlyList().stream().filter(ComputingNode::isSensor).count();
		assertEquals(11, sensors);
	}

	@Test
	void dataCenterAttributesAreReadFromTheXmlFiles() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		generator.generateDatacentersAndDevices();

		ComputingNode cloud = generator.getCloudOnlyList().get(0);
		assertEquals(TYPES.CLOUD, cloud.getType());
		assertNull(cloud.getName());
		assertEquals(200, cloud.getNumberOfCPUCores());
		assertEquals(40_000, cloud.getMipsPerCore(), 0.0001);
		assertEquals(8_000_000, cloud.getTotalMipsCapacity(), 0.0001);
		assertEquals(16_000, cloud.getRamCapacity(), 0.0001);
		assertEquals(1_000_000, cloud.getTotalStorage(), 0.0001);
		assertEquals(5776, cloud.getEnergyModel().getMaxActiveConsumption(), 0.0001);
		assertEquals(0, cloud.getEnergyModel().getIdleConsumption(), 0.0001);

		ComputingNode edge = generator.getEdgeOnlyList().get(0);
		assertEquals(TYPES.EDGE_DATACENTER, edge.getType());
		assertEquals("dc1", edge.getName());
		assertTrue(edge.isPeripheral());
		assertEquals(100, edge.getMobilityModel().getCurrentLocation().getXPos(), 0.0001);
		assertEquals(100, edge.getMobilityModel().getCurrentLocation().getYPos(), 0.0001);
		assertEquals(10, edge.getNumberOfCPUCores());
		assertEquals(400_000, edge.getTotalMipsCapacity(), 0.0001);
		assertEquals(16_000, edge.getRamCapacity(), 0.0001);
		assertEquals(200_000, edge.getTotalStorage(), 0.0001);
		assertEquals(250, edge.getEnergyModel().getMaxActiveConsumption(), 0.0001);
		assertEquals(100, edge.getEnergyModel().getIdleConsumption(), 0.0001);
	}

	@Test
	void edgeDeviceAttributesAreReadFromTheXmlFile() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		generator.generateEdgeDevices();

		ComputingNode device = generator.getMistOnlyList().get(0);
		assertEquals(TYPES.EDGE_DEVICE, device.getType());
		assertFalse(device.isSensor());
		assertEquals(8, device.getNumberOfCPUCores());
		assertEquals(25_000, device.getMipsPerCore(), 0.0001);
		assertEquals(200_000, device.getTotalMipsCapacity(), 0.0001);
		assertEquals(256, device.getTotalStorage(), 0.0001);
		assertEquals(4_000, device.getRamCapacity(), 0.0001);
		assertTrue(device.isGeneratingTasks());
		assertTrue(device.getMobilityModel().isMobile());
		assertEquals(1.8, device.getMobilityModel().getSpeed(), 0.0001);
		assertEquals(100, device.getMobilityModel().getMinPauseDuration(), 0.0001);
		assertEquals(400, device.getMobilityModel().getMaxPauseDuration(), 0.0001);
		assertEquals(60, device.getMobilityModel().getMinMobilityDuration(), 0.0001);
		assertEquals(100, device.getMobilityModel().getMaxMobilityDuration(), 0.0001);
		assertTrue(device.getMobilityModel().getCurrentLocation().getXPos() >= 0
				&& device.getMobilityModel().getCurrentLocation().getXPos() < SimulationParameters.simulationMapWidth);
		assertTrue(device.getMobilityModel().getCurrentLocation().getYPos() >= 0
				&& device.getMobilityModel().getCurrentLocation().getYPos() < SimulationParameters.simulationMapLength);

		EnergyModelComputingNode energyModel = device.getEnergyModel();
		assertTrue(energyModel.isBatteryPowered());
		assertEquals(18.75, energyModel.getBatteryCapacity(), 0.0001);
		assertEquals(100, energyModel.getBatteryLevelPercentage(), 0.0001);
		assertEquals("wifi", energyModel.getConnectivityType());
	}

	@Test
	void cloudDataCenterIsUsedAsOrchestratorWhenDeployedOnCloud() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = "CLOUD";

		generator.generateDataCenters(SimulationParameters.cloudDataCentersFile, TYPES.CLOUD);

		assertEquals(1, generator.getCloudOnlyList().size());
		assertEquals(1, generator.getOrchestratorsList().size());
		assertSame(generator.getCloudOnlyList().get(0), generator.getOrchestratorsList().get(0));
	}

	@Test
	void edgeDataCentersAreUsedAsOrchestratorsWhenDeployedOnEdge() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = "EDGE";

		generator.generateDataCenters(SimulationParameters.edgeDataCentersFile, TYPES.EDGE_DATACENTER);

		assertEquals(1, generator.getEdgeOnlyList().size());
		assertEquals(1, generator.getOrchestratorsList().size());
		assertSame(generator.getEdgeOnlyList().get(0), generator.getOrchestratorsList().get(0));
	}

	@Test
	void edgeDataCentersAreNotOrchestratorsForOtherDeployments() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = "CLOUD";

		generator.generateDataCenters(SimulationParameters.edgeDataCentersFile, TYPES.EDGE_DATACENTER);

		assertEquals(1, generator.getEdgeOnlyList().size());
		assertTrue(generator.getOrchestratorsList().isEmpty());
	}

	@Test
	void cloudDataCenterIsNotAnOrchestratorWhenDeployedOnEdge() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = "EDGE";

		generator.generateDataCenters(SimulationParameters.cloudDataCentersFile, TYPES.CLOUD);

		assertEquals(1, generator.getCloudOnlyList().size());
		assertTrue(generator.getOrchestratorsList().isEmpty());
	}

	@Test
	void xmlOrchestratorFlagAddsTheNodeToTheOrchestratorsList() throws Exception {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		String xml = "<datacenter>"
				+ "<idleConsumption>1</idleConsumption><maxConsumption>2</maxConsumption>"
				+ "<isOrchestrator>true</isOrchestrator><cores>2</cores><mips>100</mips>"
				+ "<ram>10</ram><storage>10</storage>"
				+ "</datacenter>";

		ComputingNode node = generator.createComputingNode(parseElement(xml, "datacenter"), TYPES.CLOUD);

		assertTrue(node.isOrchestrator());
		assertEquals(1, generator.getOrchestratorsList().size());
		assertSame(node, generator.getOrchestratorsList().get(0));
	}

	@Test
	void duplicateEdgeDataCenterLocationsAreRejected() throws Exception {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		Element element = parseElement(readResource("edge_datacenters.xml"), "datacenter");
		ComputingNode first = generator.createComputingNode(element, TYPES.EDGE_DATACENTER);
		generator.getEdgeOnlyList().add(first);

		assertThrows(IllegalArgumentException.class,
				() -> generator.createComputingNode(element, TYPES.EDGE_DATACENTER));
	}

	@Test
	void missingDeviceInstancesAreFilledWithTheLastDeviceType() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGeneratorWithMockedLogger();
		SimulationParameters.edgeDevicesFile = writeTempFile(tempDir, "edge_devices_50.xml",
				"<edge_devices>" + deviceXml(50) + "</edge_devices>").toString();

		generator.generateEdgeDevices();

		assertEquals(20, generator.getMistOnlyList().size());
		assertEquals(20, generator.getMistOnlyListSensorsExcluded().size());
		assertEquals(20, generator.getAllNodesList().size());
	}

	/**
	 * The generator warns through the simulation logger before padding the device
	 * list. The real logger cannot format the "100%" literal of that warning
	 * (String.format interprets it as a conversion), which aborts generation, so a
	 * mock logger is used to exercise the padding logic itself.
	 */
	private DefaultComputingNodesGenerator newGeneratorWithMockedLogger() {
		SimulationManager mockedManager = mock(SimulationManager.class);
		when(mockedManager.getSimulation()).thenReturn(simulation);
		when(mockedManager.getScenario()).thenReturn(new Scenario(20, 0, 0));
		when(mockedManager.getSimulationLogger()).thenReturn(mock(SimLog.class));
		return new DefaultComputingNodesGenerator(mockedManager,
				com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel.class, DefaultComputingNode.class);
	}

	@Test
	void percentagesAboveHundredStopTheGeneration() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGeneratorWithMockedLogger();
		SimulationParameters.edgeDevicesFile = writeTempFile(tempDir, "edge_devices_200.xml",
				"<edge_devices>" + deviceXml(200) + "</edge_devices>").toString();

		generator.generateEdgeDevices();

		// Generation stops as soon as more devices than expected are created.
		assertEquals(21, generator.getMistOnlyList().size());
	}

	@Test
	void deviceFileWithoutDeviceTypesGeneratesNothing() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGeneratorWithMockedLogger();
		SimulationParameters.edgeDevicesFile = writeTempFile(tempDir, "edge_devices_empty.xml",
				"<edge_devices></edge_devices>").toString();

		generator.generateEdgeDevices();

		assertTrue(generator.getMistOnlyList().isEmpty());
		assertTrue(generator.getAllNodesList().isEmpty());
	}

	@Test
	void unreadableDeviceFileIsHandledGracefully() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();
		SimulationParameters.edgeDevicesFile = tempDir.resolve("missing_devices.xml").toString();

		SimLogCapture.mute();
		try {
			generator.generateEdgeDevices();
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(generator.getMistOnlyList().isEmpty());
	}

	@Test
	void unreadableDataCenterFileIsHandledGracefully() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = newGenerator();

		SimLogCapture.mute();
		try {
			generator.generateDataCenters(tempDir.resolve("missing_cloud.xml").toString(), TYPES.CLOUD);
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(generator.getCloudOnlyList().isEmpty());
		assertTrue(generator.getAllNodesList().isEmpty());
	}

	@Test
	void constructionFailuresOfDeviceInstancesAreSwallowed() {
		createSimulationManager(20);
		DefaultComputingNodesGenerator generator = new DefaultComputingNodesGenerator(simulationManager,
				com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel.class, BrokenComputingNode.class);
		Element device = parseElement(readResource("edge_devices.xml"), "device");

		SimLogCapture.mute();
		try {
			generator.generateDevicesInstances(device);
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(generator.getMistOnlyList().isEmpty());
		assertNotNull(device);
	}
}
