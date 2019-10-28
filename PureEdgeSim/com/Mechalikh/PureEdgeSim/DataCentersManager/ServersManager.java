package com.Mechalikh.PureEdgeSim.DataCentersManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple; 
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.Mechalikh.PureEdgeSim.LocationManager.Location; 
import com.Mechalikh.PureEdgeSim.LocationManager.MobilityManager;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimulationManager; 

public class ServersManager {
	private List<EdgeDataCenter> datacentersList; 
	private List<EdgeVM> vmList;  
	private List<EdgeDataCenter> orchestratorsList;
	private SimulationManager simulationManager;

	public ServersManager(SimulationManager simulationManager) {
		datacentersList = new ArrayList<EdgeDataCenter>();
		orchestratorsList = new ArrayList<EdgeDataCenter>();
		vmList = new ArrayList<EdgeVM>();
		this.setSimulationManager(simulationManager);
	}

	public void generateDatacentersAndDevices() throws Exception {
		generateCloudDataCenters();
		generateFogDataCenters();
		generateEdgeDev();
		// Select where the orchestrators are deployed
		if(SimulationParameters.ENABLE_ORCHESTRATORS)
		selectOrchestrators();
		getSimulationManager().getSimulationLogger().print("ServersManager- Datacenters and devices were generated");

	}

	private void selectOrchestrators() {
		for (int i = 0; i < datacentersList.size(); i++) {
			if (SimulationParameters.DEPLOY_ORCHESTRATOR.equals("")
					|| (SimulationParameters.DEPLOY_ORCHESTRATOR.equals("CLOUD")
							&& datacentersList.get(i).getType() == SimulationParameters.TYPES.CLOUD)) {
				datacentersList.get(i).setOrchestrator(true);
				orchestratorsList.add(datacentersList.get(i));
			} else if (SimulationParameters.DEPLOY_ORCHESTRATOR.equals("FOG")
					&& datacentersList.get(i).getType() == SimulationParameters.TYPES.FOG) {
				datacentersList.get(i).setOrchestrator(true);
				orchestratorsList.add(datacentersList.get(i));
			}
		}

	}

	public void generateEdgeDev() throws Exception {
		// Generate edge devices instances from edge devices types in xml file
		Document doc = SimulationParameters.edgeDevicesDoc;
		NodeList edgeDevicesList = doc.getElementsByTagName("device");
		int instancesPercentage = 0;
		Element edgeElement = null;
		
		// Load all devices types in edgedevices.xml file
		for (int i = 0; i < edgeDevicesList.getLength(); i++) {
			Node edgeNode = edgeDevicesList.item(i);
			edgeElement = (Element) edgeNode;
			instancesPercentage = Integer
					.parseInt(edgeElement.getElementsByTagName("percentage").item(0).getTextContent());
			
			// Find the number of instances of this type of devices
			float devicesInstances =getSimulationManager().getScenario().getDevicesCount()* instancesPercentage / 100;

			for (int j = 0; j < devicesInstances; j++) {
				if (datacentersList.size() > getSimulationManager().getScenario().getDevicesCount()+ SimulationParameters.NUM_OF_FOG_DATACENTERS) {
					getSimulationManager().getSimulationLogger().print(
							"ServersManager- Wrong percentages values (the sum is superior than 100%), check edge_devices.xml file !");
					break; 
				}

				datacentersList.add(createDatacenter(edgeElement, SimulationParameters.TYPES.EDGE));

			}
		}
		if (datacentersList.size() < getSimulationManager().getScenario().getDevicesCount()) // if percentage of generated devices is < 100%
			getSimulationManager().getSimulationLogger().print(
					"ServersManager- Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !");
		// Add more devices
		int missingInstances = getSimulationManager().getScenario().getDevicesCount() - datacentersList.size();
		for (int k = 0; k < missingInstances; k++) {
			datacentersList.add(createDatacenter(edgeElement, SimulationParameters.TYPES.EDGE));   
		}

	}

	private void generateFogDataCenters() throws Exception {
		// Fill list with fog data centers
		Document doc = SimulationParameters.fogDevicesDoc;
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			datacentersList.add(createDatacenter(datacenterElement, SimulationParameters.TYPES.FOG)); 
		}
	}

	private void generateCloudDataCenters() throws Exception {
		// Fill the list with cloud datacenters
		Document doc = SimulationParameters.cloudDevicesDoc;
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			datacentersList.add(createDatacenter(datacenterElement, SimulationParameters.TYPES.CLOUD)); 
		}
	}

	private EdgeDataCenter createDatacenter(Element datacenterElement, SimulationParameters.TYPES level)
			throws Exception {

		int x_position = -1;
		int y_position = -1;

		List<Host> hostList = createHosts(datacenterElement, level);

		EdgeDataCenter datacenter = null;
		Location datacenterLocation= null;
		datacenter = new EdgeDataCenter(getSimulationManager().getSimulation(), hostList, new VmAllocationPolicySimple());
		if (level == SimulationParameters.TYPES.FOG) {
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			x_position = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			y_position = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			datacenterLocation=new Location(x_position, y_position);
		} else if (level == SimulationParameters.TYPES.EDGE) { 
			datacenter.setMobile(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent()));
			datacenter.setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			datacenter.setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batterycapacity").item(0).getTextContent()));
			
			// Generate random location for edge devices
			datacenterLocation = new Location(new Random().nextInt(SimulationParameters.AREA_LENGTH),
					new Random().nextInt(SimulationParameters.AREA_LENGTH)); 
			getSimulationManager().getSimulationLogger().deepLog("ServersManager- Edge device:" + datacentersList.size() + "    location: ( "
					+ datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
		}
		
		double idleConsumption=
				Double.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption=
				Double.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
		EnergyModel energyModel= new EnergyModel(maxConsumption, idleConsumption);
		datacenter.setEnergyModel(energyModel);
		datacenter.setOrchestrator(Boolean
				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent())); 
		datacenter.setType(level);
		datacenter.setMobilityManager(new MobilityManager(datacenterLocation));
		return datacenter;
	}

	private List<Host> createHosts(Element datacenterElement, SimulationParameters.TYPES type) {

		// Here are the steps needed to create a hosts and vms for that datacenter.
		List<Host> hostList = new ArrayList<Host>();

		NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
		for (int j = 0; j < hostNodeList.getLength(); j++) {

			Node hostNode = hostNodeList.item(j);
			Element hostElement = (Element) hostNode;
			int numOfCores = Integer.parseInt(hostElement.getElementsByTagName("core").item(0).getTextContent());
			double mips = Double.parseDouble(hostElement.getElementsByTagName("mips").item(0).getTextContent());
			long storage = Long.parseLong(hostElement.getElementsByTagName("storage").item(0).getTextContent());
			long bandwidth;
			long ram;

			if (type == SimulationParameters.TYPES.CLOUD) {
				bandwidth = SimulationParameters.WAN_BANDWIDTH / hostNodeList.getLength();
				ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			} else {
				bandwidth = SimulationParameters.BANDWIDTH_WLAN / hostNodeList.getLength();
				ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			}

			// A Machine contains one or more PEs or CPUs/Cores. Therefore, should
			// create a list to store these PEs before creating
			// a Machine.
			List<Pe> peList = new ArrayList<Pe>();

			// Create PEs and add these into the list.
			// for a quad-core machine, a list of 4 PEs is required:
			for (int i = 0; i < numOfCores; i++) {
				peList.add(new PeSimple(mips, new PeProvisionerSimple())); // need to store Pe id and MIPS Rating
			}

			ResourceProvisioner ramProvisioner = new ResourceProvisionerSimple();
			ResourceProvisioner bwProvisioner = new ResourceProvisionerSimple();
			VmScheduler vmScheduler = new VmSchedulerSpaceShared();

			// Create Hosts with its id and list of PEs and add them to the list of machines
			Host host = new HostSimple(ram, bandwidth, storage, peList);
			host.setRamProvisioner(ramProvisioner).setBwProvisioner(bwProvisioner).setVmScheduler(vmScheduler);

			NodeList vmNodeList = hostElement.getElementsByTagName("VM");
			for (int k = 0; k < vmNodeList.getLength(); k++) {
				Node vmNode = vmNodeList.item(k);
				Element vmElement = (Element) vmNode;
				// VM Parameters 
				long vmNumOfCores = Long.parseLong(vmElement.getElementsByTagName("core").item(0).getTextContent());
				double vmMips = Double.parseDouble(vmElement.getElementsByTagName("mips").item(0).getTextContent());
				long vmStorage = Long.parseLong(vmElement.getElementsByTagName("storage").item(0).getTextContent());
				long vmBandwidth;
				int vmRam;

				vmBandwidth = bandwidth / vmNodeList.getLength();
				vmRam = Integer.parseInt(vmElement.getElementsByTagName("ram").item(0).getTextContent());

				CloudletScheduler tasksScheduler;

				if (SimulationParameters.CPU_ALLOCATION_POLICY.equals("SPACE_SHARED"))  
					tasksScheduler = new TasksSchedulerSpaceShared();
				else  
					tasksScheduler = new TasksSchedulerTimeShared();

				EdgeVM vm = new EdgeVM(vmList.size(), vmMips, vmNumOfCores);
				vm.setRam(vmRam).setBw(vmBandwidth).setSize(vmStorage).setCloudletScheduler(tasksScheduler);
				vm.getUtilizationHistory().enable();
				vm.setHost(host);
				vmList.add(vm);
			}
			hostList.add(host);
		}

		return hostList;
	}

	public List<EdgeVM> getVmList() {
		return vmList;

	}
 

	public List<EdgeDataCenter> getDatacenterList() {
		return datacentersList;
	}

	public List<EdgeDataCenter> getOrchestratorsList() {
		return orchestratorsList;
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

}
