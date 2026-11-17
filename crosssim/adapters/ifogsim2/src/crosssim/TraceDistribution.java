/* iFogSim2 adapter for the P1 common benchmark.
 * A distribution that replays exact absolute arrival times from the shared
 * trace, instead of sampling inter-arrival times from a parametric law.
 */
package crosssim;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.fog.utils.distribution.Distribution;

public class TraceDistribution extends Distribution {
    private final List<Double> arrivals = new ArrayList<>();
    private int index = 0;
    private double previous = 0.0;

    /** @param path CSV with an arrival_s column (header present). */
    public TraceDistribution(String path, String classId) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            String header = reader.readLine();
            int arrivalCol = -1, classCol = -1, workflowCol = -1;
            String[] columns = header.split(",");
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equals("arrival_s")) arrivalCol = i;
                if (columns[i].equals("class")) classCol = i;
                if (columns[i].equals("workflow_id")) workflowCol = i;
            }
            Set<String> seen = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] f = line.split(",", -1);
                if (classCol >= 0 && !f[classCol].equals(classId)) continue;
                String workflow = workflowCol >= 0 ? f[workflowCol] : line;
                if (!seen.add(workflow)) continue;   // one arrival per workflow
                arrivals.add(Double.parseDouble(f[arrivalCol]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Collections.sort(arrivals);
    }

    @Override
    public double getNextValue() {
        if (index >= arrivals.size()) return 1e9;   // stop emitting
        double gap = arrivals.get(index) - previous;
        previous = arrivals.get(index);
        index++;
        return Math.max(gap, CloudSimEpsilon.VALUE);
    }

    @Override public int getDistributionType() { return Distribution.DETERMINISTIC; }
    @Override public double getMeanInterTransmitTime() { return 0.0; }
    public int size() { return arrivals.size(); }
}

final class CloudSimEpsilon {
    static final double VALUE = 1e-9;
}
