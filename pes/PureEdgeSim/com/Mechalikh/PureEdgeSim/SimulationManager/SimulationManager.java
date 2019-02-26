package com.Mechalikh.PureEdgeSim.SimulationManager;

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
import com.Mechalikh.PureEdgeSim.TasksOrchestration.edgeOrchestrator;

public class SimulationManager extends CloudSimEntity {
	public static final int SEND_TASK_FROM_ORCH_TO_DESTINATION = 0; 
	private static final int PRINT_LOG = 2;
	private static final int SHOW_PROGRESS = 3;
	public static final int EXECUTE_TASK = 4;
	public static final int TRANSFER_RESULTS_TO_ORCH = 5;
	public static final int RESULT_RETURN_FINISHED = 6;
	public static final int SEND_TO_ORCH = 1;
	private CustomBroker broker;
	private List<Task> tasksList;
	private edgeOrchestrator edgeOrch;
	private ServersManager SM;
	private CloudSim simulation;
	private SimLog simLog;
	private int progress = 1;
	private int lastWrittenNumber = 0;
	private int oldProgress = -1;
	private Scenario scenario;
	private NetworkModel networkModel;
	private List<EdgeDataCenter> orchestratorsList;

	public SimulationManager(SimLog simLog, CloudSim simulation, Scenario scenario) throws Exception {
		super(simulation);
		this.simulation = simulation;
		this.simLog = simLog;
		this.scenario = scenario;
		// Third step: Create Broker
		broker = createBroker();
		int brokerId = broker.getId();
		SM = new ServersManager(simulation, simLog);

		// load datacenters and hosts and create VMs
		SM.fillDatacentersList(scenario.getDevicesCount(), brokerId);

		// get orchestrators list from the server manager
		orchestratorsList = SM.getOrchestratorsList();

		// submit vm list to the broker
		simLog.deepLog(simulation.clock() + " : Main, Submitting VM list to the broker");
		broker.submitVmList(SM.getVmList());

		// Generate tasks list
		double simulationTime = SimulationParameters.SIMULATION_TIME;
		int tasksRate = SimulationParameters.TASKS_PER_EDGE_DEVICE_PER_MINUTES;
		TaskGenerator TG = new BasicTasksGenerator();
		TG.generate(simulationTime, scenario.getDevicesCount(), tasksRate, SM.getFogDataCentersCount(),
				SM.getDatacenterList());
		tasksList = TG.getTaskList();
		// initialize logger variables
		simLog.setGeneratedTasks(tasksList.size());
		simLog.setCurrentOrchPolicy(scenario.getStringOrchPolicy());

		// initiate the orchestrator : send tasks and VMs lists to the orchestrator
		edgeOrch = new edgeOrchestrator(broker, SM.getVmList(), simLog, scenario);

		// initialize the network model
		networkModel = new NetworkModel(simulation, this);
	}

	// start simulation
	public void startSimulation() {
		simLog.print("SimulationManager,  Orchestration policy= " + scenario.getStringOrchPolicy()
				+ " -  Orchestration criteria= " + scenario.getStringOrchCriteria() + " -  number of edge devices= "
				+ scenario.getDevicesCount());
		// Sixth step: Starts the simulation
		simulation.start();
	}

	@Override
	public void startEntity() {
		// Tasks scheduling
		for (int i = 0; i < tasksList.size(); i++) {
			schedule(this, tasksList.get(i).getTime(), SEND_TO_ORCH, tasksList.get(i));
		} 
		schedule(this, SimulationParameters.SIMULATION_TIME, PRINT_LOG);

		// if (!SimulationParameters.PARALLEL) // show progress only if parallelism is
		// disabled
		schedule(this, SimulationParameters.SIMULATION_TIME / 100, SHOW_PROGRESS);
		simLog.print("Simulation progress: ");
		simLog.printSameLine("Simulation progress :");
		simLog.printSameLine("[", "red");
	}

	@Override
	public void processEvent(SimEvent ev) {
		Task t = (Task) ev.getData();
		switch (ev.getTag()) {
		case SEND_TO_ORCH: // send the offloading request to the closest orchestrator 
			double min = -1;
			int selected = 0;
			double distance;
			for (int i = 0; i < orchestratorsList.size(); i++) {
				if (orchestratorsList.get(i).getType() != SimulationParameters.TYPES.CLOUD) {
					distance = Math.abs(Math.sqrt(Math
							.pow((t.getEdgeDevice().getLocation().getXPos()
									- orchestratorsList.get(i).getLocation().getXPos()), 2)
							+ Math.pow((t.getEdgeDevice().getLocation().getYPos()
									- orchestratorsList.get(i).getLocation().getYPos()), 2)));
					if (min == -1 || min > distance) {
						min = distance;
						selected = i;
					}
				}
			}
			if (orchestratorsList.size() == 0) {
				simLog.printSameLine("SimulationManager, Error no orchestrator found", "red");
				break;
			}
			t.setOrchestrator(orchestratorsList.get(selected));
			if (!t.getEdgeDevice().isDead()) { // check if the device is still alive
			scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_ORCH, t);
			} else { // although set this tasks as failed
				simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
			}
			break;
		case SEND_TASK_FROM_ORCH_TO_DESTINATION: // send the request from the orchestrator to the offloading destination 
			try { 
					edgeOrch.initialize(t); // find the best VM for executing the task
					if (t.getVm() == Vm.NULL)
						return;// stop in case no resource was available for this task, the offloading is
								// failed
					if (t.getEdgeDevice().getId() != t.getVm().getHost().getDatacenter().getId()// if the task is offloaded
							&& t.getOrchestrator()!= ((EdgeDataCenter)t.getVm().getHost().getDatacenter()) ) { //and the orchestrator place is not the offloading destination
						scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION, t);
						// update the enrgy consumption of the orchestrator and the device that
						// offloaded the task
						t.getEdgeDevice()
								.addConsumption(t.getFileSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);
						t.getOrchestrator()
								.addConsumption(t.getFileSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);

					} else { // the task will be executed locally / no offloading or will be executed where the orchestrator is deployed (no network usage)
						scheduleNow(this, EXECUTE_TASK, t);
					}
 
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break;
		case EXECUTE_TASK:

			try {
				if (!t.getEdgeDevice().isDead()) { // check if the device is alive
					// submit tasks to the broker
					broker.submitCloudlet(t); // execute task 
					if (t.getEdgeDevice().getId() != t.getVm().getHost().getDatacenter().getId()) { // if the task is
						// offloaded
						scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION, t);
                        //update the enrgy consumption of the orchestrator and the device that will execute the task
						((EdgeDataCenter)t.getVm().getHost().getDatacenter())
								.addConsumption(t.getFileSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);
						t.getOrchestrator()
								.addConsumption(t.getFileSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);
					}
					// if the container was downloaded
					if (SimulationParameters.ENABLE_REGISTRY) {
						((EdgeDataCenter) t.getVm().getHost().getDatacenter())
								.addConsumption(t.getContainerSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);
						// registry.addConsumption(t.getContainerSize()*SimulationParameters.POWER_CONS_PER_MEGABYTE);
					}
				} else { // although set this tasks as failed
					simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break;
		case TRANSFER_RESULTS_TO_ORCH:// transfer the results to the orchestrator 
			try {
				if (!t.getEdgeDevice().isDead()) { // check if the device is alive
					// calculate the download time

					if (t.getEdgeDevice().getId() != t.getVm().getHost().getDatacenter().getId()) { // if the task was
																									// offloaded

						scheduleNow(networkModel, NetworkModel.ADD_RESULT_TO_ORCH, t);
					} else { // the task will be executed locally / no offloading
						scheduleNow(this, RESULT_RETURN_FINISHED, t);
					}

				} else { // although, set this tasks as failed
					simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break;
		case RESULT_RETURN_FINISHED: 
			try { 
				if (!t.getEdgeDevice().isDead()) { // check if the device is alive
					// a simple representation of task completion time

					if ((t.getSimulation().clock() - t.getTime()) + t.getDownloadLanNetworkUsage()
							+ t.getUploadLanNetworkUsage() > t.getMaxLatency()
							&& t.getFailureReason() != Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY) {
						t.setFailureReason(Task.Status.FAILED_DUE_TO_LATENCY);
						t.setStatus(Cloudlet.Status.FAILED);

					}

					// add the energy consumption of returning the task results to the device,
					// although it is failed due to latency, the results will be returned.
					if (t.getEdgeDevice().getId() != (t.getVm().getHost().getDatacenter()).getId()) { // if the
																											// task was
																											// offloaded
																											// / not
																											// executed
																											// locally
						// update energy consumption of the device that generated the task
						t.getEdgeDevice()
								.addConsumption(t.getOutputSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE); 
						//update the energy consumption of the device that executed the task
						((EdgeDataCenter)t.getVm().getHost().getDatacenter()).addConsumption(t.getOutputSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);
						//update the energy consumption of the orchestrator that linked between these two devices
						t.getOrchestrator().addConsumption(2*t.getOutputSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);
					}

				} else { // although set this tasks as failed
					simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break; 
		case SHOW_PROGRESS:
			progress = 100 * broker.getCloudletFinishedList().size() / simLog.getGeneratedTasks(); // calculate the
																									// progress
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

		case PRINT_LOG:

			// Final step: Print results when simulation is over
			List<Task> finishedTasks = broker.getCloudletFinishedList();
			// submit vmList , and tasks list to the logger, and print vm utilization and
			// tasks execution results , and update log file
			if (SimulationParameters.WAIT_FOR_TASKS
					&& (double) finishedTasks.size() / (double) (simLog.getGeneratedTasks()
							- simLog.getNotGeneratedBecauseDead() - simLog.getTasksFailedRessourcesUnavailable()) < 1) {
				// 1 = 100% , 0,9= 90%
				// some tasks may take hours to be executed that's why we don't wait until
				// all of them get executed, but we only wait for 99% of tasks to be executed at
				// least, to end the simulation. that's why we set it to " < 0.99"
				// especially when 1% doesn't affect the simulation results that much, change
				// this value to lower ( 95% or 90%) in order to make simulation faster. however
				// this may affect the results
				schedule(this, 10, PRINT_LOG);
				break;
			}

			simLog.printSameLine(" 100% ]", "red");
			// updating network usage 
			simLog.setNetworkModel(networkModel);
			// show results and stop the simulation
			simLog.showIterationResults(SM, finishedTasks);
			simulation.terminate();
			break;
		default:
			simLog.print("Unknown event type");
			break;
		}

	}

	@Override
	public void shutdownEntity() {

	}

	// create the broker
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

	 

	 

}
