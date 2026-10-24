package com.mechalikh.pureedgesim.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.network.NetworkLink;

class EnergyModelNetworkLinkTest {

	@Test
	void currentConsumptionIsEnergyPerBitTimesUsedBandwidth() {
		NetworkLink link = mock(NetworkLink.class);
		when(link.getUsedBandwidth()).thenReturn(200.0);
		when(link.getTotalTransferredData()).thenReturn(1000.0);
		EnergyModelNetworkLink model = new EnergyModelNetworkLink(0.5, link);

		assertSame(link, model.getLink());
		assertEquals(0.5, model.getEnergyPerBit());
		assertEquals(100.0, model.getCurrentEnergyConsumption());
		assertEquals(500.0, model.getTotalEnergyConsumption());
	}

	@Test
	void zeroEnergyPerBitProducesZeroConsumption() {
		NetworkLink link = mock(NetworkLink.class);
		when(link.getUsedBandwidth()).thenReturn(1234.0);
		when(link.getTotalTransferredData()).thenReturn(5678.0);
		EnergyModelNetworkLink model = new EnergyModelNetworkLink(0, link);

		assertEquals(0, model.getCurrentEnergyConsumption());
		assertEquals(0, model.getTotalEnergyConsumption());
	}

	@Test
	void nullModelReturnsZeroConsumption() {
		EnergyModelNetworkLinkNull model = EnergyModelNetworkLinkNull.getInstance();

		assertSame(model, EnergyModelNetworkLinkNull.getInstance());
		assertSame(NetworkLink.NULL, model.getLink());
		assertEquals(0, model.getEnergyPerBit());
		assertEquals(0.0, model.getCurrentEnergyConsumption());
		assertEquals(0.0, model.getTotalEnergyConsumption());
	}

	@Test
	void nullModelCanBeCreatedDirectly() {
		EnergyModelNetworkLinkNull model = new EnergyModelNetworkLinkNull();
		assertEquals(0, model.getCurrentEnergyConsumption());
		assertEquals(0, model.getTotalEnergyConsumption());
	}
}
