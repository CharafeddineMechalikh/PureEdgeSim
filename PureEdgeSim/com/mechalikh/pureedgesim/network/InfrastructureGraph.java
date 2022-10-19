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
package com.mechalikh.pureedgesim.network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;

/**
 * Infrastructure topology connecting compute nodes with network links in a
 * weighted graph.
 *
 * The implementation is a bit messy because it complies with the outdated
 * CloudSim NetworkTopology interface.
 */
public class InfrastructureGraph {

	public static final InfrastructureGraph NULL = new InfrastructureGraph() {
	};

	protected DirectedWeightedMultigraph<ComputingNode, NetworkLink> graph;

	protected Map<Long, GraphPath<ComputingNode, NetworkLink>> pathsMap = new LinkedHashMap<>();

	public InfrastructureGraph() {
		graph = new DirectedWeightedMultigraph<>(NetworkLink.class);
	}

	public void addLink(NetworkLink networkLink) {
		graph.addVertex(networkLink.getSrc());
		graph.addVertex(networkLink.getDst());
		graph.addEdge(networkLink.getSrc(), networkLink.getDst(), networkLink);
		graph.setEdgeWeight(networkLink, networkLink.getLatency()); // in jgrapht all access to the weight of an edge
																	// must go through the graph interface
	}

	public double getDelay(final ComputingNode computingNode, final ComputingNode computingNode2) {
		// The Network interfaces in CloudSimPlus are not very good, the try catch
		// should not be necessary
		FloydWarshallShortestPaths<ComputingNode, NetworkLink> algorithm = new FloydWarshallShortestPaths<>(graph);
		try {
			return algorithm.getPathWeight(computingNode, computingNode2); // Returns Double.POSITIVE_INFINITY if no
																			// path exists
		} catch (IllegalArgumentException e) {
			return Double.POSITIVE_INFINITY;
		}
	}

	public void removeLink(ComputingNode src, ComputingNode dest) {
		graph.removeEdge(src, dest);
	}

	public void removeLink(NetworkLink link) {
		graph.removeEdge(link);
	}

	public GraphPath<ComputingNode, NetworkLink> getPath(final ComputingNode computingNode, final ComputingNode node) {
		DijkstraShortestPath<ComputingNode, NetworkLink> algorithm = new DijkstraShortestPath<>(graph);
		try {
			return assertNotNull(algorithm.getPath(computingNode, node));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Cannot get a path from node " + computingNode.getId() + " (Class: "
					+ computingNode.getClass().getSimpleName() + " type: " + computingNode.getType() + ") to "
					+ node.getId() + " (Class: " + node.getClass().getSimpleName() + " type: " + node.getType()
					+ "). Possible solutions : add links in edge_datacenter.xml file (see examples), Or check your topology creator class.");
		}
	}

	protected GraphPath<ComputingNode, NetworkLink> assertNotNull(GraphPath<ComputingNode, NetworkLink> path) {
		if (null == path)
			throw new IllegalArgumentException();
		return path;
	}

	public DirectedWeightedMultigraph<ComputingNode, NetworkLink> getGraph() {
		return graph;
	}

	public List<NetworkLinkWifi> getWifiLinks() {
		return getEdgeByType(NetworkLinkWifi.class);
	}

	public List<NetworkLinkEthernet> getEthernetLinks() {
		return getEdgeByType(NetworkLinkEthernet.class);
	}

	public List<NetworkLinkCellular> get4gLinks() {
		return getEdgeByType(NetworkLinkCellular.class);
	}

	public List<NetworkLinkWanUp> getWanUpLinks() {
		return getEdgeByType(NetworkLinkWanUp.class);
	}

	public List<NetworkLinkWanDown> getWanDownLinks() {
		return getEdgeByType(NetworkLinkWanDown.class);
	}

	public List<NetworkLink> getWanLinks() {
		return getEdgeByType(NetworkLinkTypes.WAN);
	}

	public List<NetworkLink> getManLinks() {
		return getEdgeByType(NetworkLinkTypes.MAN);
	}

	public List<NetworkLink> getLanLinks() {
		return getEdgeByType(NetworkLinkTypes.LAN);
	}

	protected <T extends NetworkLink> List<T> getEdgeByType(Class<T> x) {
		return getGraph().edgeSet().stream().filter(x::isInstance).map(x::cast).collect(Collectors.toList());
	}

	protected List<NetworkLink> getEdgeByType(NetworkLinkTypes type) {
		return getGraph().edgeSet().stream().filter(n -> n.getType().equals(type)).collect(Collectors.toList());
	}

	public Map<Long, GraphPath<ComputingNode, NetworkLink>> getPathsMap() {
		return pathsMap;
	}

	public void savePathsToMap(List<ComputingNode> list) {
		// Save shortest paths in map to use them later
		for (int i = 0; i < list.size(); i++) {
			ComputingNode from = list.get(i);
			for (int j = 0; j < list.size(); j++) { 
				ComputingNode to = list.get(j);
				pathsMap.put(getUniqueId(from.getId(), to.getId()), this.getPath(from, to));
			}
		}
	}

	// Get a unique id using Cantor pairing function
	public long getUniqueId(int a, int b) {
		return (long) ((1 / 2.0) * (a + b) * (a + b + 1)) + b;
	}
}
