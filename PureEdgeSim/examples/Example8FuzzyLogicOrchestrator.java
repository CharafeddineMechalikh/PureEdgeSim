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

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;
import com.mechalikh.pureedgesim.tasksorchestration.DefaultOrchestrator;

import net.sourceforge.jFuzzyLogic.FIS;


public class Example8FuzzyLogicOrchestrator extends DefaultOrchestrator {

	public Example8FuzzyLogicOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	protected int findComputingNode(String[] architecture, Task task) {
		if ("ROUND_ROBIN".equals(algorithm))
			return roundRobin(architecture, task);
		else if ("FUZZY_LOGIC".equals(algorithm))
			return fuzzyLogic(task);
		else {
			throw new IllegalArgumentException(getClass().getName()+" - Unknown orchestration algorithm '" + algorithm
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
				(SimulationParameters.WAN_BANDWIDTH_BITS_PER_SECOND - simulationManager.getNetworkModel().getWanUpUtilization())/SimulationParameters.WAN_BANDWIDTH_BITS_PER_SECOND); 
		fis.setVariable("taskLength", task.getLength());
		fis.setVariable("delay", task.getMaxLatency());
		fis.setVariable("cpuUsage", cpuUsage / count);

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
		int vm = -1;
		String fileName = "PureEdgeSim/examples/Example8_settings/stage2.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		for (int i = 0; i < nodeList.size(); i++) {
			if (offloadingIsPossible(task, nodeList.get(i), architecture2)
					&& nodeList.get(i).getTotalStorage() > 0) {
				if (!task.getEdgeDevice().getMobilityModel().isMobile())
					fis.setVariable("vm_local", 0);
				else
					fis.setVariable("vm_local", 0);
				fis.setVariable("vm", (1 - nodeList.get(i).getAvgCpuUtilization()) * nodeList.get(i).getMipsCapacity() / 1000);
				fis.evaluate();

				if (min == -1 || min > fis.getVariable("offload").defuzzify()) {
					min = fis.getVariable("offload").defuzzify();
					vm = i;
				}
			}
		}
		return vm;
	}


	@Override
	public void resultsReturned(Task task) {
	}

}
