package com.Mechalikh.PureEdgeSim.TasksGenerator;
 
import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;

public class BasicTasksGenerator extends TaskGenerator {  
	public BasicTasksGenerator() {
		super();

	}
  @Override
	public void generate(double simulationTime, int devicesCount, int tasksPerDevicePerMinute, int fogDatacentersCount,
			List<EdgeDataCenter> list) {
//		this.datacentersList = list.subList(fogDatacentersCount, devicesCount + fogDatacentersCount);
		this.datacentersList = list.subList(list.size()-devicesCount, list.size());
	    simulationTime = simulationTime / 60; // in minutes
		for (int dev = 0; dev < devicesCount; dev++) { // for each device

			int app = new Random().nextInt(SimulationParameters.APPS_COUNT); // pickup a random application type for every device
			datacentersList.get(dev).setApplication(app); // assign this application to that device
			for (int st = 0; st < simulationTime; st++) { // for each minute
				// generating tasks
				int time = st * 60;
				time += new Random().nextInt(59);// pickup random second in this minute "st";
				if (time < 12 && st == 0)
					time += 12;
				insert(time, app, dev);
			}
		}
	}

	private void insert(int time, int app, int dev) {
		// long randomValue= new Random().nextInt(500); //generate a random value just
		// once
		double maxLatency = (long) SimulationParameters.APPLICATIONS_TABLE[app][0]; // Load length from application file
		long length = (long) SimulationParameters.APPLICATIONS_TABLE[app][3]; // Load length from application file
		long requestSize = (long) SimulationParameters.APPLICATIONS_TABLE[app][1];
		long outputSize = (long) SimulationParameters.APPLICATIONS_TABLE[app][2];
		int pesNumber = (int) SimulationParameters.APPLICATIONS_TABLE[app][4]; 
		long containerSize = (int) SimulationParameters.APPLICATIONS_TABLE[app][5]; // the size of the container
		Task[] task = new Task[SimulationParameters.TASKS_PER_EDGE_DEVICE_PER_MINUTES];
		int id;
		// generate tasks for every edge device
		for (int i = 0; i < SimulationParameters.TASKS_PER_EDGE_DEVICE_PER_MINUTES; i++) {
			id = taskList.size();
			UtilizationModel utilizationModel = new UtilizationModelFull();
			task[i] = new Task(id, length, pesNumber);
			task[i].setFileSize(requestSize).setOutputSize(outputSize).setUtilizationModel(utilizationModel);
			task[i].setTime(time); 
			task[i].setContainerSize(containerSize);
			task[i].setMaxLatency(maxLatency);
			task[i].setEdgeDevice(datacentersList.get(dev)); // the device that generate this task (the origin)  
			taskList.add(task[i]);
			// simLog.deepLog("BasicTasksGenerator, Task "+ id+ " with execution time "+
			// time+ " (s) generated.");
		}
	}
   
}
