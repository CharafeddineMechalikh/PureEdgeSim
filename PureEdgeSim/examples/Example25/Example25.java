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
 * Replay of a recorded production workload.
 * <p>
 * The scenario runs one hour of a published Azure LLM inference trace, the code
 * completion workload of the Splitwise dataset, on a fleet of 50 devices served
 * by one edge data center. {@link Example25TraceTaskGenerator} turns every
 * recorded invocation into one request: its arrival time, its prompt and
 * completion sizes, its compute length and its latency budget all come from the
 * trace, so the burstiness of real traffic reaches the simulator untouched.
 * <p>
 * The same scenario can be run with the requests drawn at random instead, at the
 * same mean rate and with the same mean sizes:
 * <pre>
 * mvn exec:java -Dexec.mainClass="examples.Example25"
 * mvn exec:java -Dexec.mainClass="examples.Example25" -Dexample25.syntheticBaseline=true
 * </pre>
 * The first command replays the trace, the second generates the synthetic
 * equivalent. Compare the deadline misses and the peak CPU utilization of the
 * two runs printed in the console: the mean load is the same, the trace is the
 * one that overloads the server.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 7.0
 *
 * @see Example25TraceTaskGenerator
 */
public class Example25 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example25/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example25/output/";

	/**
	 * Creates and runs the simulation, replaying the trace unless the synthetic
	 * baseline was requested.
	 */
	public Example25() {
		Simulation sim = new Simulation();

		if (!Boolean.getBoolean("example25.syntheticBaseline"))
			sim.setCustomTaskGenerator(Example25TraceTaskGenerator.class);

		sim.setCustomSettingsFolder(SETTINGS);
		sim.setCustomOutputFolder(OUTPUT);

		sim.launchSimulation();
	}

	/**
	 * Starts the example.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		new Example25();
	}
}
