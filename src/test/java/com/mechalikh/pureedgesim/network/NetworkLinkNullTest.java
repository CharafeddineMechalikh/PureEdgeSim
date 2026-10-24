package com.mechalikh.pureedgesim.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink;

class NetworkLinkNullTest {

	@Test
	void nullLinkImplementsAllAccessorsWithNeutralValues() {
		NetworkLinkNull link = new NetworkLinkNull();

		assertEquals(0, link.getLatency());
		assertSame(ComputingNode.NULL, link.getSrc());
		assertSame(ComputingNode.NULL, link.getDst());
		assertEquals(0, link.getBandwidth(5));
		assertEquals(0, link.getBandwidth(0));
		assertEquals(0, link.getUsedBandwidth());
		assertSame(EnergyModelNetworkLink.NULL, link.getEnergyModel());
		assertEquals(0, link.getTotalTransferredData());
	}

	@Test
	void staticNullConstantIsANullLink() {
		assertTrue(NetworkLink.NULL instanceof NetworkLinkNull);
		assertSame(EnergyModelNetworkLink.NULL, NetworkLink.NULL.getEnergyModel());
	}
}
