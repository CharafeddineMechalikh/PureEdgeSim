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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * How a battery powered fleet dies, minute by minute, and what the placement
 * policy did about it.
 * <p>
 * The standard report answers {@i how many} devices died and when the first one
 * went; it prints one number per run. A placement policy is not judged on one
 * number: the question is whether the fleet falls early or late, and whether the
 * requests it loses are lost with the devices that died or somewhere else. This
 * manager therefore keeps a census of the alive devices at the end of every
 * simulated minute, and adds up the three ways a task can be lost to energy:
 * never generated because its device was already dead, failed because its device
 * died while it was queued or running, and failed because the queue it was put
 * in was too long to drain before the deadline.
 * <p>
 * The counters of the placement policy come from the orchestrator the manager is
 * registered with, which the framework keeps as one instance per scenario.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example28
 * @see Example28BatteryAwareOrchestrator
 */
public class Example28FleetReporter extends DefaultSimulationManager implements OnSimulationEndListener {

	/** Event that counts the devices still alive. */
	private static final int CENSUS = 200;
	/** Seconds between two census points. */
	private static final double CENSUS_INTERVAL = 60;

	/** Devices alive at the end of each simulated minute, first entry at t = 0. */
	private final List<Integer> aliveByMinute = new ArrayList<>();
	/** Tasks whose device was already dead when they should have been generated. */
	private int notGeneratedTasks;
	/** Tasks that died with their device, queued or running. */
	private int deviceDeathTasks;
	/** Tasks that came back too late. */
	private int latencyTasks;

	/**
	 * Creates the reporter for one scenario.
	 *
	 * @param simLog       the simulation logger
	 * @param pureEdgeSim  the simulation engine
	 * @param simulationId the simulation ID
	 * @param iteration    the iteration number
	 * @param scenario     the scenario being simulated
	 */
	public Example28FleetReporter(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
	}

	/**
	 * Opens the census at full fleet and starts the first minute.
	 */
	@Override
	public void onSimulationStart() {
		super.onSimulationStart();
		aliveByMinute.add(aliveDevices().size());
		schedule(this, CENSUS_INTERVAL, CENSUS);
	}

	/**
	 * Delegates the event to the default manager and takes the census when it is
	 * time.
	 *
	 * @param ev the event to process
	 */
	@Override
	public void processEvent(Event ev) {
		if (ev.getTag() == CENSUS) {
			aliveByMinute.add(aliveDevices().size());
			schedule(this, CENSUS_INTERVAL, CENSUS);
			return;
		}
		super.processEvent(ev);
	}

	/**
	 * Delegates the failure check to the default manager and sorts the failure into
	 * the three ways energy can cost a task.
	 * <p>
	 * The framework books a task whose device was already dead when it was due to
	 * send it as a task that was never generated, but it does not change the failure
	 * reason it carries: both that case and a task that died in flight are reported
	 * as failed because the device is dead. The phase the check runs at is what
	 * separates them, and it is what this reporter counts on.
	 *
	 * @param task  the task being checked
	 * @param phase the phase at which the failure is checked
	 * @return whether the task failed
	 */
	@Override
	public boolean taskFailed(Task task, int phase) {
		boolean failed = super.taskFailed(task, phase);
		if (!failed)
			return false;

		Task.FailureReason reason = task.getFailureReason();
		boolean died = reason == Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD
				|| reason == Task.FailureReason.NOT_GENERATED_BECAUSE_DEVICE_DEAD;
		if (died && phase == 0)
			notGeneratedTasks++;
		else if (died)
			deviceDeathTasks++;
		else if (reason == Task.FailureReason.FAILED_DUE_TO_LATENCY)
			latencyTasks++;
		return true;
	}

	/**
	 * Prints the collapse of the fleet and what the policy did about it.
	 */
	@Override
	public void onSimulationEnd() {
		List<ComputingNode> fleet = devices();
		int died = fleet.size() - aliveDevices().size();
		double first = Double.MAX_VALUE;
		double last = -1;
		for (ComputingNode device : fleet) {
			if (!device.isDead())
				continue;
			first = Math.min(first, device.getDeathTime());
			last = Math.max(last, device.getDeathTime());
		}

		Example28BatteryAwareOrchestrator policy = policy();
		String name = getScenario().getStringOrchAlgorithm();

		SimLog.println("%s - fleet: %d devices, %d died, first at %s s, last at %s s.", name, fleet.size(), died,
				at(1, first == Double.MAX_VALUE ? 0 : first), at(1, Math.max(0, last)));
		SimLog.println("%s - devices alive by minute: %s.", name, describeCensus());
		SimLog.println("%s - tasks lost: %d never generated because their device was dead, %d died with their device, "
				+ "%d came back too late.", name, notGeneratedTasks, deviceDeathTasks, latencyTasks);
		if (policy != null)
			SimLog.println(
					"%s - policy: %d decisions, %d candidates refused for the deadline, %d placements moved off the "
							+ "fastest device, %d kept at home, %d tasks with nobody left to serve them, mean chosen "
							+ "autonomy %s s.",
					name, policy.getDecisions(), policy.getDeadlineRefusals(), policy.getMovedPlacements(),
					policy.getLocalPlacements(), policy.getUnplacedTasks(), at(1, policy.getMeanChosenAutonomy()));
	}

	/**
	 * Returns the edge devices of the scenario, sensors excluded, which is the list
	 * the orchestrator ranks.
	 *
	 * @return the fleet
	 */
	private List<ComputingNode> devices() {
		return getDataCentersManager().getComputingNodesGenerator().getMistOnlyListSensorsExcluded();
	}

	/**
	 * Returns the devices that still have power.
	 *
	 * @return the alive part of the fleet
	 */
	private List<ComputingNode> aliveDevices() {
		List<ComputingNode> alive = new ArrayList<>();
		for (ComputingNode device : devices())
			if (!device.isDead())
				alive.add(device);
		return alive;
	}

	/**
	 * Returns the placement policy this manager is registered with, when the
	 * scenario uses one.
	 *
	 * @return the orchestrator, or null when the framework created another one
	 */
	private Example28BatteryAwareOrchestrator policy() {
		return edgeOrchestrator instanceof Example28BatteryAwareOrchestrator
				? (Example28BatteryAwareOrchestrator) edgeOrchestrator
				: null;
	}

	/**
	 * Describes the census as one number per minute.
	 *
	 * @return the alive counts, separated by spaces
	 */
	private String describeCensus() {
		StringBuilder text = new StringBuilder();
		for (Integer alive : aliveByMinute) {
			if (text.length() > 0)
				text.append(' ');
			text.append(alive);
		}
		return text.toString();
	}

	/**
	 * Formats one duration so that a run reads the same way on every machine.
	 *
	 * @param digits the number of decimals
	 * @param value  the value to print
	 * @return the value, with a point as the decimal separator
	 */
	private static String at(int digits, double value) {
		return String.format(Locale.US, "%." + digits + "f", value);
	}
}
