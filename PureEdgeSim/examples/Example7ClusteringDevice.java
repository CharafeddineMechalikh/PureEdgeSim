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
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

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
public class Example7ClusteringDevice extends DefaultComputingNode {
	private double weight = 0;
	private Example7ClusteringDevice parent;
	protected Example7ClusteringDevice Orchestrator;
	private double originalWeight = 0;
	private double weightDrop = 0.1;
	public List<Example7ClusteringDevice> cluster;
	private static final int UPDATE_CLUSTERS = 11000;
	private int time = -30;
	private List<ComputingNode> edgeDevices;
	private List<ComputingNode> orchestratorsList;

	public Example7ClusteringDevice(SimulationManager simulationManager, double mipsCapacity, int numberOfPes,
			double storage, double ram) {
		super(simulationManager, mipsCapacity, numberOfPes, storage, ram);
		cluster = new ArrayList<Example7ClusteringDevice>();
		edgeDevices = simulationManager.getDataCentersManager().getComputingNodesGenerator().getMistOnlyList();
		orchestratorsList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getOrchestratorsList();
	}

	/**
	 * The clusters update will be done by scheduling events, the first event has to
	 * be scheduled within the startInternal() method:
	 */

	@Override
	public void startInternal() {
		super.startInternal();
		schedule(this, 1, UPDATE_CLUSTERS);
	}

	/**
	 * The scheduled event will be processed in processEvent(). To update the
	 * clusters continuously (a loop) another event has to be scheduled right after
	 * processing the previous one:
	 */
	@Override
	public void processEvent(Event ev) {
		switch (ev.getTag()) {
		case UPDATE_CLUSTERS:
			if ("CLUSTER".equals(SimulationParameters.deployOrchestrators) && (getSimulation().clock() - time > 30)) {
				time = (int) getSimulation().clock();

				// Update clusters.
				for (int i = 0; i < edgeDevices.size(); i++)
					((Example7ClusteringDevice) edgeDevices.get(i)).updateCluster();

				// Schedule the next update.
				schedule(this, 1, UPDATE_CLUSTERS);
			}

			break;
		default:
			super.processEvent(ev);
			break;
		}
	}

	public void updateCluster() {
		originalWeight = getOriginalWeight();
		if ((getOrchestratorWeight() < originalWeight) || ((parent != null)
				&& (this.getMobilityModel().distanceTo(parent) > SimulationParameters.edgeDevicesRange))) {
			setOrchestrator(this);
			weight = getOrchestratorWeight();
		}

		compareWeightWithNeighbors();

	}

	public double getOriginalWeight() {
		int neighbors = 1; // to avoid devision by zero
		double distance = 0;
		for (int i = 0; i < edgeDevices.size(); i++) {
			if (distance <= SimulationParameters.edgeDevicesRange) {
				// neighbor
				neighbors++;
			}

		}
		double battery = 2;
		double mobility = 1;
		if (getMobilityModel().isMobile())
			mobility = 0;
		if (getEnergyModel().isBatteryPowered())
			battery = getEnergyModel().getBatteryLevelPercentage();
		double mips = this.getMipsPerCore();

		// mips is divided by 200000 to normalize it, it is out of the parenthesis so
		// the weight becomes 0 when mips = 0
		return weight = mips / 200000 * ((battery * 0.5 / neighbors) + (neighbors * 0.2) + (mobility * 0.3));

	}

	private double getOrchestratorWeight() {
		if (this.isOrchestrator())
			return originalWeight;
		if (this.Orchestrator == null || !this.Orchestrator.isOrchestrator())
			return 0;
		return getOrchestrator().getOrchestratorWeight();
	}

	public void setOrchestrator(Example7ClusteringDevice newOrchestrator) {
		// this device has changed its cluster, so it should be removed from the
		// previous one
		if (Orchestrator != null)
			Orchestrator.cluster.remove(this);

		// If the new orchestrator is another device (not this one)
		if (this != newOrchestrator) {
			// if this device is no more an orchestrator, its cluster will be joined with
			// the cluster of the new orchestrator
			if (isOrchestrator()) {
				newOrchestrator.cluster.addAll(this.cluster);
			}
			// now remove it cluster after
			cluster.clear();
			// remove this device from orchestrators list
			orchestratorsList.remove(this);
			// set the new orchestrator as the parent node ( a tree-like topology)
			parent = newOrchestrator;
			// this device is no more an orchestrator so set it to false
			this.setAsOrchestrator(false);

			// in case the cluster doesn't has this device as member
			if (!newOrchestrator.cluster.contains(this))
				newOrchestrator.cluster.add(this);
		}
		// configure the new orchestrator (it can be another device, or this device)
		newOrchestrator.setAsOrchestrator(true);
		newOrchestrator.Orchestrator = newOrchestrator;
		newOrchestrator.parent = null;
		// in case the cluster doesn't has the orchestrator as member
		if (!newOrchestrator.cluster.contains(newOrchestrator))
			newOrchestrator.cluster.add(newOrchestrator);
		// add the new orchestrator to the list
		if (!orchestratorsList.contains(newOrchestrator))
			orchestratorsList.add(newOrchestrator);

	}

	private void compareWeightWithNeighbors() {
		for (int i = 2; i < edgeDevices.size(); i++) {
			if (this.getMobilityModel().distanceTo(edgeDevices.get(i)) <= SimulationParameters.edgeDevicesRange
					// neighbors
					&& (weight < ((Example7ClusteringDevice) edgeDevices.get(i)).weight)) {

				setOrchestrator((Example7ClusteringDevice) edgeDevices.get(i));
				weight = getOrchestratorWeight() * weightDrop;

			}

		}
	}

	public Example7ClusteringDevice getOrchestrator() {
		return Orchestrator;
	}

}
