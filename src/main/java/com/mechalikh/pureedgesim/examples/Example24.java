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
 * Dynamic repartition of a service graph.
 * <p>
 * The scenario runs the video analytics pipeline on 200 devices, half of them
 * mobile and some of them losing their battery, while four edge data centers
 * serve them. {@link Example24DynamicRepartitionOrchestrator} recomputes the
 * tier of every subtask every 30 seconds from the current queues, CPU
 * utilization, capacity, and reachable nodes, and replans immediately when a
 * planned tier loses all of its nodes.
 * <p>
 * The same scenario can be run with a plan computed once and never updated:
 * <pre>
 * mvn exec:java -Dexec.mainClass="examples.Example24"
 * mvn exec:java -Dexec.mainClass="examples.Example24" -Dexample24.frozenPlan=true
 * </pre>
 * The first command is the dynamic placement, the second the static baseline.
 * Compare the successful requests, the average request makespan, and the
 * number of subtasks moved by the replans printed in the console.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example24DynamicRepartitionOrchestrator
 */
public class Example24 {

	private static final String SETTINGS = "examples/Example24/settings/";
	private static final String OUTPUT = "output/Example24_output/";

	/**
	 * Runs the dynamic repartition scenario.
	 */
	public Example24() {
		Simulation sim = new Simulation();

		sim.setCustomEdgeOrchestrator(Example24DynamicRepartitionOrchestrator.class);

		sim.setCustomOutputFolder(OUTPUT);
		sim.setCustomSettingsFolder(SETTINGS);

		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example24();
	}
}
