package com.mechalikh.pureedgesim.simulationengine;

import java.util.stream.Collectors;

public abstract class SimEntity {
	private PureEdgeSim simulator;
	private int id;

	public SimEntity(PureEdgeSim simulator) {
		setSimulator(simulator);
		id = EnvironmentConstants.entitiesList.stream().filter(getClass()::isInstance).collect(Collectors.toList()).size();
		setId(id);
		simulator.addEntity(this); 
	}

	public SimEntity() {
	}
	
	private void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public PureEdgeSim getSimulator() {
		return simulator;
	}

	public void setSimulator(PureEdgeSim simulator) {
		this.simulator = simulator;
	}

	protected void schedule(SimEntity simEntity, Double time, int tag) {
		simulator.insert(new Event(simEntity, simulator.clock() + time, tag));
	}

	protected void schedule(SimEntity simEntity, int time, int tag) {
		simulator.insert(new Event(simEntity, simulator.clock() + time, tag));
	}

	protected void schedule(SimEntity simEntity, Double time, int tag, Object data) {
		simulator.insert(new Event(simEntity, simulator.clock() + time, tag, data));
	}

	protected void scheduleNow(SimEntity simEntity, int tag) {
		simulator.insertFirst(new Event(simEntity, simulator.clock(), tag));
	}

	protected void scheduleNow(SimEntity simEntity, int tag, Object data) {
		simulator.insertFirst(new Event(simEntity, simulator.clock(), tag, data));
	}

	public PureEdgeSim getSimulation() {
		return simulator;
	}

	public abstract void startInternal();

	public abstract void processEvent(Event e);

}
