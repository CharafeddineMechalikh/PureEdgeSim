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
package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.List;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task; 

/**
 * An interface to be implemented by each class that provides "computing node"
 * features. The interface implements the Null Object Design Pattern in order to
 * start avoiding {@link NullPointerException} when using the
 * {@link ComputingNode#NULL} object instead of attributing {@code null} to
 * {@link ComputingNode} variables.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public interface ComputingNode {

	/**
	 * An attribute that implements the Null Object Design Pattern to avoid
	 * NullPointerException when using the NULL object instead of attributing null
	 * to EnergyModelNetworkLink variables.
	 */
	ComputingNode NULL = new ComputingNodeNull();

	/**
	 * Called when a task has been offloaded to this computing node. The task will
	 * be added to the execution queue.
	 * 
	 * @param task the task to execute.
	 * @return 
	 */
	void submitTask(Task task);

	/**
	 * Gets the type of this computing node, e.g.
	 * {@link SimulationParameters.TYPES#CLOUD},
	 * {@link SimulationParameters.TYPES#EDGE_DATACENTER}, or
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return the type of this computing node.
	 */
	SimulationParameters.TYPES getType();

	/**
	 * Sets the type of this computing node, e.g.
	 * {@link SimulationParameters.TYPES#CLOUD},
	 * {@link SimulationParameters.TYPES#EDGE_DATACENTER}, or
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @param type The type of this computing node.
	 */
	void setType(SimulationParameters.TYPES type);

	/**
	 * Sets the name of this edge data center node. Called only if the type of this
	 * node is {@link SimulationParameters.TYPES#EDGE_DATACENTER}. The name is given
	 * by the user in the edge_datacenters.xml file. It is needed in order to create
	 * the topology.
	 * 
	 * @see DefaultTopologyCreator#getDataCenterByName(String name)
	 * 
	 * @param name The name of this edge data center.
	 */
	void setName(String name);

	/**
	 * Gets the name of this computing node (for now, the name is only given if this
	 * computing node type is {@link SimulationParameters.TYPES#CLOUD}. It is needed
	 * in order to create the topology.
	 * 
	 * @return the name of this computing node.
	 * 
	 * @see DefaultTopologyCreator#getDataCenterByName(String name)
	 * @see #setName(String)
	 */
	String getName();

	/**
	 * Checks if this computing node is an orchestrator.
	 * 
	 * @return true if this computing node is set as orchestrator.
	 * 
	 * @see #setAsOrchestrator(boolean)
	 */
	boolean isOrchestrator();

	/**
	 * When true, it sets this computing node as an orchestrator. By doing so, the
	 * tasks will be sent to this node to make offloading/placement decisions.
	 * 
	 * @param isOrchestrator whether this computing node is orchestrator or not.
	 * 
	 * @see #isOrchestrator()
	 */
	void setAsOrchestrator(boolean isOrchestrator);
	
	/**
	 * Sets the node that orchestrates the tasks on behalf of this one. Used only
	 * when the type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}
	 * 
	 * @param orchestrator the node that orchestrates the tasks of this device.
	 * 
	 * @see #isOrchestrator()
	 */
	void setOrchestrator(ComputingNode orchestrator);

	/**
	 * Gets the node that orchestrates the tasks on behalf of this one. Used only
	 * when the type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return the orchestrator of this edge device.
	 * 
	 * @see DefaultSimulationManager#sendTaskToOrchestrator(Task task)
	 * @see #setAsOrchestrator(boolean)
	 */
	ComputingNode getOrchestrator();

	/**
	 * Sets whether this computing node generates tasks (e.g. an IoT sensor). Used
	 * only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @see ComputingNodesGenerator#generateEdgeDevices()
	 * @see #isGeneratingTasks()
	 * 
	 * @param generateTasks true when this edge device can generate tasks, false
	 *                      otherwise.
	 */
	void enableTaskGeneration(boolean generateTasks);

	/**
	 * Gets whether this computing node generates tasks (e.g. an IoT sensor). Used
	 * only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}
	 * 
	 * @return whether this computing node generates tasks (e.g. an IoT sensor), or
	 *         not.
	 * 
	 * @see ComputingNodesGenerator#generateEdgeDevices()
	 * @see #enableTaskGeneration(boolean)
	 */
	boolean isGeneratingTasks();

	/**
	 * Gets the network link that is used currently to send data to the cloud or
	 * edge data centers. Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return the network link used to transfer data to the cloud or edge data
	 *         centers.
	 * @see #setCurrentUpLink(NetworkLink)
	 * @see #setCurrentDownLink(NetworkLink)
	 * @see #getCurrentDownLink()
	 * @see #getCurrentWiFiLink()
	 * @see #setCurrentWiFiLink(NetworkLink)
	 */
	NetworkLink getCurrentUpLink();

	/**
	 * Sets the network link that is used currently to send data to the cloud or
	 * edge data centers.Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @param currentUpLink the network link.
	 * @see #getCurrentUpLink()
	 * @see #getCurrentDownLink()
	 * @see #setCurrentDownLink(NetworkLink)
	 * @see #getCurrentWiFiLink()
	 * @see #setCurrentWiFiLink(NetworkLink)
	 */
	void setCurrentUpLink(NetworkLink currentUpLink);

	/**
	 * Gets the network link that is used currently to receive data from the cloud
	 * or edge data centers. Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return the network link used to transfer data from the cloud or edge data
	 *         centers to this device.
	 * @see #setCurrentDownLink(NetworkLink)
	 * @see #setCurrentUpLink(NetworkLink)
	 * @see #getCurrentUpLink()
	 * @see #getCurrentWiFiLink()
	 * @see #setCurrentWiFiLink(NetworkLink)
	 */
	NetworkLink getCurrentDownLink();

	/**
	 * Sets the network link that is used currently to receive data from the cloud
	 * or edge data centers. Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @see #setCurrentUpLink(NetworkLink)
	 * @see #getCurrentUpLink()
	 * @see #getCurrentDownLink()
	 * @see #getCurrentWiFiLink()
	 * @see #setCurrentWiFiLink(NetworkLink)
	 * 
	 * @param currentDownLink the network link.
	 */
	void setCurrentDownLink(NetworkLink currentDownLink);

	/**
	 * Gets the network link that is used currently to send data to nearby edge
	 * devices (peer to peer).Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return the network link used to transfer to nearby edge devices.
	 * @see #setCurrentWiFiLink(NetworkLink)
	 * @see #getCurrentUpLink()
	 * @see #getCurrentDownLink()
	 * @see #setCurrentDownLink(NetworkLink)
	 * @see #setCurrentUpLink(NetworkLink)
	 */
	NetworkLink getCurrentWiFiLink();

	/**
	 * Sets the network link that is used currently to send data to nearby edge
	 * devices (peer to peer). Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @param currentWiFiDeviceToDeviceLink the network link used to transfer to
	 *                                      nearby edge devices.
	 * @see #getCurrentWiFiLink()
	 * @see #getCurrentUpLink()
	 * @see #getCurrentDownLink()
	 * @see #setCurrentDownLink(NetworkLink)
	 * @see #setCurrentUpLink(NetworkLink)
	 */
	void setCurrentWiFiLink(NetworkLink currentWiFiDeviceToDeviceLink);

	/**
	 * Whether this device is out of battery. Used only when the type of this node
	 * is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return true if this device is out of battery.
	 */
	boolean isDead();

	/**
	 * Gets the time in seconds when this device was out of battery. Used only when
	 * the type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return time when this device was out of battery.
	 */
	double getDeathTime();

	/**
	 * Gets the used energy model.
	 * 
	 * @return the energy model.
	 */
	EnergyModelComputingNode getEnergyModel();

	/**
	 * Sets the energy model.
	 * 
	 * @param energyModel the energy model.
	 */
	void setEnergyModel(EnergyModelComputingNode energyModel);

	/**
	 * Gets the used mobility model.
	 * 
	 * @return the mobility model.
	 */
	MobilityModel getMobilityModel();

	/**
	 * Sets the mobility model.
	 * 
	 * @param mobilityModel the mobility model.
	 */
	void setMobilityModel(MobilityModel mobilityModel);

	/**
	 * Gets whether edge devices can connect to this edge data center directly (via
	 * a single hop), or not. Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DATACENTER}.
	 * 
	 * @return true if edge devices can connect to this edge data center directly.
	 */
	boolean isPeripheral();

	/**
	 * Sets whether edge devices can connect to this edge data center directly (via
	 * a single hop), or not. Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DATACENTER}.
	 * 
	 * @param peripheral true to set the edge data center to peripheral, false
	 *                   otherwise.
	 * @see #isPeripheral()
	 */
	void setPeriphery(boolean peripheral);

	/**
	 * Sets where the application of this edge device has been placed. Used only
	 * when the type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @param node the computing node that executes the tasks of this device.
	 */
	void setApplicationPlacementLocation(ComputingNode node);

	/**
	 * Gets where the application of this edge device has been placed. Used only
	 * when the type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return the computing node that executes the tasks of this device.
	 */
	ComputingNode getApplicationPlacementLocation();

	/**
	 * Gets if the application of this edge device has been placed. Used only when
	 * the type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return true if the application of this edge device has been placed.
	 * @see #setApplicationPlaced(boolean)
	 */
	boolean isApplicationPlaced();

	/**
	 * Sets if the application of this edge device has been placed. It is
	 * automatically set to false when this device moves away from the node where
	 * its application has been placed previously. Used only when the type of this
	 * node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @see #setApplicationPlacementLocation(ComputingNode)
	 * @param isApplicationPlaced true if the application of this edge device has
	 *                            been placed.
	 */
	void setApplicationPlaced(boolean isApplicationPlaced);

	/**
	 * Gets the number of CPU cores this computing node has.
	 * 
	 * @return the number of cores.
	 * 
	 * @see #getNumberOfCPUCores()
	 * @see #getTotalMipsCapacity()
	 * @see #setTotalMipsCapacity(double)
	 * @see #getMipsPerCore()
	 */
	double getNumberOfCPUCores();

	/**
	 * Sets the number of CPU cores this computing node has.
	 * 
	 * @param numberOfCPUCores the number of cores.
	 * 
	 * @see #getNumberOfCPUCores()
	 * @see #getTotalMipsCapacity()
	 * @see #setTotalMipsCapacity(double)
	 * @see #getMipsPerCore()
	 */
	void setNumberOfCPUCores(int numberOfCPUCores);

	/**
	 * Gets the type of application this edge device is using. Used only when the
	 * type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @return the type of application.
	 */
	int getApplicationType();

	/**
	 * Sets the type of application this edge device is using. Used only when the
	 * type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @param applicationType the type of application.
	 */
	void setApplicationType(int applicationType);

	/**
	 * Gets the amount storage (in Megabytes) that is available on this computing node.
	 * 
	 * @return the available storage.
	 */
	double getAvailableStorage();

	/**
	 * Sets the amount storage (in Megabytes) that is available on this computing node.
	 * 
	 * @param availableStorage the available storage.
	 */
	void setAvailableStorage(double availableStorage);

	/**
	 * Gets the average CPU utilization of this computing node from the beginning of
	 * the simulation.
	 * 
	 * @return the average CPU utilization.
	 */
	double getAvgCpuUtilization();

	/**
	 * Gets the CPU utilization of this computing node at this instant of the
	 * simulation.
	 * 
	 * @return the current CPU utilization percentage.
	 */
	double getCurrentCpuUtilization();

	/**
	 * Gets whether this computing node is idle or not.
	 * 
	 * @return true if the CPU of this computing node is not used.
	 */
	boolean isIdle();

	/**
	 * Sets whether this computing node is idle or not.
	 * 
	 * @param isIdle true if the CPU of this computing node is not used, false
	 *               otherwise.
	 */
	void setIdle(boolean isIdle);

	/**
	 * Increments the CPU utilization when a new task is being executed.
	 * 
	 * @see #submitTask(Task)
	 * @see #getAvgCpuUtilization()
	 * 
	 * @param task the task being executed.
	 */
	void addCpuUtilization(Task task);

	/**
	 * Decreases the CPU utilization when a task finishes.
	 * 
	 * @see #submitTask(Task)
	 * @see #getAvgCpuUtilization()
	 * 
	 * @param task the finished task.
	 */
	void removeCpuUtilization(Task task);

	/**
	 * Gets whether this node is a sensor (no computing capabilities), or not.
	 * 
	 * @return true if this device has no computing capabilities, false otherwise.
	 * 
	 * @see #setAsSensor(boolean)
	 */
	boolean isSensor();

	/**
	 * Sets whether this node is a sensor (no computing capabilities), or not.
	 * 
	 * @param isSensor true if this device has no computing capabilities, false
	 *                 otherwise.
	 * 
	 * @see #isSensor()
	 */
	void setAsSensor(boolean isSensor);

	/**
	 * Gets the list of tasks waiting for execution.
	 * 
	 * @return the execution queue.
	 * 
	 * @see #submitTask(Task)
	 */
	List<Task> getTasksQueue();

	/**
	 * Gets the total amount of storage (in Megabytes) that this computing node has.
	 * 
	 * @return the total amount of storage.
	 * 
	 * @see #getAvailableStorage()
	 * @see #setAvailableStorage(double)
	 * @see #setStorage(double)
	 */
	double getTotalStorage();

	/**
	 * Gets the total amount of RAM (in Megabytes) that this computing node has.
	 * 
	 * @return the total amount of RAM.
	 * 
	 * @see #getAvailableRam()
	 * @see #setAvailableRam(double)
	 * @see #setRam(double)
	 */
	double getRamCapacity();

	/**
	 * Gets the amount of RAM (in Megabytes) that is available on this computing node.
	 * 
	 * @return the amount of available RAM.
	 * 
	 * @see #getRamCapacity()
	 * @see #setAvailableRam(double)
	 * @see #setRam(double)
	 */
	double getAvailableRam();

	/**
	 * Sets the total amount of RAM (in Megabytes) that this computing node has.
	 * 
	 * @param ram the total RAM on this computing node.
	 * 
	 * @see #getAvailableRam()
	 * @see #setAvailableRam(double)
	 * @see #getRamCapacity()
	 */
	void setRam(double ram);

	/**
	 * Sets the amount of RAM (in Megabytes) that is available on this computing node.
	 * 
	 * @param ram the available RAM.
	 * 
	 * @see #getAvailableRam()
	 * @see #getRamCapacity()
	 * @see #setRam(double)
	 */
	void setAvailableRam(double ram);

	/**
	 * Sets the total amount storage (in Megabytes) that this computing node has.
	 * 
	 * @param storage the amount of storage.
	 * 
	 * @see #getAvailableStorage()
	 * @see #setAvailableStorage(double)
	 * @see #getTotalStorage()
	 */
	void setStorage(double storage);

	/**
	 * Gets the total computing capacity of this computing node in MIPS.
	 * 
	 * @return total MIPS capacity.
	 */
	double getTotalMipsCapacity();

	/**
	 * Sets the total computing capacity of this computing node in MIPS.
	 * 
	 * @param totalMipsCapacity total MIPS capacity.
	 */
	void setTotalMipsCapacity(double totalMipsCapacity);

	/**
	 * Gets the computing capacity of a each CPU Core in MIPS.
	 * 
	 * @return the amount MIPS capacity per CPU core.
	 * 
	 * @see #setNumberOfCPUCores(double)
	 * @see #getNumberOfCPUCores()
	 * @see #getTotalMipsCapacity()
	 * @see #setTotalMipsCapacity(double)
	 */
	double getMipsPerCore();

	/**
	 * Gets the Id of this computing node.
	 * 
	 * @return the id of this computing node.
	 */
	public int getId();

}
