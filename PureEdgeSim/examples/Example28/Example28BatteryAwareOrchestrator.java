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
package examples;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

/**
 * Places a task on the device that can best afford it, and asks again on the next
 * one.
 * <p>
 * The quantity the ranking is written in is the <i>autonomy</i>: the seconds a
 * candidate could still stay alive, drawing its idle power, once it has paid both for
 * this task and for the work it already has in its queue. It is what a battery powered
 * fleet actually runs out of, and it prices a placement the way the battery does
 * rather than the way the processor does: a task costs a device the power above idle
 * it draws while computing it, which the framework books against the whole capacity of
 * the machine, and the worth of that charge is measured against what the device burns
 * doing nothing. The same task therefore costs the wearable three times the autonomy it
 * costs the gateway, since the wearable takes six times as long over it at a third of
 * the extra power and only has a quarter of the battery to pay for it.
 * <p>
 * Two rules, one behind each switch of {@link Example28}:
 * <ul>
 * <li>{@code batteryAware} ranks the candidates by the autonomy left to them instead of
 * by how fast they would finish the task. A fleet whose devices die of the work they
 * were handed is kept intact by placement: the work has to be spread over the devices
 * that can afford it rather than gathered on the quickest ones.</li>
 * <li>{@code perTask} asks the question again for every task. The framework places an
 * application once per device and reuses the answer, which is right for a static fleet
 * and wrong here, because this fleet is only static in its shape: the autonomy of a
 * device changes each time it serves something and falls by itself as the battery
 * drains. Reusing the first answer turns it into a commitment to serve one neighbour for
 * the whole run, which is the concentration the ranking was supposed to avoid.</li>
 * </ul>
 * <p>
 * Candidates that would hand the result back after the deadline of the request are
 * dropped before either ranking runs, and counted by {@link #getDeadlineRefusals()}.
 * That filter is not a switch: a policy that spares a battery by returning the task
 * late has not placed it anywhere. The device of the task is exempt, since refusing its
 * own request would only lose it. The {@link #resultsReturned(Task)} hook clears the
 * placement for successes and failures alike, because both leave the battery where the
 * last task put it.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example28
 * @see Example28FleetReporter
 */
public class Example28BatteryAwareOrchestrator extends Orchestrator {

	/** The energy model works in watt-hours; the autonomy is stated in seconds. */
	private static final double SECONDS_PER_HOUR = 3600;

	/** Decisions taken, one per task that reached the orchestrator. */
	private int decisions;
	/** Candidate times dropped because they would have returned the task late. */
	private int deadlineRefusals;
	/** Decisions that did not go to the fastest eligible device. */
	private int movedPlacements;
	/** Tasks kept by their own device because everything else was refused. */
	private int localPlacements;
	/** Tasks with no candidate left at all. */
	private int unplacedTasks;
	/** Sum of the autonomies left on the chosen devices, to average over the run. */
	private double chosenAutonomyTotal;

	/**
	 * Creates the orchestrator for one scenario.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example28BatteryAwareOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		if (!"BATTERY_AWARE".equals(algorithmName))
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - Unknown orchestration algorithm '" + algorithmName
							+ "', expected BATTERY_AWARE.");
	}

	@Override
	protected int findComputingNode(String[] architectureLayers, Task task) {
		decisions++;
		ComputingNode local = task.getEdgeDevice();
		int chosen = -1;
		int fastest = -1;
		double bestScore = Double.MAX_VALUE;
		double bestServiceTime = Double.MAX_VALUE;

		for (int i = 0; i < nodeList.size(); i++) {
			ComputingNode node = nodeList.get(i);
			if (!offloadingIsPossible(task, node, architectureLayers))
				continue;

			double serviceTime = serviceTime(task, node);
			if (serviceTime < bestServiceTime) {
				bestServiceTime = serviceTime;
				fastest = i;
			}

			// The device of the task is never refused: it would only lose its own request.
			if (node != local && serviceTime > task.getMaxLatency()) {
				deadlineRefusals++;
				continue;
			}

			double autonomy = autonomy(node, task);

			// Lower is better for both rankings: the autonomy is negated to be maximised.
			double score = Example28.batteryAware() ? -autonomy : serviceTime;
			if (score < bestScore) {
				bestScore = score;
				chosen = i;
			}
		}

		if (chosen < 0) {
			unplacedTasks++;
			return -1;
		}
		if (chosen != fastest)
			movedPlacements++;
		if (chosen == nodeList.indexOf(local))
			localPlacements++;

		chosenAutonomyTotal += autonomy(nodeList.get(chosen), task);
		return chosen;
	}

	/**
	 * This orchestrator decides when it is asked and schedules nothing of its own.
	 *
	 * @param e unused
	 */
	@Override
	public void processEvent(Event e) {
	}

	/**
	 * Asks for a new placement on the next task of the device this one came back
	 * from: the autonomy that justified this decision has changed since it was made.
	 *
	 * @param task the task that reached its end
	 */
	@Override
	public void resultsReturned(Task task) {
		if (Example28.perTask())
			task.getEdgeDevice().setApplicationPlaced(false);
	}

	/**
	 * Returns the seconds the candidate could keep drawing its idle power after paying
	 * for this task and for the work it has already promised, which is both what the
	 * ranking maximises. A device that is not on a battery has no limit, which is what
	 * makes it the favourite of both rankings as long as one exists.
	 *
	 * @param node the candidate
	 * @param task the task it would be handed
	 * @return the seconds of life left on the candidate after that task
	 */
	static double autonomy(ComputingNode node, Task task) {
		EnergyModelComputingNode energy = node.getEnergyModel();
		if (!energy.isBatteryPowered())
			return Double.MAX_VALUE;

		double idle = energy.getIdleConsumption();
		if (idle <= 0)
			return Double.MAX_VALUE;
		return (energy.getBatteryLevelWattHour() - taskEnergy(node, task) - backlogEnergy(node)) * SECONDS_PER_HOUR
				/ idle;
	}

	/**
	 * Estimates the watt-hours this task costs the candidate, with the same linear
	 * power model the framework books when the task starts: the power above idle,
	 * spread over the whole capacity of the machine.
	 *
	 * @param node the candidate
	 * @param task the task it would be handed
	 * @return the energy the candidate would spend on that task, in watt-hours
	 */
	static double taskEnergy(ComputingNode node, Task task) {
		EnergyModelComputingNode energy = node.getEnergyModel();
		double capacity = node.getTotalMipsCapacity();
		if (capacity <= 0)
			return 0;
		return (energy.getMaxActiveConsumption() - energy.getIdleConsumption()) / SECONDS_PER_HOUR
				* task.getLength() / capacity;
	}

	/**
	 * Sums the watt-hours the candidate still owes to the tasks that are already queued
	 * inside it. They will be computed, and the framework will book their energy the
	 * same way it books this one, so a device with a long queue has less left than its
	 * battery level suggests.
	 *
	 * @param node the candidate
	 * @return the energy the candidate owes its queue, in watt-hours
	 */
	static double backlogEnergy(ComputingNode node) {
		double total = 0;
		for (Task queued : node.getTasksQueue())
			total += taskEnergy(node, queued);
		return total;
	}

	/**
	 * Estimates the seconds this task would take on a candidate: its execution time
	 * on one core, times how many tasks are already ahead of it, spread over the
	 * cores of the device.
	 *
	 * @param task the task to place
	 * @param node the candidate
	 * @return the time it would spend there, in seconds
	 */
	static double serviceTime(Task task, ComputingNode node) {
		double mips = node.getMipsPerCore();
		if (mips <= 0)
			return Double.MAX_VALUE;
		double execution = task.getLength() / mips;
		double waiting = node.getTasksQueue().size() / Math.max(1, node.getNumberOfCPUCores());
		return execution * (1 + waiting);
	}

	/** @return the number of placement decisions taken */
	public int getDecisions() {
		return decisions;
	}

	/** @return the number of times a candidate was refused for missing the deadline */
	public int getDeadlineRefusals() {
		return deadlineRefusals;
	}

	/** @return the number of placements that did not go to the fastest candidate */
	public int getMovedPlacements() {
		return movedPlacements;
	}

	/** @return the number of tasks a device kept for itself */
	public int getLocalPlacements() {
		return localPlacements;
	}

	/** @return the number of tasks that found no candidate at all */
	public int getUnplacedTasks() {
		return unplacedTasks;
	}

	/** @return the mean autonomy, in seconds, left on the devices that were chosen */
	public double getMeanChosenAutonomy() {
		return decisions == 0 ? 0 : chosenAutonomyTotal / decisions;
	}
}
