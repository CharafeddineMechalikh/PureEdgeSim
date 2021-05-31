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
package com.mechalikh.pureedgesim.tasksorchestration;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.events.SimEvent;

import com.mechalikh.pureedgesim.simulationcore.SimulationManager;
import com.mechalikh.pureedgesim.simulationcore.SimulationManagerAbstract;

public class CustomBroker extends DatacenterBrokerSimple {

	private SimulationManagerAbstract simulationManager;

	public CustomBroker(CloudSim simulation) {
		super(simulation);
	}

	@Override
	public void processEvent(final SimEvent ev) {
		super.processEvent(ev);
		switch (ev.getTag()) {
			case CloudSimTags.CLOUDLET_RETURN: // the task execution finished 
				scheduleNow(simulationManager, SimulationManager.TRANSFER_RESULTS_TO_ORCH, ev.getData());
				break;
			default:
				break;
		}
	}

	public <SM extends SimulationManagerAbstract> void setSimulationManager(SM simulationManagerAbstract) {
		this.simulationManager = simulationManagerAbstract;

	}

}
