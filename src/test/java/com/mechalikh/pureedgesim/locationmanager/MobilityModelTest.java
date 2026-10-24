package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class MobilityModelTest extends AbstractTest {

	private static class StubMobilityModel extends MobilityModel {
		StubMobilityModel(SimulationManager simulationManager, Location location) {
			super(simulationManager, location);
		}

		StubMobilityModel() {
			super();
		}

		@Override
		protected Location getNextLocation(Location location) {
			return new Location(location.getXPos() + 1, location.getYPos() + 1);
		}
	}

	private SimulationManager simulationManager;
	private DataCentersManager dataCentersManager;
	private ComputingNodesGenerator computingNodesGenerator;
	private List<ComputingNode> edgeNodes;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		dataCentersManager = mock(DataCentersManager.class);
		computingNodesGenerator = mock(ComputingNodesGenerator.class);
		edgeNodes = new ArrayList<>();
		when(simulationManager.getDataCentersManager()).thenReturn(dataCentersManager);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(computingNodesGenerator);
		when(computingNodesGenerator.getEdgeOnlyList()).thenReturn(edgeNodes);
		SimulationParameters.simulationDuration = 2;
		SimulationParameters.updateInterval = 1;
		SimulationParameters.edgeDataCentersRange = 50;
	}

	private ComputingNode edgeNode(Location location, boolean peripheral) {
		ComputingNode node = mock(ComputingNode.class);
		when(node.isPeripheral()).thenReturn(peripheral);
		when(node.getMobilityModel()).thenReturn(new StubMobilityModel(simulationManager, location));
		return node;
	}

	@Test
	void defaultConstructorLeavesNeutralState() {
		StubMobilityModel model = new StubMobilityModel();
		assertNull(model.getCurrentLocation());
		assertNull(model.getSimulationManager());
		assertFalse(model.isMobile());
		assertEquals(0, model.getSpeed());
		assertEquals(0, model.getMinPauseDuration());
		assertEquals(0, model.getMaxPauseDuration());
		assertEquals(0, model.getMinMobilityDuration());
		assertEquals(0, model.getMaxMobilityDuration());
		assertSame(ComputingNode.NULL, model.getClosestEdgeDataCenter());
	}

	@Test
	void constructorStoresLocationAndSimulationManager() {
		Location location = new Location(3, 4);
		StubMobilityModel model = new StubMobilityModel(simulationManager, location);
		assertSame(location, model.getCurrentLocation());
		assertSame(simulationManager, model.getSimulationManager());
	}

	@Test
	void settersReturnTheModelForChaining() {
		StubMobilityModel model = new StubMobilityModel();
		assertSame(model, model.setMobile(true));
		assertSame(model, model.setSpeed(1.5));
		assertSame(model, model.setMinPauseDuration(10));
		assertSame(model, model.setMaxPauseDuration(20));
		assertSame(model, model.setMinMobilityDuration(30));
		assertSame(model, model.setMaxMobilityDuration(40));

		assertTrue(model.isMobile());
		assertEquals(1.5, model.getSpeed());
		assertEquals(10, model.getMinPauseDuration());
		assertEquals(20, model.getMaxPauseDuration());
		assertEquals(30, model.getMinMobilityDuration());
		assertEquals(40, model.getMaxMobilityDuration());
	}

	@Test
	void setSimulationManagerReplacesManager() {
		StubMobilityModel model = new StubMobilityModel();
		SimulationManager other = mock(SimulationManager.class);
		model.setSimulationManager(other);
		assertSame(other, model.getSimulationManager());
	}

	@Test
	void updateLocationReadsPrecomputedPathWithinSimulationTime() {
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		Location pathLocation = new Location(7, 8);
		model.path.put(1000, pathLocation);

		assertSame(pathLocation, model.updateLocation(1.0));
		assertSame(pathLocation, model.getCurrentLocation());
	}

	@Test
	void updateLocationKeepsCurrentLocationAfterSimulationEnds() {
		Location start = new Location(1, 1);
		StubMobilityModel model = new StubMobilityModel(simulationManager, start);

		assertSame(start, model.updateLocation(3.0));
		assertSame(start, model.getCurrentLocation());
	}

	@Test
	void distanceToUsesCurrentLocationOfBothMobilityModels() {
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		ComputingNode other = mock(ComputingNode.class);
		when(other.getMobilityModel())
				.thenReturn(new StubMobilityModel(simulationManager, new Location(3, 4)));

		assertEquals(5, model.distanceTo(other));
	}

	@Test
	void generatePathWithoutMobilityOnlyFindsClosestDataCenter() {
		ComputingNode dataCenter = edgeNode(new Location(10, 0), true);
		edgeNodes.add(dataCenter);
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		model.setMobile(false);

		model.generatePath();

		assertSame(dataCenter, model.getClosestEdgeDataCenter());
		assertTrue(model.path.isEmpty());
		assertTrue(model.datacentersMap.isEmpty());
	}

	@Test
	void generatePathWithMobilityFillsPathAndDataCenterMaps() {
		ComputingNode dataCenter = edgeNode(new Location(10, 0), true);
		edgeNodes.add(dataCenter);
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		model.setMobile(true);

		model.generatePath();

		assertEquals(3, model.path.size());
		assertEquals(3, model.datacentersMap.size());
		assertEquals(new Location(0, 0), model.path.get(0));
		assertEquals(new Location(1, 1), model.path.get(1000));
		assertEquals(new Location(2, 2), model.path.get(2000));
		assertSame(dataCenter, model.datacentersMap.get(1000));
	}

	@Test
	void getDataCenterIgnoresNonPeripheralAndFarNodes() {
		ComputingNode nonPeripheral = edgeNode(new Location(0, 0), false);
		ComputingNode farPeripheral = edgeNode(new Location(1000, 0), true);
		edgeNodes.add(nonPeripheral);
		edgeNodes.add(farPeripheral);
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));

		assertSame(ComputingNode.NULL, model.getDataCenter());
	}

	@Test
	void getDataCenterReturnsNullObjectForEmptyList() {
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		assertSame(ComputingNode.NULL, model.getDataCenter());
	}

	@Test
	void closestDataCenterIsReadFromPathWhileMobileAndWithinSimulationTime() {
		PureEdgeSim simulation = mock(PureEdgeSim.class);
		when(simulationManager.getSimulation()).thenReturn(simulation);
		ComputingNode dataCenter = edgeNode(new Location(10, 0), true);
		edgeNodes.add(dataCenter);
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		model.setMobile(true);
		model.generatePath();
		when(simulation.clock()).thenReturn(1.0);

		assertSame(dataCenter, model.getClosestEdgeDataCenter());
	}

	@Test
	void closestDataCenterFallsBackAfterSimulationTime() {
		PureEdgeSim simulation = mock(PureEdgeSim.class);
		when(simulationManager.getSimulation()).thenReturn(simulation);
		ComputingNode dataCenter = edgeNode(new Location(10, 0), true);
		edgeNodes.add(dataCenter);
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		model.setMobile(true);
		model.generatePath();
		when(simulation.clock()).thenReturn(5.0);

		assertSame(dataCenter, model.getClosestEdgeDataCenter());
	}

	@Test
	void closestDataCenterIsStaticForNonMobileDevices() {
		ComputingNode dataCenter = edgeNode(new Location(10, 0), true);
		edgeNodes.add(dataCenter);
		StubMobilityModel model = new StubMobilityModel(simulationManager, new Location(0, 0));
		model.setMobile(false);
		model.generatePath();

		assertSame(dataCenter, model.getClosestEdgeDataCenter());
	}
}
