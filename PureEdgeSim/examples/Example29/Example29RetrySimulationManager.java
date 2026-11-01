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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTask;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Takes the shared uplink down for half a minute and measures what the clients
 * do about it.
 * <p>
 * The uplink carries every request of the fleet, so when it stops delivering,
 * every device loses its request at the same instant. What happens next is the
 * point of the example, and it is decided by the client, not by the network:
 *
 * <ul>
 * <li>{@link Policy#NONE} is the framework default. The request is lost, nobody
 * retries, and the fleet resumes when the uplink comes back.</li>
 * <li>{@link Policy#BUDGET} is a client that tries again at most
 * {@value #BUDGET_ATTEMPTS} times, waits a backoff that doubles, and spreads its
 * attempts with a jitter derived from the request, so the fleet does not come
 * back in lockstep.</li>
 * <li>{@link Policy#STORM} is the same client without a budget and without a
 * backoff: it retries as fast as the request can fail, which is what a default
 * gRPC or HTTP client does on a timeout. Every attempt is a real transfer over
 * the same uplink, so the retries compete with the traffic they are trying to
 * recover, and the recovery outlasts the outage.</li>
 * </ul>
 * <p>
 * Two things make the storm measurable here and impossible to measure in the
 * simulators that apply a constant transmission delay: the transfer model
 * charges the delay the link actually has when the bytes arrive, and the link is
 * shared, so a retry that arrives while a hundred others are queued waits behind
 * them. The damage of the storm is then not the requests it failed to recover
 * but the requests that were healthy when the uplink came back and missed their
 * deadline in the queue the storm left behind. That is what this manager counts
 * and prints.
 * <p>
 * A request record stays open across a retry, so the framework's own request
 * columns report one request per client request and not one per attempt.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example29
 */
public class Example29RetrySimulationManager extends DefaultSimulationManager implements OnSimulationEndListener {

	/** What a client does with a request that failed. */
	public enum Policy {
		/** Accept the loss, as the framework does by default. */
		NONE,
		/** Retry a bounded number of times, with backoff and jitter. */
		BUDGET,
		/** Retry immediately, as many times as the deadline allows. */
		STORM
	}

	/** Event that takes the shared uplink down. */
	private static final int UPLINK_DOWN = 100;
	/** Event that brings it back up. */
	private static final int UPLINK_UP = 101;

	/** Seconds into the run at which the uplink stops delivering. */
	static final double OUTAGE_BEGINS = 240;
	/** Seconds it stays down. */
	static final double OUTAGE_LASTS = 30;
	/** Attempts a patient client allows per request, the first one included. */
	static final int BUDGET_ATTEMPTS = 3;
	/**
	 * Attempts a client with no budget allows before it treats the request as lost.
	 * High enough that the deadline, not the counter, ends the storm.
	 */
	static final int STORM_ATTEMPTS = 100;
	/** Seconds a patient client waits before its first retry, doubled after that. */
	static final double BACKOFF = 2;
	/** Identifiers of the retries, kept above the ones the generator hands out. */
	private static final int RETRY_ID_BASE = 10_000_000;

	/** What the client of this run does with a failed request. */
	private final Policy policy;
	/** Whether this run injects the outage at all. */
	private final boolean outage;
	/** Whether the uplink is currently dropping what crosses it. */
	private boolean uplinkDown;
	/** Time at which the uplink came back, or {@code NaN} when it never went down. */
	private double uplinkUpTime = Double.NaN;

	/** The first attempt of each request, by the identifier of any of its attempts. */
	private final Map<Integer, Integer> originOf = new HashMap<>();
	/** Attempts made so far, per request. */
	private final Map<Integer, Integer> attempts = new HashMap<>();
	/** Absolute end to end deadline of the request, per request. */
	private final Map<Integer, Double> deadlines = new HashMap<>();
	/** Arrival of the request, per request. */
	private final Map<Integer, Double> arrivals = new HashMap<>();
	/** Requests that lost an attempt with the uplink down. */
	private final Set<Integer> hitByOutage = new HashSet<>();
	/** Those of them that were later delivered. */
	private final Set<Integer> recovered = new HashSet<>();
	/** Tasks whose request record is kept open because a retry will report for it. */
	private final Set<Integer> openRequest = new HashSet<>();

	/** Identifier the next retry gets. */
	private int nextRetryId = 1;
	/** Attempts that crossed the uplink and were lost with it. */
	private int outageLosses;
	/** Retries the client sent. */
	private int retriesSent;
	/** Healthy requests that failed by latency once the uplink was back. */
	private int collateralFailures;
	/** Longest time any request spent on the network, in seconds. */
	private double worstNetworkTime;

	/**
	 * Creates a simulation manager for one scenario, with the arm of the run read
	 * from the {@code example29.policy} and {@code example29.outage} properties.
	 *
	 * @param simLog       the simulation logger
	 * @param pureEdgeSim  the simulation engine
	 * @param simulationId the simulation ID
	 * @param iteration    the iteration number
	 * @param scenario     the scenario being simulated
	 */
	public Example29RetrySimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
		this.policy = Policy.valueOf(System.getProperty("example29.policy", Policy.NONE.name()));
		this.outage = !"false".equals(System.getProperty("example29.outage", "true"));
	}

	/**
	 * Schedules the outage, then lets the default manager start the traffic.
	 */
	@Override
	public void onSimulationStart() {
		super.onSimulationStart();
		if (!outage)
			return;
		schedule(this, OUTAGE_BEGINS, UPLINK_DOWN);
		schedule(this, OUTAGE_BEGINS + OUTAGE_LASTS, UPLINK_UP);
	}

	/**
	 * Handles the two outage events, delegates the rest to the default manager, and
	 * records how long the slowest request spent on the network.
	 *
	 * @param ev the event to process
	 */
	@Override
	public void processEvent(Event ev) {
		if (ev.getTag() == UPLINK_DOWN) {
			uplinkDown = true;
			SimLog.println("%s - the shared uplink went down at %.0f s", getClass().getSimpleName(),
					simulation.clock());
			return;
		}
		if (ev.getTag() == UPLINK_UP) {
			uplinkDown = false;
			uplinkUpTime = simulation.clock();
			SimLog.println("%s - the shared uplink came back at %.0f s", getClass().getSimpleName(), uplinkUpTime);
			return;
		}
		super.processEvent(ev);
		if (ev.getTag() == RESULT_RETURN_FINISHED && ev.getData() instanceof Task)
			worstNetworkTime = Math.max(worstNetworkTime, ((Task) ev.getData()).getActualNetworkTime());
	}

	/**
	 * Fails a request that reaches the uplink while it is down, and hands every
	 * other failure to the default manager, then offers the client its retry.
	 *
	 * @param task  the task being checked
	 * @param phase the phase at which the failure is checked
	 * @return whether the task failed
	 */
	@Override
	public boolean taskFailed(Task task, int phase) {
		if (phase == 1 && uplinkDown) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_NETWORK_OUTAGE);
			outageLosses++;
			if (isFirstAttempt(task))
				hitByOutage.add(requestOf(task));
			boolean retry = willRetry(task);
			if (retry)
				// The request is not over: the retry will report for it, so its counter
				// must not be closed on this attempt.
				openRequest.add(task.getId());
			boolean failed = setFailed(task, phase);
			if (retry)
				scheduleRetry(task);
			return failed;
		}
		return super.taskFailed(task, phase);
	}

	/**
	 * Keeps the request record open when a retry is on its way, and records a
	 * request that came back after losing an attempt with the uplink down.
	 *
	 * @param task   the task that reached a terminal state
	 * @param failed whether it failed
	 */
	@Override
	protected void requestTaskFinished(Task task, boolean failed) {
		if (failed && openRequest.remove(task.getId()))
			return;
		super.requestTaskFinished(task, failed);
		if (!failed && hitByOutage.contains(requestOf(task)))
			recovered.add(requestOf(task));
		else if (failed && isCollateral(task))
			collateralFailures++;
	}

	/**
	 * Prints the cost of the outage and of what the client did about it.
	 */
	@Override
	public void onSimulationEnd() {
		int requests = hitByOutage.size();
		SimLog.println("%s - %s: %s %d requests lost an attempt with the uplink down, %d attempts lost, "
				+ "%d retries sent, %d of those requests recovered, %d never came back, %d healthy requests "
				+ "failed by latency once it was back, slowest request %.1f s on the network",
				getClass().getSimpleName(), policy, outageLine(), requests, outageLosses, retriesSent,
				recovered.size(), requests - recovered.size(), collateralFailures, worstNetworkTime);
	}

	/**
	 * @return the sentence describing the outage of this run
	 */
	private String outageLine() {
		if (!outage)
			return "no outage.";
		return String.format("outage %.0f to %.0f s.", OUTAGE_BEGINS, OUTAGE_BEGINS + OUTAGE_LASTS);
	}

	/**
	 * Decides whether this client tries the request again.
	 * <p>
	 * A retry is useless once the request deadline cannot be met, so the client
	 * gives up when the next attempt would start too late. The budget is what
	 * separates the two arms: a patient client stops after
	 * {@value #BUDGET_ATTEMPTS} attempts, a stormy one only when it runs out of
	 * time or reaches {@value #STORM_ATTEMPTS}.
	 *
	 * @param task the task that just failed
	 * @return whether a retry will be scheduled
	 */
	private boolean willRetry(Task task) {
		if (policy == Policy.NONE)
			return false;
		int made = attempts.getOrDefault(requestOf(task), 1);
		int allowed = policy == Policy.STORM ? STORM_ATTEMPTS : BUDGET_ATTEMPTS;
		if (made >= allowed)
			return false;
		double delay = backoff(made, requestOf(task));
		return simulation.clock() + delay < deadlineOf(task);
	}

	/**
	 * Schedules the next attempt of a request at the time this client waits.
	 *
	 * @param task the task that just failed
	 */
	private void scheduleRetry(Task task) {
		int request = requestOf(task);
		int made = attempts.getOrDefault(request, 1);
		attempts.put(request, made + 1);
		double delay = backoff(made, request);
		double start = simulation.clock() + delay;
		Task retry = copy(task, request, start, deadlineOf(task) - start);
		originOf.put(retry.getId(), request);
		retriesSent++;
		schedule(this, delay, SEND_TO_ORCH, retry);
	}

	/**
	 * @param made    attempts already made, this one included
	 * @param request the request being retried
	 * @return how long this client waits before trying again, in seconds
	 */
	private double backoff(int made, int request) {
		if (policy == Policy.STORM)
			return 0;
		// A deterministic jitter between half and one and a half times the backoff,
		// so that the fleet does not come back in lockstep.
		double jitter = 0.5 + (request * 37 % 100) / 100.0;
		return BACKOFF * Math.pow(2, made - 1) * jitter;
	}

	/**
	 * Copies the failed attempt into a new task that carries the same request, so
	 * the framework reports one request per client request.
	 *
	 * @param failed    the attempt that failed
	 * @param request   the request it belongs to
	 * @param start     the time at which the retry is sent
	 * @param latency   the time left before the request deadline, in seconds
	 * @return the task to send
	 */
	private Task copy(Task failed, int request, double start, double latency) {
		Task retry = new DefaultTask(RETRY_ID_BASE + nextRetryId);
		retry.setSerial(RETRY_ID_BASE + nextRetryId);
		nextRetryId++;
		retry.setType(failed.getType());
		retry.setFileSizeInBits((long) failed.getFileSizeInBits());
		retry.setOutputSizeInBits((long) failed.getOutputSizeInBits());
		retry.setContainerSizeInBits(failed.getContainerSizeInBits());
		retry.setApplicationID(failed.getApplicationID());
		retry.setLength(failed.getLength());
		retry.setMaxLatency(latency);
		retry.setEdgeDevice(failed.getEdgeDevice());
		retry.setRegistry(failed.getRegistry());
		retry.setRequest(request, 1, arrivals.get(request), deadlines.get(request));
		retry.setTime(start);
		return retry;
	}

	/**
	 * @param task a task of this run
	 * @return the request it belongs to, which is itself for a first attempt
	 */
	private int requestOf(Task task) {
		return originOf.getOrDefault(task.getId(), task.getId());
	}

	/**
	 * @param task a task of this run
	 * @return whether nothing was sent for its request before
	 */
	private boolean isFirstAttempt(Task task) {
		return !originOf.containsKey(task.getId());
	}

	/**
	 * @param task a task of this run
	 * @return the absolute deadline of its request, remembered at its first failure
	 */
	private double deadlineOf(Task task) {
		int request = requestOf(task);
		Double known = deadlines.get(request);
		if (known != null)
			return known;
		double deadline = task.getRequestDeadline() > 0 ? task.getRequestDeadline()
				: task.getTime() + task.getMaxLatency();
		double arrival = task.getRequestStartTime() > 0 ? task.getRequestStartTime() : task.getTime();
		deadlines.put(request, deadline);
		arrivals.put(request, arrival);
		return deadline;
	}

	/**
	 * @param task a task that just failed
	 * @return whether it is a healthy request of the fleet, failed by latency after
	 *         the uplink came back: the damage the storm did to traffic that was
	 *         never in the outage
	 */
	private boolean isCollateral(Task task) {
		return outage && !Double.isNaN(uplinkUpTime) && simulation.clock() >= uplinkUpTime
				&& isFirstAttempt(task) && !hitByOutage.contains(requestOf(task))
				&& task.getFailureReason() == Task.FailureReason.FAILED_DUE_TO_LATENCY;
	}
}
