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
package examples;

import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/** You must read this to understand 
 * This is a simple example showing how to launch simulation using a custom
 * network model. The CustomNetworkModel.java is located under the examples/f
 * older. As you can see, this class extends the MainApplication class provided
 * by PureEdgeSim, which is required for this example to work.
 * 
 * In this example, we will implement the cooperative caching algorithm
 * presented in the following paper: Mechalikh, C., Taktak, H., Moussa, F.:
 * Towards a Scalable and QoS-Aware Load Balancing Platform for Edge Computing
 * Environments. The 2019 International Conference on High Performance Computing
 * & Simulation (2019) 684-691
 *
 * Before running this example you need to
 * 
 * 1/ enable the registry in the simulation parameters file by setting
 * enable_registry=true registry_mode=CACHE
 * 
 * 2/ enable orchestrators in the simulation parameters file by setting
 * enable_orchestrators=true deploy_orchestrator=CLUSTER
 * 
 * you can then compare between registry_mode=CLOUD in which the containers are
 * downloaded from the cloud everytime and registry_mode=CACHE in which the
 * frequently needed containers are cached in edge devices. Same for
 * deploy_orchestrator=CLUSTER and deploy_orchestrator=CLOUD. where the
 * orchestrators are deployed on the cluster heads or on the cloud.
 * 
 * Try to use the MIST_ONLY architecture, in order to see clearly the difference
 * in WAN usage (no tasks offloading to the cloud, so the wan will only be used
 * by containers). To see the effect, try with 60 minutes simulation time.
 * 
 * You will see that the cooperative caching algorithm decreases the WAN usage
 * remarkably.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 2.3
 */
public class Example7CustomNetworkModel extends DefaultNetworkModel {

	private static final int MAX_NUMBER_OF_REPLICAS = 8;

	public Example7CustomNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	protected void transferFinished(TransferProgress transfer) {
		if (transfer.getTransferType() == TransferProgress.Type.TASK && SimulationParameters.enableRegistry
				&& "CACHE".equals(SimulationParameters.registryMode)) {
			// the offloading request has been received, now pull the container in order to
			// execute the task
			pullContainer(transfer.getTask());

			updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getEdgeDevice(),
					transfer.getTask().getOffloadingDestination());

		} else if (transfer.getTransferType() == TransferProgress.Type.CONTAINER) {

			// the container has been downloaded, keep it in cache
			keepReplica(transfer.getTask());

			// execute the task
			containerDownloadFinished(transfer);

			updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getRegistry(),
					transfer.getTask().getEdgeDevice());

		} else // use the default method to handle everything else
			super.transferFinished(transfer);
	}

	private void keepReplica(Task task) {

		// Check if there are enough replicas before keeping a new one
		Example7CachingDevice edgeDevice = (Example7CachingDevice) task.getEdgeDevice();
		if (canKeepReplica(edgeDevice, task)) {
			// bits to MBytes
			if (edgeDevice.getAvailableStorage() >= task.getContainerSizeInMBytes()) {
				saveImage(edgeDevice, task);
			} else {
				// while the storage is not enough
				freeStorage(edgeDevice, task);
			}
			// if the memory is enough
			if (edgeDevice.getAvailableStorage() >= task.getContainerSizeInMBytes()) {
				saveImage(edgeDevice, task);
			}
		}
	}

	private boolean canKeepReplica(Example7CachingDevice edgeDevice, Task task) {
		return ("CACHE".equals(SimulationParameters.registryMode)
				&& ((Example7CachingDevice) edgeDevice.getOrchestrator())
						.countContainer(task.getApplicationID()) < MAX_NUMBER_OF_REPLICAS);
	}

	private void freeStorage(Example7CachingDevice edgeDevice, Task task) {
		// while the available storage is not enough
		double min = 0;
		while (storageIsNotEnough(edgeDevice, task)) {
			min = edgeDevice.getMinContainerCost();
			if (edgeDevice.getCost(task) <= min || min == -1) {
				// delete the app with the highest cost
				edgeDevice.deleteMinAapp();
			} else {
				break;
			}
		}
	}

	private boolean storageIsNotEnough(Example7CachingDevice edgeDevice, Task task) {
		return (edgeDevice.getAvailableStorage() < task.getContainerSizeInMBytes()
				&& edgeDevice.getTotalStorage() >= task.getContainerSizeInMBytes());
	}

	private void saveImage(Example7CachingDevice edgeDevice, Task task) {
		edgeDevice.setAvailableStorage(edgeDevice.getAvailableStorage() - task.getContainerSizeInMBytes());
		edgeDevice.cache.add(task);
		int[] array = new int[2];
		array[0] = task.getApplicationID();
		array[1] = task.getEdgeDevice().getId();
		((Example7CachingDevice) edgeDevice.getOrchestrator()).Remotecache.add(array);
	}

	private void pullContainer(Task task) {
		if (!((Example7CachingDevice) task.getEdgeDevice().getOrchestrator())
				.hasRemoteContainer(task.getApplicationID())) {
			// No replica found
			scheduleNow(this, NetworkModel.DOWNLOAD_CONTAINER, task);
		} else { // replica found
			pullFromCache(task);
		}
	}

	private void pullFromCache(Task task) {

		((Example7CachingDevice) task.getOrchestrator()).addRequest(task);
		if (((Example7CachingDevice) task.getOffloadingDestination()).hasContainer(task.getApplicationID())
				|| ((Example7CachingDevice) task.getOffloadingDestination()).getType() == TYPES.CLOUD) {
			// This device has a replica in its cache, so execute a task directly
			scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, task);
		} else {
			int from = ((Example7CachingDevice) task.getEdgeDevice().getOrchestrator())
					.findReplica(task.getApplicationID());
			// The IDs are shifted by 2 (to avoid the cloud data center and the edge data
			// centers)
			task.setRegistry(
					simulationManager.getDataCentersManager().getComputingNodesGenerator().getAllNodesList().get(from));
			// Pull container from another edge device
			scheduleNow(this, NetworkModel.DOWNLOAD_CONTAINER, task);
		}

	}

}
