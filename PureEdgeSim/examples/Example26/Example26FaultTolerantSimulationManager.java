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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Simulation manager that crashes the primary orchestrator and measures what it
 * costs.
 * <p>
 * Halfway through the run the first of the two orchestrating devices is
 * switched off. From that moment the requests that reach it are lost until
 * another device takes the orchestration over: with the standby of the scenario
 * that is the time it boots, without it the time a crashed machine needs to come
 * back up. The manager re-points the fleet through
 * {@link Example26OrchestratedDevice#getOrchestrator()}, which always follows the
 * orchestrator that is alive, so no device keeps sending its requests to a dead
 * machine.
 * <p>
 * Everything the run has to say about that event is counted here and printed
 * when the simulation ends: the requests the crash cost, per class of traffic,
 * the success rate and the mean end to end delay of each class, how many hard
 * real-time tasks jumped a queue, and what the learning orchestrator finally
 * believes about each placement rule.
 * Nothing of it needs a change in the framework: the default manager already
 * fails a request whose orchestrator is dead, this one only counts those
 * failures and reads them back.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example26
 * @see Example26OrchestratedDevice
 * @see Example26FaultTolerantOrchestrator
 */
public class Example26FaultTolerantSimulationManager extends DefaultSimulationManager implements OnSimulationEndListener {

	/** Event that switches the primary orchestrator off. */
	private static final int INJECT_FAULT = 100;
	/** Event that brings the standby orchestrator up. */
	private static final int START_BACKUP = 101;
	/** Event that reboots the primary orchestrator when there is no standby. */
	private static final int REBOOT_PRIMARY = 102;

	/** Seconds the cold standby of the scenario needs to serve its first request. */
	static final double BACKUP_START_TIME = 10;
	/** Seconds a crashed device needs to boot again, longer than a warm standby. */
	static final double PRIMARY_REBOOT_TIME = 60;

	/** Simulated time at which the primary orchestrator was switched off. */
	private double faultTime;
	/** Simulated time at which the orchestration was back. */
	private double recoveryTime;
	/** Identifier of the device that crashed. */
	private int failedOrchestratorId = -1;
	/** Identifier of the device that took over, when one did. */
	private int backupOrchestratorId = -1;
	/** Requests lost because their orchestrator was down, per class of traffic. */
	private final Map<String, Integer> crashLosses = new LinkedHashMap<>();
	/** Tasks whose outcome has already been recorded, so none is counted twice. */
	private final Set<Integer> auditedTasks = new HashSet<>();
	/** Outcome of the run, per class of traffic: {succeeded, failed}. */
	private final Map<String, int[]> outcomes = new LinkedHashMap<>();
	/** Sum of the total delay of those tasks, per class of traffic, in seconds. */
	private final Map<String, Double> delays = new LinkedHashMap<>();

	/**
	 * Creates a simulation manager for one scenario.
	 *
	 * @param simLog       the simulation logger
	 * @param pureEdgeSim  the simulation engine
	 * @param simulationId the simulation ID
	 * @param iteration    the iteration number
	 * @param scenario     the scenario being simulated
	 */
	public Example26FaultTolerantSimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId,
			int iteration, Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
	}

	/**
	 * Puts the standby orchestrator on its shelf and schedules the crash, then lets
	 * the default manager start the traffic.
	 */
	@Override
	public void onSimulationStart() {
		super.onSimulationStart();
		List<ComputingNode> orchestrators = orchestrators();
		if (orchestrators.size() > 1)
			// A cold standby: it is there, it is powered, and it does nothing yet.
			standby(orchestrators).turnOff();
		if (Example26.FAULT && !orchestrators.isEmpty())
			schedule(this, SimulationParameters.simulationDuration / 2, INJECT_FAULT);
	}

	/**
	 * Handles the crash and the recovery events and delegates the rest of them to
	 * the default manager.
	 *
	 * @param ev the event to process
	 */
	@Override
	public void processEvent(Event ev) {
		if (ev.getTag() == INJECT_FAULT) {
			injectFault();
			return;
		}
		if (ev.getTag() == START_BACKUP) {
			recoveryTime = simulation.clock();
			Example26OrchestratedDevice standby = standby(orchestrators());
			backupOrchestratorId = standby.getId();
			standby.turnOn();
			SimLog.println("%s - the standby orchestrator %d took over at %.0f s", getClass().getSimpleName(),
					backupOrchestratorId, recoveryTime);
			return;
		}
		if (ev.getTag() == REBOOT_PRIMARY) {
			recoveryTime = simulation.clock();
			primary(orchestrators()).turnOn();
			SimLog.println("%s - the primary orchestrator %d rebooted at %.0f s", getClass().getSimpleName(),
					failedOrchestratorId, recoveryTime);
			return;
		}
		super.processEvent(ev);
		if (ev.getTag() == RESULT_RETURN_FINISHED && ev.getData() instanceof Task)
			recordOutcome((Task) ev.getData());
	}

	/**
	 * Delegates the failure check to the default manager, then records the outcome
	 * of the task and, when it is a request left in flight with the crashed
	 * orchestrator, counts it as a loss of the crash.
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
		if (phase == 1 && !task.getEdgeDevice().isDead() && task.getOrchestrator().isDead()
				&& task.getFailureReason() == Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD)
			crashLosses.merge(task.getType(), 1, Integer::sum);
		recordOutcome(task);
		return true;
	}

	/**
	 * Prints what the crash cost and how each class of traffic fared, once the
	 * simulation has run out of events.
	 */
	@Override
	public void onSimulationEnd() {
		int lost = crashLosses.values().stream().mapToInt(Integer::intValue).sum();
		SimLog.println("%s - fault tolerance: orchestrator %d crashed at %.0f s, %s", getClass().getSimpleName(),
				failedOrchestratorId, faultTime, recoveryLine(lost));
		SimLog.println("%s - success by class: %s", getClass().getSimpleName(), describeOutcomes());
		SimLog.println("%s - delay by class: %s", getClass().getSimpleName(), describeDelays());
		SimLog.println("%s - robustness: %d hard real-time tasks jumped a queue, %d tasks were reallocated, "
				+ "%d found no device at all", getClass().getSimpleName(), queueJumps(),
				orchestrator().getReallocatedTasks(), orchestrator().getUnplacedTasks());
		SimLog.println("%s - learned rules: %s", getClass().getSimpleName(), describeRules());
	}

	/**
	 * Switches the primary orchestrator off and starts the clock of the recovery
	 * that the scenario selected.
	 */
	private void injectFault() {
		Example26OrchestratedDevice primary = primary(orchestrators());
		if (primary == null)
			return;
		faultTime = simulation.clock();
		failedOrchestratorId = primary.getId();
		primary.turnOff();
		SimLog.println("%s - the primary orchestrator %d crashed at %.0f s", getClass().getSimpleName(),
				failedOrchestratorId, faultTime);
		if (Example26.BACKUP && orchestrators().size() > 1)
			schedule(this, BACKUP_START_TIME, START_BACKUP);
		else
			// Nobody takes over: the fleet keeps sending its requests to a dead device
			// until that device is repaired.
			schedule(this, PRIMARY_REBOOT_TIME, REBOOT_PRIMARY);
	}

	/**
	 * @param lost how many requests the crash cost
	 * @return the sentence describing who took the orchestration back and when
	 */
	private String recoveryLine(int lost) {
		if (!Example26.FAULT)
			return "no fault was injected";
		String recovery = backupOrchestratorId < 0 ? "nothing took over"
				: "standby " + backupOrchestratorId + " took over at " + String.format("%.0f", recoveryTime) + " s";
		return recovery + ", " + lost + " requests lost with the primary (" + describeCrashLosses() + ")";
	}

	/**
	 * Records the outcome of a task once, under the class of its application.
	 *
	 * @param task the task whose results came back, or that was lost
	 */
	private void recordOutcome(Task task) {
		if (!auditedTasks.add(task.getId()))
			return;
		int[] counters = outcomes.computeIfAbsent(task.getType(), key -> new int[2]);
		counters[task.getStatus() == Task.Status.SUCCESS ? 0 : 1]++;
		delays.merge(task.getType(), task.getTotalDelay(), Double::sum);
	}

	/**
	 * @return the mean end to end delay of every class, which is what the queue
	 *         priority of the scenario is measured by: a task that jumps a queue
	 *         comes back earlier even when both versions of the run report it late
	 */
	private String describeDelays() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<String, int[]> entry : outcomes.entrySet()) {
			int total = entry.getValue()[0] + entry.getValue()[1];
			if (text.length() > 0)
				text.append(", ");
			text.append(entry.getKey()).append(' ')
					.append(Math.round(1000 * delays.get(entry.getKey()) / total)).append(" ms");
		}
		return text.length() == 0 ? "no task came back" : text.toString();
	}

	/**
	 * @return the success rate of every class, with the counts behind it
	 */
	private String describeOutcomes() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<String, int[]> entry : outcomes.entrySet()) {
			if (text.length() > 0)
				text.append(", ");
			int succeeded = entry.getValue()[0];
			int total = succeeded + entry.getValue()[1];
			text.append(entry.getKey()).append(' ').append(total == 0 ? 0 : 100 * succeeded / total).append("% (")
					.append(succeeded).append('/').append(total).append(')');
		}
		return text.length() == 0 ? "no task came back" : text.toString();
	}

	/**
	 * @return the losses of the crash, split by class of traffic
	 */
	private String describeCrashLosses() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<String, Integer> entry : crashLosses.entrySet()) {
			if (text.length() > 0)
				text.append(", ");
			text.append(entry.getKey()).append(' ').append(entry.getValue());
		}
		return text.length() == 0 ? "none" : text.toString();
	}

	/**
	 * @return the value the agent learned for each rule, with how often it was used
	 */
	private String describeRules() {
		Example26FaultTolerantOrchestrator orchestrator = orchestrator();
		StringBuilder text = new StringBuilder();
		for (Example26FaultTolerantOrchestrator.Rule rule : Example26FaultTolerantOrchestrator.Rule.values()) {
			int[] usage = orchestrator.getRuleUsage(rule);
			if (text.length() > 0)
				text.append(", ");
			text.append(rule).append(" used ").append(usage[0]).append(" (").append(usage[1]).append(" ok, ")
					.append(usage[2]).append(" lost)");
		}
		return text.append("; ").append(orchestrator.getExplorations()).append(" explorations").toString();
	}

	/**
	 * @return the orchestrator of this run, which this example always sets
	 */
	private Example26FaultTolerantOrchestrator orchestrator() {
		return (Example26FaultTolerantOrchestrator) edgeOrchestrator;
	}

	/**
	 * @return the devices that carry the orchestration
	 */
	private List<ComputingNode> orchestrators() {
		return dataCentersManager.getComputingNodesGenerator().getOrchestratorsList();
	}

	/**
	 * @param orchestrators the orchestrating devices
	 * @return the primary one, the first of the list, or {@code null} when the
	 *         scenario has none
	 */
	private Example26OrchestratedDevice primary(List<ComputingNode> orchestrators) {
		return orchestrators.isEmpty() ? null : (Example26OrchestratedDevice) orchestrators.get(0);
	}

	/**
	 * @param orchestrators the orchestrating devices
	 * @return the standby one, the second of the list
	 */
	private Example26OrchestratedDevice standby(List<ComputingNode> orchestrators) {
		return (Example26OrchestratedDevice) orchestrators.get(1);
	}

	/**
	 * @return how many hard real-time tasks jumped a queue in this run
	 */
	private int queueJumps() {
		int jumps = 0;
		for (ComputingNode node : dataCentersManager.getComputingNodesGenerator().getAllNodesListSensorsExcluded())
			if (node instanceof Example26OrchestratedDevice)
				jumps += ((Example26OrchestratedDevice) node).getQueueJumps();
		return jumps;
	}
}
