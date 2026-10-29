/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract.Files;

/**
 * Congestion on a shared uplink, and what the two network models make of it.
 * <p>
 * Twenty devices upload a video clip to the cloud once a minute, and all of
 * them share one metropolitan link. The link is narrower than the traffic it is
 * given: the requests offer four megabits per second and the link carries three,
 * so the backlog cannot drain and the queue grows for the whole run. That is the
 * scenario {@link Example27CongestionAuditor} measures, on two architectures at
 * once, because the same traffic served by the edge data center never touches
 * the congested link and its delay is the reference the cloud delay has to be
 * read against.
 * <p>
 * The example exists to show what {@code realistic_network_model} is worth. With
 * it off, which is the framework default, a transfer finishes in a single update:
 * it is charged the bandwidth shared with the transfers that happen to be on the
 * same link at the same instant, and then it is gone. Nothing accumulates, so a
 * link that is offered more than it can carry looks merely a little slow. With
 * it on, a transfer stays on the link until its bytes have really crossed it, and
 * every new transfer then competes with the ones already in flight, which is the
 * behaviour of a saturated link and the reason requests start missing their
 * deadline.
 * <p>
 * The two modes run the same scenario, so the run compares with itself:
 * 
 * <pre>
 * mvn exec:java -Dexec.mainClass="examples.Example27"
 * mvn exec:java -Dexec.mainClass="examples.Example27" -Dexample27.realistic=false
 * </pre>
 * 
 * The second line swaps the parameters file for
 * {@code settings/linear-network-model.properties}, which differs by one setting
 * and by nothing else. The summary printed at the end of each iteration gives the
 * mean, median and ninetieth percentile of the network time, the part of the end
 * to end delay that the network accounts for, and the failure reasons.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example27CongestionAuditor
 */
public class Example27 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example27/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example27/output/";

	/** Whether transfers advance over the link in steps, as on a real network. */
	public static final boolean REALISTIC = !"false".equals(System.getProperty("example27.realistic"));

	/**
	 * Runs the congested scenario on the cloud and on the edge, with the auditor
	 * that measures where the delay of each request comes from.
	 */
	public Example27() {
		Simulation sim = new Simulation();

		sim.setCustomSimulationManager(Example27CongestionAuditor.class);
		sim.setCustomSettingsFolder(SETTINGS);
		sim.setCustomOutputFolder(OUTPUT);

		// The two modes are the same scenario with one setting flipped, so the linear
		// model gets its own parameters file and every other file stays as it is.
		if (!REALISTIC)
			sim.setCustomFilePath(SETTINGS + "linear-network-model.properties", Files.SIMULATION_PARAMETERS);

		sim.launchSimulation();
	}

	/**
	 * Starts the example.
	 *
	 * @param args the mode switch, as {@code -Dexample27.realistic=false} system
	 *             property; pass it before the main class, on the JVM arguments,
	 *             not after it
	 */
	public static void main(String[] args) {
		new Example27();
	}
}
