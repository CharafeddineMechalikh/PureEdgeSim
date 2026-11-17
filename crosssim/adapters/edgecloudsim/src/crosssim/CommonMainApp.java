/* Entry point for the EdgeCloudSim common-scenario adapter. */
package crosssim;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class CommonMainApp {
    public static void main(String[] args) throws Exception {
        Log.disable();
        SimLogger.enablePrintLog();
        if (args.length != 5) {
            throw new IllegalArgumentException("config edge_devices applications outdir iteration");
        }
        String configFile = args[0], edgeDevicesFile = args[1], applicationsFile = args[2];
        String outputFolder = args[3];
        int iterationStart = Integer.parseInt(args[4]);

        SimSettings SS = SimSettings.getInstance();
        if (!SS.initialize(configFile, edgeDevicesFile, applicationsFile)) {
            throw new IllegalStateException("cannot initialize simulation settings");
        }
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        SimLogger.printLine("[cbs] EdgeCloudSim common scenario started " + df.format(new Date()));

        for (int iteration = iterationStart; iteration <= iterationStart; iteration++) {
            File dir = new File(outputFolder);
            if (!dir.exists()) dir.mkdirs();
            SimLogger.getInstance().simStarted(outputFolder,
                    "SIMRESULT_CBS_" + System.getProperty("cbs.arch", "EDGE_AND_CLOUD"));
            for (int j = SS.getMinNumOfMobileDev(); j <= SS.getMaxNumOfMobileDev();
                 j += SS.getMobileDevCounterSize()) {
                for (String policy : SS.getOrchestratorPolicies()) {
                    for (String scenario : SS.getSimulationScenarios()) {
                        CloudSim.init(2, Calendar.getInstance(), false, 0.01);
                        ScenarioFactory factory = new CommonScenarioFactory(
                                j, SS.getSimulationTime(), policy, scenario);
                        new SimManager(factory, j, scenario, policy).startSimulation();
                    }
                }
            }
        }
        SimLogger.printLine("[cbs] EdgeCloudSim common scenario finished");
    }
}
