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

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * The abstract class of a computing node.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public abstract class AbstractNode extends SimEntity implements ComputingNode {

	protected static final int UPDATE_STATUS = 1;

	/**
	 * The type of this node, whether a cloud data center, an edge data center, or
	 * an edge/IoT device.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator#generateDatacentersAndDevices()
	 */
	protected SimulationParameters.TYPES nodeType;

	/**
	 * The simulation manager, that links between the different modules.
	 */
	protected SimulationManager simulationManager;

	/**
	 * The name of this computing node (used when generating the topology). It is
	 * defined by the user in the edge_datacenters.xml.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator#getDataCenterByName(String
	 *      name)
	 */
	protected String name;

	/**
	 * The node that orchestrates the task of this device.
	 * 
	 * @see DefaultSimulationManager#sendTaskToOrchestrator(Task task)
	 */
	protected ComputingNode orchestrator = ComputingNode.NULL;

	/**
	 * Whether this computing node (IoT device in this case) generates tasks or not.
	 * 
	 */
	protected boolean isGeneratingTasks = false;

	/**
	 * If true, this device will orchestrate tasks.
	 * 
	 * @see DefaultSimulationManager#sendTaskToOrchestrator(Task task)
	 */
	protected boolean isOrchestrator = false;

	protected AbstractNode(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
	}

	/**
	 * Defines the logic to be performed by the computing node when the simulation
	 * starts.
	 */
	@Override
	public void startInternal() {
		scheduleNow(this, UPDATE_STATUS);
	}

	@Override
	public void processEvent(Event e) {
		if (e.getTag() == UPDATE_STATUS) {
			updateStatus();
			schedule(this, SimulationParameters.updateInterval, UPDATE_STATUS);
		}
	}

	protected abstract void updateStatus();

	/**
	 * Returns the type of this computing node, e.g. Cloud, Edge, or Mist.
	 * 
	 * @return nodeType: The type of this computing node.
	 */
	public SimulationParameters.TYPES getType() {
		return nodeType;
	}

	/**
	 * Defines the type of this computing node, e.g. Cloud, Edge, or Mist.
	 * 
	 * @param type The type of this computing node.
	 */
	public void setType(SimulationParameters.TYPES type) {
		this.nodeType = type;
	}

	/**
	 * It defines the name of this edge data center node. Called only if the type of
	 * this node is "Edge". The name is given by the user in the
	 * edge_datacenters.xml file. It will be used when creating the topology.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator#getDataCenterByName(String
	 *      name)
	 * 
	 * @param name The name of this edge data center.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the name of this computing node (for now, the name is given only to
	 * edge data centers).
	 * 
	 * @return name: The name of this computing node.
	 * 
	 * @see com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator#getDataCenterByName(String
	 *      name)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns true if this computing node is set as orchestrator.
	 * 
	 * @return isOrchestrator: Whether this computing node is orchestrator or not.
	 * 
	 */
	public boolean isOrchestrator() {
		return isOrchestrator;
	}

	/**
	 * When true, it sets this computing node as an orchestrator. By doing so, the
	 * tasks will be sent to this node to make offloading/placement decisions.
	 * 
	 * @param isOrchestrator Whether this computing node is orchestrator or not.
	 * 
	 */
	public void setAsOrchestrator(boolean isOrchestrator) {
		this.isOrchestrator = isOrchestrator;
		this.orchestrator = this; 
	} 

	/**
	 * Sets the node that orchestrates the tasks on behalf of this one. Used only
	 * when the type of this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}
	 * 
	 * @param orchestrator the node that orchestrates the tasks of this device.
	 * 
	 * @see #isOrchestrator()
	 */
	public void setOrchestrator(ComputingNode orchestrator) {
		orchestrator.setAsOrchestrator(true);
		this.orchestrator = orchestrator;
	}

	/**
	 * Whether this computing node generates tasks (e.g. an IoT sensor).
	 * 
	 * @see ComputingNodesGenerator#generateEdgeDevices()
	 */
	public void enableTaskGeneration(boolean generateTasks) {
		this.isGeneratingTasks = generateTasks;
	}

	/**
	 * @return isGeneratingTasks: If this computing node generates tasks (e.g. an
	 *         IoT sensor), or not.
	 * 
	 * @see ComputingNodesGenerator#generateEdgeDevices()
	 */
	public boolean isGeneratingTasks() {
		return this.isGeneratingTasks;
	}

	/**
	 * Defines the logic to be performed by the computing node when the simulation
	 * ends.
	 */
	@Override
	public void onSimulationEnd() {
		// Do something when the simulation finishes.
	}

}
