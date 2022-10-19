package com.mechalikh.pureedgesim.energy;

/**
 * A class that implements the Null Object Design Pattern for the
 * {@link EnergyModelNetworkLink} class. Needed to avoid
 * {@link NullPointerException} when using the NULL object instead of
 * attributing null to EnergyModelNetworkLink variables.
 */
public class EnergyModelNetworkLinkNull extends EnergyModelNetworkLink {

	public EnergyModelNetworkLinkNull() {
		super(0, null);
	}

	@Override
	public double getCurrentEnergyConsumption() {
		return 0.0;
	}

	@Override
	public double getTotalEnergyConsumption() {
		return 0.0;
	}

}
