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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
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
public class Example7CachingDevice extends Example7ClusteringDevice {

	public List<Task> cache = new ArrayList<Task>();
	public List<int[]> Remotecache = new ArrayList<int[]>();
	public Map<Integer, Integer> probability = new HashMap<Integer, Integer>();

	public Example7CachingDevice(SimulationManager simulationManager, double mipsCapacity, int numberOfPes,
			double storage, double ram) {
		super(simulationManager, mipsCapacity, numberOfPes, storage, ram);

		// Initialize probability map
		for (int i = 0; i < SimulationParameters.applicationList.size(); i++)
			probability.put(i, 0);
	}

	public boolean hasContainer(double appId) {
		for (int i = 0; i < cache.size(); i++) {
			if (cache.get(i).getApplicationID() == appId)
				return true;
		}
		return false;
	}

	public int countContainer(double appId) {
		int j = 0;
		for (int i = 0; i < Remotecache.size(); i++) {
			if (Remotecache.get(i)[0] == appId)
				j++;
		}
		return j;
	}

	public boolean hasRemoteContainer(double appId) {
		for (int i = 0; i < Remotecache.size(); i++) {
			if (Remotecache.get(i)[0] == appId)
				return true;
		}
		return false;
	}

	public int findReplica(double appId) {
		for (int i = 0; i < Remotecache.size(); i++) {
			if (Remotecache.get(i)[0] == appId)
				return Remotecache.get(i)[1];
		}
		return -1;
	}

	public double getMinContainerCost() {
		double mincost = -1;
		for (int i = 0; i < cache.size(); i++) {
			if (getCost(cache.get(i)) < mincost || mincost == -1)
				mincost = getCost(cache.get(i));
		}
		return mincost;
	}

	public double getCost(Task task) {
		double maxSize = 1;
		double T = 3;
		double MaxP = -1;
		for (int i = 0; i < cache.size(); i++) {
			if (cache.get(i).getContainerSizeInMBytes() > maxSize)
				maxSize = cache.get(i).getContainerSizeInMBytes();
			if (getProbability(cache.get(i).getApplicationID()) >= MaxP)
				MaxP = getProbability(cache.get(i).getApplicationID());
		}

		return 1 - (getProbability(task.getApplicationID()) / MaxP) * countT(task)
				* (task.getContainerSizeInMBytes() / (T * maxSize));
	}

	private double countT(Task task) {
		int count = 0;
		Example7CachingDevice orch = this;
		if (!isOrchestrator)
			orch = (Example7CachingDevice) getOrchestrator();
		for (int i = 0; i < orch.Remotecache.size(); i++)
			if (orch.Remotecache.get(i)[0] == task.getApplicationID())
				count++;
		return count;
	}

	private double getProbability(int appId) {
		Example7CachingDevice orch = this;

		if (!isOrchestrator)
			orch = (Example7CachingDevice) getOrchestrator();

		return orch.probability.get(appId);

	}

	public void addRequest(Task task) { // update application requesting probability
		Example7CachingDevice orch = this;

		if (!isOrchestrator)
			orch = (Example7CachingDevice) getOrchestrator();
		orch.probability.put(task.getApplicationID(), orch.probability.get(task.getApplicationID()) + 1);

	}

	public void deleteMinAapp() {
		int app = getAppWithMinCost();
		if (app != -1) {
			this.setAvailableStorage(this.getAvailableStorage() + cache.get(app).getContainerSizeInMBytes());
			Example7CachingDevice orch = this;
			if (!isOrchestrator)
				orch = (Example7CachingDevice) getOrchestrator();
			removeFromRemote(orch, cache.get(app));
			cache.remove(app);
		}
	}

	private int getAppWithMinCost() {
		double minCost = -1;
		int app = -1;
		for (int i = 0; i < cache.size(); i++) {
			if (getCost(cache.get(i)) < minCost || minCost == -1) {
				minCost = getCost(cache.get(i));
				app = i;
			}
		}
		return app;
	}

	private void removeFromRemote(Example7CachingDevice orch, Task task) {
		for (int i = 0; i < orch.Remotecache.size(); i++) {
			if (orch.Remotecache.get(i)[0] == task.getApplicationID()) {
				orch.Remotecache.get(i)[1]--;
			}
		}
	}

}
