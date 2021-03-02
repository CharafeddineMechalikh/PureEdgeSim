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

public class ClusterEdgeDevice extends DefaultDataCenter {
	private static final int UPDATE_CLUSTERS = 11000; // Avoid conflicting with CloudSim Plus Tags
	private double weight = 0;
	private ClusterEdgeDevice parent;
	protected ClusterEdgeDevice Orchestrator;
	private double originalWeight = 0;
	private double weightDrop = 0.7;
	private int time = 0;
	public List<Task> cache = new ArrayList<Task>();
	public List<ClusterEdgeDevice> cluster;
	public List<double[]> Remotecache = new ArrayList<double[]>();
	public List<double[]> probability = new ArrayList<double[]>();

	public ClusterEdgeDevice(SimulationManager simulationManager, List<? extends Host> hostList,
			List<? extends Vm> vmList) {
		super(simulationManager, hostList, vmList);
		cluster = new ArrayList<ClusterEdgeDevice>();
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
		if (getEnergyModel().isBatteryPowered())
			battery = getEnergyModel().getBatteryLevel() / 100;
		double mips = 0;
		if (getResources().getVmList().size() > 0)
			mips = getResources().getVmList().get(0).getMips();
		if (neighbors > 0)
			avg_distance = avg_distance / neighbors / SimulationParameters.EDGE_DEVICES_RANGE;

		// mips is divided by 200000 to normalize it, it is out of the parenthesis so
		// the weight becomes 0 when mips = 0
		return weight = mips / 200000 * ((battery * 0.5 / neighbors) + (neighbors * 0.2) + (mobility * 0.3));

	}

	private double getDistance(ClusterEdgeDevice device1, DataCenter device2) {
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

	public void setOrchestrator(ClusterEdgeDevice newOrchestrator) {
		//this device has changed its cluster, so it should be removed from the previous one
		if (Orchestrator != null)
			Orchestrator.cluster.remove(this);
		
		// If the new orchestrator is another device (not this one)
		if (this != newOrchestrator) {
			//if this device is no more an orchestrator, its cluster will be joined with the cluster of the new orchestrator
			if (isOrchestrator) {
				newOrchestrator.cluster.addAll(this.cluster);
			}
			// now remove it cluster after
			cluster.clear();
			//remove this device from orchestrators list
			simulationManager.getServersManager().getOrchestratorsList().remove(this);
			//set the new orchestrator as the parent node ( a tree-like topology)
			parent = newOrchestrator;
			// this device is no more an orchestrator so set it to false
			isOrchestrator = false;
			//in case the cluster doesn't has this device as member
			if (!newOrchestrator.cluster.contains(this))
				newOrchestrator.cluster.add(this);
		}
        // configure the new orchestrator (it can be another device, or this device)
		newOrchestrator.isOrchestrator = true;
		newOrchestrator.Orchestrator = newOrchestrator;
		newOrchestrator.parent = null;
		//in case the cluster doesn't has the orchestrator as member
		if (!newOrchestrator.cluster.contains(newOrchestrator))
			newOrchestrator.cluster.add(newOrchestrator);
		//add the new orchestrator to the list
		if (!simulationManager.getServersManager().getOrchestratorsList().contains(newOrchestrator))
			simulationManager.getServersManager().getOrchestratorsList().add(newOrchestrator);

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
					&& (this.weight < ((ClusterEdgeDevice) simulationManager.getServersManager().getDatacenterList()
							.get(i)).weight)) {

				setOrchestrator((ClusterEdgeDevice) simulationManager.getServersManager().getDatacenterList().get(i)
						.getOrchestrator());
				this.parent = (ClusterEdgeDevice) simulationManager.getServersManager().getDatacenterList().get(i);
				this.weight = ((ClusterEdgeDevice) simulationManager.getServersManager().getDatacenterList()
						.get(i)).weight
						* ((ClusterEdgeDevice) simulationManager.getServersManager().getDatacenterList().get(i)
								.getOrchestrator()).weightDrop;
			}

		}
	}

	public ClusterEdgeDevice getOrchestrator() {
		if (Orchestrator == null)
			Orchestrator = this;
		return Orchestrator;
	}

	public Vm getVM() {
		return getResources().getVmList().get(0);
	}

}
