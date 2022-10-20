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

public class Example5 {
	/**
	 * This is a simple example showing how to launch simulation using a custom task
	 * orchesrator. The CustomEdgeOrchestrator.java is located under the examples/
	 * folder. As you can see, this class extends the Main class provided by
	 * PureEdgeSim, which is required for this example to work.
	 * 
	 * @author Charafeddine Mechalikh
	 * @since PureEdgeSim 2.2
	 */
	public Example5() {

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();
		/*
		 * To use your custom Edge orchestrator class, do this: The custom orchestrator
		 * class can be found in the examples folder. by removing this line, pureEdgeSim
		 * will use the default orchestrator class.
		 */
		sim.setCustomEdgeOrchestrator(Example8FuzzyLogicOrchestrator.class);

		/*
		 * This custom class uses another orchestrator algorithm called
		 * Increase_Lifetime, that avoids offloading the tasks to battery-powered
		 * devices. This algorithm wotks better when you use the ALL architecture you
		 * can compare its performance to the Round-Robin and Trade-off algorithms used
		 * by the default orchestrator class, as this algorihtm relies more on the cloud
		 * and the edge data centers (cloud and edge computing). You can use your own
		 * algorithm by adding it to your custom class. After adding it to the
		 * orchestrator class,to use it you need to add it to the simulation parameters
		 * file (under the settings/ folder). To use the PureEdgeSim default edge
		 * orchestrator class you can also uncomment this:
		 */
		// setCustomEdgeOrchestrator(DefaultEdgeOrchestrator.class);

		// Start the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example5();
	}

}
