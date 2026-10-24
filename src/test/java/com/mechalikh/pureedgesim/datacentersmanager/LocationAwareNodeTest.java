package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;

/**
 * Tests {@link LocationAwareNode}: mobility wiring, connection to the closest
 * edge data center, application un-placement when moving away, and the different
 * orchestrator deployment strategies.
 */
class LocationAwareNodeTest extends DatacentersTestSupport {

	private DefaultComputingNode node;
	private MobilityModel mobilityModel;
	private NetworkLink upLink;
	private NetworkLink downLink;

	@BeforeEach
	void setUp() {
		createSimulationManager(1);
		node = new DefaultComputingNode(simulationManager, 10, 1, 100, 100);
		node.setType(TYPES.EDGE_DEVICE);
		mobilityModel = mock(MobilityModel.class);
		node.setMobilityModel(mobilityModel);
		upLink = new NetworkLink(node, ComputingNode.NULL, simulationManager, NetworkLinkTypes.WAN);
		downLink = new NetworkLink(ComputingNode.NULL, node, simulationManager, NetworkLinkTypes.WAN);
		// DOWN_LINK is backed by the same field as UP_LINK, so the last assignment
		// decides which link is used by both orientations.
		node.setCurrentLink(downLink, LinkOrientation.DOWN_LINK);
		node.setCurrentLink(upLink, LinkOrientation.UP_LINK);
		node.setCurrentLink(new NetworkLink(), LinkOrientation.DEVICE_TO_DEVICE);
	}

	private Event statusEvent() {
		return new Event(node, simulationManager.getSimulation().clock(), AbstractNode.UPDATE_STATUS);
	}

	@Test
	void simulationStartGeneratesTheMobilityPath() {
		node.onSimulationStart();

		verify(mobilityModel).generatePath();
		assertEquals(1, simulation.getEventsQueue().size());
		assertEquals(AbstractNode.UPDATE_STATUS, simulation.getEventsQueue().first().getTag());
	}

	@Test
	void mobileNodeUpdatesItsLocationAndConnectsToTheClosestDataCenter() {
		DefaultComputingNode edgeDataCenter = new DefaultComputingNode(simulationManager, 1000, 10, 1000, 1000);
		edgeDataCenter.setType(TYPES.EDGE_DATACENTER);
		when(mobilityModel.isMobile()).thenReturn(true);
		when(mobilityModel.updateLocation(0)).thenReturn(new Location(1, 2));
		when(mobilityModel.getClosestEdgeDataCenter()).thenReturn(edgeDataCenter);
		setClock(0);

		node.processEvent(statusEvent());

		verify(mobilityModel).updateLocation(0);
		assertSame(edgeDataCenter, upLink.getDst());
		// DOWN_LINK access currently maps to the up link; connectWith therefore
		// updates the source of the up link too.
		assertSame(edgeDataCenter, upLink.getSrc());
		assertFalse(node.isApplicationPlaced());
	}

	@Test
	void fixedNodeDoesNotUpdateItsLocation() {
		when(mobilityModel.isMobile()).thenReturn(false);

		node.processEvent(statusEvent());

		verify(mobilityModel, never()).updateLocation(anyDouble());
		verify(mobilityModel, never()).getClosestEdgeDataCenter();
	}

	@Test
	void movingAwayFromTheApplicationHostUnplacesTheApplication() {
		DefaultComputingNode host = new DefaultComputingNode(simulationManager, 10, 1, 100, 100);
		host.setType(TYPES.EDGE_DEVICE);
		NetworkLink deviceLink = new NetworkLink(node, host, simulationManager, NetworkLinkTypes.LAN);
		node.setCurrentLink(deviceLink, LinkOrientation.DEVICE_TO_DEVICE);
		node.setApplicationPlaced(true);
		when(mobilityModel.isMobile()).thenReturn(true);
		when(mobilityModel.updateLocation(0)).thenReturn(new Location(10, 10));
		when(mobilityModel.getClosestEdgeDataCenter()).thenReturn(host);
		when(mobilityModel.distanceTo(host)).thenReturn((double) SimulationParameters.edgeDataCentersRange + 1);

		node.processEvent(statusEvent());

		assertFalse(node.isApplicationPlaced());
	}

	@Test
	void stayingCloseToTheApplicationHostKeepsThePlacement() {
		DefaultComputingNode host = new DefaultComputingNode(simulationManager, 10, 1, 100, 100);
		host.setType(TYPES.EDGE_DEVICE);
		NetworkLink deviceLink = new NetworkLink(node, host, simulationManager, NetworkLinkTypes.LAN);
		node.setCurrentLink(deviceLink, LinkOrientation.DEVICE_TO_DEVICE);
		node.setApplicationPlaced(true);
		when(mobilityModel.isMobile()).thenReturn(true);
		when(mobilityModel.updateLocation(0)).thenReturn(new Location(10, 10));
		when(mobilityModel.getClosestEdgeDataCenter()).thenReturn(host);
		when(mobilityModel.distanceTo(host)).thenReturn(1.0);

		node.processEvent(statusEvent());

		assertTrue(node.isApplicationPlaced());
	}

	@Test
	void deadMobileNodeStopsUpdatingItsLocation() {
		EnergyModelComputingNode energyModel = mock(EnergyModelComputingNode.class);
		when(energyModel.isBatteryPowered()).thenReturn(true);
		when(energyModel.getBatteryLevelWattHour()).thenReturn(0.0);
		node.setEnergyModel(energyModel);
		when(mobilityModel.isMobile()).thenReturn(true);

		node.processEvent(statusEvent());

		assertTrue(node.isDead());
		verify(mobilityModel, never()).updateLocation(anyDouble());
	}

	@Test
	void orchestratorIsTheNodeItselfWhenOrchestratorsAreDisabled() {
		SimulationParameters.enableOrchestrators = false;
		assertSame(node, node.getOrchestrator());
	}

	@Test
	void orchestratorCanBeTheCloudWhenDeploymentIsEmpty() {
		assertCloudOrchestrator("");
	}

	@Test
	void orchestratorCanBeTheCloudWhenDeploymentIsCloud() {
		assertCloudOrchestrator("CLOUD");
	}

	private void assertCloudOrchestrator(String deployment) {
		DefaultComputingNode cloud = new DefaultComputingNode(simulationManager, 1000, 10, 1000, 1000);
		cloud.setType(TYPES.CLOUD);
		ComputingNodesGenerator generator = mock(ComputingNodesGenerator.class);
		when(generator.getCloudOnlyList()).thenReturn(List.of(cloud));
		wireDataCentersManager(generator, null);
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = deployment;

		assertSame(cloud, node.getOrchestrator());
		assertSame(cloud, node.getOrchestrator(), "the choice must be cached");
		verify(generator, times(1)).getCloudOnlyList();
	}

	@Test
	void orchestratorCanBeTheClosestEdgeDataCenter() {
		DefaultComputingNode edgeDataCenter = new DefaultComputingNode(simulationManager, 1000, 10, 1000, 1000);
		edgeDataCenter.setType(TYPES.EDGE_DATACENTER);
		when(mobilityModel.getClosestEdgeDataCenter()).thenReturn(edgeDataCenter);
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = "EDGE";

		assertSame(edgeDataCenter, node.getOrchestrator());
		verify(mobilityModel).getClosestEdgeDataCenter();
	}

	@Test
	void orchestratorCanBeTheNodeItselfInMistDeployment() {
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = "MIST";

		assertSame(node, node.getOrchestrator());
	}

	@Test
	void orchestratorIsChosenByLowestTopologyDelayForCustomDeployments() {
		DefaultComputingNode first = new DefaultComputingNode(simulationManager, 1000, 10, 1000, 1000);
		DefaultComputingNode second = new DefaultComputingNode(simulationManager, 1000, 10, 1000, 1000);
		ComputingNodesGenerator generator = mock(ComputingNodesGenerator.class);
		when(generator.getOrchestratorsList()).thenReturn(List.of(first, second));
		InfrastructureGraph topology = mock(InfrastructureGraph.class);
		when(topology.getDelay(node, first)).thenReturn(0.2);
		when(topology.getDelay(node, second)).thenReturn(0.9);
		wireDataCentersManager(generator, topology);
		SimulationParameters.enableOrchestrators = true;
		SimulationParameters.deployOrchestrators = "REGISTRY";

		assertSame(first, node.getOrchestrator());
		verify(topology).getDelay(node, first);
		verify(topology).getDelay(node, second);
	}

	private void wireDataCentersManager(ComputingNodesGenerator generator, InfrastructureGraph topology) {
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		if (topology != null) {
			when(dataCentersManager.getTopology()).thenReturn(topology);
		}
		simulationManager.setDataCentersManager(dataCentersManager);
	}

	@Test
	void disabledOrchestratorsDoNotRequireAnyTopology() {
		SimulationParameters.enableOrchestrators = false;
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		simulationManager.setDataCentersManager(dataCentersManager);

		assertSame(node, node.getOrchestrator());

		verifyNoInteractions(dataCentersManager);
	}
}
