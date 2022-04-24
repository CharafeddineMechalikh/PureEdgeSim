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
import com.mechalikh.pureedgesim.taskgenerator.DefaultTasksGenerator; 

public class Example1 {
	/**
	 * This is a simple example showing how to launch simulation using custom
	 * mobility model, energy model, custom edge orchestrator, custom tasks
	 * generator, and custom edge devices. By removing them, it will use the
	 * default models provided by PureEdgeSim. 
	 * 
	 * @author Charafeddine Mechalikh
	 * @since  PureEdgeSim 5.0
	 */
	public Example1() {	
		//Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// To change the mobility model
		sim.setCustomMobilityModel(Example2CustomMobilityModel.class);

		// To change the tasks orchestrator
		sim.setCustomEdgeOrchestrator(Example8FuzzyLogicOrchestrator.class);

		// To change the tasks generator
		sim.setCustomTaskGenerator(DefaultTasksGenerator.class); 

		/* to use the default one you can simply delete or comment those lines */

		// Finally,you can launch the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example1();
	}

}
