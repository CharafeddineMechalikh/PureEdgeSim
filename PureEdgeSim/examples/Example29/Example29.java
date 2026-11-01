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

/**
 * A shared uplink that goes down for thirty seconds, and the fleet that retries
 * into it.
 * <p>
 * Twenty phones send one small request a minute to the cloud, over one
 * metropolitan uplink of a megabit per second. At four minutes the uplink stops
 * delivering for thirty seconds, and every request that reaches it in that window
 * is lost with it. The example runs the same failure four times, with four
 * clients, and the comparison is the point:
 *
 * <pre>
 * none:   no retry, which is what the framework does by default
 * budget: three attempts, exponential backoff, jitter
 * storm:  ten attempts, no backoff, no jitter
 * storm without the outage: the same client on a healthy uplink
 * </pre>
 * <p>
 * The fourth run is the control: it shows that a retrying client costs nothing
 * while nothing fails, so every difference between the first three runs is the
 * outage and the recovery it provoked. The number to read is not the requests the
 * outage took, which no client can avoid, but the requests that were healthy when
 * the uplink came back and still missed their deadline in the backlog the storm
 * left behind.
 * <p>
 * Each run is a separate process, because a run that follows another one reuses
 * the settings and the seed of the first:
 *
 * <pre>
 * mvn exec:java -Dexec.mainClass="examples.Example29"
 * mvn exec:java -Dexec.mainClass="examples.Example29" -Dexample29.policy=BUDGET
 * mvn exec:java -Dexec.mainClass="examples.Example29" -Dexample29.policy=STORM
 * mvn exec:java -Dexec.mainClass="examples.Example29" -Dexample29.policy=STORM -Dexample29.outage=false
 * </pre>
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.1
 *
 * @see Example29RetrySimulationManager
 */
public class Example29 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example29/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example29/output/";

	/**
	 * Runs the scenario once, with the client and the outage selected on the command
	 * line.
	 */
	public Example29() {
		Simulation sim = new Simulation();
		sim.setCustomSimulationManager(Example29RetrySimulationManager.class);
		sim.setCustomSettingsFolder(SETTINGS);
		sim.setCustomOutputFolder(OUTPUT);
		sim.launchSimulation();
	}

	/**
	 * Starts the example.
	 *
	 * @param args unused; select the arm with {@code -Dexample29.policy} and
	 *             {@code -Dexample29.outage}, as JVM arguments before the main
	 *             class
	 */
	public static void main(String[] args) {
		new Example29();
	}
}
