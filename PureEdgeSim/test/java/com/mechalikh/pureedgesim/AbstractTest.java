package com.mechalikh.pureedgesim;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationAbstract.Files;

/**
 * Base class for all tests. Resets the static simulation parameters before each
 * test so tests do not leak configuration into one another.
 */
public abstract class AbstractTest {

	public static final String SETTINGS_FOLDER = "PureEdgeSim/test/resources/test-settings/";
	public static final String OUTPUT_FOLDER = "target/test-output/";

	protected static final String FAST_SETTINGS = "fast.properties";
	protected static final String FULL_SETTINGS = "simulation_parameters.properties";
	protected static final String ORCHESTRATOR_SETTINGS = "orchestrators.properties";

	@BeforeEach
	void resetSimulationParameters() throws Exception {
		for (Field field : SimulationParameters.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
				field.setAccessible(true);
				Class<?> type = field.getType();
				if (type == boolean.class) {
					field.setBoolean(null, false);
				} else if (type == int.class) {
					field.setInt(null, 0);
				} else if (type == double.class) {
					field.setDouble(null, 0);
				} else if (type == long.class) {
					field.setLong(null, 0);
				} else {
					field.set(null, null);
				}
			}
		}

		SimulationParameters.simulationParametersFile = SETTINGS_FOLDER + "simulation_parameters.properties";
		SimulationParameters.applicationFile = SETTINGS_FOLDER + "applications.xml";
		SimulationParameters.edgeDataCentersFile = SETTINGS_FOLDER + "edge_datacenters.xml";
		SimulationParameters.edgeDevicesFile = SETTINGS_FOLDER + "edge_devices.xml";
		SimulationParameters.cloudDataCentersFile = SETTINGS_FOLDER + "cloud.xml";
		SimulationParameters.outputFolder = OUTPUT_FOLDER;

		SimulationParameters.simulationDuration = 60;
		SimulationParameters.updateInterval = 1;
		SimulationParameters.chartsUpdateInterval = 1;
		SimulationParameters.batchSize = 100;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDevicesRange = 50;
		SimulationParameters.edgeDataCentersRange = 200;
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		SimulationParameters.pauseLength = 0;
		SimulationParameters.wanBandwidthBitsPerSecond = 20_000_000;
		SimulationParameters.wanLatency = 0.06;
		SimulationParameters.wanWattHourPerBit = 46.7 * 2.7777777777778e-13;
		SimulationParameters.manBandwidthBitsPerSecond = 1_000_000_000;
		SimulationParameters.manLatency = 0.01;
		SimulationParameters.wifiBandwidthBitsPerSecond = 1_300_000_000;
		SimulationParameters.wifiLatency = 0.005;
		SimulationParameters.ethernetBandwidthBitsPerSecond = 1_000_000_000;
		SimulationParameters.ethernetLatency = 0.002;
		SimulationParameters.cellularBandwidthBitsPerSecond = 100_000_000;
		SimulationParameters.cellularLatency = 0.03;
		SimulationParameters.networkUpdateInterval = 1;
		SimulationParameters.applicationList = List.of(
				new com.mechalikh.pureedgesim.taskgenerator.Application("Hard Real-time", 20, 20, 0.02, 160_000,
						160_000, 160_000, 500),
				new com.mechalikh.pureedgesim.taskgenerator.Application("Soft Real-time", 30, 30, 0.5, 12_000_000,
						12_000_000, 400_000, 5000),
				new com.mechalikh.pureedgesim.taskgenerator.Application("Non Real-time", 3, 50, 300, 17_600_000,
						20_000_000, 1_600_000, 30000));
	}

	protected void useSettings(String parametersFile) {
		SimulationParameters.simulationParametersFile = SETTINGS_FOLDER + parametersFile;
	}
}
