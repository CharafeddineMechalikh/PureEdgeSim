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

import org.cloudbus.cloudsim.cloudlets.Cloudlet.Status;

import com.mechalikh.pureedgesim.datacentersmanager.DataCenter;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimLog;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;

import net.sourceforge.jFuzzyLogic.FIS;


public class FuzzyLogicOrchestrator extends CustomEdgeOrchestrator {

	public FuzzyLogicOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	protected int findVM(String[] architecture, Task task) {
		if ("INCREASE_LIFETIME".equals(algorithm))
			return increseLifetime(architecture, task);
		else if ("FUZZY_LOGIC".equals(algorithm))
			return fuzzyLogic(task);
		else {
			SimLog.println("");
			SimLog.println("Custom Orchestrator- Unknown orchestration algorithm '" + algorithm
					+ "', please check the simulation parameters file...");
			// Cancel the simulation
			SimulationParameters.STOP = true;
			simulationManager.getSimulation().terminate();
		}
		return -1;
	}

	private int fuzzyLogic(Task task) { 
		String fileName = "PureEdgeSim/examples/Example8_settings/stage1.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		double vmUsage = 0;
		int count = 0;
		for (int i = 0; i < vmList.size(); i++) {
			if (((DataCenter) vmList.get(i).getHost().getDatacenter()).getType() != SimulationParameters.TYPES.CLOUD) {
				vmUsage += vmList.get(i).getCpuPercentUtilization() * 100;
				count++;
				vmUsage += ((DataCenter) vmList.get(i).getHost().getDatacenter()).getResources().getAvgCpuUtilization();

			}
		}

		// set fuzzy inputs
		fis.setVariable("wan",
				SimulationParameters.WAN_BANDWIDTH / 1000 - simulationManager.getNetworkModel().getWanUtilization());
		fis.setVariable("tasklength", task.getLength());
		fis.setVariable("delay", task.getMaxLatency());
		fis.setVariable("vm", vmUsage / count);

		// Evaluate
		fis.evaluate();

		if (fis.getVariable("offload").defuzzify() > 50) {
			String[] architecture2 = { "Cloud" };
			return increseLifetime(architecture2, task);
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
		for (int i = 0; i < vmList.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture2)
					&& vmList.get(i).getStorage().getCapacity() > 0) {
				if (!task.getEdgeDevice().getMobilityManager().isMobile())
					fis.setVariable("vm_local", 0);
				else
					fis.setVariable("vm_local", 0);
				fis.setVariable("vm", (1 - vmList.get(i).getCpuPercentUtilization()) * vmList.get(i).getMips() / 1000);
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
		// How to get the task execution status, (if failed or succeed, which can be
		// used for reinforcement learning based algorithms)
		if (task.getStatus() == Status.FAILED) {
			System.err.println("CustomEdgeOrchestrator, task " + task.getId() + " has been failed, failure reason is: "
					+ task.getFailureReason());
		} else {

			System.out.println("CustomEdgeOrchestrator, task " + task.getId() + " has been successfully executed");
		}

	}

}
