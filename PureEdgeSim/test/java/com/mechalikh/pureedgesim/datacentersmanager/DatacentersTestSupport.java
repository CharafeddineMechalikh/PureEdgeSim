package com.mechalikh.pureedgesim.datacentersmanager;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;

/**
 * Shared helpers for the datacentersmanager package tests: builds a real
 * simulation manager (backed by a real {@link PureEdgeSim} engine so that
 * entities can be registered) without launching a full simulation.
 */
abstract class DatacentersTestSupport extends AbstractTest {

	protected PureEdgeSim simulation;
	protected SimLog simLog;
	protected DefaultSimulationManager simulationManager;

	/**
	 * Creates a fresh simulation manager with a scenario containing the given
	 * number of edge devices. The simulation is not started.
	 */
	protected void createSimulationManager(int devicesCount) {
		simulation = new PureEdgeSim();
		simLog = new SimLog("test", false);
		simulationManager = new DefaultSimulationManager(simLog, simulation, 1, 1,
				new Scenario(devicesCount, 0, 0));
		simLog.initialize(simulationManager, devicesCount, 0, 0);
		SimulationParameters.numberOfCloudDataCenters = 1;
		SimulationParameters.numberOfEdgeDataCenters = 1;
	}

	/**
	 * Registers a default network model on the manager. The topology creator needs
	 * it when a shared WAN link is used.
	 */
	protected DefaultNetworkModel attachDefaultNetworkModel() {
		return new DefaultNetworkModel(simulationManager);
	}

	protected DefaultComputingNodesGenerator newGenerator() {
		return new DefaultComputingNodesGenerator(simulationManager, DefaultMobilityModel.class,
				DefaultComputingNode.class);
	}

	/**
	 * Builds a fully wired data centers manager (generation + topology) using the
	 * production default classes.
	 */
	protected DataCentersManager newDataCentersManager(int devicesCount, boolean sharedWan) {
		createSimulationManager(devicesCount);
		SimulationParameters.useOneSharedWanLink = sharedWan;
		attachDefaultNetworkModel();
		return new DataCentersManager(simulationManager, DefaultMobilityModel.class, DefaultComputingNode.class,
				DefaultComputingNodesGenerator.class, DefaultTopologyCreator.class);
	}

	/**
	 * Forces the engine clock. The engine normally advances it while processing
	 * events; unit tests that call node logic directly need a controlled value.
	 */
	protected void setClock(double time) {
		try {
			Field field = PureEdgeSim.class.getDeclaredField("time");
			field.setAccessible(true);
			field.setDouble(simulation, time);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot set the simulation clock", e);
		}
	}

	protected static String readResource(String name) {
		try {
			return new String(Files.readAllBytes(new File(SETTINGS_FOLDER + name).toPath()),
					StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot read test resource " + name, e);
		}
	}

	protected static Path writeTempFile(Path directory, String fileName, String content) {
		try {
			Path file = directory.resolve(fileName);
			Files.write(file, content.getBytes(StandardCharsets.UTF_8));
			return file;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot write temporary file " + fileName, e);
		}
	}

	protected static Element parseElement(String xml, String tagName) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
			return (Element) document.getElementsByTagName(tagName).item(0);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot parse test XML", e);
		}
	}

	protected static String datacenterXml(String name, boolean periphery, int x, int y) {
		return "<datacenter name=\"" + name + "\">"
				+ "<periphery>" + periphery + "</periphery>"
				+ "<idleConsumption>100</idleConsumption>"
				+ "<maxConsumption>250</maxConsumption>"
				+ "<isOrchestrator>false</isOrchestrator>"
				+ "<location><x_pos>" + x + "</x_pos><y_pos>" + y + "</y_pos></location>"
				+ "<cores>10</cores><mips>40000</mips><ram>16000</ram><storage>200000</storage>"
				+ "</datacenter>";
	}
}
