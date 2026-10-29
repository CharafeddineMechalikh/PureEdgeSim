package examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
 * Where the delay of every request came from, and how many of them were late.
 * <p>
 * The default report gives one number per scenario: the average network time.
 * Congestion is not an average, it is a tail, so this manager keeps the network
 * time of every task that reached its end, and prints its median, its ninetieth
 * percentile and its maximum together with the mean. It also prints how much of
 * the end to end delay of the sampled tasks the network accounts for, which is
 * what says whether the requests were slowed by the link or by the queue in
 * front of the processor.
 * <p>
 * The two hooks it uses are the ones the framework already calls: the results
 * returning to the device, and {@code taskFailed}, which the default manager
 * consults at each of its three checkpoints. A task is sampled once, at its
 * first terminal event, so a request that fails is never also counted as a
 * success.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example27
 */
public class Example27CongestionAuditor extends DefaultSimulationManager implements OnSimulationEndListener {

	/** Network time, in seconds, of every task that reached its end. */
	private final List<Double> networkTimes = new ArrayList<>();
	/** End to end delay, in seconds, of the same tasks. */
	private final List<Double> totalDelays = new ArrayList<>();
	/** Number of tasks whose results reached their device. */
	private int succeededTasks;
	/** Number of failed tasks for each failure reason. */
	private final Map<Task.FailureReason, Integer> failures = new EnumMap<>(Task.FailureReason.class);
	/** Identifiers of the tasks already sampled, so a task is counted once. */
	private final Set<Integer> sampledTasks = new HashSet<>();

	/**
	 * Creates the auditor for one iteration of the scenario.
	 *
	 * @param simLog       the simulation logger
	 * @param pureEdgeSim  the simulation engine
	 * @param simulationId the simulation ID
	 * @param iteration    the iteration number
	 * @param scenario     the scenario being simulated
	 */
	public Example27CongestionAuditor(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
	}

	/**
	 * Delegates the event to the default manager and samples the task whose results
	 * just reached their device.
	 *
	 * @param ev the event to process
	 */
	@Override
	public void processEvent(Event ev) {
		super.processEvent(ev);
		if (ev.getTag() == RESULT_RETURN_FINISHED && ev.getData() instanceof Task)
			sample((Task) ev.getData(), true);
	}

	/**
	 * Delegates the failure check to the default manager and samples the task when
	 * it fails.
	 *
	 * @param task  the task being checked
	 * @param phase the phase at which the failure is checked
	 * @return whether the task failed
	 */
	@Override
	public boolean taskFailed(Task task, int phase) {
		boolean failed = super.taskFailed(task, phase);
		if (failed)
			sample(task, false);
		return failed;
	}

	/**
	 * Records the delay breakdown of one task, once.
	 *
	 * @param task    the task that reached its end
	 * @param success whether its results came back in time
	 */
	protected void sample(Task task, boolean success) {
		if (!sampledTasks.add(task.getId()))
			return;
		if (success) {
			succeededTasks++;
		} else {
			failures.merge(task.getFailureReason(), 1, Integer::sum);
		}
		networkTimes.add(task.getActualNetworkTime());
		totalDelays.add(task.getActualNetworkTime() + task.getWatingTime() + task.getActualCpuTime());
	}

	/**
	 * Prints the delay breakdown of the iteration once the simulation has ended.
	 * The numbers are formatted in a fixed locale so that a run reads the same way
	 * on every machine.
	 */
	@Override
	public void onSimulationEnd() {
		if (networkTimes.isEmpty()) {
			SimLog.println("%s - no task reached its end.", getScenario().getStringOrchArchitecture());
			return;
		}
		Collections.sort(networkTimes);
		double networkTotal = sum(networkTimes);
		double delayTotal = sum(totalDelays);
		int failed = failures.values().stream().mapToInt(Integer::intValue).sum();
		String architecture = getScenario().getStringOrchArchitecture();

		SimLog.println("%s - %d tasks sampled, %d succeeded, %d failed (%s).", architecture, networkTimes.size(),
				succeededTasks, failed, describeFailures());
		SimLog.println("%s - network time: mean %s s, median %s s, p90 %s s, max %s s.", architecture,
				at(3, networkTotal / networkTimes.size()), at(3, quantile(networkTimes, 0.5)),
				at(3, quantile(networkTimes, 0.9)), at(3, networkTimes.get(networkTimes.size() - 1)));
		SimLog.println("%s - the network accounts for %s percent of the end to end delay.", architecture,
				at(1, delayTotal == 0 ? 0 : 100 * networkTotal / delayTotal));
	}

	/**
	 * Formats one number the way the console of this example reports it.
	 *
	 * @param digits the number of decimals
	 * @param value  the value to format
	 * @return the value, with a point as the decimal separator
	 */
	private static String at(int digits, double value) {
		return String.format(Locale.US, "%." + digits + "f", value);
	}

	/**
	 * Returns the value below which the given fraction of the sorted samples fall.
	 *
	 * @param sorted  the samples, ascending
	 * @param fraction the quantile, between 0 and 1
	 * @return the sample at that quantile
	 */
	private static double quantile(List<Double> sorted, double fraction) {
		return sorted.get((int) Math.min(sorted.size() - 1, Math.round(fraction * (sorted.size() - 1))));
	}

	/**
	 * Sums a list of durations.
	 *
	 * @param values the durations
	 * @return their total
	 */
	private static double sum(List<Double> values) {
		return values.stream().mapToDouble(Double::doubleValue).sum();
	}

	/**
	 * Describes the failures by reason.
	 *
	 * @return a comma separated breakdown, or a note when no task failed
	 */
	private String describeFailures() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<Task.FailureReason, Integer> entry : failures.entrySet()) {
			if (text.length() > 0)
				text.append(", ");
			text.append(entry.getKey()).append(' ').append(entry.getValue());
		}
		return text.length() == 0 ? "no failures" : text.toString();
	}
}
