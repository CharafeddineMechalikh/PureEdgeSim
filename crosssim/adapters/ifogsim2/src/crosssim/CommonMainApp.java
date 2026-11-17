/* iFogSim2 adapter for the P1 common benchmark.
 * Usage (from the vendored iFogSim2 root):
 *   java -cp <classpath> crosssim.CommonMainApp <trace.csv> <topology.json> <results.csv>
 */
package crosssim;

import java.util.*;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.PhysicalTopology;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.utils.GeoLocation;
import org.fog.utils.JsonToTopology;

public class CommonMainApp {
    private static final int MI = 1000;

    public static void main(String[] args) {
        if (args.length != 5) {
            throw new IllegalArgumentException("trace topology results duration seed");
        }
        String trace = args[0], topology = args[1], results = args[2];
        double duration = Double.parseDouble(args[3]);
        int seed = Integer.parseInt(args[4]);
        System.setProperty("cbs.ifog.results", results);

        try {
            Log.disable();
            CloudSim.init(1, Calendar.getInstance(), false);
            FogBroker broker = new FogBroker("broker");
            String appId = "p1-common";

            Application app = Application.createApplication(appId, broker.getId());
            app.addAppModule("svc", MI);
            int[] lengths = {2000, 12000, 20000};
            int[] sizes = {50, 1500, 900};
            String[] classes = {"C1", "C2", "C3"};
            List<AppLoop> loops = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String cls = classes[i];
                app.addAppEdge(cls, "svc", lengths[i], sizes[i], cls + "_REQ",
                        Tuple.UP, AppEdge.SENSOR);
                app.addAppEdge("svc", cls + "_ACT", lengths[i] / 10, sizes[i] / 10,
                        cls + "_RES", Tuple.DOWN, AppEdge.MODULE);
                app.addTupleMapping("svc", cls + "_REQ", cls + "_RES",
                        new FractionalSelectivity(1.0));
                loops.add(new AppLoop(Arrays.asList(cls, "svc", cls + "_ACT")));
            }
            app.setLoops(loops);
            app.setUserId(broker.getId());

            PhysicalTopology physical = JsonToTopology.getPhysicalTopology(
                    broker.getId(), appId, topology);

            List<Sensor> loggingSensors = new ArrayList<>();
            int gateway = -1;
            for (FogDevice device : physical.getFogDevices()) {
                if (device.getLevel() == 1) { gateway = device.getId(); break; }
            }
            if (gateway < 0) gateway = physical.getFogDevices().get(0).getId();
            for (String cls : classes) {
                LoggingSensor sensor = new LoggingSensor("sensor-" + cls, cls,
                        broker.getId(), appId, new TraceDistribution(trace, cls));
                sensor.setGatewayDeviceId(gateway);
                sensor.setLatency(0.01);
                sensor.setApp(app);
                loggingSensors.add(sensor);
            }
            physical.setSensors(loggingSensors);
            List<Actuator> actuators = new ArrayList<>();
            for (String cls : classes) {
                actuators.add(new LoggingActuator("actuator-" + cls, broker.getId(), appId,
                        gateway, 0.01, new GeoLocation(0, 0),
                        cls + "_ACT", "svc"));
            }

            Controller controller = new Controller("controller", physical.getFogDevices(),
                    physical.getSensors(), actuators);
            controller.submitApplication(app, 0,
                    new ModulePlacementEdgewards(physical.getFogDevices(),
                            loggingSensors, actuators, app,
                            ModuleMapping.createModuleMapping()));
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            System.out.printf("[cbs-ifog] seed=%d duration=%.0f results=%s%n",
                    seed, duration, results);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
