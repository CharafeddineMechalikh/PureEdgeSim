package com.mechalikh.pureedgesim.DataCentersManager;

import java.util.List;

import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.hosts.Host;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;

public class DefaultDataCenter extends DataCenter {

	public DefaultDataCenter(SimulationManager simulationManager, List<? extends Host> hostList) {
		super(simulationManager, hostList);
	}

	@Override
	public void startEntity() {
		super.startEntity();
		schedule(this, simulationParameters.INITIALIZATION_TIME, UPDATE_STATUS); 
	}

	@Override
	public void processEvent(final SimEvent ev) {
		switch (ev.getTag()) {
			case UPDATE_STATUS:
				// Update energy consumption
				updateEnergyConsumption();

				// Update location
				if (isMobile()) {
					getMobilityManager().getNextLocation();
				}

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
 
}
