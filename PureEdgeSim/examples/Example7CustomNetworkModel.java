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
 *     @author Mechalikh
 **/
package examples;

import com.mechalikh.pureedgesim.network.TransferProgress;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;

public class Example7CustomNetworkModel extends DefaultNetworkModel {

	private static final int MAX_NUMBER_OF_REPLICAS = 8;

	public Example7CustomNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	protected void transferFinished(TransferProgress transfer) {
		if (transfer.getTransferType() == TransferProgress.Type.TASK && SimulationParameters.ENABLE_REGISTRY
				&& "CACHE".equals(SimulationParameters.registry_mode)) {
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
			if (edgeDevice.getAvailableStorage() >= toMegaBytes(task.getContainerSize())) {
				saveImage(edgeDevice, task);
			} else {
				// while the storage is not enough
				freeStorage(edgeDevice, task);
			}
			// if the memory is enough
			if (edgeDevice.getAvailableStorage() >= toMegaBytes(task.getContainerSize())) {
				saveImage(edgeDevice, task);
			}
		}
	}

	private boolean canKeepReplica(Example7CachingDevice edgeDevice, Task task) {
		return ("CACHE".equals(SimulationParameters.registry_mode)
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
		return (edgeDevice.getAvailableStorage() < toMegaBytes(task.getContainerSize())
				&& edgeDevice.getTotalStorage() >= toMegaBytes(task.getContainerSize()));
	}

	private void saveImage(Example7CachingDevice edgeDevice, Task task) {
		edgeDevice.setAvailableStorage(edgeDevice.getAvailableStorage() - toMegaBytes(task.getContainerSize()));
		edgeDevice.cache.add(task);
		int[] array = new int[2];
		array[0] = task.getApplicationID();
		array[1] = task.getEdgeDevice().getId();
		((Example7CachingDevice) edgeDevice.getOrchestrator()).Remotecache.add(array);
	}

	private double toMegaBytes(long bits) {
		return bits / 8000000;
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
			scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, task);
		} else {
			int from = ((Example7CachingDevice) task.getEdgeDevice().getOrchestrator())
					.findReplica(task.getApplicationID());
			// The IDs are shifted by 2 (to avoid the cloud data center and the edge data
			// centers) 
			task.setRegistry(simulationManager.getDataCentersManager().getNodesList().get(from));
			// Pull container from another edge device
			scheduleNow(this, NetworkModel.DOWNLOAD_CONTAINER, task);
		}

	}

}
