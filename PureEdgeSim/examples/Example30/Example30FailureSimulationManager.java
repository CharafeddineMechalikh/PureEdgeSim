package examples;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Simulation manager of Example 30: injects the crashes of the {@code crash}
 * scenario and reports what each orchestrator lost.
 * <p>
 * In the {@code crash} scenario, every helper device (one that computes and
 * sends no requests) inside a random disk of {@value #CRASH_RADIUS} meters goes
 * down at 120, 240 and 360 s, and stays down. One edge data center is also down
 * from {@value #EDGE_DOWN} s to {@value #EDGE_UP} s, unless the system property
 * {@code example30.edgeOutage} is {@code false}. The disks and the data
 * center are drawn from the scenario seed, so every orchestrator faces the same
 * failures. In the {@code battery} scenario devices die when their battery
 * empties. In every scenario, a result is lost when the moving helper that
 * computed it is out of range of the requesting device by the time it returns.
 * <p>
 * At the end of the run it prints one line and, when the system property
 * {@code example30.out} names a file, appends the metrics to it as one JSON
 * object.
 */
public class Example30FailureSimulationManager extends DefaultSimulationManager
		implements OnSimulationEndListener {

	static final double CRASH_RADIUS = 150;
	static final double[] CRASH_TIMES = { 120, 240, 360 };
	static final double EDGE_DOWN = 300;
	static final double EDGE_UP = 360;

	private static final int CRASH = 12000;
	private static final int DATACENTER_DOWN = 12001;
	private static final int DATACENTER_UP = 12002;

	private final String failureScenario;
	private final boolean edgeOutage;
	private final Random failures;
	private String workloadHash = "";
	private int requestsGenerated;
	private ComputingNode downDatacenter = ComputingNode.NULL;

	private final Map<Integer, double[]> openRequests = new HashMap<>();
	private final List<Double> makespans = new ArrayList<>();
	private int requestsFailed;
	private int subtasksSucceeded;
	private int subtasksFailed;
	private final Map<String, Integer> failureReasons = new TreeMap<>();
	private final Map<String, Integer> placements = new TreeMap<>();
	private int crashedDevices;

	/**
	 * Creates the manager, with the signature the simulation calls by reflection.
	 *
	 * @param simLog       the simulation logger
	 * @param pureEdgeSim  the simulation engine
	 * @param simulationId the simulation identifier
	 * @param iteration    the iteration
	 * @param scenario     the scenario
	 */
	public Example30FailureSimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId,
			int iteration, Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
		failureScenario = System.getProperty("example30.scenario", "crash");
		edgeOutage = !"false".equals(System.getProperty("example30.edgeOutage", "true"));
		failures = new Random(Randomness.getSeed() * 7919L + 17L);
	}

	@Override
	public void onSimulationStart() {
		workloadHash = hashTaskList();
		super.onSimulationStart();
		if (!"crash".equals(failureScenario))
			return;
		for (double time : CRASH_TIMES)
			schedule(this, time, CRASH);
		if (!edgeOutage)
			return;
		schedule(this, EDGE_DOWN, DATACENTER_DOWN);
		schedule(this, EDGE_UP, DATACENTER_UP);
	}

	@Override
	public void processEvent(Event ev) {
		if (ev.getTag() == CRASH) {
			crashDisk();
			return;
		}
		if (ev.getTag() == DATACENTER_DOWN) {
			List<ComputingNode> datacenters = getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList();
			if (!datacenters.isEmpty()) {
				downDatacenter = datacenters.get(failures.nextInt(datacenters.size()));
				if (downDatacenter instanceof Example30Device)
					((Example30Device) downDatacenter).crash();
			}
			return;
		}
		if (ev.getTag() == DATACENTER_UP) {
			if (downDatacenter instanceof Example30Device)
				((Example30Device) downDatacenter).restore();
			return;
		}
		super.processEvent(ev);
	}

	private void crashDisk() {
		double x = failures.nextDouble() * SimulationParameters.simulationMapLength;
		double y = failures.nextDouble() * SimulationParameters.simulationMapWidth;
		for (ComputingNode node : getDataCentersManager().getComputingNodesGenerator().getMistOnlyList()) {
			// Only helpers crash: a device that sends requests would lose them wherever
			// their subtasks ran, which no placement can change.
			if (node.isSensor() || node.isDead() || node.isGeneratingTasks() || !(node instanceof Example30Device))
				continue;
			double dx = node.getMobilityModel().getCurrentLocation().getXPos() - x;
			double dy = node.getMobilityModel().getCurrentLocation().getYPos() - y;
			if (Math.sqrt(dx * dx + dy * dy) <= CRASH_RADIUS) {
				((Example30Device) node).crash();
				crashedDevices++;
			}
		}
	}

	@Override
	public boolean taskFailed(Task task, int phase) {
		boolean failed = super.taskFailed(task, phase) || helperMovedAway(task, phase);
		if (failed && edgeOrchestrator instanceof Example30RiskAwareOrchestrator)
			((Example30RiskAwareOrchestrator) edgeOrchestrator).taskEnded(task);
		return failed;
	}

	/**
	 * Fails a result whose helper device walked out of range of the device that
	 * asked for it. PureEdgeSim checks mobility against an orchestrator device,
	 * which this scenario does not use, so the example applies this rule, the same
	 * for every algorithm.
	 */
	private boolean helperMovedAway(Task task, int phase) {
		if (phase != 3)
			return false;
		ComputingNode helper = task.getOffloadingDestination();
		ComputingNode origin = task.getEdgeDevice();
		if (helper == null || helper == ComputingNode.NULL || helper == origin
				|| helper.getType() != SimulationParameters.TYPES.EDGE_DEVICE || !helper.getMobilityModel().isMobile())
			return false;
		if (helper.getMobilityModel().distanceTo(origin) <= SimulationParameters.edgeDevicesRange)
			return false;
		task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_DEVICE_MOBILITY);
		simLog.incrementTasksFailedMobility(task);
		return setFailed(task, phase);
	}

	@Override
	protected void requestTaskFinished(Task task, boolean failed) {
		super.requestTaskFinished(task, failed);
		if (failed) {
			subtasksFailed++;
			String reason = String.valueOf(task.getFailureReason());
			failureReasons.put(reason, failureReasons.getOrDefault(reason, 0) + 1);
		} else {
			subtasksSucceeded++;
		}
		if (task.getOffloadingDestination() != null && task.getOffloadingDestination() != ComputingNode.NULL) {
			String layer = String.valueOf(task.getOffloadingDestination().getType());
			placements.put(layer, placements.getOrDefault(layer, 0) + 1);
		}
		if (task.getRequestId() < 0)
			return;
		// {start time, subtasks left, failed}
		double[] request = openRequests.get(task.getRequestId());
		if (request == null) {
			request = new double[] { task.getRequestStartTime(), Math.max(1, task.getRequestTaskCount()), 0 };
			openRequests.put(task.getRequestId(), request);
		}
		if (failed)
			request[2] = 1;
		if (--request[1] > 0)
			return;
		openRequests.remove(task.getRequestId());
		if (request[2] > 0)
			requestsFailed++;
		else
			makespans.add(simulation.clock() - request[0]);
	}

	@Override
	public void onSimulationEnd() {
		Map<String, Double> energy = new TreeMap<>();
		int batteryDeaths = 0;
		for (ComputingNode node : getDataCentersManager().getComputingNodesGenerator().getAllNodesList()) {
			if (node.getEnergyModel() == null)
				continue;
			String layer = String.valueOf(node.getType());
			energy.put(layer, energy.getOrDefault(layer, 0.0) + node.getEnergyModel().getTotalEnergyConsumption());
			boolean crashed = node instanceof Example30Device && ((Example30Device) node).isCrashed();
			if (node.isDead() && !crashed && node.getEnergyModel().isBatteryPowered())
				batteryDeaths++;
		}
		int finished = requestsFailed + makespans.size();
		double failureRate = finished == 0 ? 0 : requestsFailed / (double) finished;
		SimLog.println("%s - %s %s %s: %d of %d requests failed (%.4f), %d subtasks failed, %d crashed devices",
				getClass().getSimpleName(), failureScenario, scenario.getStringOrchAlgorithm(),
				scenario.getStringOrchArchitecture(), requestsFailed, finished, failureRate, subtasksFailed,
				crashedDevices);
		String out = System.getProperty("example30.out");
		if (out == null || out.trim().isEmpty())
			return;
		writeJson(Paths.get(out), energy, batteryDeaths, finished, failureRate);
	}

	private void writeJson(Path path, Map<String, Double> energy, int batteryDeaths, int finished,
			double failureRate) {
		List<Double> sorted = new ArrayList<>(makespans);
		Collections.sort(sorted);
		double mean = 0;
		for (double m : sorted)
			mean += m;
		mean = sorted.isEmpty() ? 0 : mean / sorted.size();
		double p95 = sorted.isEmpty() ? 0 : sorted.get((int) Math.min(sorted.size() - 1, Math.ceil(0.95 * sorted.size()) - 1));
		StringBuilder json = new StringBuilder();
		json.append('{');
		field(json, "scenario", failureScenario);
		field(json, "edge_outage", "crash".equals(failureScenario) && edgeOutage);
		field(json, "algorithm", scenario.getStringOrchAlgorithm());
		field(json, "architecture", scenario.getStringOrchArchitecture());
		field(json, "devices", scenario.getDevicesCount());
		field(json, "seed", Randomness.isSeeded() ? Randomness.getSeed() : -1);
		field(json, "workload_hash", workloadHash);
		field(json, "requests_generated", requestsGenerated);
		field(json, "requests_finished", finished);
		field(json, "requests_failed", requestsFailed);
		field(json, "request_failure_rate", failureRate);
		field(json, "makespan_mean_s", mean);
		field(json, "makespan_p95_s", p95);
		field(json, "subtasks_succeeded", subtasksSucceeded);
		field(json, "subtasks_failed", subtasksFailed);
		field(json, "crashed_devices", crashedDevices);
		field(json, "battery_deaths", batteryDeaths);
		map(json, "failure_reasons", failureReasons);
		map(json, "placements", placements);
		map(json, "energy_wh", energy);
		json.setLength(json.length() - 1);
		json.append("}\n");
		try {
			if (path.getParent() != null)
				Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.APPEND)) {
				writer.write(json.toString());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot write " + path, e);
		}
	}

	private static void field(StringBuilder json, String key, Object value) {
		json.append('"').append(key).append("\":");
		if (value instanceof String)
			json.append('"').append(value).append('"');
		else if (value instanceof Double)
			json.append(String.format(Locale.ROOT, "%.6f", (Double) value));
		else
			json.append(value);
		json.append(',');
	}

	private static void map(StringBuilder json, String key, Map<String, ? extends Number> values) {
		json.append('"').append(key).append("\":{");
		for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
			json.append('"').append(entry.getKey()).append("\":");
			if (entry.getValue() instanceof Double)
				json.append(String.format(Locale.ROOT, "%.6f", entry.getValue().doubleValue()));
			else
				json.append(entry.getValue());
			json.append(',');
		}
		if (!values.isEmpty())
			json.setLength(json.length() - 1);
		json.append("},");
	}

	/**
	 * Hashes the arrival time, device, application and subtask of every task the
	 * generator produced, so runs of different orchestrators can be checked to
	 * share one workload.
	 */
	private String hashTaskList() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			java.util.Set<Integer> requests = new java.util.HashSet<>();
			for (java.util.Iterator<Task> it = taskList.iterator(); it.hasNext();) {
				Task task = it.next();
				String line = String.format(Locale.ROOT, "%d|%.6f|%d|%d|%s;", task.getId(), task.getTime(),
						task.getEdgeDevice().getId(), task.getApplicationID(), task.getSubtaskName());
				digest.update(line.getBytes(StandardCharsets.UTF_8));
				requests.add(task.getRequestId() < 0 ? -task.getId() - 1 : task.getRequestId());
			}
			requestsGenerated = requests.size();
			StringBuilder hex = new StringBuilder();
			for (byte b : digest.digest())
				hex.append(String.format("%02x", b));
			return hex.substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
