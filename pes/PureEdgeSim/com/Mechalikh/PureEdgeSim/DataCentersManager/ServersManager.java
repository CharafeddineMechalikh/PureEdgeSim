package com.Mechalikh.PureEdgeSim.DataCentersManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
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
import com.Mechalikh.PureEdgeSim.LocationManager.Mobility;
import com.Mechalikh.PureEdgeSim.LocationManager.MobilityManager;
import com.Mechalikh.PureEdgeSim.ScenarioManager.SimulationParameters;
import com.Mechalikh.PureEdgeSim.SimulationManager.SimLog; 

public class ServersManager {
	private List<EdgeDataCenter> datacentersList;
	private CloudSim simulation;
	private List<EdgeVM> vmList;
	private SimLog simLog;
	private List<Host> fogHostsList;
	private int fogDatacentersCount = 0;

	public ServersManager(CloudSim simulation, SimLog simLog) {
		datacentersList = new ArrayList<EdgeDataCenter>();
		fogHostsList = new ArrayList<Host>();
		vmList = new ArrayList<EdgeVM>();
		this.simulation = simulation;
		this.simLog = simLog;
	}

	public void fillDatacentersList(int edgeDevicesInstancesCount, int broker) throws Exception {
		fillListWithCloudDataCenters();
		fillListWithFogDataCenters();
		fillListWithEdgeDev(edgeDevicesInstancesCount); 
		simLog.print("ServersManager, Datacenters List Created");

	}

	// start edge devices
	public void fillListWithEdgeDev(int edgeDevicesInstancesCount) throws Exception {
		// generate edge devices instances from edge devices types in xml file
		Document doc = SimulationParameters.edgeDevicesDoc;
		NodeList edgeDevicesList = doc.getElementsByTagName("device");
		int instancesPercentage = 0;
		Element edgeElement = null;
		for (int i = 0; i < edgeDevicesList.getLength(); i++) { // load all devices types in edgedevices.xml file
			Node edgeNode = edgeDevicesList.item(i);
			edgeElement = (Element) edgeNode;
			instancesPercentage = Integer
					.parseInt(edgeElement.getElementsByTagName("percentage").item(0).getTextContent());
			float devicesInstances = edgeDevicesInstancesCount * instancesPercentage / 100; // how many instances of
																							// this device type;

			for (int j = 0; j < devicesInstances; j++) {
				if (datacentersList.size() > edgeDevicesInstancesCount + fogDatacentersCount) { // the list size is
																								// superior than devices
					// number, due to some wrong percentage
					// inputs
					simLog.print(
							"ServersManager, Wrong percentages values (the sum is superior than 100%), check edge_devices.xml file !");
					break;// don't insert devices anymore, to preserve the simulated number.
				}

				datacentersList.add(createDatacenter(edgeElement, SimulationParameters.TYPES.EDGE));

			}
		}
		if (datacentersList.size() < edgeDevicesInstancesCount) // if the sum of the values is inferior than 100%
			simLog.print(
					"ServersManager, Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !");
		// add more devices
		int missingInstances = edgeDevicesInstancesCount - datacentersList.size();
		for (int k = 0; k < missingInstances; k++) {
			datacentersList.add(createDatacenter(edgeElement, SimulationParameters.TYPES.EDGE)); // we add some
																									// instances of the
																									// last edge
			// element to the list.

		}

	}

	private void fillListWithFogDataCenters() throws Exception {
		// fill list with fog data centers
		Document doc = SimulationParameters.fogDevicesDoc;
		NodeList datacenterList = doc.getElementsByTagName("datacenter"); 
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			datacentersList.add(createDatacenter(datacenterElement, SimulationParameters.TYPES.FOG));
			fogDatacentersCount++;
			simLog.incFogdc();
		}
	}

	private void fillListWithCloudDataCenters() throws Exception {
		// fill the list with cloud datacenters
		Document doc = SimulationParameters.cloudDevicesDoc;
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			datacentersList.add(createDatacenter(datacenterElement, SimulationParameters.TYPES.CLOUD));
			simLog.incClouddc();
		}
	}

	private EdgeDataCenter createDatacenter(Element datacenterElement, SimulationParameters.TYPES level)
			throws Exception {
		 
		int x_position = -1;
		int y_position = -1;

	 
		List<Host> hostList = createHosts(datacenterElement, level);
		 

		// 6. Finally, we need to create a PowerDatacenter object.
		EdgeDataCenter datacenter = null;
		datacenter = new EdgeDataCenter(simulation, hostList, new VmAllocationPolicySimple());
		datacenter.setType(level);
		if (level == SimulationParameters.TYPES.FOG) {
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			x_position = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			y_position = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			datacenter.setLocation(new Location(x_position, y_position));
		} else if (level == SimulationParameters.TYPES.EDGE) {
			datacenter.setMobile(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent()));
			datacenter.setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			datacenter.setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batterycapacity").item(0).getTextContent()));
			if(fogHostsList.size()>0) {
			int fogHostIndex = new Random().nextInt(fogHostsList.size());
			Location location = ((EdgeDataCenter) fogHostsList.get(fogHostIndex).getDatacenter()).getLocation();
			datacenter.setLocation(location);
			
			if (datacenter.isMobile()) {
				Mobility mob=new MobilityManager(fogHostsList);
				datacenter.setLocationChanges(mob.generateLocationChanges());
			}
			simLog.deepLog("ServersManager, Edge device:" + datacentersList.size() + "    location: ( "
					+ location.getXPos() + "," + location.getYPos() + " )");
		}else datacenter.setLocation(new Location(x_position,y_position)); 
			}
		datacenter.setIdleConsumption(
				Double.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent()));
		datacenter.setMaxConsumption(
				Double.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent()));
 
		datacenter.setSchedulingInterval(SimulationParameters.INTERVAL_TO_SEND_EVENT);
		datacenter.setLogger(simLog); 
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
			if (SimulationParameters.VM_MIGRATION && type == SimulationParameters.TYPES.FOG) {
			ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			bandwidth = SimulationParameters.BANDWIDTH_WLAN * 10;
			}
			if (type == SimulationParameters.TYPES.CLOUD) {
				bandwidth = SimulationParameters.WAN_BANDWIDTH / hostNodeList.getLength();
				ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			} else {
				bandwidth = SimulationParameters.BANDWIDTH_WLAN / hostNodeList.getLength();
				ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			}

			// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
			// create a list to store these PEs before creating
			// a Machine.
			List<Pe> peList = new ArrayList<Pe>();

			// 3. Create PEs and add these into the list.
			// for a quad-core machine, a list of 4 PEs is required:
			for (int i = 0; i < numOfCores; i++) {
				peList.add(new PeSimple(mips, new PeProvisionerSimple())); // need to store Pe id and MIPS Rating
			}

			ResourceProvisioner ramProvisioner = new ResourceProvisionerSimple();
			ResourceProvisioner bwProvisioner = new ResourceProvisionerSimple();
			VmScheduler vmScheduler = new VmSchedulerSpaceShared();

			// 4. Create Hosts with its id and list of PEs and add them to the list of
			// machines
			Host host = new HostSimple(ram, bandwidth, storage, peList);
			host.setRamProvisioner(ramProvisioner).setBwProvisioner(bwProvisioner).setVmScheduler(vmScheduler);

			NodeList vmNodeList = hostElement.getElementsByTagName("VM");
			for (int k = 0; k < vmNodeList.getLength(); k++) {
				Node vmNode = vmNodeList.item(k);
				Element vmElement = (Element) vmNode;
				// VM Parameters
			    // String vmm = vmElement.getAttribute("vmm");
				long vmNumOfCores = Long.parseLong(vmElement.getElementsByTagName("core").item(0).getTextContent());
				double vmMips = Double.parseDouble(vmElement.getElementsByTagName("mips").item(0).getTextContent());
				long vmStorage = Long.parseLong(vmElement.getElementsByTagName("storage").item(0).getTextContent());
				long vmBandwidth;
				int vmRam;
				if (SimulationParameters.VM_MIGRATION && type == SimulationParameters.TYPES.FOG) {
				vmBandwidth = 4000;
			    vmRam = 1500;
				} else {
					vmBandwidth = bandwidth / vmNodeList.getLength();
					vmRam = Integer.parseInt(vmElement.getElementsByTagName("ram").item(0).getTextContent());
			    }
				
				CloudletScheduler tasksScheduler; 
				 
				if (SimulationParameters.CPU_ALLOCATION_POLICY.equals("Space_Shared")) // if virtual machines
					tasksScheduler= new TasksSchedulerSpaceShared();
				else// containers
					tasksScheduler= new TasksSchedulerTimeShared();
				
				EdgeVM vm = new EdgeVM(vmList.size(), vmMips, vmNumOfCores);
				vm.setRam(vmRam).setBw(vmBandwidth).setSize(vmStorage)
						.setCloudletScheduler(tasksScheduler);
				vm.setType(type);
				vm.getUtilizationHistory().enable();
				vm.setHost(host);
				vmList.add(vm);
			}
			hostList.add(host);
			if (type == SimulationParameters.TYPES.FOG)
				fogHostsList.add(host);
		}

		return hostList;
	}

	public List<EdgeVM> getVmList() {
		return vmList;

	}

	// CPU utilization of all VMs
	public double[] getVmUtilization() {
		double[] vmUsage = new double[vmList.size()];

		for (int i = 0; i < vmList.size(); i++) {
			vmUsage[i] += vmList.get(i).getCloudletScheduler().getRequestedCpuPercentUtilization(simulation.clock());
		}
		return vmUsage;
	}

	public int getFogDataCentersCount() {
		return fogDatacentersCount;
	}

	public List<EdgeDataCenter> getDatacenterList() {
		return datacentersList;
	}

}
