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
	 * orchestrator. The orchestrator used here is
	 * {@link Example8FuzzyLogicOrchestrator}, which implements the fuzzy-logic
	 * algorithm of Example 8. By removing the setCustomEdgeOrchestrator line,
	 * PureEdgeSim uses the default orchestrator.
	 * 
	 * This example needs the jFuzzyLogic jar from PureEdgeSim/libs/ and is excluded
	 * from the default Maven build for that reason. Its scenario, in
	 * examples/Example5/settings/, keeps the default files and adds FUZZY_LOGIC to
	 * the algorithm list.
	 * 
	 * @author Charafeddine Mechalikh
	 * @since PureEdgeSim 2.2
	 */
	private static final String SETTINGS = "PureEdgeSim/examples/Example5/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example5/output/";

	public Example5() {

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		sim.setCustomOutputFolder(OUTPUT);
		sim.setCustomSettingsFolder(SETTINGS);

		/*
		 * To use your custom Edge orchestrator class, do this: The custom orchestrator
		 * class can be found under the examples package. By removing this line,
		 * PureEdgeSim will use the default orchestrator class.
		 */
		sim.setCustomEdgeOrchestrator(Example8FuzzyLogicOrchestrator.class);

		/*
		 * The fuzzy-logic orchestrator accepts the algorithm name "FUZZY_LOGIC" in
		 * addition to the built-in ROUND_ROBIN. Both are listed in the
		 * orchestration_algorithms property of the settings file, so the run compares
		 * them. To use the PureEdgeSim default orchestrator class you can also
		 * uncomment:
		 */
		// setCustomEdgeOrchestrator(DefaultOrchestrator.class);

		// Start the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example5();
	}

}
