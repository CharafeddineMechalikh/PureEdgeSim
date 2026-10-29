package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mechalikh.pureedgesim.SimLogCapture;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLinkCellularDown;
import com.mechalikh.pureedgesim.network.NetworkLinkCellularUp;
import com.mechalikh.pureedgesim.network.NetworkLinkEthernet;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiDown;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiUp;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Tests every link-generation strategy of {@link DefaultTopologyCreator}: WAN
 * sharing, wired/wireless access links, LAN attachment, MAN links declared in
 * XML and error handling.
 */
class DefaultTopologyCreatorTest extends DatacentersTestSupport {

	@TempDir
	Path tempDir;

	/**
	 * Captures the node returned by {@link DefaultTopologyCreator#createWanLink()}
	 * so tests can assert whether a shared router or the cloud is used.
	 */
	public static class RecordingDefaultTopologyCreator extends DefaultTopologyCreator {
		public ComputingNode wanNode;

		public RecordingDefaultTopologyCreator(SimulationManager simulationManager,
				ComputingNodesGenerator computingNodesGenerator) {
			super(simulationManager, computingNodesGenerator);
		}

		@Override
		protected ComputingNode createWanLink() {
			wanNode = super.createWanLink();
			return wanNode;
		}
	}

	private DefaultComputingNodesGenerator generateInputs(String edgeDataCentersFile, boolean sharedWan) {
		createSimulationManager(20);
		SimulationParameters.useOneSharedWanLink = sharedWan;
		attachDefaultNetworkModel();
		DefaultComputingNodesGenerator generator = newGenerator();
		generator.generateDataCenters(SimulationParameters.cloudDataCentersFile, TYPES.CLOUD);
		generator.generateDataCenters(edgeDataCentersFile, TYPES.EDGE_DATACENTER);
		generator.generateEdgeDevices();
		return generator;
	}

	private static NetworkLink firstEdgeDeviceWanLink(DefaultTopologyCreator creator) {
		return creator.getInfrastructureTopology().getWanLinks().stream()
				.filter(link -> link.getSrc().getType() == TYPES.EDGE_DEVICE).findFirst()
				.orElseThrow(() -> new AssertionError("no WAN link from an edge device"));
	}

	@Test
	void sharedWanCreatesARouterAndConnectsEveryDeviceToIt() {
		DefaultComputingNodesGenerator generator = generateInputs(SimulationParameters.edgeDataCentersFile, true);
		RecordingDefaultTopologyCreator creator = new RecordingDefaultTopologyCreator(simulationManager, generator);

		creator.generateTopologyGraph();

		assertNotNull(creator.wanNode);
		assertTrue(creator.wanNode instanceof Router);
		assertSame(creator.wanNode, firstEdgeDeviceWanLink(creator).getDst());
		assertSame(simulationManager, creator.getSimulationManager());
		assertNotNull(creator.getInfrastructureTopology());
		assertSame(creator.getTopology(), creator.getInfrastructureTopology());
		// 20 devices * 2 WAN links + the 2 WAN links of dc1 (router-cloud links are
		// IGNORE).
		assertEquals(42, creator.getInfrastructureTopology().getWanLinks().size());
		assertEquals(0, creator.getInfrastructureTopology().getManLinks().size());
		assertEquals(4, creator.getInfrastructureTopology().getPathsMap().size());

		ComputingNode edgeDataCenter = generator.getEdgeOnlyList().get(0);
		ComputingNode device = generator.getMistOnlyList().get(0);
		NetworkLink upLink = device.getCurrentLink(LinkOrientation.UP_LINK);
		assertTrue(upLink instanceof NetworkLinkWifiUp);
		assertSame(device, upLink.getSrc());
		assertSame(edgeDataCenter, upLink.getDst());
		NetworkLink downLink = device.getCurrentLink(LinkOrientation.DOWN_LINK);
		assertTrue(downLink instanceof NetworkLinkWifiDown);
		assertSame(edgeDataCenter, downLink.getSrc());
		assertSame(device, downLink.getDst());
		assertSame(edgeDataCenter, device.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE).getDst());
	}

	@Test
	void dedicatedWanUsesTheCloudNodeDirectly() {
		DefaultComputingNodesGenerator generator = generateInputs(SimulationParameters.edgeDataCentersFile, false);
		RecordingDefaultTopologyCreator creator = new RecordingDefaultTopologyCreator(simulationManager, generator);

		creator.generateTopologyGraph();

		ComputingNode cloud = generator.getCloudOnlyList().get(0);
		assertSame(cloud, creator.wanNode);
		assertFalse(creator.wanNode instanceof Router);
		assertSame(cloud, firstEdgeDeviceWanLink(creator).getDst());
		assertEquals(22, creator.getTopology().getGraph().vertexSet().size());
	}

	@Test
	void cellularAndEthernetDevicesUseTheirOwnLinkImplementations() {
		DefaultComputingNodesGenerator generator = generateInputs(SimulationParameters.edgeDataCentersFile, true);
		List<ComputingNode> devices = generator.getMistOnlyList();
		devices.get(0).getEnergyModel().setConnectivityType("cellular");
		devices.get(1).getEnergyModel().setConnectivityType("ethernet");
		DefaultTopologyCreator creator = new DefaultTopologyCreator(simulationManager, generator);

		creator.generateTopologyGraph();

		// One WAN and one LAN connection in each direction for each device.
		assertEquals(4, creator.getInfrastructureTopology().getCellularLinks().size());
		assertEquals(4, creator.getInfrastructureTopology().getEthernetLinks().size());
		assertTrue(creator.getInfrastructureTopology().getWifiLinks().size() >= 40);

		ComputingNode edgeDataCenter = generator.getEdgeOnlyList().get(0);
		NetworkLink cellularLink = devices.get(0).getCurrentLink(LinkOrientation.UP_LINK);
		assertTrue(cellularLink instanceof NetworkLinkCellularUp);
		assertSame(devices.get(0), cellularLink.getSrc());
		assertSame(edgeDataCenter, cellularLink.getDst());
		NetworkLink ethernetLink = devices.get(1).getCurrentLink(LinkOrientation.UP_LINK);
		assertTrue(ethernetLink instanceof NetworkLinkEthernet);
		assertSame(devices.get(1), ethernetLink.getSrc());
		assertSame(edgeDataCenter, ethernetLink.getDst());
	}

	@Test
	void unknownConnectivityTypeIsRejected() {
		DefaultComputingNodesGenerator generator = generateInputs(SimulationParameters.edgeDataCentersFile, true);
		generator.getMistOnlyList().get(0).getEnergyModel().setConnectivityType("bluetooth");
		DefaultTopologyCreator creator = new DefaultTopologyCreator(simulationManager, generator);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				creator::generateTopologyGraph);

		assertTrue(exception.getMessage().contains("Unknown connectivity type"));
		assertTrue(exception.getMessage().contains("bluetooth"));
	}

	@Test
	void devicesOutsideTheDataCenterRangeAreConnectedToTheNullNode() {
		DefaultComputingNodesGenerator generator = generateInputs(SimulationParameters.edgeDataCentersFile, true);
		SimulationParameters.edgeDataCentersRange = 0;
		DefaultTopologyCreator creator = new DefaultTopologyCreator(simulationManager, generator);

		creator.generateTopologyGraph();

		ComputingNode device = generator.getMistOnlyList().get(0);
		NetworkLink upLink = device.getCurrentLink(LinkOrientation.UP_LINK);
		assertSame(device, upLink.getSrc());
		assertSame(ComputingNode.NULL, upLink.getDst());
		NetworkLink downLink = device.getCurrentLink(LinkOrientation.DOWN_LINK);
		assertSame(ComputingNode.NULL, downLink.getSrc());
		assertSame(device, downLink.getDst());
		assertSame(ComputingNode.NULL,
				device.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE).getDst());
		// The device can still reach the cloud through its WAN link.
		assertEquals(4, creator.getTopology().getPathsMap().size());
	}

	@Test
	void manLinksAreCreatedFromTheEdgeDataCenterXmlFile() {
		String edgeXml = "<edge_datacenters>"
				+ datacenterXml("dc1", true, 100, 100)
				+ datacenterXml("dc2", false, 150, 150)
				+ datacenterXml("dc3", true, 170, 170)
				+ "<network_links>"
				+ "<link><from>dc1</from><to>dc2</to></link>"
				+ "<link><from>dc1</from><to>dc3</to></link>"
				+ "</network_links>"
				+ "</edge_datacenters>";
		Path edgeFile = writeTempFile(tempDir, "edge_datacenters_two_dcs.xml", edgeXml);
		DefaultComputingNodesGenerator generator = generateInputs(edgeFile.toString(), true);
		SimulationParameters.edgeDataCentersFile = edgeFile.toString();

		List<ComputingNode> edgeDataCenters = generator.getEdgeOnlyList();
		assertEquals(3, edgeDataCenters.size());
		ComputingNode dc1 = edgeDataCenters.get(0);
		ComputingNode dc2 = edgeDataCenters.get(1);
		ComputingNode dc3 = edgeDataCenters.get(2);
		assertTrue(dc1.isPeripheral());
		assertFalse(dc2.isPeripheral());
		assertTrue(dc3.isPeripheral());
		// Deterministic distances: dc3 is farther than dc1 but still in range.
		for (ComputingNode device : generator.getMistOnlyList()) {
			MobilityModel mobility = mock(MobilityModel.class);
			when(mobility.distanceTo(dc1)).thenReturn(30.0);
			when(mobility.distanceTo(dc3)).thenReturn(50.0);
			device.setMobilityModel(mobility);
		}
		DefaultTopologyCreator creator = new DefaultTopologyCreator(simulationManager, generator);

		creator.generateTopologyGraph();

		assertEquals(4, creator.getTopology().getManLinks().size());
		assertEquals(16, creator.getTopology().getPathsMap().size());
		assertSame(dc1, generator.getMistOnlyList().get(0).getCurrentLink(LinkOrientation.UP_LINK).getDst());
		assertSame(dc1, generator.getMistOnlyList().get(0).getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE).getDst());
		assertSame(dc2, creator.getDataCenterByName("dc2"));
		assertSame(dc3, creator.getDataCenterByName("dc3"));
		assertSame(ComputingNode.NULL, creator.getDataCenterByName("unknown"));
	}

	@Test
	void brokenEdgeDataCenterFileIsHandledGracefully() {
		createSimulationManager(1);
		DefaultComputingNodesGenerator generator = newGenerator();
		DefaultTopologyCreator creator = new DefaultTopologyCreator(simulationManager, generator);
		SimulationParameters.edgeDataCentersFile = tempDir.resolve("missing_edge_dcs.xml").toString();

		SimLogCapture.mute();
		try {
			creator.generateTopologyFromXmlFile();
		} finally {
			SimLogCapture.unmute();
		}

		assertTrue(creator.getTopology().getGraph().vertexSet().isEmpty());
	}
}
