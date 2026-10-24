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

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;

import net.sourceforge.jFuzzyLogic.FIS;

/**
 * In this example we show how to implement a Fuzzy Logic based orchestration
 * algorithm, we tried to implement this algorithm but with a little
 * modification in order to support mist computing (computing at the extreme
 * edge). The algorithm can be found in this paper here:
 * 
 * C. Sonmez, A. Ozgovde and C. Ersoy, "Fuzzy Workload Orchestration for Edge
 * Computing," in IEEE Transactions on Network and Service Management, vol. 16,
 * no. 2, pp. 769-782, June 2019.
 * 
 * We also started with stage 2 and then stage 1, as this decreases the
 * algorithm complexity. Hence, shorter simulation time.
 * 
 * To use it you must add JFuzzy_Logic jar file PureEdgeSim/Libs/ folder
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 3.0
 */
public class Example8FuzzyLogicOrchestrator extends DefaultOrchestrator {

	public Example8FuzzyLogicOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	protected int findComputingNode(String[] architecture, Task task) {
		if ("ROUND_ROBIN".equals(algorithmName))
			return roundRobin(architecture, task);
		else if ("FUZZY_LOGIC".equals(algorithmName))
			return fuzzyLogic(task);
		else {
			throw new IllegalArgumentException(getClass().getName() + " - Unknown orchestration algorithm '" + algorithmName
					+ "', please check the simulation parameters file...");
		}
	}

	private int fuzzyLogic(Task task) {
		String fileName = "PureEdgeSim/examples/Example8_settings/stage1.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		double cpuUsage = 0;
		int count = 0;
		for (int i = 0; i < nodeList.size(); i++) {
			if (nodeList.get(i).getType() != SimulationParameters.TYPES.CLOUD) {
				count++;
				cpuUsage += nodeList.get(i).getAvgCpuUtilization();

			}
		}

		// Set fuzzy inputs
		fis.setVariable("wan",
				(SimulationParameters.wanBandwidthBitsPerSecond
						- simulationManager.getNetworkModel().getWanUpUtilization())
						/ SimulationParameters.wanBandwidthBitsPerSecond);
		fis.setVariable("taskLength", task.getLength());
		fis.setVariable("delay", task.getMaxLatency());
		fis.setVariable("cpuUsage", count > 0 ? cpuUsage / count : 1);

		// Evaluate
		fis.evaluate();

		if (fis.getVariable("offload").defuzzify() > 50) {
			String[] architecture2 = { "Cloud" };
			return tradeOff(architecture2, task);
		} else {
			String[] architecture2 = { "Edge", "Mist" };
			return stage2(architecture2, task);
		}

	}

	private int stage2(String[] architecture2, Task task) {
		double min = -1;
		int selected = -1;
		String fileName = "PureEdgeSim/examples/Example8_settings/stage2.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		for (int i = 0; i < nodeList.size(); i++) {
			if (offloadingIsPossible(task, nodeList.get(i), architecture2) && nodeList.get(i).getTotalStorage() > 0) {

				fis.setVariable("vm_local", 1 - task.getEdgeDevice().getAvgCpuUtilization()
						* task.getEdgeDevice().getTotalMipsCapacity() / 1000);
				fis.setVariable("vm",
						(1 - nodeList.get(i).getAvgCpuUtilization()) * nodeList.get(i).getTotalMipsCapacity() / 1000);
				fis.evaluate();

				if (min == -1 || min > fis.getVariable("offload").defuzzify()) {
					min = fis.getVariable("offload").defuzzify();
					selected = i;
				}
			}
		}
		return selected;
	}

	@Override
	public void resultsReturned(Task task) {
	}

}
