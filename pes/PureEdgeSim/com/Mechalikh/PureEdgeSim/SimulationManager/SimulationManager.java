package com.Mechalikh.PureEdgeSim.SimulationManager;

import java.util.List;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.events.SimEvent;

import com.Mechalikh.PureEdgeSim.DataCentersManager.EdgeDataCenter;
import com.Mechalikh.PureEdgeSim.DataCentersManager.ServersManager;
import com.Mechalikh.PureEdgeSim.Network.NetworkModel;
import com.Mechalikh.PureEdgeSim.ScenarioManager.Scenario;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.TasksGenerator.BasicTasksGenerator;
import com.Mechalikh.PureEdgeSim.TasksGenerator.Task;
import com.Mechalikh.PureEdgeSim.TasksGenerator.TaskGenerator;
import com.Mechalikh.PureEdgeSim.TasksOrchestration.CustomBroker;
import com.Mechalikh.PureEdgeSim.TasksOrchestration.Orchestrator;
import com.Mechalikh.PureEdgeSim.TasksOrchestration.edgeOrchestrator; 

public class SimulationManager extends CloudSimEntity {
	private static final int OFFLOAD_TASK = 0;
	private static final int VM_LOG = 1;
	private static final int PRINT_LOG = 2;
	private static final int SHOW_PROGRESS = 3;
	public static final int EXECUTE_TASK = 4; 
	public static final int TRANSFER_RESULTS = 5;
	public static final int RESULT_RETURN_FINISHED=6;
	private CustomBroker broker;
	private List<Task> tasksList;
	private Orchestrator edgeOrch;
	private ServersManager SM;
	private CloudSim simulation;
	private SimLog simLog;
	private int progress = 1;
	private int lastWrittenNumber = 0;
	private int oldProgress = -1;
	private Scenario scenario;
	private NetworkModel networkModel; 

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
		
		//initialize the network model
		networkModel= new NetworkModel(simulation, this);
	}

	// start simulation
	public void startSimulation() { 
		simLog.print("SimulationManager,  Orchestration policy= " + scenario.getStringOrchPolicy()
		+ " -  Orchestration criteria= "	+ scenario.getStringOrchCriteria()+ " -  number of edge devices= " + scenario.getDevicesCount() );
		// Sixth step: Starts the simulation
		simulation.start(); 
	}

	@Override
	public void startEntity() {
		// Tasks scheduling 
		for (int i = 0; i < tasksList.size(); i++) {
			schedule(this, tasksList.get(i).getTime(), OFFLOAD_TASK, tasksList.get(i));
		}
		schedule(this, SimulationParameters.INTERVAL_TO_SEND_EVENT, VM_LOG);
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
		switch (ev.getTag()) {
		case OFFLOAD_TASK:
			try { 
				Task t = (Task) ev.getData(); 
				if (!t.getEdgeDevice().isDead()) { // check if the device is alive
					edgeOrch.initialize(t); // find the best VM for executing the task
					if(t.getEdgeDevice().getId()!=t.getVm().getHost().getDatacenter().getId()) { //if the task is offloaded
					 
					scheduleNow(networkModel,NetworkModel.ADD_TASK,t);  
					}
					else { // the task will be executed locally / no offloading
					 
						scheduleNow(this,EXECUTE_TASK,t);	
					}
					
				} else { // although set this tasks as failed 
					simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break; 
		case EXECUTE_TASK:
			try { 
				Task t = (Task) ev.getData();
				if (!t.getEdgeDevice().isDead()) { // check if the device is alive 
					// submit tasks to the broker 
					broker.submitCloudlet(t); //execute task
					UpdateEnergyConsumption(t); // update energy consumption and network usage
					
				} else { // although set this tasks as failed
					simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break; 
		case TRANSFER_RESULTS:
			try {  
				Task task = (Task) ev.getData();
				if (!task.getEdgeDevice().isDead()) { // check if the device is alive   
					// calculate the download time
					
					if(task.getEdgeDevice().getId()!=task.getVm().getHost().getDatacenter().getId()) { //if the task was offloaded
					 
					scheduleNow(networkModel,NetworkModel.ADD_RESULT,task);  
					}else { // the task will be executed locally / no offloading 
						scheduleNow(this,RESULT_RETURN_FINISHED,task);	
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
				Task task = (Task) ev.getData();
				EdgeDataCenter edgeDevice = (EdgeDataCenter) task.getVm().getHost().getDatacenter(); // find the edge device which offloaded this task
				if (!task.getEdgeDevice().isDead()) { // check if the device is alive   
					// a simple representation of task completion time
				
					
					if ((task.getSimulation().clock() - task.getTime()) + task.getDownloadLanNetworkUsage() + task.getUploadLanNetworkUsage() > task.getMaxLatency()
							&& task.getFailureReason() != Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY) {
						task.setFailureReason(Task.Status.FAILED_DUE_TO_LATENCY);
						task.setStatus(Cloudlet.Status.FAILED);
					  
					}

					//add the energy consumption of returning the task results to the device, 
					//although it is failed due to latency, the results will be returned. 
			        if(task.getEdgeDevice().getId()!= (task.getVm().getHost().getDatacenter()).getId()) { //if the task was offloaded / not executed locally
					  task.getEdgeDevice().addConsumption(task.getOutputSize()*SimulationParameters.POWER_CONS_PER_MEGABYTE);
					  edgeDevice.addConsumption(task.getOutputSize()*SimulationParameters.POWER_CONS_PER_MEGABYTE);
			        }							
					boolean failed=false;
					if(task.getStatus()== Status.FAILED)failed=true;
					edgeOrch.taskFailed(task, failed);
					
				} else { // although set this tasks as failed
					simLog.setNotGeneratedBecauseDead(simLog.getNotGeneratedBecauseDead() + 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break;
		case VM_LOG:
			simLog.updateVMutilizationLog(SM.getVmUtilization());
			schedule(this, SimulationParameters.INTERVAL_TO_SEND_EVENT, VM_LOG);
			break;
		case SHOW_PROGRESS:
			progress = 100 * broker.getCloudletFinishedList().size() / simLog.getGeneratedTasks(); // calculate the
																									// progress
			if (oldProgress != progress) {
				oldProgress = progress;
				if (progress % 10 == 0 || (progress % 10 < 5) && lastWrittenNumber + 10 < progress) {
					lastWrittenNumber = progress - progress % 10;
					if(lastWrittenNumber!=100)
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
			if (SimulationParameters.WAIT_FOR_TASKS && (double) finishedTasks.size()
					/ (double) (simLog.getGeneratedTasks() - simLog.getNotGeneratedBecauseDead() ) < 1) 
			{
			// 1 = 100% , 0,9= 90% 	
			// some tasks may take hours to be executed that's why we don't wait until
			// all of them get executed, but we only wait for 99% of tasks to be executed at
			// least, to end the simulation. that's why we set it to  " < 0.99"
			// especially when 1% doesn't affect the simulation results that much, change
			// this value to lower ( 95% or 90%) in order to make simulation faster. however
			// this may affect the results  
				schedule(this, 10, PRINT_LOG);
				break;
			}
           
			simLog.printSameLine(" 100% ]", "red");
			//updating network usage  
			//resultsReturned(finishedTasks); // update the network usage ( when wending tasks and when returning results / the upload + the download)
			simLog.setNetworkModel(networkModel);
			//show results and stop the simulation
			simLog.showIterationResults(SM.getVmList(), finishedTasks);
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
  
	private void UpdateEnergyConsumption(Task task) {
		if(task.getEdgeDevice().getId()!= (task.getVm().getHost().getDatacenter()).getId()) { //if the task was offloaded
			// update sender energy consumption 
			task.getEdgeDevice().addConsumption(task.getFileSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);

			// update receiver energy consumption
			((EdgeDataCenter) task.getVm().getHost().getDatacenter())
					.addConsumption(task.getFileSize() * SimulationParameters.POWER_CONS_PER_MEGABYTE);
			
	        }else { //if task was executed locally
	        	simLog.deepLog("Task: "+task.getId()+" is executed locally, not offloaded");
	        }
	}
	
	public void resultsReturned(List<Task>  finishedTasks) {
		// save network usage time of returning the results of the processed task
		Task task;
		for (int j=0;j<finishedTasks.size();j++) {
			 task=finishedTasks.get(j);
        if(task.getEdgeDevice().getId()!= (task.getVm().getHost().getDatacenter()).getId())
        {
        	//the task is not executed loccaly,which mean it is offloaded, so the network was used
        	 
        	if(!task.getFailureReason().equals(Task.Status.FAILED_BECAUSE_DEVICE_DEAD) 
        			&& !task.getFailureReason().equals(Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY)) {  
            // if both the device which offloaded it and the device which executed it are still alive 
        		// and the task didn't failed due to mobility
        	// which means that the results successfully returned
    	    updateResultsNetworkUsage(task);	 
        }
		}
		}
	}
  
	
	private void updateResultsNetworkUsage(Task task) { // download results
		double returnedResults = 0;
		double averageResultsSize = 0;  
		for (int i = 0; i < tasksList.size(); i++) {
			if (tasksList.get(i).getTime() == task.getTime()) {
				returnedResults++;
				averageResultsSize += tasksList.get(i).getFileSize();
			}
		}
		averageResultsSize = averageResultsSize / returnedResults; 
	}
	 
}
