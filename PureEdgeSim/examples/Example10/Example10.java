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
 * Task partitioning with a genetic algorithm.
 * <p>
 * The applications file of this example declares the same video analytics
 * pipeline as {@link Example9}. Instead of deriving the partition from the
 * graph structure, {@link GeneticPartitioner} searches the node of every
 * subtask: the fitness of a chromosome is the estimated makespan of the
 * pipeline, plus part of the time spent transferring results between nodes and
 * of the excess work of the busiest node. Subtasks that receive the same node
 * form the same partition.
 * <p>
 * The console prints the best plan and its estimate before the simulation
 * starts, then the run proceeds with that placement. Set {@code random_seed} in
 * the settings file to reproduce the same plan, and remove it to draw a
 * different one.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see GeneticPartitioner
 * @see Example10GeneticOrchestrator
 */
public class Example10 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example10/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example10/output/";

	/**
	 * Runs the genetic algorithm partitioning scenario.
	 */
	public Example10() {
		Simulation sim = new Simulation();

		// Search the subtask placement with a genetic algorithm.
		sim.setCustomEdgeOrchestrator(Example10GeneticOrchestrator.class);

		sim.setCustomOutputFolder(OUTPUT);
		sim.setCustomSettingsFolder(SETTINGS);

		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example10();
	}
}
