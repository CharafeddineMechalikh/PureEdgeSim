package examples;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Serves application containers from the cache of the destination node instead
 * of downloading them from the cloud every time.
 * <p>
 * With the registry in {@code CACHE} mode, the completed execution request is
 * the point where the default model would start a container download. This
 * model first asks the destination whether it already holds that application:
 * a hit runs the task immediately, a miss downloads the container from the
 * registry exactly as before. When the download finishes, the container is
 * stored in the destination cache, which evicts the least recently used entry
 * when the storage budget is full.
 * <p>
 * The task request itself still travels to the orchestrator, so the difference
 * between {@code CACHE} and {@code CLOUD} runs is confined to the container
 * traffic that the cache avoids.
 *
 * @since PureEdgeSim 6.0
 */
public class Example21ContainerCacheNetworkModel extends DefaultNetworkModel implements OnSimulationEndListener {

	/**
	 * Creates the network model.
	 *
	 * @param simulationManager the simulation manager.
	 */
	public Example21ContainerCacheNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Tells whether a task still needs its container downloaded before it can run
	 * on the given destination.
	 * <p>
	 * The cloud owns the registry, so it never downloads a container. A node
	 * without a cache always downloads. A caching node downloads only when the
	 * lookup misses.
	 *
	 * @param destination the node that will execute the task.
	 * @param task        the task about to execute.
	 * @return true when the container must be downloaded.
	 */
	public static boolean needsContainerDownload(ComputingNode destination, Task task) {
		if (destination.getType() == TYPES.CLOUD)
			return false;
		if (!(destination instanceof Example21CacheDevice))
			return true;
		return !((Example21CacheDevice) destination).getContainerCache().lookup(task.getApplicationID());
	}

	@Override
	protected void transferFinished(TransferProgress transfer) {
		Task task = transfer.getTask();
		if (transfer.getTransferType() == TransferProgress.Type.TASK && cacheEnabled()) {
			if (needsContainerDownload(task.getOffloadingDestination(), task))
				scheduleNow(this, NetworkModel.DOWNLOAD_CONTAINER, task);
			else
				scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, task);
			updateEdgeDevicesRemainingEnergy(transfer, task.getEdgeDevice(), task.getOffloadingDestination());
		} else if (transfer.getTransferType() == TransferProgress.Type.CONTAINER) {
			if (cacheEnabled())
				keepInCache(task);
			containerDownloadFinished(transfer);
			updateEdgeDevicesRemainingEnergy(transfer, task.getRegistry(), task.getEdgeDevice());
		} else
			super.transferFinished(transfer);
	}

	private static boolean cacheEnabled() {
		return SimulationParameters.enableRegistry && "CACHE".equals(SimulationParameters.registryMode);
	}

	private static void keepInCache(Task task) {
		ComputingNode destination = task.getOffloadingDestination();
		if (destination instanceof Example21CacheDevice)
			((Example21CacheDevice) destination).getContainerCache().store(task.getApplicationID(),
					task.getContainerSizeInMBytes());
	}

	/**
	 * Prints the aggregate cache statistics of the run: hits, misses, hit ratio,
	 * evictions, refused containers, and the storage in use.
	 */
	@Override
	public void onSimulationEnd() {
		int hits = 0;
		int misses = 0;
		int evictions = 0;
		int rejected = 0;
		double used = 0;
		double capacity = 0;
		for (ComputingNode node : simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getAllNodesList()) {
			if (!(node instanceof Example21CacheDevice) || node.getType() == TYPES.CLOUD)
				continue;
			Example21ContainerCache cache = ((Example21CacheDevice) node).getContainerCache();
			hits += cache.getHits();
			misses += cache.getMisses();
			evictions += cache.getEvictions();
			rejected += cache.getRejected();
			used += cache.getUsedStorageMB();
			capacity += cache.getCapacityMB();
		}
		int requests = hits + misses;
		double ratio = requests == 0 ? 0 : 100.0 * hits / requests;
		// The iteration results are printed before this callback, so the summary goes
		// straight to the console instead of the already flushed simulation log.
		SimLog.println(
				"Example21 - container cache: %s hits, %s misses (%s %% hit ratio), %s evictions, %s refused, %s MB used of %s MB",
				hits, misses, String.format("%.2f", ratio), evictions, rejected, String.format("%.2f", used),
				String.format("%.2f", capacity));
	}
}
