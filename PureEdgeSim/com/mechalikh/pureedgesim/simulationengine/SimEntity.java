package com.mechalikh.pureedgesim.simulationengine;

import java.util.stream.Collectors;

public abstract class SimEntity {
	protected PureEdgeSim simulation;
	protected int id;

	protected SimEntity(PureEdgeSim simulation) {
		setSimulator(simulation);
		id = simulation.entitiesList.stream().filter(getClass()::isInstance).collect(Collectors.toList())
				.size();
		setId(id);
		simulation.addEntity(this);
	}

	protected SimEntity() {
	}

	protected void setId(int id) {
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

	protected Event schedule(SimEntity simEntity, Double time, int tag) {
		Event e = new Event(simEntity, simulation.clock() + time, tag);
		simulation.insert(e);
		return e;
	}

	protected Event schedule(SimEntity simEntity, int time, int tag) {
		Event e = new Event(simEntity, simulation.clock() + time, tag);
		simulation.insert(e);
		return e;
	}

	protected Event schedule(SimEntity simEntity, Double time, int tag, Object data) {
		Event e = new Event(simEntity, simulation.clock() + time, tag, data);
		simulation.insert(e); 
		return e;
	}

	protected Event scheduleNow(SimEntity simEntity, int tag) {
		Event e = new Event(simEntity, simulation.clock(), tag);
		simulation.insertFirst(e);
		return e;
	}

	protected Event scheduleNow(SimEntity simEntity, int tag, Object data) {
		Event e = new Event(simEntity, simulation.clock(), tag, data);
		simulation.insertFirst(e);
		return e;
	}

	protected abstract void startInternal();

	protected abstract void onSimulationEnd();

	protected abstract void processEvent(Event e);

}
