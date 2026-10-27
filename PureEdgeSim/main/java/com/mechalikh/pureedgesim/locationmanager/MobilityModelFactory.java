package com.mechalikh.pureedgesim.locationmanager;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Creates the mobility model selected by the {@code model} attribute of the
 * {@code <mobility>} element of a device.
 * <p>
 * Every built-in model has a short, case insensitive name, such as
 * {@code random-walk} or {@code gauss-markov}. A model that is not listed here
 * can still be used by giving its fully qualified class name instead, as long as
 * it extends {@link MobilityModel} and has a constructor taking a
 * {@link SimulationManager} and a {@link Location}. The class configured with
 * {@code Simulation.setCustomMobilityModel(Class)} remains the default for the
 * devices that do not declare a model.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public final class MobilityModelFactory {

	/** Built-in models, mapped from their name in the scenario files. */
	private static final Map<String, Class<? extends MobilityModel>> MODELS = buildModels();

	private MobilityModelFactory() {
	}

	/**
	 * Creates a mobility model by name.
	 *
	 * @param name              the model name, or a fully qualified class name
	 * @param simulationManager the simulation manager
	 * @param location          the initial location of the device
	 * @return a new instance of the model, without any option set
	 * @throws IllegalArgumentException when the name is unknown, the class cannot be
	 *                                  loaded, or it does not provide the expected
	 *                                  constructor
	 */
	public static MobilityModel create(String name, SimulationManager simulationManager, Location location) {
		return instantiate(resolve(name), simulationManager, location);
	}

	/**
	 * Returns the class of a mobility model.
	 *
	 * @param name the model name, or a fully qualified class name
	 * @return the class to instantiate
	 * @throws IllegalArgumentException when the name is unknown or the class cannot
	 *                                  be loaded
	 */
	public static Class<? extends MobilityModel> resolve(String name) {
		if (name == null || name.trim().isEmpty())
			throw new IllegalArgumentException("No mobility model given, expected one of: "
					+ String.join(", ", knownNames()) + " (or a custom class name)");
		String key = name.trim().toLowerCase(Locale.ROOT);
		Class<? extends MobilityModel> model = MODELS.get(key);
		if (model != null)
			return model;
		if (!name.contains("."))
			throw new IllegalArgumentException("Mobility model \"" + name + "\" is not supported, expected one of: "
					+ String.join(", ", knownNames()) + " (or a custom class name)");
		try {
			Class<?> loaded = Class.forName(name.trim());
			if (!MobilityModel.class.isAssignableFrom(loaded))
				throw new IllegalArgumentException("The class \"" + name + "\" does not extend MobilityModel");
			return loaded.asSubclass(MobilityModel.class);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Mobility model class \"" + name + "\" was not found; check the "
					+ "spelling, or add it to the classpath");
		}
	}

	/**
	 * Lists the names of the built-in models.
	 *
	 * @return a sorted, unmodifiable set of model names
	 */
	public static Set<String> knownNames() {
		return Collections.unmodifiableSet(new TreeSet<>(MODELS.keySet()));
	}

	/**
	 * Returns the built-in models and the classes behind each name.
	 *
	 * @return an unmodifiable map of model name to class
	 */
	public static Map<String, Class<? extends MobilityModel>> knownModels() {
		return Collections.unmodifiableMap(MODELS);
	}

	/**
	 * Calls the {@code (SimulationManager, Location)} constructor of a model.
	 *
	 * @param modelClass        the class to instantiate
	 * @param simulationManager the simulation manager
	 * @param location          the initial location of the device
	 * @return a new instance of the model
	 * @throws IllegalArgumentException when the constructor is missing or fails
	 */
	private static MobilityModel instantiate(Class<? extends MobilityModel> modelClass,
			SimulationManager simulationManager, Location location) {
		try {
			Constructor<? extends MobilityModel> constructor = modelClass.getConstructor(SimulationManager.class,
					Location.class);
			return constructor.newInstance(simulationManager, location);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The mobility model \"" + modelClass.getName() + "\" must provide a "
					+ "public constructor taking a SimulationManager and a Location");
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new IllegalArgumentException("The mobility model \"" + modelClass.getName() + "\" could not be "
					+ "created: " + e.getMessage(), e);
		}
	}

	/**
	 * Builds the table of the built-in models, including their aliases.
	 *
	 * @return an ordered map of model name to class
	 */
	private static Map<String, Class<? extends MobilityModel>> buildModels() {
		Map<String, Class<? extends MobilityModel>> models = new LinkedHashMap<>();
		models.put("random-waypoint", DefaultMobilityModel.class);
		models.put("default", DefaultMobilityModel.class);
		models.put("random-walk", RandomWalkMobilityModel.class);
		models.put("gauss-markov", GaussMarkovMobilityModel.class);
		models.put("manhattan", ManhattanMobilityModel.class);
		models.put("waypoint", WaypointMobilityModel.class);
		models.put("circular", CircularMobilityModel.class);
		models.put("levy-walk", LevyWalkMobilityModel.class);
		models.put("brownian", BrownianMobilityModel.class);
		models.put("group", ReferencePointGroupMobilityModel.class);
		models.put("rpgm", ReferencePointGroupMobilityModel.class);
		return models;
	}

}
