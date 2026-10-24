package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator;
import com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator;
import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;
import com.mechalikh.pureedgesim.taskgenerator.TaskGenerator;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;
import com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract.Files;

class SimulationAbstractTest extends AbstractTest {

	private static class ConcreteSimulation extends SimulationAbstract {
	}

	@Test
	void defaultModelsAreTheDefaultImplementations() {
		ConcreteSimulation simulation = new ConcreteSimulation();
		assertSame(DefaultMobilityModel.class, simulation.mobilityModel);
		assertSame(DefaultComputingNode.class, simulation.computingNode);
		assertSame(DefaultTaskGenerator.class, simulation.tasksGenerator);
		assertSame(DefaultOrchestrator.class, simulation.orchestrator);
		assertSame(DefaultNetworkModel.class, simulation.networkModel);
		assertSame(DefaultSimulationManager.class, simulation.simulationManager);
		assertSame(DefaultTopologyCreator.class, simulation.topologyCreator);
		assertSame(DefaultComputingNodesGenerator.class, simulation.computingNodesGenerator);
	}

	@Test
	void customModelSettersReplaceEveryModelClass() {
		ConcreteSimulation simulation = new ConcreteSimulation();

		simulation.setCustomComputingNode(DefaultComputingNode.class);
		simulation.setCustomTaskGenerator(DefaultTaskGenerator.class);
		simulation.setCustomEdgeOrchestrator(DefaultOrchestrator.class);
		simulation.setCustomMobilityModel(DefaultMobilityModel.class);
		simulation.setCustomNetworkModel(DefaultNetworkModel.class);
		simulation.setCustomTopologyCreator(DefaultTopologyCreator.class);
		simulation.setCustomSimulationManager(DefaultSimulationManager.class);
		simulation.setCustomComputingNodesGenerator(DefaultComputingNodesGenerator.class);

		assertSame(DefaultComputingNode.class, simulation.computingNode);
		assertSame(DefaultTaskGenerator.class, simulation.tasksGenerator);
		assertSame(DefaultOrchestrator.class, simulation.orchestrator);
		assertSame(DefaultMobilityModel.class, simulation.mobilityModel);
		assertSame(DefaultNetworkModel.class, simulation.networkModel);
		assertSame(DefaultTopologyCreator.class, simulation.topologyCreator);
		assertSame(DefaultSimulationManager.class, simulation.simulationManager);
		assertSame(DefaultComputingNodesGenerator.class, simulation.computingNodesGenerator);
	}

	@Test
	void customOutputFolderUpdatesTheSimulationParameters() {
		ConcreteSimulation simulation = new ConcreteSimulation();
		simulation.setCustomOutputFolder("some/output/folder");
		assertEquals("some/output/folder", SimulationParameters.outputFolder);
	}

	@Test
	void customSettingsFolderUpdatesAllFiveFiles() {
		ConcreteSimulation simulation = new ConcreteSimulation();
		simulation.setCustomSettingsFolder("my/settings/");

		assertEquals("my/settings/simulation_parameters.properties", SimulationParameters.simulationParametersFile);
		assertEquals("my/settings/applications.xml", SimulationParameters.applicationFile);
		assertEquals("my/settings/edge_datacenters.xml", SimulationParameters.edgeDataCentersFile);
		assertEquals("my/settings/edge_devices.xml", SimulationParameters.edgeDevicesFile);
		assertEquals("my/settings/cloud.xml", SimulationParameters.cloudDataCentersFile);
	}

	@Test
	void customFilePathUpdatesEachFileIndividually() {
		ConcreteSimulation simulation = new ConcreteSimulation();

		simulation.setCustomFilePath("p.properties", Files.SIMULATION_PARAMETERS);
		simulation.setCustomFilePath("a.xml", Files.APPLICATIONS_FILE);
		simulation.setCustomFilePath("edc.xml", Files.EDGE_DATACENTERS_FILE);
		simulation.setCustomFilePath("ed.xml", Files.EDGE_DEVICES_FILE);
		simulation.setCustomFilePath("c.xml", Files.CLOUD_FILE);

		assertEquals("p.properties", SimulationParameters.simulationParametersFile);
		assertEquals("a.xml", SimulationParameters.applicationFile);
		assertEquals("edc.xml", SimulationParameters.edgeDataCentersFile);
		assertEquals("ed.xml", SimulationParameters.edgeDevicesFile);
		assertEquals("c.xml", SimulationParameters.cloudDataCentersFile);
	}

	@Test
	void filesEnumContainsEverySupportedConfigurationFile() {
		assertEquals(5, Files.values().length);
		assertEquals(Files.SIMULATION_PARAMETERS, Files.valueOf("SIMULATION_PARAMETERS"));
	}
}
