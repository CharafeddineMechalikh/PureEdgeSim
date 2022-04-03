/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.datacentersmanager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;
import com.mechalikh.pureedgesim.network.NetworkLinkCellularDown;
import com.mechalikh.pureedgesim.network.NetworkLinkCellularUp;
import com.mechalikh.pureedgesim.network.NetworkLinkEthernet;
import com.mechalikh.pureedgesim.network.NetworkLinkMan;
import com.mechalikh.pureedgesim.network.NetworkLinkWanDown;
import com.mechalikh.pureedgesim.network.NetworkLinkWanUp;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiDeviceToDevice;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiDown;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiUp;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class TopologyCreator {
	private ComputingNodesGenerator computingNodesGenerator;
	private SimulationManager simulationManager;
	private InfrastructureGraph infrastructureTopology;

	public TopologyCreator(SimulationManager simulationManager, ComputingNodesGenerator computingNodesGenerator) {
		infrastructureTopology = new InfrastructureGraph();
		this.simulationManager = simulationManager;
		this.computingNodesGenerator = computingNodesGenerator;
	}

	public void generateTopologyGraph() {

		// Here we will generate our topology.
		// In edge devices.xml file, devices can connect using 4G LTE, WiFi, and
		// Ethernet.
		// Let's connect all the edge devices to the cloud.
		ComputingNode wanNode = createWanLink();

		// Now, we link each edge device with that cloud data center
		for (ComputingNode node : computingNodesGenerator.getEdgeDevicesList())
			// The link is obviously a WAN link, but can be either 4G LTE, WiFi, or
			// Ethernet, according to the edge_devices.xml file
			connect(node, wanNode, NetworkLinkTypes.WAN);

		// Now that we have linked all the devices to the cloud, let's link edge data
		// centers together (in this case the link is a MAN link).
		// To make it easy, the links are defined in edge_datacenters.xml file. We just
		// have to call the function below.
		// In this scenario (see edge_data centers.xml file). We have three data
		// centers. The first one is not peripheral, which means edge devices cannot be
		// linked to it directly. The other two are peripheral, meaning edge devices can
		// connect to them directly.
		// These data centers are interconnected (a link from 1 to 2, 2 to 3, and 3 to
		// 1)
		generateTopologyFromXmlFile();

		// At this stage we have connected all edge devices to the cloud, and connected
		// the edge data centers together. But we didn't connect any edge data center
		// with the cloud. Let's connect at least one of them. We chose the first one.
		infrastructureTopology.addLink(
				new NetworkLinkWanUp(getDataCenterByName("dc1"), wanNode, simulationManager, NetworkLinkTypes.WAN));
		infrastructureTopology.addLink(
				new NetworkLinkWanDown(wanNode, getDataCenterByName("dc1"), simulationManager, NetworkLinkTypes.WAN));

		// What remains is to link edge devices with the clousest edge data center
		for (ComputingNode node : computingNodesGenerator.getEdgeDevicesList()) {
			// Link this device with a close edge data center
			double range = SimulationParameters.EDGE_DATACENTERS_RANGE;
			ComputingNode closestDC = ComputingNode.NULL;
			for (ComputingNode edgeDC : computingNodesGenerator.getEdgeDatacenterList()) {
				if (node.getMobilityModel().distanceTo(edgeDC) <= range && edgeDC.isPeripheral()) {
					range = node.getMobilityModel().distanceTo(edgeDC);
					closestDC = edgeDC;
				}
			}
			// Notice that this link is given the LAN tag. When mobile devices change there
			// location, they will automatically connect with the closes peripheral edge
			// data center.
			connect(node, closestDC, NetworkLinkTypes.LAN);

			// Now for each device, we will create a (device to device LAN link). This link
			// will be used for peer to peer communications (from edge device to edge
			// device). We set the destination to the closest data center just for now.
			// It will be replaced by a nearby edge device when needed.
			node.setCurrentWiFiLink(
					new NetworkLinkWifiDeviceToDevice(node, closestDC, simulationManager, NetworkLinkTypes.LAN));
			infrastructureTopology.addLink(node.getCurrentWiFiLink());
		}

	}

	private ComputingNode createWanLink() {

		// To do so, first let's get the cloud data center.
		// If you have more than one data center, you will need to link them all
		ComputingNode cloud = computingNodesGenerator.getCloudDatacenterList().get(0);

		// If we want all data to be sent over the same wan network.
		if (SimulationParameters.ONE_SHARED_WAN_NETWORK) {
			// We need to create another node to link with the cloud.
			ComputingNode metroRouter = new Router(simulationManager);

			// After that, we can link it with the cloud. We select type IGNORE to avoid
			// measuring energy consumption twice.
			NetworkLinkWanUp wanUp = new NetworkLinkWanUp(metroRouter, cloud, simulationManager,
					NetworkLinkTypes.IGNORE);
			NetworkLinkWanDown wanDown = new NetworkLinkWanDown(cloud, metroRouter, simulationManager,
					NetworkLinkTypes.IGNORE);
			infrastructureTopology.addLink(wanUp);
			infrastructureTopology.addLink(wanDown);

			// To enable the real time WAN chart, and use the WAN bandwidth in orchestration
			// algorithms like in Example 8:
			simulationManager.getNetworkModel().setWanLinks(wanUp, wanDown);
			return metroRouter;
		} else
			return cloud;

	}

	private void generateTopologyFromXmlFile() {
		// Fill list with edge data centers

		try {
			InputStream serversFile = new FileInputStream(SimulationParameters.EDGE_DATACENTERS_FILE);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(serversFile);

			// Create the network topology
			NodeList networkLinks = doc.getElementsByTagName("link");
			for (int i = 0; i < networkLinks.getLength(); i++) {
				Element networkLink = (Element) networkLinks.item(i);
				createNetworkLink(networkLink);
			}
			serversFile.close();
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private void createNetworkLink(Element networkLinkElement) {
		ComputingNode dcFrom = getDataCenterByName(
				networkLinkElement.getElementsByTagName("from").item(0).getTextContent());
		ComputingNode dcTo = getDataCenterByName(
				networkLinkElement.getElementsByTagName("to").item(0).getTextContent());
		infrastructureTopology.addLink(new NetworkLinkMan(dcFrom, dcTo, simulationManager, NetworkLinkTypes.MAN));
		infrastructureTopology.addLink(new NetworkLinkMan(dcTo, dcFrom, simulationManager, NetworkLinkTypes.MAN));
	}

	private ComputingNode getDataCenterByName(String name) {
		// Check the edge data centers list 
		for (int i = 0; i < computingNodesGenerator.getEdgeDatacenterList().size(); i++)
			if (computingNodesGenerator.getEdgeDatacenterList().get(i).getName().equals(name))
				return computingNodesGenerator.getEdgeDatacenterList().get(i);

		return ComputingNode.NULL;
	}

	private void connect(ComputingNode computingNode1, ComputingNode computingNode2, NetworkLinkTypes type) {

		// If this device is connected using WiFi, then create a WiFi link
		if ("wifi".equals(computingNode1.getEnergyModel().getConnectivityType())) {
			computingNode1
					.setCurrentUpLink(new NetworkLinkWifiUp(computingNode1, computingNode2, simulationManager, type));
			infrastructureTopology.addLink(computingNode1.getCurrentUpLink());
			computingNode1.setCurrentDownLink(
					new NetworkLinkWifiDown(computingNode2, computingNode1, simulationManager, type));
			infrastructureTopology.addLink(computingNode1.getCurrentDownLink());
		} else
		// If this device is connected using cellular network, then create a cellular
		// link
		if ("cellular".equals(computingNode1.getEnergyModel().getConnectivityType())) {
			computingNode1.setCurrentUpLink(
					new NetworkLinkCellularUp(computingNode1, computingNode2, simulationManager, type));
			infrastructureTopology.addLink(computingNode1.getCurrentUpLink());
			computingNode1.setCurrentDownLink(
					new NetworkLinkCellularDown(computingNode2, computingNode1, simulationManager, type));
			infrastructureTopology.addLink(computingNode1.getCurrentDownLink());
		} else
		// If this device is connected using Ethernet, then create an Ethernet link
		if ("ethernet".equals(computingNode1.getEnergyModel().getConnectivityType())) {
			computingNode1
					.setCurrentUpLink(new NetworkLinkEthernet(computingNode1, computingNode2, simulationManager, type));
			infrastructureTopology.addLink(computingNode1.getCurrentUpLink());
			computingNode1.setCurrentDownLink(
					new NetworkLinkEthernet(computingNode2, computingNode1, simulationManager, type));
			infrastructureTopology.addLink(computingNode1.getCurrentDownLink());
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - Unknown connectivity type, check the edge_devices.xml file, available types for edge devices are: wifi, ethernet, and cellular (case sensitive)");
		}
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public InfrastructureGraph getTopology() {
		return infrastructureTopology;
	}
}
