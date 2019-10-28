package com.Mechalikh.PureEdgeSim.SimulationManager;

import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.vms.Vm;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.DataCentersManager.ServersManager;
import com.Mechalikh.PureEdgeSim.Network.NetworkModel;
import com.Mechalikh.PureEdgeSim.ScenarioManager.Scenario;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.TasksGenerator.BasicTasksGenerator;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;
import com.Mechalikh.PureEdgeSim.TasksGenerator.TaskGenerator;
import com.Mechalikh.PureEdgeSim.TasksOrchestration.CustomBroker;
import com.Mechalikh.PureEdgeSim.TasksOrchestration.EdgeOrchestrator;

public class SimulationManager extends CloudSimEntity {
	public static final int Base = 1000; // avoid conflict with CloudSim Plus tags
	public static final int SEND_TASK_FROM_ORCH_TO_DESTINATION = Base + 8;
	private static final int PRINT_LOG = Base + 1;
	private static final int SHOW_PROGRESS = Base + 2;
	public static final int EXECUTE_TASK = Base + 3;
	public static final int TRANSFER_RESULTS_TO_ORCH = Base + 4;
	public static final int RESULT_RETURN_FINISHED = Base + 5;
	public static final int SEND_TO_ORCH = Base + 6;
	public static final int UPDATE_REAL_TIME_CHARTS = Base + 7;
	private CustomBroker broker;
	private List<Task> tasksList;
	private EdgeOrchestrator edgeOrchestrator;
	private ServersManager serversManager;
	SimulationVisualizer simulationVisualizer;
	private CloudSim simulation;
	private int simulationId;
	private int iteration;
	private SimLog simLog;
	private int progress = 1;
	private int lastWrittenNumber = 0;
	private int oldProgress = -1;
	private Scenario scenario;
	private NetworkModel networkModel;
	private List<EdgeDataCenter> orchestratorsList;
	private double failedTasksCount = 0;

	public SimulationManager(SimLog simLog, CloudSim simulation, int simulationId, int iteration, Scenario scenario)
			throws Exception {
		super(simulation);
		this.simulation = simulation;
		this.simLog = simLog;
		this.scenario = scenario;
		this.simulationId = simulationId;
		this.iteration = iteration;

		// Create Broker
		broker = createBroker();

		// Generate all data centers, servers, an devices
		serversManager = new ServersManager(this);
		serversManager.generateDatacentersAndDevices();

		// Get orchestrators list from the server manager
		orchestratorsList = serversManager.getOrchestratorsList();

		// Submit vm list to the broker
		simLog.deepLog("SimulationManager- Submitting VM list to the broker");
		broker.submitVmList(serversManager.getVmList());

		// Generate tasks list
		TaskGenerator TG = new BasicTasksGenerator(this);
		TG.generate();
		tasksList = TG.getTaskList();

		// Initialize logger variables
		simLog.setGeneratedTasks(tasksList.size());
		simLog.setCurrentOrchPolicy(scenario.getStringOrchArchitecture());

		// Initiate the orchestrator : send tasks and VMs lists to the orchestrator
		edgeOrchestrator = new EdgeOrchestrator(this);

		// Initialize the network model
		networkModel = new NetworkModel(simulation, this);

		// Show real time results during the simulation
		if (SimulationParameters.DISPLAY_REAL_TIME_CHARTS && !SimulationParameters.PARALLEL)
			simulationVisualizer = new SimulationVisualizer(this);
	}

	// Start simulation
	public void startSimulation() {
		simLog.print("SimulationManager-  Orchestration algorithm= " + scenario.getStringOrchAlgorithm()
				+ "-  Architechitecture= " + scenario.getStringOrchArchitecture() + " -  number of edge devices= "
				+ scenario.getDevicesCount());
		simulation.start();
	}

	@Override
	public void startEntity() {
		simLog.print("SimulationManager- Simulation: " + this.simulationId + "  , iteration: " + this.iteration);

		// Tasks scheduling
		for (int i = 0; i < tasksList.size(); i++) {
			if (!SimulationParameters.ENABLE_ORCHESTRATORS)
				tasksList.get(i).setOrchestrator(tasksList.get(i).getEdgeDevice());
			schedule(this, tasksList.get(i).getTime(), SEND_TO_ORCH, tasksList.get(i));
		}

		// Scheduling the end of the simulation
		schedule(this, SimulationParameters.SIMULATION_TIME, PRINT_LOG);

		// Updating real time charts
		if (SimulationParameters.DISPLAY_REAL_TIME_CHARTS && !SimulationParameters.PARALLEL)
			schedule(this, SimulationParameters.INITIALIZATION_TIME, UPDATE_REAL_TIME_CHARTS);

		// Show simulation progress
		schedule(this, SimulationParameters.INITIALIZATION_TIME, SHOW_PROGRESS);
		simLog.printSameLine("Simulation progress :", "red");
		simLog.printSameLine("[", "red");
	}

	@Override
	public void processEvent(SimEvent ev) {
		Task task = (Task) ev.getData();
		switch (ev.getTag()) {
		case SEND_TO_ORCH:
			// Send the offloading request to the closest orchestrator
			sendTaskToOrchestrator(task);
			break;

		case SEND_TASK_FROM_ORCH_TO_DESTINATION:
			// Send the request from the orchestrator to the offloading destination
			sendFromOrchToDestination(task);
			break;

		case EXECUTE_TASK:
			// Execute the task
			executeTask(task);
			break;

		case TRANSFER_RESULTS_TO_ORCH:
			// Transfer the results to the orchestrator
			sendResultsToOchestrator(task);
			break;

		case RESULT_RETURN_FINISHED:
			// Result returned to edge device
			resultsReturned(task);
			break;

		case SHOW_PROGRESS:
			// Calculate the simulation progress
			progress = 100 * broker.getCloudletFinishedList().size() / simLog.getGeneratedTasks();
			if (oldProgress != progress) {
				oldProgress = progress;
				if (progress % 10 == 0 || (progress % 10 < 5) && lastWrittenNumber + 10 < progress) {
					lastWrittenNumber = progress - progress % 10;
					if (lastWrittenNumber != 100)
						simLog.printSameLine(" " + lastWrittenNumber + " ", "red");
				} else
					simLog.printSameLine("#", "red");
			}
			schedule(this, SimulationParameters.SIMULATION_TIME / 100, SHOW_PROGRESS);
			break;

		case UPDATE_REAL_TIME_CHARTS:
			// Update simulation Map
			simulationVisualizer.UpdateCharts();

			// Schedule the next update
			schedule(this, SimulationParameters.CHARTS_UPDATE_INTERVAL, UPDATE_REAL_TIME_CHARTS);
			break;

		case PRINT_LOG:
			// Print results when simulation is over
			List<Task> finishedTasks = broker.getCloudletFinishedList();

			// If some tasks have not been executed
			if (SimulationParameters.WAIT_FOR_TASKS
					&& (double) finishedTasks.size() / (double) (simLog.getGeneratedTasks()
							- simLog.getNotGeneratedBecauseDead() - simLog.getTasksFailedRessourcesUnavailable()) < 1) {
				// 1 = 100% , 0,9= 90%
				// Some tasks may take hours to be executed that's why we don't wait until
				// all of them get executed, but we only wait for 99% of tasks to be executed at
				// least, to end the simulation. that's why we set it to " < 0.99"
				// especially when 1% doesn't affect the simulation results that much, change
				// this value to lower ( 95% or 90%) in order to make simulation faster. however
				// this may affect the results
				schedule(this, 10, PRINT_LOG);
				break;
			}

			simLog.printSameLine(" 100% ]", "red");

			if (SimulationParameters.DISPLAY_REAL_TIME_CHARTS && !SimulationParameters.PARALLEL) {
				// Close real time charts after the end of the simulation
				if (SimulationParameters.AUTO_CLOSE_REAL_TIME_CHARTS)
					simulationVisualizer.close();
				try {
					// Save those charts in bitmap and vector formats
					if (SimulationParameters.SAVE_CHARTS)
						simulationVisualizer.saveCharts();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Show results and stop the simulation
			simLog.showIterationResults(finishedTasks);

			// Terminate the simulation
			simulation.terminate();
			break;
		default:
			simLog.print("Unknown event type");
			break;
		}

	}

	private void resultsReturned(Task task) {
		try {
			// Check if the device is alive
			if (!task.getEdgeDevice().isDead()) {
				// If the tasks is faled because of high delays
				if ((task.getSimulation().clock() - task.getTime()) > task.getMaxLatency()
						&& task.getFailureReason() != Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY) {
					task.setFailureReason(Task.Status.FAILED_DUE_TO_LATENCY);
					task.setStatus(Cloudlet.Status.FAILED);

				}

			} else { // Set this tasks as failed
				simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
			}
			if (task.getStatus() == Cloudlet.Status.FAILED)
				failedTasksCount++;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void sendResultsToOchestrator(Task task) {
		try {
			// Check if the device is alive
			if (!task.getEdgeDevice().isDead()) {

				// If the task was offloaded
				if (task.getEdgeDevice().getId() != task.getVm().getHost().getDatacenter().getId()) {
					scheduleNow(networkModel, NetworkModel.SEND_RESULT_TO_ORCH, task);

				} else { // The task has been executed locally / no offloading
					scheduleNow(this, RESULT_RETURN_FINISHED, task);
				}

			} else { // Otherwise, set this tasks as failed
				simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void executeTask(Task task) {
		try {
			// Check if the device is alive
			if (!task.getEdgeDevice().isDead()) {
				// Submit tasks to the broker for execution
				broker.submitCloudlet(task);

			} else { // Otherwise set this tasks as failed
				simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void sendFromOrchToDestination(Task task) {
		try {
			// Find the best VM for executing the task
			edgeOrchestrator.initialize(task);

			// Stop in case no resource was available for this task, the offloading is
			// failed
			if (task.getVm() == Vm.NULL)
				return;

			// If the task is offloaded
			// and the orchestrator is not the offloading destination
			if (task.getEdgeDevice().getId() != task.getVm().getHost().getDatacenter().getId()
					&& task.getOrchestrator() != ((EdgeDataCenter) task.getVm().getHost().getDatacenter())) {
				scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION, task);

			} else { // The task will be executed locally / no offloading or will be executed where
						// the orchestrator is deployed (no network usage)
				scheduleNow(this, EXECUTE_TASK, task);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void sendTaskToOrchestrator(Task task) {
		// Send the offloading request to the closest orchestrator
		double min = -1;
		int selected = 0;
		double distance;
		for (int i = 0; i < orchestratorsList.size(); i++) {
			if (orchestratorsList.get(i).getType() != SimulationParameters.TYPES.CLOUD) {
				distance = Math.abs(Math.sqrt(Math
						.pow((task.getEdgeDevice().getLocation().getXPos()
								- orchestratorsList.get(i).getLocation().getXPos()), 2)
						+ Math.pow((task.getEdgeDevice().getLocation().getYPos()
								- orchestratorsList.get(i).getLocation().getYPos()), 2)));
				if (min == -1 || min > distance) {
					min = distance;
					selected = i;
				}
			}
		}
		if (SimulationParameters.ENABLE_ORCHESTRATORS) {
			if (orchestratorsList.size() == 0) {
				simLog.printSameLine("SimulationManager- Error no orchestrator found", "red");
				return;
			} 
			task.setOrchestrator(orchestratorsList.get(selected));
		}
		
		if (!task.getEdgeDevice().isDead()) { // check if the device is still alive
			scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_ORCH, task);
		} else { // otherwise set this tasks as failed
			simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
		}
	}

	@Override
	public void shutdownEntity() {

	}

	private CustomBroker createBroker() {
		CustomBroker broker = null;
		try {
			broker = new CustomBroker(simulation);
			broker.setSimulationManager(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	public NetworkModel getNetworkModel() {
		return this.networkModel;
	}

	public int getSimulationId() {
		return this.simulationId;
	}

	public SimLog getSimulationLogger() {
		return simLog;
	}

	public ServersManager getServersManager() {
		return serversManager;
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public CustomBroker getBroker() {
		return this.broker;
	}

	public double getFailureRate() {
		double result = (failedTasksCount * 100) / tasksList.size();
		failedTasksCount = 0;
		return result;
	}

	public int getIterationId() {
		return this.iteration;
	}

}
