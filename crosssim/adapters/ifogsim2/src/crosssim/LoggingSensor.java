/* Sensor that logs emitted tuples for the common-scenario adapter. */
package crosssim;

import org.fog.entities.Sensor;

public class LoggingSensor extends Sensor {
    private int emitted = 0;

    public LoggingSensor(String name, String tupleType, int userId, String appId,
                         org.fog.utils.distribution.Distribution distribution) {
        super(name, tupleType, userId, appId, distribution);
        // iFogSim2's 5-arg Sensor constructor leaves sensorName null, which
        // breaks TimeKeeper loop matching. The loop starts at the tuple type.
        setSensorName(tupleType);
    }

    @Override
    public void startEntity() {
        // Do not apply iFogSim2's default 50 s TRANSMISSION_START_DELAY: the
        // shared trace fixes the first arrival explicitly.
        send(getGatewayDeviceId(), org.cloudbus.cloudsim.core.CloudSim.getMinTimeBetweenEvents(),
                org.fog.utils.FogEvents.SENSOR_JOINED, getGeoLocation());
        send(getId(), getTransmitDistribution().getNextValue(),
                org.fog.utils.FogEvents.EMIT_TUPLE);
    }

    @Override
    public void transmit() {
        if (emitted++ < 3) {
            System.out.printf("[cbs-sensor] %s emit at %.6f%n",
                    getTupleType(), org.cloudbus.cloudsim.core.CloudSim.clock());
        }
        super.transmit();
    }
}
