package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Tests the resolution of the {@code model} attribute of the {@code <mobility>}
 * element: the built-in names and their aliases, the custom classes given by
 * their fully qualified name, and the errors raised for the names that cannot be
 * used.
 */
class MobilityModelFactoryTest extends AbstractTest {

	/** A model defined outside the framework, selected by its class name. */
	public static class CustomModel extends MobilityModel {
		public CustomModel(SimulationManager simulationManager, Location location) {
			super(simulationManager, location);
		}

		@Override
		protected Location getNextLocation(Location location) {
			return location;
		}
	}

	/** A model that does not provide the constructor the framework expects. */
	public static class ModelWithoutTheRightConstructor extends MobilityModel {
		public ModelWithoutTheRightConstructor(String ignored) {
		}

		@Override
		protected Location getNextLocation(Location location) {
			return location;
		}
	}

	/** A model that fails while being created. */
	public static class BrokenModel extends MobilityModel {
		public BrokenModel(SimulationManager simulationManager, Location location) {
			super(simulationManager, location);
			throw new IllegalStateException("the radius must be positive");
		}

		@Override
		protected Location getNextLocation(Location location) {
			return location;
		}
	}

	private final SimulationManager simulationManager = mock(SimulationManager.class);
	private final Location start = new Location(10, 20);

	private void assertBuilds(String name, Class<? extends MobilityModel> expected) {
		MobilityModel model = MobilityModelFactory.create(name, simulationManager, start);

		assertEquals(expected, model.getClass(), "\"" + name + "\" did not build the expected model");
		assertEquals(start, model.getCurrentLocation());
		assertTrue(model.getParameters().isEmpty(), "the factory must not set any option");
	}

	@Test
	void everyBuiltInModelIsReachedByName() {
		assertBuilds("random-waypoint", DefaultMobilityModel.class);
		assertBuilds("random-walk", RandomWalkMobilityModel.class);
		assertBuilds("gauss-markov", GaussMarkovMobilityModel.class);
		assertBuilds("manhattan", ManhattanMobilityModel.class);
		assertBuilds("waypoint", WaypointMobilityModel.class);
		assertBuilds("circular", CircularMobilityModel.class);
		assertBuilds("levy-walk", LevyWalkMobilityModel.class);
		assertBuilds("brownian", BrownianMobilityModel.class);
		assertBuilds("group", ReferencePointGroupMobilityModel.class);
	}

	@Test
	void aliasesPointToTheSameModels() {
		assertBuilds("default", DefaultMobilityModel.class);
		assertBuilds("rpgm", ReferencePointGroupMobilityModel.class);
	}

	@Test
	void namesAreCaseInsensitiveAndTrimmed() {
		assertBuilds(" GAUSS-MARKOV ", GaussMarkovMobilityModel.class);
		assertBuilds("Levy-Walk", LevyWalkMobilityModel.class);
	}

	@Test
	void aCustomModelIsReachedByItsClassName() {
		assertBuilds(CustomModel.class.getName(), CustomModel.class);
	}

	@Test
	void knownNamesListEveryBuiltInModelOncePerClass() {
		Set<String> names = MobilityModelFactory.knownNames();

		assertTrue(names.containsAll(List.of("random-waypoint", "random-walk", "gauss-markov", "manhattan", "waypoint",
				"circular", "levy-walk", "brownian", "group", "rpgm", "default")), "a model is missing: " + names);
		List<String> returned = new ArrayList<>(names);
		List<String> sorted = new ArrayList<>(returned);
		Collections.sort(sorted);
		assertEquals(sorted, returned, "the names are not returned in alphabetical order");
		assertThrows(UnsupportedOperationException.class, () -> names.add("teleport"),
				"the list of names must not be modifiable");
	}

	@Test
	void knownModelsMapsEveryNameToAClass() {
		assertEquals(MobilityModelFactory.knownNames().size(), MobilityModelFactory.knownModels().size());
		for (String name : MobilityModelFactory.knownNames())
			assertTrue(MobilityModel.class.isAssignableFrom(MobilityModelFactory.knownModels().get(name)),
					name + " does not point to a mobility model");
		assertThrows(UnsupportedOperationException.class,
				() -> MobilityModelFactory.knownModels().put("teleport", DefaultMobilityModel.class));
	}

	@Test
	void anUnknownShortNameListsTheModelsItCouldHaveChosen() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> MobilityModelFactory.create("teleport", simulationManager, start));

		assertTrue(error.getMessage().contains("teleport"), "the name must be quoted: " + error.getMessage());
		assertTrue(error.getMessage().contains("gauss-markov"),
				"the message must list the models: " + error.getMessage());
	}

	@Test
	void anEmptyNameIsRejected() {
		IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
				() -> MobilityModelFactory.create(null, simulationManager, start));
		assertTrue(missing.getMessage().contains("No mobility model given"));

		assertThrows(IllegalArgumentException.class, () -> MobilityModelFactory.create("   ", simulationManager, start));
	}

	@Test
	void aClassNameThatIsNotAMobilityModelIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> MobilityModelFactory.resolve("java.util.Random"));

		assertTrue(error.getMessage().contains("does not extend MobilityModel"), "unexpected message: " + error);
	}

	@Test
	void aClassNameThatWasNotCompiledIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> MobilityModelFactory.resolve("examples.GhostMobilityModel"));

		assertTrue(error.getMessage().contains("was not found"), "unexpected message: " + error);
	}

	@Test
	void aModelWithoutTheExpectedConstructorIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> MobilityModelFactory.create(ModelWithoutTheRightConstructor.class.getName(), simulationManager,
						start));

		assertTrue(error.getMessage().contains("must provide a public constructor"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void aModelThatFailsToBuildReportsWhy() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> MobilityModelFactory.create(BrokenModel.class.getName(), simulationManager, start));

		assertTrue(error.getMessage().contains("could not be created"), "unexpected message: " + error.getMessage());
	}
}
