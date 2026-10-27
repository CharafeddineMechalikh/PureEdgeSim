package com.mechalikh.pureedgesim.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

class NetworkLinkSubclassesTest extends AbstractTest {

	private static final double WIFI_DEVICE_TX = 2;
	private static final double WIFI_DEVICE_RX = 3;
	private static final double WIFI_AP_TX = 5;
	private static final double WIFI_AP_RX = 7;
	private static final double WIFI_WAN = 11;
	private static final double MAN_ENERGY = 13;
	private static final double ETHERNET_ENERGY = 17;
	private static final double CELLULAR_DEVICE_TX = 19;
	private static final double CELLULAR_DEVICE_RX = 23;
	private static final double CELLULAR_BS_UP = 29;
	private static final double CELLULAR_BS_DOWN = 31;

	private SimulationManager simulationManager;
	private ComputingNode src;
	private ComputingNode dst;

	@BeforeEach
	void setUp() {
		simulationManager = NetworkTestSupport.newSimulationManager();
		src = NetworkTestSupport.node(1, SimulationParameters.TYPES.EDGE_DEVICE);
		dst = NetworkTestSupport.node(2, SimulationParameters.TYPES.EDGE_DATACENTER);
		SimulationParameters.wifiDeviceTransmissionWattHourPerBit = WIFI_DEVICE_TX;
		SimulationParameters.wifiDeviceReceptionWattHourPerBit = WIFI_DEVICE_RX;
		SimulationParameters.wifiAccessPointTransmissionWattHourPerBit = WIFI_AP_TX;
		SimulationParameters.wifiAccessPointReceptionWattHourPerBit = WIFI_AP_RX;
		SimulationParameters.wanWattHourPerBit = WIFI_WAN;
		SimulationParameters.manWattHourPerBit = MAN_ENERGY;
		SimulationParameters.ethernetWattHourPerBit = ETHERNET_ENERGY;
		SimulationParameters.cellularDeviceTransmissionWattHourPerBit = CELLULAR_DEVICE_TX;
		SimulationParameters.cellularDeviceReceptionWattHourPerBit = CELLULAR_DEVICE_RX;
		SimulationParameters.cellularBaseStationWattHourPerBitUpLink = CELLULAR_BS_UP;
		SimulationParameters.cellularBaseStationWattHourPerBitDownLink = CELLULAR_BS_DOWN;
	}

	@Test
	void wifiUpUsesWifiValuesForLan() {
		NetworkLinkWifiUp link = new NetworkLinkWifiUp(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN);
		assertSame(src, link.getSrc());
		assertSame(dst, link.getDst());
		assertEquals(NetworkLink.NetworkLinkTypes.LAN, link.getType());
		assertEquals(SimulationParameters.wifiBandwidthBitsPerSecond, link.getBandwidth(1));
		assertEquals(SimulationParameters.wifiLatency, link.getLatency());
		assertEquals(WIFI_DEVICE_TX + WIFI_AP_RX, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void wifiUpAddsWanEnergyAndLimitsBandwidthForWan() {
		NetworkLinkWifiUp link = new NetworkLinkWifiUp(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.WAN);
		assertEquals(Math.min(SimulationParameters.wanBandwidthBitsPerSecond,
				SimulationParameters.wifiBandwidthBitsPerSecond), link.getBandwidth(1));
		assertEquals(SimulationParameters.wanLatency, link.getLatency());
		assertEquals(WIFI_DEVICE_TX + WIFI_AP_RX + WIFI_WAN, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void wifiDownUsesWifiValuesForLan() {
		NetworkLinkWifiDown link = new NetworkLinkWifiDown(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN);
		assertEquals(SimulationParameters.wifiBandwidthBitsPerSecond, link.getBandwidth(1));
		assertEquals(SimulationParameters.wifiLatency, link.getLatency());
		assertEquals(WIFI_AP_TX + WIFI_DEVICE_RX, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void wifiDownAddsWanEnergyAndLatencyForWan() {
		NetworkLinkWifiDown link = new NetworkLinkWifiDown(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.WAN);
		assertEquals(SimulationParameters.wanLatency, link.getLatency());
		assertEquals(WIFI_AP_TX + WIFI_DEVICE_RX + WIFI_WAN, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void wifiDeviceToDeviceUsesDeviceEnergyModel() {
		NetworkLinkWifiDeviceToDevice link = new NetworkLinkWifiDeviceToDevice(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN);
		assertEquals(SimulationParameters.wifiBandwidthBitsPerSecond, link.getBandwidth(1));
		assertEquals(SimulationParameters.wifiLatency, link.getLatency());
		assertEquals(WIFI_DEVICE_TX + WIFI_DEVICE_RX, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void wanUpAndDownUseWanValues() {
		NetworkLinkWanUp up = new NetworkLinkWanUp(src, dst, simulationManager, NetworkLink.NetworkLinkTypes.WAN);
		NetworkLinkWanDown down = new NetworkLinkWanDown(dst, src, simulationManager,
				NetworkLink.NetworkLinkTypes.WAN);
		assertEquals(SimulationParameters.wanBandwidthBitsPerSecond, up.getBandwidth(1));
		assertEquals(SimulationParameters.wanLatency, up.getLatency());
		assertEquals(WIFI_WAN, up.getEnergyModel().getEnergyPerBit());
		assertEquals(SimulationParameters.wanBandwidthBitsPerSecond, down.getBandwidth(1));
		assertEquals(SimulationParameters.wanLatency, down.getLatency());
		assertEquals(WIFI_WAN, down.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void manLinkUsesManValues() {
		NetworkLinkMan link = new NetworkLinkMan(src, dst, simulationManager, NetworkLink.NetworkLinkTypes.MAN);
		assertEquals(SimulationParameters.manBandwidthBitsPerSecond, link.getBandwidth(1));
		assertEquals(SimulationParameters.manLatency, link.getLatency());
		assertEquals(MAN_ENERGY, link.getEnergyModel().getEnergyPerBit());
		assertEquals(NetworkLink.NetworkLinkTypes.MAN, link.getType());
	}

	@Test
	void ethernetLinkUsesEthernetValuesForLan() {
		NetworkLinkEthernet link = new NetworkLinkEthernet(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN);
		assertEquals(SimulationParameters.ethernetBandwidthBitsPerSecond, link.getBandwidth(1));
		assertEquals(SimulationParameters.ethernetLatency, link.getLatency());
		assertEquals(ETHERNET_ENERGY, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void ethernetLinkAddsWanValuesForWan() {
		NetworkLinkEthernet link = new NetworkLinkEthernet(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.WAN);
		assertEquals(Math.min(SimulationParameters.wanBandwidthBitsPerSecond,
				SimulationParameters.ethernetBandwidthBitsPerSecond), link.getBandwidth(1));
		assertEquals(SimulationParameters.wanLatency, link.getLatency());
		assertEquals(ETHERNET_ENERGY + WIFI_WAN, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void cellularUpUsesCellularValuesForLan() {
		NetworkLinkCellularUp link = new NetworkLinkCellularUp(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN);
		assertEquals(SimulationParameters.cellularBandwidthBitsPerSecond, link.getBandwidth(1));
		assertEquals(SimulationParameters.cellularLatency, link.getLatency());
		assertEquals(CELLULAR_DEVICE_TX + CELLULAR_BS_UP, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void cellularUpAddsWanValuesForWan() {
		NetworkLinkCellularUp link = new NetworkLinkCellularUp(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.WAN);
		assertEquals(Math.min(SimulationParameters.wanBandwidthBitsPerSecond,
				SimulationParameters.cellularBandwidthBitsPerSecond), link.getBandwidth(1));
		assertEquals(SimulationParameters.wanLatency, link.getLatency());
		assertEquals(CELLULAR_DEVICE_TX + CELLULAR_BS_UP + WIFI_WAN, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void cellularDownUsesCellularValuesForLan() {
		NetworkLinkCellularDown link = new NetworkLinkCellularDown(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.LAN);
		assertEquals(SimulationParameters.cellularBandwidthBitsPerSecond, link.getBandwidth(1));
		assertEquals(SimulationParameters.cellularLatency, link.getLatency());
		assertEquals(CELLULAR_DEVICE_RX + CELLULAR_BS_DOWN, link.getEnergyModel().getEnergyPerBit());
	}

	@Test
	void cellularDownAddsWanValuesForWan() {
		NetworkLinkCellularDown link = new NetworkLinkCellularDown(src, dst, simulationManager,
				NetworkLink.NetworkLinkTypes.WAN);
		assertEquals(SimulationParameters.wanLatency, link.getLatency());
		assertEquals(CELLULAR_DEVICE_RX + CELLULAR_BS_DOWN + WIFI_WAN, link.getEnergyModel().getEnergyPerBit());
	}
}
