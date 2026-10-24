package com.mechalikh.pureedgesim.simulationvisualizer;

import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;

/**
 * Simulation manager used by the visualizer tests. It captures the instance
 * created during a bootstrap simulation (so the tests can reuse a real
 * data-centers manager / computing nodes generator), and allows the tests to
 * override the simulation clock, scenario, network model and failure rate.
 * <p>
 * This class must be public with a public constructor because
 * {@code SimulationThread} instantiates it reflectively.
 */
public class BootstrapSimulationManager extends DefaultSimulationManager {

	/** The manager created by the last bootstrap simulation. */
	public static volatile BootstrapSimulationManager last;

	private Scenario scenarioOverride;
	private PureEdgeSim simulationOverride;
	private NetworkModel networkModelOverride;
	private double failureRateOverride;

	public BootstrapSimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
		last = this;
	}

	public void overrideScenario(Scenario scenario) {
		this.scenarioOverride = scenario;
	}

	public void overrideSimulation(PureEdgeSim simulation) {
		this.simulationOverride = simulation;
	}

	public void overrideNetworkModel(NetworkModel networkModel) {
		this.networkModelOverride = networkModel;
	}

	public void overrideFailureRate(double failureRate) {
		this.failureRateOverride = failureRate;
	}

	@Override
	public Scenario getScenario() {
		return scenarioOverride == null ? super.getScenario() : scenarioOverride;
	}

	@Override
	public PureEdgeSim getSimulation() {
		return simulationOverride == null ? super.getSimulation() : simulationOverride;
	}

	@Override
	public NetworkModel getNetworkModel() {
		return networkModelOverride == null ? super.getNetworkModel() : networkModelOverride;
	}

	@Override
	public double getFailureRate() {
		return failureRateOverride;
	}
}
