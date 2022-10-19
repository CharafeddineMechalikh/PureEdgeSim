/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.simulationengine;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;

/**
 * The {@code PureEdgeSim} class represents the discrete event simulation (DES)
 * engine. It manages all the events and the simulation entities.
 * <p>
 * An instance of this class is created by the
 * {@link com.mechalikh.pureedgesim.simulationmanager.SimulationThread
 * SimulationThread} in the
 * {@link com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
 * startSimulation()} method. But it is not started until the
 * {@link com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager#startSimulation()
 * startSimulation()} method of the
 * {@link com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager
 * SimulationManager} is called.
 * <p>
 * Once the PureEdgeSim simulation engine has started, it notifies all
 * simulation entities of the start of the simulation in order to schedule their
 * first event. This is guaranteed by the {@link SimEntity#startInternal()
 * startInternal()} method.
 * 
 * @see com.mechalikh.pureedgesim.simulationengine.PureEdgeSim#start()
 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
 * @see SimEntity#startInternal()
 * @see com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager#startSimulation()
 * @see com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager#startInternal()
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class PureEdgeSim {
	List<SimEntity> entitiesList = new ArrayList<>();
	protected double time;
	protected boolean isRunning = true;
	protected FutureQueue<Event> events;

	/**
	 * Gets the current simulation time in seconds.
	 * 
	 * @return the simulation time in seconds.
	 * 
	 * @see #clockInMinutes()
	 * @see #start()
	 */
	public double clock() {
		return time;
	}

	/**
	 * Creates an instance of the PureEdgeSim discrete event simulation engine.
	 * 
	 * @see PureEdgeSim
	 * @see #start()
	 */
	public PureEdgeSim() {
		events = new FutureQueue<>();
	}

	/**
	 * Starts the simulation. First, it notifies all simulation entities that the
	 * simulation has started by calling their {@link SimEntity#startInternal()
	 * startInternal()} method. The entities will then schedule their first events.
	 * Once, all the entities has scheduled their fist events, a loop will go
	 * through all those events to process them one by one, and the simulation time
	 * is updated according to the time of last processed event. With each processed
	 * events new events will added to the queue by the those entities. The
	 * simulation will stop either if the event queue is empty, or when the user
	 * terminates it by calling the {@link PureEdgeSim#terminate() terminate()}
	 * method.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.Simulation#launchSimulation()
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 * @see com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager#startSimulation()
	 * @see SimEntity#startInternal()
	 * @see #terminate()
	 */
	public void start() {
		// Notify all entities that the simulation has started.
		entitiesList.forEach(SimEntity::startInternal);

		while (runClockTickAndProcessFutureEvents(Double.MAX_VALUE) && isRunning) {
			// All the processing happens inside the method called above
		}

		// Iteration finished, notify all entities and clear their list
		entitiesList.forEach(SimEntity::onSimulationEnd);
		entitiesList.clear();
	}
 
	/**
	 * Processes future events as long as the simulation end time has not reached.
	 * 
	 * @param until the time when simulation should be terminated.
	 * @return true if an event has been processed, false if the events queue empty,
	 *         in this case the simulation will be terminated.
	 * 
	 * @see #start()
	 * @see #processFutureEventsHappeningAtSameTimeOfTheFirstOne(Event)
	 */
	protected boolean runClockTickAndProcessFutureEvents(final double until) {

		if (getEventsQueue().isEmpty()) {
			return false;
		}

		final Event first = events.first();
		if (first.getTime() <= until) {
			processFutureEventsHappeningAtSameTimeOfTheFirstOne(first);
			return true;
		}

		return false;
	}

	/**
	 * Processes events happening at the same time as the first event in the queue,
	 * and updates the simulation time.
	 * 
	 * @param firstEvent the first event in the queue at this instant of the
	 *                   simulation.
	 * @see #start()
	 * @see #runClockTickAndProcessFutureEvents(double)
	 * @see #processEvent(Event)
	 */
	protected void processFutureEventsHappeningAtSameTimeOfTheFirstOne(final Event firstEvent) {
		processEvent(firstEvent);
		getEventsQueue().remove(firstEvent);

		while (!getEventsQueue().isEmpty()) {
			final Event evt = getEventsQueue().first();
			if (evt.getTime() != firstEvent.getTime())
				break;
			processEvent(evt);
			events.remove(evt);
		}
	}

	/**
	 * Processes an event and updates the simulation time.
	 * 
	 * @param event the event to process.
	 * @see #start()
	 * @see #runClockTickAndProcessFutureEvents(double)
	 * @see #processFutureEventsHappeningAtSameTimeOfTheFirstOne(Event)
	 */
	protected void processEvent(final Event event) {
		if (event.getTime() < time) {
			final String msg = "Past event detected. Event time: %.2f Simulation clock: %.2f";
			throw new IllegalArgumentException(String.format(msg, event.getTime(), time));
		}

		time = event.getTime();
		event.getSimEntity().processEvent(event);

	}

	/**
	 * Adds an event to the queue
	 * 
	 * @param event the new event.
	 * @see SimEntity#schedule(SimEntity, Double, int)
	 * @see SimEntity#schedule(SimEntity, Double, int, Object)
	 * @see SimEntity#startInternal()
	 * @see FutureQueue
	 * @see #start()
	 * @see #runClockTickAndProcessFutureEvents(double)
	 * @see #processFutureEventsHappeningAtSameTimeOfTheFirstOne(Event)
	 */
	void insert(Event event) {
		events.add(event);
	}

	/**
	 * Adds an event to the head of the queue
	 * 
	 * @param event the new event.
	 * @see SimEntity#schedule(SimEntity, Double, int)
	 * @see SimEntity#schedule(SimEntity, Double, int, Object)
	 * @see SimEntity#startInternal()
	 * @see FutureQueue
	 * @see #start()
	 * @see #runClockTickAndProcessFutureEvents(double)
	 * @see #processFutureEventsHappeningAtSameTimeOfTheFirstOne(Event)
	 */
	public void insertFirst(Event event) {
		getEventsQueue().addFirst(event);
	}

	/**
	 * Adds a simulation entity to the entities list. The simulation entities are
	 * added to this list before starting the simulation. When the simulation is
	 * started it notifies all of them of the beginning of the simulation in order
	 * to schedule their first events.
	 * 
	 * @param simEntity the new simulation entity.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager
	 *      simulationManager)
	 * @see #start()
	 * @see SimEntity#startInternal()
	 */
	public void addEntity(SimEntity simEntity) {
		entitiesList.add(simEntity);
	}

	/**
	 * Terminates the simulation.
	 * 
	 * @see #start()
	 * @see PureEdgeSim
	 */
	public void terminate() {
		isRunning = false;
	}

	/**
	 * Gets the current simulation time in minutes.
	 * 
	 * @return simulation time in minutes.
	 * 
	 * @see #clock()
	 * @see #start()
	 */
	public int clockInMinutes() {
		return (int) (time / 60);
	}

	/**
	 * Gets the list of generated events.
	 * 
	 * @return events queue
	 */
	public FutureQueue<Event> getEventsQueue() {
		return events;
	}

}
