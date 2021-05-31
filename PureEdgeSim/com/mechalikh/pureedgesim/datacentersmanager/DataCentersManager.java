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
package com.mechalikh.pureedgesim.datacentersmanager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationcore.SimulationManager;

public class DataCentersManager {
	private List<DataCenter> datacentersList;
	private List<Vm> vmList;
	private List<DataCenter> orchestratorsList;
	private SimulationManager simulationManager;
	private Class<? extends MobilityModel> mobilityManager;
	private Class<? extends EnergyModel> energyModel;
	private Class<? extends DataCenter> edgeDataCenterType;

	public DataCentersManager(SimulationManager simulationManager, Class<? extends MobilityModel> mobilityManager,
			Class<? extends EnergyModel> energyModel, Class<? extends DataCenter> edgedatacenter) {
		datacentersList = new ArrayList<>();
		orchestratorsList = new ArrayList<>();
		vmList = new ArrayList<>();
		this.mobilityManager = mobilityManager;
		this.energyModel = energyModel;
		this.edgeDataCenterType = edgedatacenter;
		setSimulationManager(simulationManager);
	}

	public void generateDatacentersAndDevices() throws Exception {
		generateDataCenters(SimulationParameters.CLOUD_DATACENTERS_FILE, SimulationParameters.TYPES.CLOUD);
		generateDataCenters(SimulationParameters.EDGE_DATACENTERS_FILE, SimulationParameters.TYPES.EDGE_DATACENTER);
		generateEdgeDevices();
		// Select where the orchestrators are deployed
		if (SimulationParameters.ENABLE_ORCHESTRATORS)
			selectOrchestrators();
		getSimulationManager().getSimulationLogger().print("ServersManager- Datacenters and devices were generated");
	}

	private void selectOrchestrators() {
		for (DataCenter edgeDataCenter : datacentersList) {
			if ("".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)
					|| ("CLOUD".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)
							&& edgeDataCenter.getType() == SimulationParameters.TYPES.CLOUD)) {
				edgeDataCenter.setAsOrchestrator(true);
				orchestratorsList.add(edgeDataCenter);
			} else if ("EDGE".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)
					&& edgeDataCenter.getType() == SimulationParameters.TYPES.EDGE_DATACENTER) {
				edgeDataCenter.setAsOrchestrator(true);
				orchestratorsList.add(edgeDataCenter);
			} else if ("MIST".equals(SimulationParameters.DEPLOY_ORCHESTRATOR)
					&& edgeDataCenter.getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
				edgeDataCenter.setAsOrchestrator(true);
				orchestratorsList.add(edgeDataCenter);
			}
		}

	}

	public void generateEdgeDevices() throws Exception {
		// Generate edge devices instances from edge devices types in xml file
		File devicesFile = new File(SimulationParameters.EDGE_DEVICES_FILE);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(devicesFile);
		NodeList edgeDevicesList = doc.getElementsByTagName("device");
		Element edgeElement = null;

		// Load all devices types in edge_devices.xml file
		for (int i = 0; i < edgeDevicesList.getLength(); i++) {
			Node edgeNode = edgeDevicesList.item(i);
			edgeElement = (Element) edgeNode;
			generateDevicesInstances(edgeElement);
		}
		// if percentage of generated devices is < 100%
		if (datacentersList.size() < getSimulationManager().getScenario().getDevicesCount())
			getSimulationManager().getSimulationLogger().print(
					"ServersManager- Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !");
		// Add more devices
		int missingInstances = getSimulationManager().getScenario().getDevicesCount() - datacentersList.size();
		for (int k = 0; k < missingInstances; k++) {
			datacentersList.add(createDatacenter(edgeElement, SimulationParameters.TYPES.EDGE_DEVICE));
		}

	}

	private void generateDevicesInstances(Element edgeElement) throws Exception {
		int instancesPercentage = Integer
				.parseInt(edgeElement.getElementsByTagName("percentage").item(0).getTextContent());

		// Find the number of instances of this type of devices
		float devicesInstances = getSimulationManager().getScenario().getDevicesCount() * instancesPercentage / 100;

		for (int j = 0; j < devicesInstances; j++) {
			if (datacentersList.size() > getSimulationManager().getScenario().getDevicesCount()
					+ SimulationParameters.NUM_OF_EDGE_DATACENTERS) {
				getSimulationManager().getSimulationLogger().print(
						"ServersManager- Wrong percentages values (the sum is superior than 100%), check edge_devices.xml file !");
				break;
			}

			datacentersList.add(createDatacenter(edgeElement, SimulationParameters.TYPES.EDGE_DEVICE));

		}
	}

	private void generateDataCenters(String file, SimulationParameters.TYPES type) throws Exception {
		// Fill list with edge data centers
		File serversFile = new File(file);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(serversFile);
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			datacentersList.add(createDatacenter(datacenterElement, type));
		}
	}

	private DataCenter createDatacenter(Element datacenterElement, SimulationParameters.TYPES type) throws Exception {
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int x_position = -1;
		int y_position = -1;

		List<Vm> list = new ArrayList<>();
		List<Host> hostList = createHosts(datacenterElement, type, list);

		Location datacenterLocation = null;
		Constructor<?> datacenterConstructor = edgeDataCenterType.getConstructor(SimulationManager.class, List.class,
				List.class);
		DataCenter datacenter = (DataCenter) datacenterConstructor.newInstance(getSimulationManager(), hostList, list);
		double idleConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
		datacenter.setAsOrchestrator(Boolean
				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));
		Constructor<?> energyConstructor = energyModel.getConstructor(double.class, double.class);
		datacenter.setEnergyModel(energyConstructor.newInstance(maxConsumption, idleConsumption));

		if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			x_position = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			y_position = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			datacenterLocation = new Location(x_position, y_position);
		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			mobile = Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent());
			speed = Double.parseDouble(datacenterElement.getElementsByTagName("speed").item(0).getTextContent());
			minPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
			maxPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("maxPauseDuration").item(0).getTextContent());
			minMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
			maxMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("maxMobilityDuration").item(0).getTextContent());
			datacenter.getEnergyModel().setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			datacenter.getEnergyModel().setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batteryCapacity").item(0).getTextContent()));
			datacenter.setTasksGeneration(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("generateTasks").item(0).getTextContent()));
			// Generate random location for edge devices
			datacenterLocation = new Location(new Random().nextInt(SimulationParameters.AREA_LENGTH),
					new Random().nextInt(SimulationParameters.AREA_LENGTH));
			getSimulationManager().getSimulationLogger().deepLog("ServersManager- Edge device:" + datacentersList.size()
					+ "    location: ( " + datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
		}
		datacenter.setType(type);
		Constructor<?> mobilityConstructor = mobilityManager.getConstructor(Location.class, boolean.class, double.class,
				double.class, double.class, double.class, double.class);
		datacenter.setMobilityManager(mobilityConstructor.newInstance(datacenterLocation, mobile, speed,
				minPauseDuration, maxPauseDuration, minMobilityDuration, maxMobilityDuration));
		return datacenter;
	}

	private List<Host> createHosts(Element datacenterElement, SimulationParameters.TYPES type, List<Vm> list) {

		// Here are the steps needed to create a hosts and vms for that datacenter.
		List<Host> hostList = new ArrayList<>();

		NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
		for (int j = 0; j < hostNodeList.getLength(); j++) {

			Node hostNode = hostNodeList.item(j);
			Element hostElement = (Element) hostNode;
			int numOfCores = Integer.parseInt(hostElement.getElementsByTagName("core").item(0).getTextContent());
			double mips = Double.parseDouble(hostElement.getElementsByTagName("mips").item(0).getTextContent());
			long storage = Long.parseLong(hostElement.getElementsByTagName("storage").item(0).getTextContent());
			long bandwidth = (type == SimulationParameters.TYPES.CLOUD ? SimulationParameters.WAN_BANDWIDTH
					: SimulationParameters.BANDWIDTH_WLAN) / hostNodeList.getLength();
			long ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			// A Machine contains one or more PEs or CPUs/Cores. Therefore, should
			// create a list to store these PEs before creating
			// a Machine.
			List<Pe> peList = new ArrayList<>();

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

			// Load host virtual machines
			loadVms(host, hostElement, bandwidth, list);
			hostList.add(host);
		}

		return hostList;
	}

	private void loadVms(Host host, Element hostElement, long bandwidth, List<Vm> list) {
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

			tasksScheduler = "SPACE_SHARED".equals(SimulationParameters.CPU_ALLOCATION_POLICY)
					? new CloudletSchedulerSpaceShared()
					: new CloudletSchedulerTimeShared();

			Vm vm = new VmSimple(vmList.size(), vmMips, vmNumOfCores);
			vm.setRam(vmRam).setBw(vmBandwidth).setSize(vmStorage).setCloudletScheduler(tasksScheduler);
			vm.setHost(host);
			vmList.add(vm);
			list.add(vm);
		}
	}

	public List<Vm> getVmList() {
		return vmList;

	}

	public List<DataCenter> getDatacenterList() {
		return datacentersList;
	}

	public List<DataCenter> getOrchestratorsList() {
		return orchestratorsList;
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}
}
