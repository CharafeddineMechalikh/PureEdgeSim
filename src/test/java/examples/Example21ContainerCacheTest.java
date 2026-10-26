package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.Task;

class Example21ContainerCacheTest extends AbstractTest {

	private Task taskWithApplication(int applicationId) {
		Task task = mock(Task.class);
		when(task.getApplicationID()).thenReturn(applicationId);
		return task;
	}

	@Test
	void aStoredContainerIsServedFromTheCache() {
		Example21ContainerCache cache = new Example21ContainerCache(3);

		assertTrue(cache.store(1, 1.5));
		assertTrue(cache.contains(1));
		assertTrue(cache.lookup(1));
		assertEquals(1, cache.getHits());
		assertEquals(0, cache.getMisses());
		assertEquals(1.5, cache.getUsedStorageMB(), 1e-9);
	}

	@Test
	void aMissingContainerCountsAsAMiss() {
		Example21ContainerCache cache = new Example21ContainerCache(3);

		assertFalse(cache.lookup(4));
		assertEquals(1, cache.getMisses());
		assertEquals(0, cache.getHitRatio());
	}

	@Test
	void storingAnAlreadyCachedApplicationRefreshesItWithoutDuplicatingItsSize() {
		Example21ContainerCache cache = new Example21ContainerCache(3);

		cache.store(1, 1.5);
		cache.store(1, 1.5);

		assertEquals(1, cache.size());
		assertEquals(1.5, cache.getUsedStorageMB(), 1e-9);
	}

	@Test
	void theLeastRecentlyUsedEntryIsEvictedFirst() {
		Example21ContainerCache cache = new Example21ContainerCache(3);
		cache.store(1, 1);
		cache.store(2, 1);
		cache.store(3, 1);

		cache.lookup(1);
		cache.store(4, 1);

		assertFalse(cache.contains(2), "the untouched entry should be evicted");
		assertTrue(cache.contains(1), "the recently used entry should survive");
		assertTrue(cache.contains(3));
		assertTrue(cache.contains(4));
		assertEquals(1, cache.getEvictions());
		assertEquals(3, cache.getUsedStorageMB(), 1e-9);
	}

	@Test
	void storingALargeContainerEvictsSeveralEntriesUntilItFits() {
		Example21ContainerCache cache = new Example21ContainerCache(3);
		cache.store(1, 0.02);
		cache.store(2, 1.5);

		assertTrue(cache.store(3, 2.2));

		assertFalse(cache.contains(1));
		assertFalse(cache.contains(2));
		assertEquals(2.2, cache.getUsedStorageMB(), 1e-9);
		assertEquals(2, cache.getEvictions());
	}

	@Test
	void aContainerLargerThanTheWholeCacheIsRefusedWithoutEvictingTheOthers() {
		Example21ContainerCache cache = new Example21ContainerCache(2);
		cache.store(1, 1.5);

		assertFalse(cache.store(2, 3));

		assertTrue(cache.contains(1));
		assertEquals(1, cache.getRejected());
		assertEquals(0, cache.getEvictions());
	}

	@Test
	void aZeroCapacityCacheRefusesEveryContainer() {
		Example21ContainerCache cache = new Example21ContainerCache(0);

		assertFalse(cache.store(1, 0.02));
		assertEquals(0, cache.size());
		assertEquals(1, cache.getRejected());
	}

	@Test
	void theHitRatioCombinesHitsAndMisses() {
		Example21ContainerCache cache = new Example21ContainerCache(3);

		cache.lookup(1);
		cache.store(1, 1.5);
		cache.lookup(1);
		cache.lookup(1);

		assertEquals(2, cache.getHits());
		assertEquals(1, cache.getMisses());
		assertEquals(2.0 / 3, cache.getHitRatio(), 1e-9);
	}

	@Test
	void theCloudNeverDownloadsAContainer() {
		ComputingNode cloud = mock(ComputingNode.class);
		when(cloud.getType()).thenReturn(TYPES.CLOUD);

		assertFalse(Example21ContainerCacheNetworkModel.needsContainerDownload(cloud, taskWithApplication(1)));
	}

	@Test
	void aNodeWithoutACacheAlwaysDownloads() {
		ComputingNode node = mock(ComputingNode.class);
		when(node.getType()).thenReturn(TYPES.EDGE_DATACENTER);

		assertTrue(Example21ContainerCacheNetworkModel.needsContainerDownload(node, taskWithApplication(1)));
	}

	@Test
	void aCachedApplicationDoesNotNeedADownload() {
		Example21ContainerCache cache = new Example21ContainerCache(3);
		cache.store(7, 1.5);
		Example21CacheDevice node = mock(Example21CacheDevice.class);
		when(node.getType()).thenReturn(TYPES.EDGE_DATACENTER);
		doReturn(cache).when(node).getContainerCache();

		assertFalse(Example21ContainerCacheNetworkModel.needsContainerDownload(node, taskWithApplication(7)));
		assertEquals(1, cache.getHits());
	}

	@Test
	void theFirstRequestMissesAndTheSecondHits() {
		Example21ContainerCache cache = new Example21ContainerCache(3);
		Example21CacheDevice node = mock(Example21CacheDevice.class);
		when(node.getType()).thenReturn(TYPES.EDGE_DATACENTER);
		doReturn(cache).when(node).getContainerCache();
		Task task = taskWithApplication(7);

		assertTrue(Example21ContainerCacheNetworkModel.needsContainerDownload(node, task));
		cache.store(7, 1.5);
		assertFalse(Example21ContainerCacheNetworkModel.needsContainerDownload(node, task));
		assertEquals(1, cache.getMisses());
		assertEquals(1, cache.getHits());
	}

	@Test
	@Tag("integration")
	void aRealSimulationServesContainersFromTheCache() throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream console = new ByteArrayOutputStream();
		System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
		try {
			Simulation simulation = new Simulation();
			simulation.setCustomComputingNode(Example21CacheDevice.class);
			simulation.setCustomNetworkModel(Example21ContainerCacheNetworkModel.class);
			simulation.setCustomSettingsFolder(SETTINGS_FOLDER);
			simulation.setCustomOutputFolder(OUTPUT_FOLDER);
			useSettings("containercache.properties");
			simulation.launchSimulation();
		} finally {
			System.setOut(originalOut);
		}

		String output = console.toString(StandardCharsets.UTF_8);
		assertTrue(output.contains("Example21 - container cache:"), "the run should report its cache statistics");
		Matcher matcher = Pattern.compile("container cache: (\\d+) hits, (\\d+) misses").matcher(output);
		assertTrue(matcher.find(), "the cache summary should contain the hit and miss counts");
		assertTrue(Integer.parseInt(matcher.group(1)) > 0, "at least one request should be served from the cache");

		Path latest = Files.list(Path.of(OUTPUT_FOLDER))
				.filter(Files::isDirectory)
				.filter(path -> Character.isDigit(path.getFileName().toString().charAt(0)))
				.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
				.orElseThrow(() -> new AssertionError("no output folder"));
		Path csv = Files.walk(latest)
				.filter(path -> path.toString().endsWith(".csv"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no results csv in " + latest));
		String results = Files.readString(csv);
		assertTrue(results.contains("EDGE_ONLY"), "the csv should name the architecture that ran");
		assertTrue(results.contains("TRADE_OFF"), "the csv should name the algorithm that ran");
	}
}
