package com.mechalikh.pureedgesim.DataCentersManager;

import java.util.List;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;

public class DefaultDataCenter extends DataCenter {
	protected static final int UPDATE_STATUS = 2000; // Avoid conflicting with CloudSim Plus Tags

	public DefaultDataCenter(SimulationManager simulationManager, List<? extends Host> hostList,
			List<? extends Vm> vmList) {
		super(simulationManager, hostList, vmList);
	}

	@Override
	public void startEntity() {
		super.startEntity();
		schedule(this, SimulationParameters.INITIALIZATION_TIME, UPDATE_STATUS);
	}

	@Override
	public void processEvent(final SimEvent ev) {
		switch (ev.getTag()) {
		case UPDATE_STATUS:
			updateStatus();

			if (!isDead()) {
				schedule(this, SimulationParameters.UPDATE_INTERVAL, UPDATE_STATUS);
			}

			break;
		default:
			super.processEvent(ev);
			break;
		}

	}

	private void updateStatus() {

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
}
