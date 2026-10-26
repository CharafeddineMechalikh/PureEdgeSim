package examples;

import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * A computing node that keeps the application containers it has downloaded in a
 * storage-aware cache.
 * <p>
 * Every node reserves at most {@value #CACHE_CAPACITY_MB} megabytes of its
 * storage for containers, and never more than the storage it has. The cache is
 * filled by {@link Example21ContainerCacheNetworkModel} and consulted before a
 * container is downloaded again.
 *
 * @since PureEdgeSim 6.0
 */
public class Example21CacheDevice extends DefaultComputingNode {

	/** Storage that each node dedicates to cached containers, in megabytes. */
	public static final double CACHE_CAPACITY_MB = 3;

	private final Example21ContainerCache containerCache;

	/**
	 * Creates a node with a container cache sized from its storage.
	 *
	 * @param simulationManager the simulation manager.
	 * @param mipsPerformance   capacity of one CPU core, in million instructions
	 *                          per second.
	 * @param numberOfCPUCores  number of CPU cores.
	 * @param storage           total storage, in megabytes.
	 * @param ram               total RAM, in megabytes.
	 */
	public Example21CacheDevice(SimulationManager simulationManager, double mipsPerformance, int numberOfCPUCores,
			double storage, double ram) {
		super(simulationManager, mipsPerformance, numberOfCPUCores, storage, ram);
		containerCache = new Example21ContainerCache(Math.min(CACHE_CAPACITY_MB, storage));
	}

	/** The container cache of this node. */
	public Example21ContainerCache getContainerCache() {
		return containerCache;
	}
}
