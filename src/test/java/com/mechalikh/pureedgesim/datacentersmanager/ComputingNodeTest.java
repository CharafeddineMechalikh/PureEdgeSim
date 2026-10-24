package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

/**
 * Contract tests for the {@link ComputingNode} interface, exercised through the
 * default implementation. Focuses on construction values, typed accessors and
 * link management; queueing and execution are covered by
 * {@link DefaultComputingNodeTest}.
 */
class ComputingNodeTest extends DatacentersTestSupport {

	private DefaultComputingNode node;

	@BeforeEach
	void setUp() {
		createSimulationManager(1);
		node = new DefaultComputingNode(simulationManager, 2.5, 4, 2048, 8192);
	}

	@Test
	void constructorInitializesResources() {
		assertNotNull(node);
		assertEquals(2.5, node.getMipsPerCore(), 0.0001);
		assertEquals(4, node.getNumberOfCPUCores(), 0.0001);
		assertEquals(10.0, node.getTotalMipsCapacity(), 0.0001);
		assertEquals(2048, node.getTotalStorage(), 0.0001);
		assertEquals(2048, node.getAvailableStorage(), 0.0001);
		assertEquals(8192, node.getRamCapacity(), 0.0001);
		assertEquals(8192, node.getAvailableRam(), 0.0001);
		assertFalse(node.isSensor());
		assertTrue(node.isIdle());
	}

	@Test
	void nullComputingNodeIsASingleton() {
		assertSame(ComputingNode.NULL, ComputingNode.NULL);
		assertSame(ComputingNodeNull.getInstance(), ComputingNode.NULL);
	}

	@Test
	void energyAndMobilityModelsCanBeReplaced() {
		EnergyModelComputingNode energyModel = mock(EnergyModelComputingNode.class);
		MobilityModel mobilityModel = mock(MobilityModel.class);

		assertSame(EnergyModelComputingNode.NULL, node.getEnergyModel());
		assertSame(MobilityModel.NULL, node.getMobilityModel());

		node.setEnergyModel(energyModel);
		node.setMobilityModel(mobilityModel);

		assertSame(energyModel, node.getEnergyModel());
		assertSame(mobilityModel, node.getMobilityModel());
	}

	@Test
	void currentLinksAreStoredPerOrientation() {
		NetworkLink upLink = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.LAN);
		NetworkLink deviceToDeviceLink = new NetworkLink(node, node, simulationManager,
				NetworkLinkTypes.LAN);

		assertSame(NetworkLink.NULL, node.getCurrentLink(LinkOrientation.UP_LINK));
		assertSame(NetworkLink.NULL, node.getCurrentLink(LinkOrientation.DOWN_LINK));
		assertSame(NetworkLink.NULL, node.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));

		node.setCurrentLink(upLink, LinkOrientation.UP_LINK);
		assertSame(upLink, node.getCurrentLink(LinkOrientation.UP_LINK));

		node.setCurrentLink(deviceToDeviceLink, LinkOrientation.DEVICE_TO_DEVICE);
		assertSame(deviceToDeviceLink, node.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));

		// The down-link orientation currently resolves to the up link (existing
		// behaviour of NetworkingNode#getCurrentLink).
		NetworkLink downLink = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.LAN);
		node.setCurrentLink(downLink, LinkOrientation.DOWN_LINK);
		assertSame(downLink, node.getCurrentLink(LinkOrientation.DOWN_LINK));
		assertSame(downLink, node.getCurrentLink(LinkOrientation.UP_LINK));
	}

	@Test
	void peripheralAndApplicationPlacementFlagsAreAccessible() {
		assertFalse(node.isPeripheral());
		node.setPeriphery(true);
		assertTrue(node.isPeripheral());

		assertSame(ComputingNode.NULL, node.getApplicationPlacementLocation());
		assertFalse(node.isApplicationPlaced());
		node.setApplicationPlaced(true);
		assertTrue(node.isApplicationPlaced());
	}

	@Test
	void sensorAndIdleFlagsCanBeChanged() {
		assertFalse(node.isSensor());
		node.setAsSensor(true);
		assertTrue(node.isSensor());

		assertTrue(node.isIdle());
		node.setIdle(false);
		assertFalse(node.isIdle());
	}

	@Test
	void simpleSettersUpdateTheirValues() {
		node.setType(TYPES.EDGE_DEVICE);
		assertEquals(TYPES.EDGE_DEVICE, node.getType());

		node.setName("device");
		assertEquals("device", node.getName());

		node.setApplicationType(2);
		assertEquals(2, node.getApplicationType());

		node.setStorage(500);
		assertEquals(500, node.getTotalStorage(), 0.0001);

		node.setAvailableStorage(400);
		assertEquals(400, node.getAvailableStorage(), 0.0001);

		node.setRam(600);
		assertEquals(600, node.getRamCapacity(), 0.0001);

		node.setAvailableRam(300);
		assertEquals(300, node.getAvailableRam(), 0.0001);

		node.setNumberOfCPUCores(6);
		assertEquals(6, node.getNumberOfCPUCores(), 0.0001);

		node.setTotalMipsCapacity(1200);
		assertEquals(1200, node.getTotalMipsCapacity(), 0.0001);
	}

	@Test
	void idsAreAssignedPerConcreteClass() {
		DefaultComputingNode other = new DefaultComputingNode(simulationManager, 1, 1, 10, 10);
		assertTrue(other.getId() >= 0);
		assertNotSame(node.getId(), other.getId());
	}

	@Test
	void nullNodeIgnoresTypeAndNameChanges() {
		ComputingNode.NULL.setType(TYPES.CLOUD);
		ComputingNode.NULL.setName("cloud");
		assertEquals(TYPES.NULL, ComputingNode.NULL.getType());
		assertEquals("", ComputingNode.NULL.getName());
		assertEquals(-1, ComputingNode.NULL.getId());
	}
}
