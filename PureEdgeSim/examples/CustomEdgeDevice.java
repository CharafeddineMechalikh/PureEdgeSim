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

import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.DataCentersManager.DefaultDataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;

public class CustomEdgeDevice extends DefaultDataCenter {
	private static final int UPDATE_CLUSTERS = 11000; // Avoid conflicting with CloudSim Plus Tags
	private double weight = 0;
	private CustomEdgeDevice parent;
	private CustomEdgeDevice Orchestrator;
	private double originalWeight = 0;
	private double weightDrop = 0.7;
	private int time = 0;
	public List<Task> cache = new ArrayList<Task>();
	public List<CustomEdgeDevice> cluster;
	public List<double[]> Remotecache = new ArrayList<double[]>();
	public List<double[]> probability = new ArrayList<double[]>();

	public CustomEdgeDevice(SimulationManager simulationManager, List<? extends Host> hostList,
			List<? extends Vm> vmList) {
		super(simulationManager, hostList, vmList);
		cluster = new ArrayList<CustomEdgeDevice>();
	}

	// The clusters update will be done by scheduling events, the first event has to
	// be scheduled within the startEntity() method:
	@Override
	public void startEntity() {
		schedule(this, SimulationParameters.INITIALIZATION_TIME + 1, UPDATE_CLUSTERS); // must be 0.1 because we are
		super.startEntity();
	}

	// The scheduled event will be processed in processEvent(). To update the
	// clusters continuously (a loop) another event has to be scheduled right after
	// processing the previous one:
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case UPDATE_CLUSTERS:

			if (this.getType() == SimulationParameters.TYPES.EDGE_DEVICE
					&& "CLUSTER".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)
					&& (getSimulation().clock() - time > 30)) {
				time = (int) getSimulation().clock();
				cluster();
				// schedule the next update
				schedule(this, 3, UPDATE_CLUSTERS);
			}
			break;
		default:
			super.processEvent(ev);
			break;
		}
	}

	public double getOriginalWeight() {
		int neighbors = 0;
		double distance = 0;
		double avg_distance = 0;
		for (int i = 0; i < simulationManager.getServersManager().getDatacenterList().size(); i++) {
			if (simulationManager.getServersManager().getDatacenterList().get(i)
					.getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
				distance = getDistance(this, simulationManager.getServersManager().getDatacenterList().get(i));
				if (distance < SimulationParameters.EDGE_DEVICES_RANGE) {
					// neighbor
					neighbors++;
					avg_distance += distance;
				}
			}
		}
		double battery = 2;
		double mobility = 1;
		if (getMobilityManager().isMobile())
			mobility = 0;
		if (getEnergyModel().isBattery())
			battery = getEnergyModel().getBatteryLevel() / 100;
		double mips = 0;
		if (getVmList().size() > 0)
			mips = getVmList().get(0).getMips();
		if (neighbors > 0)
			avg_distance = avg_distance / neighbors / SimulationParameters.EDGE_DEVICES_RANGE;

		// mips is divided by 200000 to normalize it, it is out of the parenthesis so
		// the weight becomes 0 when mips = 0
		return weight = mips / 200000 * ((battery * 0.5 / neighbors) + (neighbors * 0.2) + (mobility * 0.3));

	}

	private double getDistance(CustomEdgeDevice device1, DataCenter device2) {
		return Math.abs(Math.sqrt(Math
				.pow((device1.getMobilityManager().getCurrentLocation().getXPos()
						- device2.getMobilityManager().getCurrentLocation().getXPos()), 2)
				+ Math.pow((device1.getMobilityManager().getCurrentLocation().getYPos()
						- device2.getMobilityManager().getCurrentLocation().getYPos()), 2)));
	}

	private double getOrchestratorWeight() {
		if (this.isOrchestrator)
			return this.originalWeight;
		if (this.Orchestrator == null)
			return 0;
		if (!this.Orchestrator.isOrchestrator)
			return this.originalWeight;
		return this.getOrchestrator().getOrchestratorWeight();
	}

	public void setOrchestrator(CustomEdgeDevice edgeDataCenter) {
		if (this == edgeDataCenter) {
			if (this.Orchestrator != null)
				this.Orchestrator.cluster.remove(this);
			this.Orchestrator = this;
			this.isOrchestrator = true;
			this.parent = null;
			if (!this.cluster.contains(this))
				this.cluster.add(this);
			if (!simulationManager.getServersManager().getOrchestratorsList().contains(this))
				simulationManager.getServersManager().getOrchestratorsList().add(this);
		}

		else {
			if (this.isOrchestrator) {
				for (int i = 0; i < cluster.size(); i++) {
					if (!edgeDataCenter.cluster.contains(this.cluster.get(i)))
						edgeDataCenter.cluster.add(this.cluster.get(i));
				}
			}
			this.cluster.clear();
			if (Orchestrator != null)
				this.Orchestrator.cluster.remove(this);
			simulationManager.getServersManager().getOrchestratorsList().remove(this);
			this.parent = edgeDataCenter;
			this.Orchestrator = edgeDataCenter;
			this.isOrchestrator = false;
			if (!edgeDataCenter.cluster.contains(this))
				edgeDataCenter.cluster.add(this);

			if (!edgeDataCenter.cluster.contains(edgeDataCenter))
				edgeDataCenter.cluster.add(edgeDataCenter);

			edgeDataCenter.Orchestrator = edgeDataCenter;
			edgeDataCenter.isOrchestrator = true;
			edgeDataCenter.parent = null;
			if (!simulationManager.getServersManager().getOrchestratorsList().contains(edgeDataCenter))
				simulationManager.getServersManager().getOrchestratorsList().add(edgeDataCenter);
		}

	}

	private void cluster() {
		originalWeight = getOriginalWeight();
		if ((this.getOrchestratorWeight() < originalWeight) || ((this.parent != null)
				&& (getDistance(this, this.parent) > SimulationParameters.EDGE_DEVICES_RANGE))) {
			setOrchestrator(this);
			this.weight = originalWeight;
		}

		compareWeightWithNeighbors();

	}

	private void compareWeightWithNeighbors() {
		for (int i = 2; i < simulationManager.getServersManager().getDatacenterList().size(); i++) {
			if (simulationManager.getServersManager().getDatacenterList().get(i)
					.getType() == SimulationParameters.TYPES.EDGE_DEVICE
					&& getDistance(this, simulationManager.getServersManager().getDatacenterList()
							.get(i)) <= SimulationParameters.EDGE_DEVICES_RANGE
					// neighbors
					&& (this.weight < ((CustomEdgeDevice) simulationManager.getServersManager().getDatacenterList()
							.get(i)).weight)) {

				setOrchestrator((CustomEdgeDevice) simulationManager.getServersManager().getDatacenterList().get(i)
						.getOrchestrator());
				this.parent = (CustomEdgeDevice) simulationManager.getServersManager().getDatacenterList().get(i);
				this.weight = ((CustomEdgeDevice) simulationManager.getServersManager().getDatacenterList()
						.get(i)).weight
						* ((CustomEdgeDevice) simulationManager.getServersManager().getDatacenterList().get(i)
								.getOrchestrator()).weightDrop;
			}

		}
	}

	public CustomEdgeDevice getOrchestrator() {
		if (Orchestrator == null)
			Orchestrator = this;
		return this.Orchestrator;
	}

	public Vm getVM() {
		return this.getVmList().get(0);
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
		double MaxP = 1;
		for (int i = 0; i < cache.size(); i++) {
			if (cache.get(i).getContainerSize() > maxSize)
				maxSize = cache.get(i).getContainerSize();
			if (getProbability(cache.get(i).getApplicationID()) > MaxP)
				MaxP = getProbability(cache.get(i).getApplicationID());
		}

		return 1 - (getProbability(task.getApplicationID()) / MaxP) * countT(task)
				* (task.getContainerSize() / (T * maxSize));
	}

	private double countT(Task task) {
		int count = 0;
		CustomEdgeDevice orch;
		if (isOrchestrator)
			orch = this;
		else
			orch = this.getOrchestrator();
		for (int i = 0; i < orch.Remotecache.size(); i++)
			if (orch.Remotecache.get(i)[0] == task.getApplicationID())
				count++;
		return count;
	}

	private double getProbability(double appId) {
		CustomEdgeDevice orch;
		if (isOrchestrator)
			orch = this;
		else
			orch = this.getOrchestrator();
		for (int i = 0; i < orch.probability.size(); i++)
			if (orch.probability.get(i)[0] == appId)
				return orch.probability.get(i)[1];
		return 0;
	}

	public void addRequest(Task task) { // update application requesting probability
		boolean found = false;
		CustomEdgeDevice orch;
		if (isOrchestrator)
			orch = this;
		else
			orch = this.getOrchestrator();
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
			CustomEdgeDevice orch;
			if (isOrchestrator)
				orch = this;
			else
				orch = this.getOrchestrator();
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

	private void removeFromRemote(CustomEdgeDevice orch, Task task) {
		for (int i = 0; i < orch.Remotecache.size(); i++) {
			if (orch.Remotecache.get(i)[0] == task.getApplicationID()) {
				orch.Remotecache.get(i)[1]--;
			}
		}
	}

}
