package examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;

/**
 * Loads the settings of every example so that an invalid value is reported by
 * the test suite instead of by a user who runs the example.
 */
class ExampleSettingsTest extends AbstractTest {

	@Test
	void everyExampleSettingsFileParses() throws IOException {
		try (Stream<Path> examples = Files.list(Path.of("examples"))) {
			List<Path> files = examples
					.filter(Files::isDirectory)
					.map(folder -> folder.resolve("settings/simulation_parameters.properties"))
					.filter(Files::exists)
					.sorted()
					.toList();
			assertTrue(files.size() >= 4, "the examples with their own settings should be found");
			for (Path file : files)
				assertTrue(new ParametersParser(file.toString()).parse(), "failed to parse " + file);
		}
	}
}
