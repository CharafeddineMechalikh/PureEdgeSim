package com.mechalikh.pureedgesim.DataCentersManager;

import java.util.List;

import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.hosts.Host;
import com.mechalikh.pureedgesim.ScenarioManager.simulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;

public class DefaultDataCenter extends DataCenter {
	protected static final int UPDATE_STATUS = 2000; // Avoid conflicting with CloudSim Plus Tags

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
			updateStatus();

			if (!isDead()) {
				schedule(this, simulationParameters.UPDATE_INTERVAL, UPDATE_STATUS);
			}

			break;
		default:
			super.processEvent(ev);
			break;
		}
	}

	private void updateStatus() {
		// Update Cpu Utilization
		updateCpuUtilization();
		// Update energy consumption
		updateEnergyConsumption();

		// Update location
		if (getMobilityManager().isMobile()) {
			getMobilityManager().getNextLocation();
		}
	}

	private void updateEnergyConsumption() {
		// update the energy consumption
		getEnergyModel().updateCpuEnergyConsumption(getResources().getCurrentCpuUtilization());

		if (getEnergyModel().isBattery()
				&& this.getEnergyModel().getTotalEnergyConsumption() > getEnergyModel().getBatteryCapacity()) {
			setDeath(true, simulationManager.getSimulation().clock());
		}
	}

	protected void updateCpuUtilization() {
		getResources().setIdle(true);
		double vmUsage = 0;
		double currentCpuUtilization = 0;

		// get the cpu usage of all vms
		for (int i = 0; i < this.getVmList().size(); i++) {
			vmUsage = this.getVmList().get(i).getCloudletScheduler()
					.getRequestedCpuPercentUtilization(simulationManager.getSimulation().clock());
			currentCpuUtilization += vmUsage; // the current utilization
			getResources().setTotalCpuUtilization(getResources().getTotalCpuUtilization() + vmUsage);
			getResources().incrementUtilizationFrequency(); // in order to get the average usage from the total usage
			if (vmUsage != 0)
				getResources().setIdle(false); // set as active (not idle) if at least one vm is used
		}

		if (this.getVmList().size() > 0)
			currentCpuUtilization = currentCpuUtilization / this.getVmList().size();

		// update current CPU utilization
		getResources().setCurrentCpuUtilization(currentCpuUtilization);

	}
}
