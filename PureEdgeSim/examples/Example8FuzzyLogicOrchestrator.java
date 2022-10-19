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

import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;

import net.sourceforge.jFuzzyLogic.FIS;

public class Example8FuzzyLogicOrchestrator extends DefaultOrchestrator {

	public Example8FuzzyLogicOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	protected int findComputingNode(String[] architecture, Task task, List<ComputingNode> nodesList) {
		if ("ROUND_ROBIN".equals(algorithm))
			return roundRobin(architecture, task, nodesList);
		else if ("FUZZY_LOGIC".equals(algorithm))
			return fuzzyLogic(task, nodesList);
		else {
			throw new IllegalArgumentException(getClass().getName() + " - Unknown orchestration algorithm '" + algorithm
					+ "', please check the simulation parameters file...");
		}
	}

	private int fuzzyLogic(Task task, List<ComputingNode> nodesList) {
		String fileName = "PureEdgeSim/examples/Example8_settings/stage1.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		double cpuUsage = 0;
		int count = 0;
		for (int i = 0; i < nodesList.size(); i++) {
			if (nodesList.get(i).getType() != SimulationParameters.TYPES.CLOUD) {
				count++;
				cpuUsage += nodesList.get(i).getAvgCpuUtilization();

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
			return tradeOff(architecture2, task, nodesList);
		} else {
			String[] architecture2 = { "Edge", "Mist" };
			return stage2(architecture2, task, nodesList);
		}

	}

	private int stage2(String[] architecture2, Task task, List<ComputingNode> nodesList) {
		double min = -1;
		int selected = -1;
		String fileName = "PureEdgeSim/examples/Example8_settings/stage2.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		for (int i = 0; i < nodesList.size(); i++) {
			if (offloadingIsPossible(task, nodesList.get(i), architecture2) && nodesList.get(i).getTotalStorage() > 0) {

				fis.setVariable("vm_local", 1 - task.getEdgeDevice().getAvgCpuUtilization()
						* task.getEdgeDevice().getTotalMipsCapacity() / 1000);
				fis.setVariable("vm",
						(1 - nodesList.get(i).getAvgCpuUtilization()) * nodesList.get(i).getTotalMipsCapacity() / 1000);
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
