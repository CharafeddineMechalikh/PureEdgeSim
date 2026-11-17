/* Actuator that logs end-to-end tuple delays for the P1 common scenario. */
package crosssim;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.entities.Actuator;
import org.fog.entities.Tuple;
import org.fog.utils.FogEvents;
import org.fog.utils.GeoLocation;
import org.fog.utils.TimeKeeper;

public class LoggingActuator extends Actuator {
    private static final String CSV = System.getProperty("cbs.ifog.results",
            "ifog_results.csv");
    private static boolean header = false;

    public LoggingActuator(String name, int userId, String appId, int gatewayDeviceId,
                           double latency, GeoLocation geoLocation, String actuatorType,
                           String srcModuleName) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation,
                actuatorType, srcModuleName);
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == FogEvents.TUPLE_ARRIVAL && ev.getData() instanceof Tuple tuple) {
            Double emit = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
            if (emit != null) {
                try {
                    boolean exists = Files.exists(Paths.get(CSV));
                    try (BufferedWriter w = Files.newBufferedWriter(Paths.get(CSV),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                        if (!exists || !header) {
                            w.write("tuple_id,actual_id,tuple_type,emit_s,arrival_s,delay_s\n");
                            header = true;
                        }
                        w.write(String.format(Locale.US, "%d,%d,%s,%.6f,%.6f,%.6f%n",
                                tuple.getCloudletId(), tuple.getActualTupleId(),
                                tuple.getTupleType(), emit, CloudSim.clock(),
                                CloudSim.clock() - emit));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        super.processEvent(ev);
    }
}
