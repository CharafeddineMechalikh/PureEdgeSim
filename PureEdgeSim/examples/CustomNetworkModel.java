package examples;

import com.mechalikh.pureedgesim.Network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.Network.FileTransferProgress;
import com.mechalikh.pureedgesim.Network.NetworkModel;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters.TYPES;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;

public class CustomNetworkModel extends DefaultNetworkModel {

	private static final int MAX_NUMBER_OF_REPLICAS = 5;

	public CustomNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	protected void transferFinished(FileTransferProgress transfer) {
		if (transfer.getTransferType() == FileTransferProgress.Type.TASK && simulationParameters.ENABLE_REGISTRY
				&& "CACHE".equals(simulationParameters.registry_mode)) {
			// the offloading request has been received, now pull the container in order to
			// execute the task
			pullContainer(transfer.getTask());

			transfer.getTask().setReceptionTime(simulationManager.getSimulation().clock());
			updateEnergyConsumption(transfer, "Destination");

			// Update logger parametersa
			simulationManager.getSimulationLogger().updateNetworkUsage(transfer);

			// Delete the transfer from the queue
			transferProgressList.remove(transfer);
			
		} else if (transfer.getTransferType() == FileTransferProgress.Type.CONTAINER) {
			// the container has been downloaded, keep it in cache
			keepReplica(transfer.getTask());

			// Update logger parameters
			simulationManager.getSimulationLogger().updateNetworkUsage(transfer);

			transfer.getTask().setReceptionTime(simulationManager.getSimulation().clock());

			// execute the task
			containerDownloadFinished(transfer);

			updateEnergyConsumption(transfer, "Container");
			
			// Delete the transfer from the queue
			transferProgressList.remove(transfer);

		} else // use the default method to handle everything else
			super.transferFinished(transfer);
	}

	private void keepReplica(Task task) {

		// Check if there are enough replicas before keeping a new one
		CustomEdgeDevice edgeDevice = (CustomEdgeDevice) task.getEdgeDevice();
		if (simulationParameters.registry_mode.equals("CACHE") && ((CustomEdgeDevice) edgeDevice.getOrchestrator())
				.countContainer(task.getApplicationID()) < MAX_NUMBER_OF_REPLICAS) {
			if (edgeDevice.getAvailableMemory() > task.getContainerSize()) {

				edgeDevice.setAvailableMemory(edgeDevice.getAvailableMemory() - task.getContainerSize());
				edgeDevice.cache.add(task);
				double[] array = new double[2];
				array[0] = task.getApplicationID();
				array[1] = task.getEdgeDevice().getId();
				((CustomEdgeDevice) edgeDevice.getOrchestrator()).Remotecache.add(array);
			} else
				// while the memory is not enough
				while (edgeDevice.getAvailableMemory() < task.getContainerSize()
						&& edgeDevice.getStorageMemory() > task.getContainerSize()) {

					double min = edgeDevice.getMinContainerCost();
					if (edgeDevice.getCost(task) < min || min == -1) {
						// delete the app with the highest cost
						edgeDevice.deleteMinAapp();
					} else {
						break;
					}
				}
			// if the memory is enough
			if (edgeDevice.getStorageMemory() > task.getContainerSize()) {
				edgeDevice.setAvailableMemory(edgeDevice.getAvailableMemory() - task.getContainerSize());
				edgeDevice.cache.add(task);
				double[] array = new double[2];
				array[0] = task.getApplicationID();
				array[1] = task.getEdgeDevice().getId();
				((CustomEdgeDevice) edgeDevice.getOrchestrator()).Remotecache.add(array);
			}
		}
	}

	private void pullContainer(Task task) {
		if (!((CustomEdgeDevice) task.getEdgeDevice().getOrchestrator()).hasRemoteContainer(task.getApplicationID())) {
			// No replica found
			scheduleNow(this, NetworkModel.DOWNLOAD_CONTAINER, task);
		} else {

			if (((CustomEdgeDevice) task.getVm().getHost().getDatacenter()).hasContainer(task.getApplicationID())
					|| ((CustomEdgeDevice) task.getVm().getHost().getDatacenter()).getType() == TYPES.CLOUD) {
				// This device has a replica in its cache, so execute a task directly
				scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, task);
			} else {
				double from = ((CustomEdgeDevice) task.getEdgeDevice().getOrchestrator())
						.findReplica(task.getApplicationID());
				task.setRegistry(simulationManager.getServersManager().getDatacenterList().get((int) from - 3));
				// Pull container from another edge device
				scheduleNow(this, NetworkModel.DOWNLOAD_CONTAINER, task);
			}

		}
	}

}
