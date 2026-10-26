package examples;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Simulation manager that audits the outcome of every task.
 * <p>
 * The default manager reports the aggregate success rate. This manager also
 * records why each failed task failed, and prints the breakdown when the
 * simulation ends, so a scenario can be compared on the reasons behind its
 * failures and not only on its final percentage.
 *
 * @since PureEdgeSim 6.0
 */
public class Example23AuditSimulationManager extends DefaultSimulationManager implements OnSimulationEndListener {

	/** Number of tasks whose results reached their device. */
	protected int succeededTasks;
	/** Number of tasks that were never sent because their device was already dead. */
	protected int notGeneratedTasks;
	/** Number of failed tasks for each failure reason. */
	protected final Map<Task.FailureReason, Integer> failures = new EnumMap<>(Task.FailureReason.class);
	/** Identifiers of the tasks already audited, so a task is counted once. */
	protected final Set<Integer> auditedTasks = new HashSet<>();

	/**
	 * Creates a simulation manager for one scenario.
	 *
	 * @param simLog       the simulation logger
	 * @param pureEdgeSim  the simulation engine
	 * @param simulationId the simulation ID
	 * @param iteration    the iteration number
	 * @param scenario     the scenario being simulated
	 */
	public Example23AuditSimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
	}

	/**
	 * Delegates the failure check to the default manager and records the reason
	 * when the task fails.
	 *
	 * @param task  the task being checked
	 * @param phase the phase at which the failure is checked
	 * @return whether the task failed
	 */
	@Override
	public boolean taskFailed(Task task, int phase) {
		boolean failed = super.taskFailed(task, phase);
		if (failed)
			recordFailure(task);
		return failed;
	}

	/**
	 * Delegates the event to the default manager and records a task whose results
	 * reached its device.
	 *
	 * @param ev the event to process
	 */
	@Override
	public void processEvent(Event ev) {
		super.processEvent(ev);
		if (ev.getTag() == RESULT_RETURN_FINISHED && ev.getData() instanceof Task)
			recordSuccess((Task) ev.getData());
	}

	/**
	 * Records that the given task succeeded, once.
	 *
	 * @param task the task that succeeded
	 */
	void recordSuccess(Task task) {
		if (auditedTasks.add(task.getId()))
			succeededTasks++;
	}

	/**
	 * Records that the given task failed, once, with its failure reason. A task
	 * that was never generated because its device died is counted separately.
	 *
	 * @param task the task that failed
	 */
	void recordFailure(Task task) {
		if (!auditedTasks.add(task.getId()))
			return;
		if (task.getFailureReason() == Task.FailureReason.NOT_GENERATED_BECAUSE_DEVICE_DEAD) {
			notGeneratedTasks++;
			return;
		}
		failures.merge(task.getFailureReason(), 1, Integer::sum);
	}

	/**
	 * Prints the audit summary once the simulation has ended.
	 */
	@Override
	public void onSimulationEnd() {
		int failed = failures.values().stream().mapToInt(Integer::intValue).sum();
		SimLog.println("%s - audit: %d succeeded, %d failed, %d not generated (%s)", getClass().getSimpleName(),
				succeededTasks, failed, notGeneratedTasks, describeFailures());
	}

	/**
	 * Describes the failures by reason.
	 *
	 * @return a comma separated breakdown, or a note when every task succeeded
	 */
	protected String describeFailures() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<Task.FailureReason, Integer> entry : failures.entrySet()) {
			if (text.length() > 0)
				text.append(", ");
			text.append(entry.getKey()).append(' ').append(entry.getValue());
		}
		return text.length() == 0 ? "no failures" : text.toString();
	}
}
