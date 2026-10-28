package examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * Task generator that replays a recorded workload instead of drawing one.
 * <p>
 * Every line of {@link #TRACE} is one invocation of a production LLM inference
 * service, with the time at which it arrived and the number of prompt and
 * completion tokens it used. Each line becomes exactly one task: the arrival
 * time sets when the request is generated, the token counts set the request and
 * result sizes and the compute length, and the completion length also sets the
 * latency budget, which is what a streaming service really guarantees.
 * <p>
 * The recorded stream is the arrival process of a whole service, not of one
 * device, so the requests are handed to the edge devices in turn. That keeps
 * the fleet evenly loaded while the instantaneous arrival rate, which is what
 * the example measures, stays exactly as recorded.
 * <p>
 * Nothing here is random: with the same trace file the same run always produces
 * the same task list, whatever {@code random_seed} says.
 *
 * @since PureEdgeSim 7.0
 */
public class Example25TraceTaskGenerator extends DefaultTaskGenerator {

	/** The recorded invocations, one per line, after a header line. */
	private static final Path TRACE = Path
			.of("PureEdgeSim/examples/Example25/trace/azure_llm_code_1h.csv");

	/** Index of the only application of this scenario in {@code applications.xml}. */
	private static final int APPLICATION = 0;

	/** Bytes of text per token, for the request and result sizes. */
	private static final double BYTES_PER_TOKEN = property("example25.bytesPerToken", 4);

	/** Millions of instructions to read one prompt token (the prefill phase). */
	private static final double MI_PER_CONTEXT_TOKEN = property("example25.miPerContextToken", 12);

	/** Millions of instructions to produce one completion token (the decode phase). */
	private static final double MI_PER_GENERATED_TOKEN = property("example25.miPerGeneratedToken", 2000);

	/** Latency budget of a request in seconds, before its completion is counted. */
	private static final double TTFT_BUDGET = property("example25.ttftBudget", 2);

	/** Latency budget granted to each generated token, in seconds. */
	private static final double DECODE_BUDGET = property("example25.decodeBudget", 0.1);

	/**
	 * Reads a tuning value from the command line, so that the mapping of tokens to
	 * bytes, instructions and deadlines can be changed without editing the code.
	 *
	 * @param name         the system property to read
	 * @param defaultValue the value used when the property was not given
	 * @return the value of the property, or the default one
	 */
	private static double property(String name, double defaultValue) {
		String value = System.getProperty(name);
		return value == null ? defaultValue : Double.parseDouble(value);
	}

	/**
	 * Creates the generator for the given simulation.
	 *
	 * @param simulationManager the simulation manager
	 */
	public Example25TraceTaskGenerator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	/**
	 * Reads the trace and turns every recorded invocation into one task.
	 * <p>
	 * Invocations that fall after the end of the simulated period are dropped and
	 * reported, so shortening {@code simulation_time} truncates the replay instead
	 * of failing it.
	 *
	 * @return the tasks of the replay, ordered by recorded arrival time
	 */
	@Override
	public FutureQueue<Task> generate() {
		devicesList.removeIf(dev -> !dev.isGeneratingTasks());
		if (devicesList.isEmpty())
			throw new IllegalStateException(
					"no edge device generates tasks, so the trace of " + TRACE + " has nowhere to go");

		Application appParams = SimulationParameters.applicationList.get(APPLICATION);
		int duration = (int) SimulationParameters.simulationDuration;
		List<String> lines = readTrace();
		int skipped = 0;
		int device = 0;

		for (int line = 1; line < lines.size(); line++) {
			String[] fields = lines.get(line).split(",");
			if (fields.length < 3)
				throw new IllegalStateException("malformed trace line " + (line + 1) + " of " + TRACE);

			long arrivalMs = Long.parseLong(fields[0].trim());
			int contextTokens = Integer.parseInt(fields[1].trim());
			int generatedTokens = Integer.parseInt(fields[2].trim());
			// One second of initialization, so the first request is not generated
			// before the deployment of the simulation is finished.
			int time = 1 + (int) Math.round(arrivalMs / 1000D);
			if (time > duration) {
				skipped++;
				continue;
			}
			insert(appParams, devicesList.get(device++ % devicesList.size()), time, contextTokens, generatedTokens);
		}

		getSimulationManager().getSimulationLogger().print("Trace replay: %d requests scheduled between 1 and %d s, "
				+ "%d dropped because they arrive after the end of the run.", id, duration, skipped);
		return getTaskList();
	}

	/**
	 * Creates the task of one recorded invocation and schedules it at its recorded
	 * arrival time. The sizes and the length come from the trace, the container
	 * size and the application type from {@code applications.xml}.
	 *
	 * @param appParams       the application declaration of the scenario
	 * @param dev             the edge device that sends the request
	 * @param time            the simulated second at which the request arrives
	 * @param contextTokens   the number of prompt tokens of the recorded invocation
	 * @param generatedTokens the number of completion tokens of the recorded
	 *                        invocation
	 */
	protected void insert(Application appParams, ComputingNode dev, int time, int contextTokens,
			int generatedTokens) {
		double latency = TTFT_BUDGET + generatedTokens * DECODE_BUDGET;
		Task task = createTask(++id).setType(appParams.getType())
				.setFileSizeInBits(Math.round(contextTokens * BYTES_PER_TOKEN * 8))
				.setOutputSizeInBits(Math.round(generatedTokens * BYTES_PER_TOKEN * 8))
				.setContainerSizeInBits(appParams.sampleContainerSize()).setApplicationID(APPLICATION)
				.setMaxLatency(latency)
				.setLength(contextTokens * MI_PER_CONTEXT_TOKEN + generatedTokens * MI_PER_GENERATED_TOKEN)
				.setEdgeDevice(dev)
				.setRegistry(getSimulationManager().getDataCentersManager().getComputingNodesGenerator()
						.getCloudOnlyList().get(0));
		task.setRequest(++requestId, 1, time, time + latency);
		scheduleTask(task, time);
	}

	/**
	 * Reads the whole trace file.
	 *
	 * @return its lines, the first one being the header
	 */
	private List<String> readTrace() {
		try {
			return Files.readAllLines(TRACE);
		} catch (IOException e) {
			throw new IllegalStateException("cannot read the trace file " + TRACE
					+ ", run the example from the repository root", e);
		}
	}
}
