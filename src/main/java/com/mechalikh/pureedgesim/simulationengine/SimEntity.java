package com.mechalikh.pureedgesim.simulationengine;

import java.util.stream.Collectors;

/**
 * Base class for every object that participates in the simulation. An entity
 * has an identifier, is registered in the engine, and communicates with other
 * entities only by scheduling {@link Event events}.
 */
public abstract class SimEntity {
	/** The event engine that owns this entity. */
	protected PureEdgeSim simulation;
	/** Identifier, unique among entities of the same concrete class. */
	protected int id;

	/**
	 * Creates an entity and registers it in the engine. The identifier is the
	 * number of existing entities of the same concrete class.
	 *
	 * @param simulation the event engine.
	 */
	protected SimEntity(PureEdgeSim simulation) {
		setSimulator(simulation);
		id = simulation.entitiesList.stream().filter(getClass()::isInstance).collect(Collectors.toList())
				.size();
		setId(id);
		simulation.addEntity(this);
	}

	/** Creates an entity that is not yet attached to an engine. */
	protected SimEntity() {
	}

	/**
	 * Sets the identifier.
	 *
	 * @param id the new identifier.
	 */
	protected void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the identifier.
	 *
	 * @return the identifier.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the event engine that owns this entity.
	 *
	 * @return the simulation engine.
	 */
	public PureEdgeSim getSimulation() {
		return simulation;
	}

	/**
	 * Attaches this entity to an event engine.
	 *
	 * @param simulator the event engine.
	 */
	public void setSimulator(PureEdgeSim simulator) {
		this.simulation = simulator;
	}

	/**
	 * Schedules an event for this entity after a delay.
	 *
	 * @param simEntity the entity that will process the event.
	 * @param time      the delay in seconds from the current simulation time.
	 * @param tag       the event type.
	 * @return the scheduled event.
	 */
	protected Event schedule(SimEntity simEntity, Double time, int tag) {
		Event e = new Event(simEntity, simulation.clock() + time, tag);
		simulation.insert(e);
		return e;
	}

	/**
	 * Schedules an event for this entity after an integer delay.
	 *
	 * @param simEntity the entity that will process the event.
	 * @param time      the delay in seconds from the current simulation time.
	 * @param tag       the event type.
	 * @return the scheduled event.
	 */
	protected Event schedule(SimEntity simEntity, int time, int tag) {
		Event e = new Event(simEntity, simulation.clock() + time, tag);
		simulation.insert(e);
		return e;
	}

	/**
	 * Schedules an event with a payload after a delay.
	 *
	 * @param simEntity the entity that will process the event.
	 * @param time      the delay in seconds from the current simulation time.
	 * @param tag       the event type.
	 * @param data      the payload.
	 * @return the scheduled event.
	 */
	protected Event schedule(SimEntity simEntity, Double time, int tag, Object data) {
		Event e = new Event(simEntity, simulation.clock() + time, tag, data);
		simulation.insert(e); 
		return e;
	}

	/**
	 * Schedules an event at the current simulation time, before other pending
	 * events at that time.
	 *
	 * @param simEntity the entity that will process the event.
	 * @param tag       the event type.
	 * @return the scheduled event.
	 */
	protected Event scheduleNow(SimEntity simEntity, int tag) {
		Event e = new Event(simEntity, simulation.clock(), tag);
		simulation.insertFirst(e);
		return e;
	}

	/**
	 * Schedules an event with a payload at the current simulation time, before
	 * other pending events at that time.
	 *
	 * @param simEntity the entity that will process the event.
	 * @param tag       the event type.
	 * @param data      the payload.
	 * @return the scheduled event.
	 */
	protected Event scheduleNow(SimEntity simEntity, int tag, Object data) {
		Event e = new Event(simEntity, simulation.clock(), tag, data);
		simulation.insertFirst(e);
		return e;
	}

	/**
	 * Processes an event delivered to this entity. Subclasses must handle every tag
	 * they schedule and should ignore tags they do not recognize.
	 *
	 * @param e the event to process.
	 */
	protected abstract void processEvent(Event e);

}
