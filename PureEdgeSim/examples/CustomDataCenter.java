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

import java.util.List;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.datacentersmanager.DefaultDataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;

/*
 * To create your own custom edge device/data center class, you need to extend the DataCenter class first.
 * Then you can add any methods you want. 
 * 
 * In this example we extended the DefaultDataCenter class (which extends the DataCenter class) instead of the DataCenter class, in order to remove code duplication 
 * (so we don't have to copy and paste the energy consumption and the CPU utilization functions of the DefaultDataCenter class). 
 * If we needed to change it, we would have extended the DataCenter class, and write our custom updateEnergyConsumption() function.
 */
public class CustomDataCenter extends DefaultDataCenter {
	private static final int DO_SOMETHING = 12000; // Avoid conflicting with CloudSim Plus Tags

	public CustomDataCenter(SimulationManager simulationManager, List<? extends Host> hostList,
			List<? extends Vm> vmList) {
		super(simulationManager, hostList, vmList);
	}

	/*
	 * This is a discrete event simulator, the devices or the simulation entities
	 * will communicate through events here is an example of how to launch an event
	 * after creating a device. where in this case, the event is sent to the device
	 * itself,in order to update its energy consumption and resources utilization
	 * history.
	 */
	@Override
	public void startInternal() {
		super.startInternal();
		schedule(this, SimulationParameters.INITIALIZATION_TIME, UPDATE_STATUS);

	}

	/*
	 * The events sent, are handled via the following method. in this example, the
	 * event was sent from this device to itself. after receiving this event, the
	 * device will update its energy consumption, its location, and its resources
	 * utilization history. after that, it will schedule the next update
	 */
	@Override
	public void processEvent(final SimEvent ev) {
		switch (ev.getTag()) {
		case DO_SOMETHING:
			System.out.println("Event received, you can do any action here");
			break;
		default:
			// process other events using the DefaultDataCenter class that we have extended
			// (the super class)
			// if you remove this line, it will not update the energy consumption and the
			// cpu utilization
			super.processEvent(ev);
			break;
		}
	}

}
