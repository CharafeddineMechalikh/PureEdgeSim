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

import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * An edge device that defends the real-time traffic it carries.
 * <p>
 * Three behaviours are added to the default device, each of them a mechanism of
 * the FTR-AdWOrch scenario:
 * <ul>
 * <li>a hard real-time task that arrives while every core is busy is put at the
 * head of the queue instead of its tail, so it waits behind other real-time work
 * only, and never behind a background computation;</li>
 * <li>a device refuses the elastic tasks it could still run when its battery is
 * almost flat, which pushes that work onto a device that can afford it and keeps
 * the energy for the tasks that cannot be moved;</li>
 * <li>the two devices that carry the orchestration can be switched off and on,
 * and every device re-points itself to whichever of them is alive, which is what
 * makes the standby take over when the primary crashes.
 * </ul>
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example26
 * @see Example26FaultTolerantSimulationManager
 */
public class Example26OrchestratedDevice extends DefaultComputingNode {

	/** Type of the application whose failure costs the most. */
	static final String HARD_REAL_TIME = "Hard Real-time";

	/** Number of hard real-time tasks this device moved to the head of its queue. */
	private int queueJumps;

	/**
	 * Creates an edge device.
	 *
	 * @param simulationManager the simulation manager
	 * @param mipsPerCore       capacity of one core, in million instructions per
	 *                          second
	 * @param numberOfCPUCores  number of cores
	 * @param storage           total storage, in megabytes
	 * @param ram               total RAM, in megabytes
	 */
	public Example26OrchestratedDevice(SimulationManager simulationManager, double mipsPerCore, int numberOfCPUCores,
			double storage, double ram) {
		super(simulationManager, mipsPerCore, numberOfCPUCores, storage, ram);
	}

	/**
	 * Executes the task as usual when a core is free, and otherwise puts a hard
	 * real-time task at the head of the queue instead of its tail.
	 *
	 * @param task the task that reached this device
	 */
	@Override
	public void submitTask(Task task) {
		if (!Example26.PRIORITY || !HARD_REAL_TIME.equals(task.getType()) || availableCores > 0
				|| tasksQueue.isEmpty()) {
			super.submitTask(task);
			return;
		}
		// Same accounting as the default device, only the position in the queue
		// changes: this task is served as soon as a core frees up.
		task.setArrivalTime(getSimulation().clock());
		setAvailableStorage(availableStorage - task.getContainerSizeInMBytes());
		tasksQueue.add(0, task);
		queueJumps++;
	}

	/**
	 * Returns whether this device can still take the responsibility of running the
	 * given task.
	 * <p>
	 * A device that has died, that no longer has the storage or the memory the
	 * container needs, or that is almost flat and is asked for elastic work, sends
	 * the task elsewhere. A hard real-time task is never refused for the sake of
	 * the battery: it is the traffic this fleet exists to carry.
	 *
	 * @param task the task the orchestrator wants to place here
	 * @return {@code true} if this device accepts to execute the task
	 */
	public boolean accepts(Task task) {
		if (isDead())
			return false;
		if (availableStorage < task.getContainerSizeInMBytes() || availableRam < task.getContainerSizeInMBytes())
			return false;
		if (HARD_REAL_TIME.equals(task.getType()))
			return true;
		return !Example26.REALLOCATION || !isAlmostFlat();
	}

	/**
	 * Returns whether the remaining energy of this device is below the level at
	 * which it stops taking work that another device could do.
	 *
	 * @return {@code true} if this device is battery powered and nearly empty
	 */
	public boolean isAlmostFlat() {
		return getEnergyModel().isBatteryPowered()
				&& getEnergyModel().getBatteryLevelPercentage() < Example26FaultTolerantOrchestrator.REALLOCATION_BATTERY_LEVEL;
	}

	/**
	 * Switches this device off, the way a crashed orchestrator is switched off.
	 * <p>
	 * The death flag is set directly instead of through
	 * {@link com.mechalikh.pureedgesim.datacentersmanager.EnergyAwareNode#setDeath setDeath},
	 * because that counter reports devices that ran out of energy. A device that
	 * crashes is a different event, and the run has to keep the two apart.
	 */
	public void turnOff() {
		isDead = true;
	}

	/**
	 * Switches this device back on: the standby booting up, or the primary being
	 * repaired.
	 */
	public void turnOn() {
		isDead = false;
	}

	/**
	 * Returns the orchestrating device this device currently works for.
	 * <p>
	 * The default node chooses once and keeps the choice. Here the choice is
	 * redone on every request, so that the fleet follows the standby as soon as the
	 * primary is down, and keeps following it after the primary comes back, as the
	 * paper describes. When every orchestrating device is down the closest one is
	 * still returned: its requests fail on a dead orchestrator, which is exactly
	 * the outage the scenario measures.
	 *
	 * @return the alive orchestrator closest to this device, or the closest one
	 *         when all of them are down
	 */
	@Override
	public ComputingNode getOrchestrator() {
		if (!SimulationParameters.enableOrchestrators)
			return this;
		List<ComputingNode> orchestrators = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getOrchestratorsList();
		ComputingNode alive = pickOrchestrator(orchestrators, true);
		if (alive != null)
			return alive;
		ComputingNode down = pickOrchestrator(orchestrators, false);
		// With no orchestrating device at all the fleet orchestrates itself.
		return down != null ? down : this;
	}

	/**
	 * Selects the closest orchestrator, optionally among the alive ones only.
	 *
	 * @param orchestrators the devices that carry the orchestration
	 * @param aliveOnly     whether a switched off device is skipped
	 * @return the closest eligible orchestrator, or {@code null} when none is
	 *         eligible
	 */
	private ComputingNode pickOrchestrator(List<ComputingNode> orchestrators, boolean aliveOnly) {
		ComputingNode closest = null;
		double bestDelay = Double.POSITIVE_INFINITY;
		for (ComputingNode candidate : orchestrators) {
			if (aliveOnly && candidate.isDead())
				continue;
			double delay = simulationManager.getDataCentersManager().getTopology().getDelay(this, candidate);
			if (delay < bestDelay) {
				bestDelay = delay;
				closest = candidate;
			}
		}
		return closest;
	}

	/**
	 * @return the number of hard real-time tasks that jumped this queue
	 */
	public int getQueueJumps() {
		return queueJumps;
	}
}
