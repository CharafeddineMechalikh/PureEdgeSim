/* Mobility adapter: EdgeCloudSim's nomadic model starts device locations at
 * t = CLIENT_ACTIVITY_START_TIME (10 s). The common trace begins at t = 0, so
 * this wrapper returns the first known location for earlier timestamps,
 * preserving the shared arrivals exactly. No mobility semantics are changed
 * after t = 10 s.
 */
package crosssim;

import edu.boun.edgecloudsim.mobility.NomadicMobility;
import edu.boun.edgecloudsim.utils.Location;

public class CommonMobilityModel extends NomadicMobility {
    public CommonMobilityModel(int devices, double simulationTime) {
        super(devices, simulationTime);
    }

    @Override
    public Location getLocation(int deviceId, double time) {
        try {
            return super.getLocation(deviceId, time);
        } catch (IllegalStateException e) {
            if (time < 10.0) {
                return super.getLocation(deviceId, 10.0);
            }
            throw e;
        }
    }
}
