package com.mechalikh.pureedgesim.simulationengine;

import java.util.stream.Collectors;

public abstract class SimEntity {
	private PureEdgeSim simulation;
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

	public PureEdgeSim getSimulation() {
		return simulation;
	}

	public void setSimulator(PureEdgeSim simulator) {
		this.simulation = simulator;
	}

	protected void schedule(SimEntity simEntity, Double time, int tag) {
		simulation.insert(new Event(simEntity, simulation.clock() + time, tag));
	}

	protected void schedule(SimEntity simEntity, int time, int tag) {
		simulation.insert(new Event(simEntity, simulation.clock() + time, tag));
	}

	protected void schedule(SimEntity simEntity, Double time, int tag, Object data) {
		simulation.insert(new Event(simEntity, simulation.clock() + time, tag, data));
	}

	protected void scheduleNow(SimEntity simEntity, int tag) {
		simulation.insertFirst(new Event(simEntity, simulation.clock(), tag));
	}

	protected void scheduleNow(SimEntity simEntity, int tag, Object data) {
		simulation.insertFirst(new Event(simEntity, simulation.clock(), tag, data));
	}

	public abstract void startInternal();

	public abstract void processEvent(Event e); 

}
