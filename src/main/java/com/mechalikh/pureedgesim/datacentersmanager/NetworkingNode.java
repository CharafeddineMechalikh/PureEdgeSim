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

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Base class for compute nodes that hold network links. Edge devices use the
 * current links to follow the closest edge data center as they move.
 */
public abstract class NetworkingNode extends AbstractNode  {
	/** Link used to reach the current edge data center. */
	protected NetworkLink currentUpLink = NetworkLink.NULL;
	/** Reserved downlink field; the default topology uses the uplink for both directions. */
	protected NetworkLink currentDownLink = NetworkLink.NULL;
	/** Direct device-to-device WiFi link. */
	protected NetworkLink currentDeviceToDeviceWifiLink = NetworkLink.NULL;
	List<ComputingNode> vertexList = new ArrayList<>();
	List<NetworkLink> edgeList = new ArrayList<>();

	/**
	 * Creates a networking node.
	 *
	 * @param simulationManager the simulation manager.
	 */
	protected NetworkingNode(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Returns the current network link of the specified type. For example, if the
	 * type is {@link LinkOrientation#UP_LINK}, it returns the link that is used currently
	 * to send data to the cloud or edge data centers. Used only when the type of
	 * this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}. The default
	 * topology represents the uplink and downlink with the same WiFi link, so both
	 * orientations return that link.
	 * 
	 * @param linkType the type of the link to retrieve
	 * @return the current network link of the specified type
	 * @see #setCurrentLink(NetworkLink,LinkOrientation)
	 */
	public NetworkLink getCurrentLink(LinkOrientation linkType) {
		switch (linkType) {
		case UP_LINK:
			return this.currentUpLink;
		case DOWN_LINK:
			return this.currentUpLink;
		case DEVICE_TO_DEVICE:
			return this.currentDeviceToDeviceWifiLink;
		default:
			throw new IllegalArgumentException("Invalid link type: " + linkType);
		}
	}

	/**
	 * Sets the network link that is used currently to send data to the cloud or
	 * edge data centers.Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}. The uplink and downlink
	 * orientations both update the same current link.
	 * 
	 * @param link     the network link.
	 * @param linkType whether the link is used for the uplink or the downlink.
	 * @see #getCurrentLink(LinkOrientation)
	 */
	public void setCurrentLink(NetworkLink link, LinkOrientation linkType) {
		switch (linkType) {
		case UP_LINK:
			this.currentUpLink = link;
			break;
		case DOWN_LINK:
			this.currentUpLink = link;
			break;
		case DEVICE_TO_DEVICE:
			this.currentDeviceToDeviceWifiLink = link;
			break;
		default:
			throw new IllegalArgumentException("Invalid link type: " + linkType);
		}
	}

}
