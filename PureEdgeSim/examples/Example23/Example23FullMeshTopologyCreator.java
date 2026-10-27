package examples;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;
import com.mechalikh.pureedgesim.network.NetworkLinkMan;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Topology creator that completes the edge backbone into a full mesh.
 * <p>
 * The topology file only declares the links that exist, which is often a ring
 * or a tree to keep the number of links low. This creator keeps that topology,
 * then adds a MAN link in both directions between every pair of edge data
 * centers that is not already directly connected. Every edge server can then
 * reach every other one in a single hop, which shortens the paths used when a
 * device is served by a remote data center and gives the backbone a redundant
 * route.
 *
 * @since PureEdgeSim 6.0
 */
public class Example23FullMeshTopologyCreator extends DefaultTopologyCreator {

	/**
	 * Creates a topology creator for the given simulation.
	 *
	 * @param simulationManager       the simulation manager
	 * @param computingNodesGenerator the computing nodes generator
	 */
	public Example23FullMeshTopologyCreator(SimulationManager simulationManager,
			ComputingNodesGenerator computingNodesGenerator) {
		super(simulationManager, computingNodesGenerator);
	}

	/**
	 * Builds the default topology, then completes the edge backbone and caches the
	 * shortest paths again so that the new links are taken into account.
	 */
	@Override
	public void generateTopologyGraph() {
		super.generateTopologyGraph();
		int added = connectAllPairs(infrastructureTopology, simulationManager,
				computingNodesGenerator.getEdgeOnlyList());
		infrastructureTopology.savePathsToMap(computingNodesGenerator.getEdgeAndCloudList());
		getSimulationManager().getSimulationLogger().print(
				"%s - Added %d directed MAN links to complete the full mesh of %d edge data centers",
				getClass().getSimpleName(), added, computingNodesGenerator.getEdgeOnlyList().size());
	}

	/**
	 * Connects every pair of the given nodes with a MAN link in both directions,
	 * skipping the directions that already exist.
	 *
	 * @param topology          the infrastructure graph to complete
	 * @param simulationManager the simulation manager used to create the links
	 * @param nodes             the nodes that must be fully interconnected
	 * @return the number of directed links added
	 */
	static int connectAllPairs(InfrastructureGraph topology, SimulationManager simulationManager,
			List<ComputingNode> nodes) {
		Set<Long> linked = new HashSet<>();
		for (NetworkLink link : topology.getManLinks())
			linked.add(key(link.getSrc().getId(), link.getDst().getId()));

		int added = 0;
		for (int i = 0; i < nodes.size(); i++)
			for (int j = i + 1; j < nodes.size(); j++) {
				ComputingNode from = nodes.get(i);
				ComputingNode to = nodes.get(j);
				added += connectDirection(topology, simulationManager, linked, from, to);
				added += connectDirection(topology, simulationManager, linked, to, from);
			}
		return added;
	}

	/**
	 * Adds a directed MAN link if that direction does not exist yet.
	 *
	 * @return 1 when a link was added, 0 otherwise
	 */
	private static int connectDirection(InfrastructureGraph topology, SimulationManager simulationManager,
			Set<Long> linked, ComputingNode from, ComputingNode to) {
		if (!linked.add(key(from.getId(), to.getId())))
			return 0;
		topology.addLink(new NetworkLinkMan(from, to, simulationManager, NetworkLinkTypes.MAN));
		return 1;
	}

	/**
	 * Builds a unique identifier for an ordered pair of node IDs.
	 *
	 * @param from the source node ID
	 * @param to   the destination node ID
	 * @return the pair identifier
	 */
	private static long key(int from, int to) {
		return ((long) from << 32) | (to & 0xFFFFFFFFL);
	}
}
