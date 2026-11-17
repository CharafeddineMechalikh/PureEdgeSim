/* Reference placement policy for the P1 common benchmark in EdgeCloudSim.
 * Per-task placement: this platform has no global periodic loop, so the
 * temporal-resolution factor is genuinely unavailable here (a capability
 * finding, not an omission). We log each decision and its timestamp.
 */
package crosssim;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.utils.SimLogger;

public class CommonEdgeOrchestrator extends EdgeOrchestrator {
    private static final String CSV = System.getProperty("cbs.decisions", "decisions.csv");
    private static boolean headerWritten = false;
    private final String architecture;

    public CommonEdgeOrchestrator(String policy, String scenario) {
        super(policy, scenario);
        this.architecture = System.getProperty("cbs.arch", "EDGE_AND_CLOUD");
    }

    @Override
    public void initialize() { }

    @Override
    public int getDeviceToOffload(Task task) {
        // Edge-only: never cloud. Edge-and-cloud / ALL: cloud as fallback is
        // implemented in getVmToOffload by returning a cloud VM when no edge
        // VM fits; this keeps the device-id contract simple.
        return SimSettings.GENERIC_EDGE_DEVICE_ID;
    }

    @Override
    public Vm getVmToOffload(Task task, int deviceId) {
        Vm selected = null;
        int hosts = SimSettings.getInstance().getNumOfEdgeHosts();
        for (int h = 0; h < hosts; h++) {
            for (EdgeVM vm : SimManager.getInstance().getEdgeServerManager().getVmList(h)) {
                double free = 100.0 - vm.getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());
                if (free >= 25.0) { selected = vm; break; }
            }
            if (selected != null) break;
        }
        String tier = selected != null ? "edge" : "cloud";
        if (selected == null && !architecture.equals("EDGE_ONLY")) {
            List<edu.boun.edgecloudsim.cloud_server.CloudVM> cloud =
                    SimManager.getInstance().getCloudServerManager().getVmList(0);
            selected = (cloud == null || cloud.isEmpty()) ? null : cloud.get(0);
            tier = selected != null ? "cloud" : "unplaced";
        }
        log(task, tier);
        return selected;
    }

    private void log(Task task, String tier) {
        try {
            boolean exists = Files.exists(Paths.get(CSV));
            try (BufferedWriter w = Files.newBufferedWriter(Paths.get(CSV),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (!exists || !headerWritten) {
                    w.write("task_id,decision_s,release_s,task_type,tier,device_id\n");
                    headerWritten = true;
                }
                w.write(String.format(Locale.US, "%d,%.6f,%.6f,%d,%s,%d%n",
                        task.getCloudletId(), CloudSim.clock(), task.getCreationTime(),
                        task.getTaskType(), tier, task.getMobileDeviceId()));
            }
        } catch (IOException e) {
            SimLogger.printLine("[cbs] decision log failed: " + e.getMessage());
        }
    }

    @Override public void processEvent(SimEvent e) { }
    @Override public void shutdownEntity() { }
    @Override public void startEntity() { }
}
