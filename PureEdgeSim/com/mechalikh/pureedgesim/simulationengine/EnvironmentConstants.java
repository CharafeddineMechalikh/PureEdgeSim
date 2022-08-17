package com.mechalikh.pureedgesim.simulationengine;

import java.util.ArrayList;
import java.util.List;

class EnvironmentConstants {

	static List<SimEntity> entitiesList = new ArrayList<>();

	/**
	 * A private constructor to prevent this class from being instantiated.
	 * 
	 */
	private EnvironmentConstants () {
		throw new IllegalStateException("EnvironmentConstants class cannot be instantiated");
	}

}
