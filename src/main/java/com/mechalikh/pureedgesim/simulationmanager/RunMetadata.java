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
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.simulationmanager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.mechalikh.pureedgesim.distribution.Randomness;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

/**
 * Writes the provenance of a simulation campaign next to its results.
 * <p>
 * Every timestamped output folder receives a {@code run.properties} file with
 * the framework version, the git commit, the random seed, and the absolute path
 * of each input file. The five resolved settings files are copied into a
 * {@code settings/} subfolder, so a result folder can be reproduced even after
 * the original scenario files have changed.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 */
public final class RunMetadata {

	/** Name of the metadata file written into every output folder. */
	public static final String FILE_NAME = "run.properties";

	/** Name of the subfolder that receives the resolved settings files. */
	public static final String SETTINGS_FOLDER_NAME = "settings";

	private RunMetadata() {
	}

	/**
	 * Writes the metadata and the settings snapshot of a campaign.
	 *
	 * @param outputFolder      the configured output folder
	 * @param campaignTimestamp the timestamp shared by the campaign's files
	 * @throws IllegalStateException when the files cannot be written
	 */
	public static void write(String outputFolder, String campaignTimestamp) {
		Path folder = Paths.get(outputFolder, campaignTimestamp).toAbsolutePath().normalize();
		synchronized (RunMetadata.class) {
			try {
				Files.createDirectories(folder);
				copySettings(folder.resolve(SETTINGS_FOLDER_NAME));
				Files.write(folder.resolve(FILE_NAME), describe(folder).getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new IllegalStateException("RunMetadata - Error, cannot write the run metadata to \"" + folder
						+ "\": " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Copies the five settings files actually used by this run, named after their
	 * role rather than after the custom file they may come from.
	 *
	 * @param folder the destination folder
	 * @throws IOException when a settings file cannot be read or copied
	 */
	private static void copySettings(Path folder) throws IOException {
		Files.createDirectories(folder);
		for (Map.Entry<String, String> entry : settingsFiles().entrySet()) {
			Path source = Paths.get(entry.getValue());
			Files.copy(source, folder.resolve(entry.getKey()), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Returns the settings files of the current run, keyed by their canonical name.
	 *
	 * @return the role-to-path map, in a stable order
	 */
	private static Map<String, String> settingsFiles() {
		Map<String, String> files = new LinkedHashMap<>();
		files.put("simulation_parameters.properties", SimulationParameters.simulationParametersFile);
		files.put("applications.xml", SimulationParameters.applicationFile);
		files.put("edge_datacenters.xml", SimulationParameters.edgeDataCentersFile);
		files.put("edge_devices.xml", SimulationParameters.edgeDevicesFile);
		files.put("cloud.xml", SimulationParameters.cloudDataCentersFile);
		return files;
	}

	/**
	 * Builds the content of {@code run.properties}.
	 *
	 * @param folder the output folder of the campaign
	 * @return the file content
	 */
	private static String describe(Path folder) {
		StringBuilder content = new StringBuilder();
		append(content, "version", version());
		append(content, "git.commit", gitCommit());
		append(content, "random.seed", Randomness.isSeeded() ? Long.toString(Randomness.getSeed()) : "unset");
		append(content, "parallelism", Boolean.toString(SimulationParameters.parallelism_enabled));
		append(content, "output.folder", path(folder));
		for (Map.Entry<String, String> entry : settingsFiles().entrySet()) {
			append(content, "settings." + entry.getKey(), path(Paths.get(entry.getValue())));
		}
		return content.toString();
	}

	/**
	 * Appends one {@code key=value} line.
	 *
	 * @param content the buffer to append to
	 * @param key     the property name
	 * @param value   the property value
	 */
	private static void append(StringBuilder content, String key, String value) {
		content.append(key).append('=').append(value.replace("\\", "\\\\")).append(System.lineSeparator());
	}

	/**
	 * Returns an absolute path with forward slashes, so the file stays readable on
	 * every platform.
	 *
	 * @param path the path to print
	 * @return the normalized absolute path
	 */
	private static String path(Path path) {
		return path.toAbsolutePath().normalize().toString().replace('\\', '/');
	}

	/**
	 * Returns the framework version injected by the build.
	 *
	 * @return the version, or {@code unknown} when the resource is missing
	 */
	static String version() {
		try (InputStream input = RunMetadata.class.getResourceAsStream("/pureedgesim-build.properties")) {
			if (input == null)
				return "unknown";
			Properties properties = new Properties();
			properties.load(input);
			return properties.getProperty("version", "unknown");
		} catch (IOException e) {
			return "unknown";
		}
	}

	/**
	 * Returns the commit of the repository the run was launched from.
	 *
	 * @return the commit hash, or {@code unknown} outside a git checkout
	 */
	static String gitCommit() {
		Path folder = Paths.get("").toAbsolutePath();
		while (folder != null) {
			Path dotGit = folder.resolve(".git");
			try {
				if (Files.isRegularFile(dotGit)) {
					String line = Files.readAllLines(dotGit, StandardCharsets.UTF_8).get(0);
					if (line.startsWith("gitdir:"))
						return commitFromGitDirectory(Paths.get(line.substring("gitdir:".length()).trim()));
				} else if (Files.isDirectory(dotGit)) {
					return commitFromGitDirectory(dotGit);
				}
			} catch (IOException | IndexOutOfBoundsException e) {
				return "unknown";
			}
			folder = folder.getParent();
		}
		return "unknown";
	}

	/**
	 * Resolves the current commit from a git directory.
	 *
	 * @param gitDirectory the {@code .git} directory
	 * @return the commit hash, or {@code unknown} when it cannot be resolved
	 * @throws IOException when {@code HEAD} cannot be read
	 */
	private static String commitFromGitDirectory(Path gitDirectory) throws IOException {
		Path head = gitDirectory.resolve("HEAD");
		if (!Files.isRegularFile(head))
			return "unknown";
		String content = Files.readAllLines(head, StandardCharsets.UTF_8).get(0).trim();
		if (!content.startsWith("ref:"))
			return content;
		Path reference = gitDirectory.resolve(content.substring("ref:".length()).trim());
		if (!Files.isRegularFile(reference))
			return "unknown";
		return Files.readAllLines(reference, StandardCharsets.UTF_8).get(0).trim();
	}
}
