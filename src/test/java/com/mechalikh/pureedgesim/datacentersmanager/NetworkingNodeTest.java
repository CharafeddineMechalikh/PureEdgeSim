package com.mechalikh.pureedgesim.datacentersmanager;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;

/**
 * Tests the link container behaviour of {@link NetworkingNode} through its
 * default concrete node.
 */
class NetworkingNodeTest extends DatacentersTestSupport {

	private DefaultComputingNode node;

	@BeforeEach
	void setUp() {
		createSimulationManager(1);
		node = new DefaultComputingNode(simulationManager, 10, 1, 100, 100);
	}

	@Test
	void linksDefaultToTheNullNetworkLink() {
		assertSame(NetworkLink.NULL, node.getCurrentLink(LinkOrientation.UP_LINK));
		assertSame(NetworkLink.NULL, node.getCurrentLink(LinkOrientation.DOWN_LINK));
		assertSame(NetworkLink.NULL, node.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));
	}

	@Test
	void eachOrientationStoresItsLink() {
		NetworkLink up = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.WAN);
		NetworkLink down = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.WAN);
		NetworkLink deviceToDevice = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.LAN);

		node.setCurrentLink(up, LinkOrientation.UP_LINK);
		assertSame(up, node.getCurrentLink(LinkOrientation.UP_LINK));

		node.setCurrentLink(deviceToDevice, LinkOrientation.DEVICE_TO_DEVICE);
		assertSame(deviceToDevice, node.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));

		// Both the up and down accessors currently resolve to the up link.
		node.setCurrentLink(down, LinkOrientation.DOWN_LINK);
		assertSame(down, node.getCurrentLink(LinkOrientation.DOWN_LINK));
		assertSame(down, node.getCurrentLink(LinkOrientation.UP_LINK));
	}

	@Test
	void nullOrientationIsRejected() {
		assertThrows(NullPointerException.class, () -> node.getCurrentLink(null));
		assertThrows(NullPointerException.class,
				() -> node.setCurrentLink(new NetworkLink(), null));
	}

	@Test
	void clearingALinkMakesTheAccessorReturnNull() {
		NetworkLink link = new NetworkLink(node, node, simulationManager, NetworkLinkTypes.LAN);
		node.setCurrentLink(link, LinkOrientation.DEVICE_TO_DEVICE);
		node.setCurrentLink(null, LinkOrientation.DEVICE_TO_DEVICE);
		assertNull(node.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));
	}

	@Test
	void unmappedEnumValuesAreRejectedByBothSettersAndGetters() throws Exception {
		// The default switch branches cannot be reached with a valid enum constant:
		// all three constants are mapped. Temporarily map one constant to the default
		// table entry to exercise the defensive IllegalArgumentException paths.
		Class<?> switchHolder = Class.forName(NetworkingNode.class.getName() + "$1");
		Field switchMap = null;
		for (Field candidate : switchHolder.getDeclaredFields()) {
			if (candidate.getName().startsWith("$SwitchMap")) {
				switchMap = candidate;
			}
		}
		assertTrue(switchMap != null, "synthetic enum switch map not found");
		switchMap.setAccessible(true);
		int[] mappings = (int[]) switchMap.get(null);
		int index = LinkOrientation.DEVICE_TO_DEVICE.ordinal();
		int original = mappings[index];
		mappings[index] = 0;
		try {
			IllegalArgumentException getException = assertThrows(IllegalArgumentException.class,
					() -> node.getCurrentLink(LinkOrientation.DEVICE_TO_DEVICE));
			assertTrue(getException.getMessage().contains("Invalid link type"));

			IllegalArgumentException setException = assertThrows(IllegalArgumentException.class,
					() -> node.setCurrentLink(new NetworkLink(), LinkOrientation.DEVICE_TO_DEVICE));
			assertTrue(setException.getMessage().contains("Invalid link type"));
		} finally {
			mappings[index] = original;
		}
	}
}
