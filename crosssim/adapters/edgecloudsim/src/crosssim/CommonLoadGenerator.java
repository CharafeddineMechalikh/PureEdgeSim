/* EdgeCloudSim adapter for the P1 common benchmark (P1-CBS v1.0).
 * Reads the shared trace produced by crosssim/common_trace.py and emits one
 * row per task with release / decision / dispatch / execution timestamps.
 */
package crosssim;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.TaskProperty;

/** Load generator that replays the simulator-independent trace. */
public class CommonLoadGenerator extends LoadGeneratorModel {
    private final String tracePath;
    private final Map<Integer, Integer> deviceType = new HashMap<>();

    public CommonLoadGenerator(int devices, double simulationTime, String scenario) {
        super(devices, simulationTime, scenario);
        this.tracePath = System.getProperty("cbs.trace");
        if (this.tracePath == null) {
            throw new IllegalArgumentException("cbs.trace must be set to the shared trace CSV");
        }
    }

    @Override
    public void initializeModel() {
        // The shared trace has one row per application service. The independent
        // arm aggregates each workflow into one task with the calibrated class
        // length, exactly as the YAFS adapter does.
        Map<String, double[]> aggregate = new LinkedHashMap<>();
        Map<String, Integer> classOf = new HashMap<>();
        Map<String, Integer> deviceOf = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(tracePath))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] f = line.split(",", -1);
                double arrival = Double.parseDouble(f[1]);
                if (arrival >= simulationTime) continue;
                String workflow = f[3];
                double[] row = aggregate.computeIfAbsent(workflow, k -> new double[]{arrival, 0, 0, 0});
                row[1] += Double.parseDouble(f[6]);   // total MI
                if (row[2] == 0) row[2] = Double.parseDouble(f[7]);  // first input
                row[3] = Double.parseDouble(f[8]);    // last output
                classOf.put(workflow, f[2].equals("C1") ? 0 : f[2].equals("C2") ? 1 : 2);
                deviceOf.put(workflow, Integer.parseInt(f[11]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        taskList = new ArrayList<>();
        for (Map.Entry<String, double[]> e : aggregate.entrySet()) {
            double[] row = e.getValue();
            int device = deviceOf.get(e.getKey()) % numberOfMobileDevices;
            taskList.add(new TaskProperty(row[0], device, classOf.get(e.getKey()),
                    1, (long) row[1], (long) row[2], (long) row[3]));
            deviceType.put(device, classOf.get(e.getKey()));
        }
        taskList.sort(Comparator.comparingDouble(TaskProperty::getStartTime));
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        return deviceType.getOrDefault(deviceId, 0);
    }
}
