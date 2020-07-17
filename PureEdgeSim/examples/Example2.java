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
 *     @author Mechalikh
 **/
package examples;
 
import com.mechalikh.pureedgesim.MainApplication; 

public class Example2 extends MainApplication {
 /**
  * This is a simple example showing how to launch simulation using a custom mobility model.
  * by removing it, pureEdgeSim will use the default model.
  * As you can see, this class extends the Main class provided by PureEdgeSim, which is required  for this example to work
  */
	public Example2(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) { 
		/* To use your custom mobility model, do this:
		The custom mobility manager class can be found in the examples folder as well.
		 by removing this line, pureEdgeSim will use the default mobility model.  
		*/
		setCustomMobilityModel(CustomMobilityManager.class);  

		// To use the PureEdgeSim default Mobility Manager you can also uncomment this: 
		// setCustomMobilityModel(MobilityManager.class);  
		
		// Start the simulation
		launchSimulation();
	} 
 
}
