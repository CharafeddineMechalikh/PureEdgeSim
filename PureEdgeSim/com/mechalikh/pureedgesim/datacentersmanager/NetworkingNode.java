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

import com.mechalikh.pureedgesim.network.NetworkLink; 
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public abstract class NetworkingNode extends AbstractNode {
	protected NetworkLink currentUpLink = NetworkLink.NULL;
	protected NetworkLink currentDownLink = NetworkLink.NULL;
	protected NetworkLink currentDeviceToDeviceWifiLink = NetworkLink.NULL; 
	List<ComputingNode> vertexList = new ArrayList<>();
	List<NetworkLink> edgeList = new ArrayList<>();
	
	protected NetworkingNode(SimulationManager simulationManager) {
		super(simulationManager);
	}

	public NetworkLink getCurrentUpLink() {
		return currentUpLink;
	}

	public void setCurrentUpLink(NetworkLink currentUpLink) {
		this.currentUpLink = currentUpLink;
	}

	public NetworkLink getCurrentDownLink() {
		return currentDownLink;
	}

	public void setCurrentDownLink(NetworkLink currentDownLink) {
		this.currentDownLink = currentDownLink;
	}

	public NetworkLink getCurrentWiFiLink() {
		return currentDeviceToDeviceWifiLink;
	}

	public void setCurrentWiFiLink(NetworkLink currentWiFiDeviceToDeviceLink) {
		this.currentDeviceToDeviceWifiLink = currentWiFiDeviceToDeviceLink;
	}
	

}
