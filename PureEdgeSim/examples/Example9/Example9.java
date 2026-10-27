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
 * Task partitioning with graph theory.
 * <p>
 * The applications file of this example declares one service graph: a video
 * analytics pipeline whose seven subtasks depend on each other. The
 * orchestrator partitions that graph before the simulation starts, using
 * connected components, balanced assignment and Kernighan-Lin cut refinement
 * ({@link GraphPartitioner}), then places each subtask on the tier of its
 * partition: mist devices, edge data centers, then the cloud.
 * <p>
 * The console prints the partitions, their load and the number of dependency
 * edges that cross two partitions. The generated tasks in the results file
 * include every subtask, and the per-partition assignment can be read from that
 * same run because each subtask keeps the name declared in the applications
 * file.
 * <p>
 * Compare with {@link Example10}, which searches the placement of the same kind
 * of graph with a genetic algorithm.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see GraphPartitioner
 * @see Example9PartitioningOrchestrator
 */
public class Example9 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example9/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example9/output/";

	/**
	 * Runs the graph partitioning scenario.
	 */
	public Example9() {
		Simulation sim = new Simulation();

		// Partition the service graphs and place each subtask on its partition.
		sim.setCustomEdgeOrchestrator(Example9PartitioningOrchestrator.class);

		sim.setCustomOutputFolder(OUTPUT);
		sim.setCustomSettingsFolder(SETTINGS);

		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example9();
	}
}
