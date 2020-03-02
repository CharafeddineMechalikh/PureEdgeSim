package examples;

import java.util.List;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.hosts.Host;

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
/*
 * To create your own custom edge device/data center class, you need to extend the DataCenter class first.
 * Then you can add any methods you want. 
 * 
 */
public class CustomDataCenter extends DataCenter {
	private static final int UPDATE_STATUS = 2000; // Avoid conflicting with CloudSim Plus Tags

	public CustomDataCenter(SimulationManager simulationManager, List<? extends Host> hostList) {
		super(simulationManager, hostList);
	}

	/*
	 * This is a discrete event simulator,
	 * the devices or the simulation entities will communicate through events 
	 * here is an example of how to launch an event after creating a device.
	 * where in this case, the event is sent to the device itself,in order to update 
	 * its energy consumption and resources utilization history. 
	 */
	@Override
	public void startEntity() {
		super.startEntity();
		schedule(this, simulationParameters.INITIALIZATION_TIME, UPDATE_STATUS);

	}
 
	/*
	 * The events sent, are handled via the following method.
	 * in this example, the event was sent from this device to itself.
	 * after receiving this event, the device will update its energy consumption,
	 * its location, and its resources utilization history.
	 * after that, it will schedule the next update  
	 */
	@Override
	public void processEvent(final SimEvent ev) {
		switch (ev.getTag()) {
		case UPDATE_STATUS:
			// Update energy consumption
			updateEnergyConsumption();

			// Update location
			if (isMobile())
				getMobilityManager().getNextLocation();

			if (!isDead()) {
				schedule(this, simulationParameters.UPDATE_INTERVAL, UPDATE_STATUS);
			}

			break;
		default:
			super.processEvent(ev);
			break;
		}
	}

	protected void updateEnergyConsumption() {
		setIdle(true);
		double vmUsage = 0;
		currentCpuUtilization = 0;

		// get the cpu usage of all vms
		for (int i = 0; i < this.getVmList().size(); i++) {
			vmUsage = this.getVmList().get(i).getCloudletScheduler()
					.getRequestedCpuPercentUtilization(simulationManager.getSimulation().clock());
			currentCpuUtilization += vmUsage; // the current utilization
			totalCpuUtilization += vmUsage;
			utilizationFrequency++; // in order to get the average usage from the total usage
			if (vmUsage != 0)
				setIdle(false); // set as active (not idle) if at least one vm is used
		}

		if (this.getVmList().size() > 0)
			currentCpuUtilization = currentCpuUtilization / this.getVmList().size();

		// update the energy consumption
		this.getEnergyModel().updateCpuEnergyConsumption(currentCpuUtilization);

		if (isBattery() && this.getEnergyModel().getTotalEnergyConsumption() > batteryCapacity) {
			isDead = true;
			deathTime = simulationManager.getSimulation().clock();
		}
	}

	public double getTotalCpuUtilization() {
		if (utilizationFrequency == 0)
			utilizationFrequency = 1;
		return totalCpuUtilization * 100 / utilizationFrequency;
	} 
}
