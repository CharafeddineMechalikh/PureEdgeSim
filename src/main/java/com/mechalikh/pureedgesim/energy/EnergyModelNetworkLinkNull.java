package com.mechalikh.pureedgesim.energy;

import com.mechalikh.pureedgesim.network.NetworkLink;

/**
 * A class that implements the Null Object Design Pattern for the
 * {@link EnergyModelNetworkLink} class. Needed to avoid
 * {@link NullPointerException} when using the NULL object instead of
 * attributing null to EnergyModelNetworkLink variables.
 */
public class EnergyModelNetworkLinkNull extends EnergyModelNetworkLink {

	/**
	 * 
	 * Represents a null energy model, used as a placeholder when a energy model
	 * is expected but none is available or applicable.
	 */
	private static final EnergyModelNetworkLinkNull instance = new EnergyModelNetworkLinkNull();
	

	/**
	 * Returns the singleton instance of the ComputingNodeNullObject.
	 * 
	 * @return the singleton instance of the ComputingNodeNullObject
	 */
	public static EnergyModelNetworkLinkNull getInstance() {
		return instance;
	}
	
	/**
	 * 
	 * Private constructor to prevent instantiation from outside the class.
	 */
	public EnergyModelNetworkLinkNull() {
		super(0, NetworkLink.NULL);
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
