package com.mechalikh.pureedgesim.simulationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

class RunMetadataTest extends AbstractTest {

	@TempDir
	Path outputFolder;

	private static final String TIMESTAMP = "2026-10-25_20-00-00";

	@AfterEach
	void resetRandomness() {
		Randomness.reset();
	}

	@Test
	void theRunPropertiesDescribeTheResolvedRun() throws IOException {
		SimulationParameters.outputFolder = outputFolder.toString();

		RunMetadata.write(SimulationParameters.outputFolder, TIMESTAMP);

		Path folder = outputFolder.resolve(TIMESTAMP);
		Properties properties = read(folder.resolve(RunMetadata.FILE_NAME));
		assertFalse(properties.getProperty("version").isBlank(), "the framework version must be recorded");
		assertFalse(properties.getProperty("git.commit").isBlank(), "the git commit must be recorded");
		assertEquals("unset", properties.getProperty("random.seed"));
		assertEquals("false", properties.getProperty("parallelism"));
		assertEquals(folder.toAbsolutePath().normalize().toString().replace('\\', '/'),
				properties.getProperty("output.folder"));
		assertEquals(Path.of(SimulationParameters.edgeDevicesFile).toAbsolutePath().normalize().toString()
				.replace('\\', '/'), properties.getProperty("settings.edge_devices.xml"));
	}

	@Test
	void everyResolvedSettingsFileIsCopiedIntoTheOutputFolder() throws IOException {
		SimulationParameters.outputFolder = outputFolder.toString();

		RunMetadata.write(SimulationParameters.outputFolder, TIMESTAMP);

		Path settings = outputFolder.resolve(TIMESTAMP).resolve(RunMetadata.SETTINGS_FOLDER_NAME);
		for (String name : List.of("simulation_parameters.properties", "applications.xml", "edge_datacenters.xml",
				"edge_devices.xml", "cloud.xml")) {
			assertTrue(Files.isRegularFile(settings.resolve(name)), name + " was not copied");
		}
		assertEquals(Files.readString(Path.of(SimulationParameters.applicationFile)),
				Files.readString(settings.resolve("applications.xml")),
				"the copy must be byte-identical to the file the run parsed");
	}

	@Test
	void aSeededRunRecordsItsSeed() throws IOException {
		SimulationParameters.outputFolder = outputFolder.toString();
		Randomness.setSeed(123456789L);

		RunMetadata.write(SimulationParameters.outputFolder, TIMESTAMP);

		assertEquals("123456789", read(outputFolder.resolve(TIMESTAMP).resolve(RunMetadata.FILE_NAME))
				.getProperty("random.seed"));
	}

	@Test
	void aMissingSettingsFileStopsTheRun() {
		SimulationParameters.outputFolder = outputFolder.toString();
		SimulationParameters.cloudDataCentersFile = outputFolder.resolve("missing-cloud.xml").toString();

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> RunMetadata.write(SimulationParameters.outputFolder, TIMESTAMP));

		assertTrue(thrown.getMessage().contains(TIMESTAMP),
				"the error must name the output folder, got: " + thrown.getMessage());
		assertTrue(thrown.getCause() instanceof IOException, "the copy error must be kept as the cause");
	}

	private static Properties read(Path file) throws IOException {
		Properties properties = new Properties();
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}
}
