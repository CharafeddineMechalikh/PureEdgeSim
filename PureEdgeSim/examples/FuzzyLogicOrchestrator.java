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

import com.mechalikh.pureedgesim.DataCentersManager.DataCenter;
import com.mechalikh.pureedgesim.ScenarioManager.SimulationParameters;
import com.mechalikh.pureedgesim.SimulationManager.SimLog;
import com.mechalikh.pureedgesim.SimulationManager.SimulationManager;
import com.mechalikh.pureedgesim.TasksGenerator.Task;
import com.mechalikh.pureedgesim.TasksOrchestration.Orchestrator;

import net.sourceforge.jFuzzyLogic.FIS;


public class FuzzyLogicOrchestrator extends Orchestrator {

	public FuzzyLogicOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	protected int findVM(String[] architecture, Task task) {
		if ("INCREASE_LIFETIME".equals(algorithm))
			return increseLifetime(architecture, task);
		else if ("FUZZY_LOGIC".equals(algorithm))
			return FuzzyLogic(architecture, task);
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

	private int FuzzyLogic(String[] architecture, Task task) { 
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
			return Stage2(architecture2, task);
		}

	}

	private int Stage2(String[] architecture2, Task task) {
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

	private int increseLifetime(String[] architecture, Task task) {
		int vm = -1;
		double minTasksCount = -1; // vm with minimum assigned tasks;
		double vmMips = 0;
		double weight;
		double minWeight = 20;
		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				weight = getWeight(task, ((DataCenter) vmList.get(i).getHost().getDatacenter()));

				if (minTasksCount == -1) { // if it is the first iteration
					minTasksCount = orchestrationHistory.get(i).size()
							- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1; // avoid
																											// devision
																											// by 0
					// if this is the first time, set the first vm as the
					vm = i; // best one
					vmMips = vmList.get(i).getMips();
					minWeight = weight;
				} else if (vmMips / (minTasksCount * minWeight) < vmList.get(i).getMips()
						/ ((orchestrationHistory.get(i).size()
								- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1)
								* weight)) {
					// if this vm has more cpu mips and less waiting tasks
					minWeight = weight;
					vmMips = vmList.get(i).getMips();
					minTasksCount = orchestrationHistory.get(i).size()
							- vmList.get(i).getCloudletScheduler().getCloudletFinishedList().size() + 1;
					vm = i;
				}
			}
		}
		// assign the tasks to the vm found
		return vm;
	}

	private double getWeight(Task task, DataCenter dataCenter) {
		double weight = 1;// if it is not battery powered
		if (dataCenter.getEnergyModel().isBattery()) {
			if (task.getEdgeDevice().getEnergyModel().getBatteryLevel() > dataCenter.getEnergyModel().getBatteryLevel())
				weight = 20; // the destination device has lower remaining power than the task offloading
								// device, in this case it is better not to offload
								// that's why the weight is high (20)
			else
				weight = 15; // in this case the destination has higher remaining power, so it is okey to
								// offload tasks for it, if the cloud and the edge data centers are absent.
		}
		return weight;
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
