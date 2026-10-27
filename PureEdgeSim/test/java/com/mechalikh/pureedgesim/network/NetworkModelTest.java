package com.mechalikh.pureedgesim.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class NetworkModelTest extends AbstractTest {

	/** Minimal concrete network model used to exercise the abstract class. */
	private static class TestNetworkModel extends NetworkModel {
		Event lastEvent;

		TestNetworkModel(SimulationManager simulationManager) {
			super(simulationManager);
		}

		@Override
		public void processEvent(Event event) {
			lastEvent = event;
		}

		@Override
		protected void updateEdgeDevicesRemainingEnergy(TransferProgress transfer, ComputingNode origin,
				ComputingNode destination) {
			// not used by these tests
		}

		@Override
		protected void transferFinished(TransferProgress transfer) {
			// not used by these tests
		}
	}

	private PureEdgeSim simulation;
	private SimulationManager simulationManager;
	private TestNetworkModel model;

	@BeforeEach
	void setUp() {
		simulation = NetworkTestSupport.newSimulation();
		simulationManager = NetworkTestSupport.newSimulationManager(simulation);
		model = new TestNetworkModel(simulationManager);
	}

	@Test
	void constructorStoresSimulationManagerAndRegistersItself() {
		assertSame(simulationManager, model.simulationManager);
		assertSame(simulation, model.getSimulation());
		verify(simulationManager).setNetworkModel(model);
	}

	@Test
	void networkModelConstantsHaveExpectedValues() {
		assertEquals(1, NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION);
		assertEquals(3, NetworkModel.DOWNLOAD_CONTAINER);
		assertEquals(4, NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_ORCH);
		assertEquals(6, NetworkModel.SEND_RESULT_TO_ORCH);
		assertEquals(7, NetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV);
		assertEquals(2, NetworkModel.TRANSFER_FINISHED);
	}

	@Test
	void setSimulationManagerReplacesManager() {
		SimulationManager other = NetworkTestSupport.newSimulationManager();
		model.setSimulationManager(other);
		assertSame(other, model.simulationManager);
	}

	@Test
	void processEventDelegatesToTheConcreteImplementation() {
		Event event = new Event(model, 1.0, 42);
		model.processEvent(event);
		assertSame(event, model.lastEvent);
	}

	@Test
	void setWanLinksAndUtilizationGettersWorkWhenSharedWanIsEnabled() {
		NetworkLinkWanUp wanUp = mock(NetworkLinkWanUp.class);
		NetworkLinkWanDown wanDown = mock(NetworkLinkWanDown.class);
		when(wanUp.getUsedBandwidth()).thenReturn(12.5);
		when(wanDown.getUsedBandwidth()).thenReturn(7.5);
		SimulationParameters.useOneSharedWanLink = true;

		model.setWanLinks(wanUp, wanDown);

		assertSame(wanUp, model.wanUp);
		assertSame(wanDown, model.wanDown);
		assertEquals(12.5, model.getWanUpUtilization());
		assertEquals(7.5, model.getWanDownUtilization());
	}

	@Test
	void wanUtilizationGettersRejectCallsWhenSharedWanIsDisabled() {
		SimulationParameters.useOneSharedWanLink = false;

		IllegalArgumentException upException = assertThrows(IllegalArgumentException.class,
				() -> model.getWanUpUtilization());
		IllegalArgumentException downException = assertThrows(IllegalArgumentException.class,
				() -> model.getWanDownUtilization());

		assertTrue(upException.getMessage().contains("getWanUpUtilization"));
		assertTrue(upException.getMessage().contains("one_shared_wan_network"));
		assertTrue(downException.getMessage().contains("getWanDownUtilization"));
	}
}
