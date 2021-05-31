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

import java.util.ArrayList;
import java.util.List;
 
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.simulationcore.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;

public class CachingEdgeDevice extends ClusterEdgeDevice {
	 
	public List<Task> cache = new ArrayList<Task>(); 
	public List<double[]> Remotecache = new ArrayList<double[]>();
	public List<double[]> probability = new ArrayList<double[]>();

	public CachingEdgeDevice(SimulationManager simulationManager, List<? extends Host> hostList,
			List<? extends Vm> vmList) {
		super(simulationManager, hostList, vmList); 
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

	public double findReplica(double appId) {
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
		double MaxP = 0;
		for (int i = 0; i < cache.size(); i++) {
			if (cache.get(i).getContainerSize() > maxSize)
				maxSize = cache.get(i).getContainerSize();
			if (getProbability(cache.get(i).getApplicationID()) >= MaxP)
				MaxP = getProbability(cache.get(i).getApplicationID());
		}

		return 1 - (getProbability(task.getApplicationID()) / MaxP) * countT(task)
				* (task.getContainerSize() / (T * maxSize));
	}

	private double countT(Task task) {
		int count = 0;
		ClusterEdgeDevice orch;
		if (isOrchestrator)
			orch = this;
		else
			orch = getOrchestrator();
		for (int i = 0; i < orch.Remotecache.size(); i++)
			if (orch.Remotecache.get(i)[0] == task.getApplicationID())
				count++;
		return count;
	}

	private double getProbability(double appId) {
		ClusterEdgeDevice orch;
		if (isOrchestrator)
			orch = this;
		else
			orch = getOrchestrator();
		for (int i = 0; i < orch.probability.size(); i++)
			if (orch.probability.get(i)[0] == appId)
				return orch.probability.get(i)[1];
		return 0;
	}

	public void addRequest(Task task) { // update application requesting probability
		boolean found = false;
		ClusterEdgeDevice orch;
		if (isOrchestrator)
			orch = this;
		else
			orch = getOrchestrator();
		for (int i = 0; i < orch.probability.size(); i++) {
			if (orch.probability.get(i)[0] == task.getApplicationID()) {
				found = true;
				orch.probability.get(i)[1]++;
			}
		}
		if (!found) {
			double[] array = new double[2];
			array[0] = task.getApplicationID();
			array[1] = 1;
			orch.probability.add(array);

		}
	}

	public void deleteMinAapp() {
		int app = getAppWithMinCost();
		if (app != -1) {
			this.getResources()
					.setAvailableMemory(this.getResources().getAvailableStorage() + cache.get(app).getContainerSize());
			ClusterEdgeDevice orch;
			if (isOrchestrator)
				orch = this;
			else
				orch = getOrchestrator();
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

	private void removeFromRemote(ClusterEdgeDevice orch, Task task) {
		for (int i = 0; i < orch.Remotecache.size(); i++) {
			if (orch.Remotecache.get(i)[0] == task.getApplicationID()) {
				orch.Remotecache.get(i)[1]--;
			}
		}
	}

}
