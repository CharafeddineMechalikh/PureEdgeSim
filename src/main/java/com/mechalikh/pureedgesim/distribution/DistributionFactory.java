package com.mechalikh.pureedgesim.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * Builds {@link Distribution} objects from the names and attributes used inside
 * the scenario XML files.
 * <p>
 * A distribution is selected by its lower-case name and parameterised through a
 * map of attribute values, exactly as they appear in the XML files. Each
 * distribution reads the attributes listed below; missing or malformed
 * attributes raise an exception that names both the distribution and the
 * attribute.
 * <ul>
 * <li>{@code constant} → {@code value}</li>
 * <li>{@code uniform} → {@code min}, {@code max}</li>
 * <li>{@code triangular} → {@code min}, {@code mode}, {@code max}</li>
 * <li>{@code normal} → {@code mean}, {@code std}</li>
 * <li>{@code lognormal} → {@code mean}, {@code std} (of the generated values)</li>
 * <li>{@code exponential} → {@code mean}</li>
 * <li>{@code gamma} / {@code erlang} → {@code shape}, {@code scale}</li>
 * <li>{@code weibull} → {@code shape}, {@code scale}</li>
 * <li>{@code pareto} → {@code scale} (minimum value), {@code shape}</li>
 * <li>{@code beta} → {@code alpha}, {@code beta}</li>
 * <li>{@code poisson} → {@code mean}</li>
 * <li>{@code bernoulli} / {@code geometric} → {@code probability}</li>
 * <li>{@code binomial} → {@code trials}, {@code probability}</li>
 * <li>{@code categorical} → {@code values} (comma separated), {@code weights}
 * (comma separated, optional)</li>
 * </ul>
 * <p>
 * {@code min} and {@code max} define the support of {@code uniform} and
 * {@code triangular}. For every other distribution they are optional and, when
 * present, wrap the result into a {@link BoundedDistribution}.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public final class DistributionFactory {

	/** Attribute that carries the distribution name. */
	public static final String NAME_ATTRIBUTE = "distribution";

	/** Attribute that carries the lower bound of a value. */
	public static final String MIN_ATTRIBUTE = "min";

	/** Attribute that carries the upper bound of a value. */
	public static final String MAX_ATTRIBUTE = "max";

	/** Lower bound used when no minimum is configured. */
	private static final double NO_MIN = Double.NEGATIVE_INFINITY;

	/** Upper bound used when no maximum is configured. */
	private static final double NO_MAX = Double.POSITIVE_INFINITY;

	/** Supported distribution names, mapped to the attributes they require. */
	private static final Map<String, String> KNOWN_DISTRIBUTIONS = buildKnownDistributions();

	private DistributionFactory() {
	}

	/**
	 * Builds a distribution from its name and parameters, using the random
	 * generator of the current thread.
	 *
	 * @param name       the distribution name, case insensitive
	 * @param parameters the attribute values, may be empty
	 * @return the distribution described by the arguments
	 * @throws IllegalArgumentException when the name is unknown or an attribute is
	 *                                  missing or invalid
	 */
	public static Distribution create(String name, Map<String, String> parameters) {
		return create(name, parameters, Randomness.getRandom());
	}

	/**
	 * Builds a distribution from its name, parameters and a given generator.
	 *
	 * @param name       the distribution name, case insensitive
	 * @param parameters the attribute values, may be empty
	 * @param random     the generator used by the distribution
	 * @return the distribution described by the arguments
	 * @throws IllegalArgumentException when the name is unknown or an attribute is
	 *                                  missing or invalid
	 */
	public static Distribution create(String name, Map<String, String> parameters, Random random) {
		if (name == null || name.trim().isEmpty())
			throw new IllegalArgumentException(
					"No distribution name given, expected one of: " + String.join(", ", knownNames()));
		String key = name.trim().toLowerCase(Locale.ROOT);
		if (!KNOWN_DISTRIBUTIONS.containsKey(key))
			throw new IllegalArgumentException("Distribution \"" + key + "\" is not supported, expected one of: "
					+ String.join(", ", knownNames()));
		return applyBounds(key, build(key, parameters, random), parameters);
	}

	/**
	 * Creates a distribution that always returns the given value. Used when a
	 * scenario file declares a plain number instead of a distribution.
	 *
	 * @param value the value returned on every draw
	 * @return a constant distribution
	 */
	public static Distribution constant(double value) {
		return new ConstantDistribution(value);
	}

	/**
	 * Lists the supported distribution names.
	 *
	 * @return the sorted set of names accepted by {@link #create(String, Map)}
	 */
	public static TreeSet<String> knownNames() {
		return new TreeSet<>(KNOWN_DISTRIBUTIONS.keySet());
	}

	/**
	 * Returns the attributes required by each distribution.
	 *
	 * @return an unmodifiable map of distribution name to required attributes
	 */
	public static Map<String, String> requiredAttributes() {
		return KNOWN_DISTRIBUTIONS;
	}

	/**
	 * Instantiates the distribution matching the given name.
	 *
	 * @param key        the normalized distribution name
	 * @param parameters the attribute values
	 * @param random     the generator used by the distribution
	 * @return the distribution, without any additional bound
	 */
	private static Distribution build(String key, Map<String, String> parameters, Random random) {
		switch (key) {
		case "constant":
			return new ConstantDistribution(required(parameters, key, "value"));
		case "uniform":
			return new UniformDistribution(random, required(parameters, key, MIN_ATTRIBUTE),
					required(parameters, key, MAX_ATTRIBUTE));
		case "triangular":
			return new TriangularDistribution(random, required(parameters, key, MIN_ATTRIBUTE),
					required(parameters, key, "mode"), required(parameters, key, MAX_ATTRIBUTE));
		case "normal":
			return new NormalDistribution(random, required(parameters, key, "mean"), required(parameters, key, "std"));
		case "lognormal":
			return new LogNormalDistribution(random, required(parameters, key, "mean"),
					required(parameters, key, "std"));
		case "exponential":
			return new ExponentialDistribution(random, required(parameters, key, "mean"));
		case "gamma":
			return new GammaDistribution(random, required(parameters, key, "shape"),
					required(parameters, key, "scale"));
		case "erlang":
			return new ErlangDistribution(random, required(parameters, key, "shape"),
					required(parameters, key, "scale"));
		case "weibull":
			return new WeibullDistribution(random, required(parameters, key, "shape"),
					required(parameters, key, "scale"));
		case "pareto":
			return new ParetoDistribution(random, required(parameters, key, "scale"),
					required(parameters, key, "shape"));
		case "beta":
			return new BetaDistribution(random, required(parameters, key, "alpha"), required(parameters, key, "beta"));
		case "poisson":
			return new PoissonDistribution(random, required(parameters, key, "mean"));
		case "bernoulli":
			return new BernoulliDistribution(random, required(parameters, key, "probability"));
		case "geometric":
			return new GeometricDistribution(random, required(parameters, key, "probability"));
		case "binomial":
			return new BinomialDistribution(random, (int) required(parameters, key, "trials"),
					required(parameters, key, "probability"));
		case "categorical":
			return new CategoricalDistribution(random, parseList(parameters, key, "values"),
					parseOptionalList(parameters, key, "weights"));
		default:
			throw new IllegalArgumentException(
					"Distribution \"" + key + "\" is not supported, expected one of: " + String.join(", ", knownNames()));
		}
	}

	/**
	 * Wraps the distribution into a {@link BoundedDistribution} when bounds are
	 * configured and the distribution does not define its support itself.
	 *
	 * @param key          the normalized distribution name
	 * @param distribution the distribution to bound
	 * @param parameters   the attribute values
	 * @return the bounded distribution, or the given one when it needs no bound
	 */
	private static Distribution applyBounds(String key, Distribution distribution, Map<String, String> parameters) {
		if (key.equals("uniform") || key.equals("triangular"))
			return distribution;
		Double min = optional(parameters, key, MIN_ATTRIBUTE);
		Double max = optional(parameters, key, MAX_ATTRIBUTE);
		if (min == null && max == null)
			return distribution;
		return new BoundedDistribution(distribution, min == null ? NO_MIN : min, max == null ? NO_MAX : max);
	}

	/**
	 * Reads a mandatory attribute.
	 *
	 * @param parameters the attribute values
	 * @param key        the distribution name, used in the error message
	 * @param attribute  the attribute name
	 * @return the parsed value
	 * @throws IllegalArgumentException when the attribute is absent or not numeric
	 */
	private static double required(Map<String, String> parameters, String key, String attribute) {
		String value = parameters.get(attribute);
		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Distribution \"" + key + "\": the attribute \"" + attribute
					+ "\" is required, expected: " + KNOWN_DISTRIBUTIONS.get(key));
		return parseDouble(key, attribute, value);
	}

	/**
	 * Reads an optional attribute.
	 *
	 * @param parameters the attribute values
	 * @param key        the distribution name, used in the error message
	 * @param attribute  the attribute name
	 * @return the parsed value, or {@code null} when absent
	 * @throws IllegalArgumentException when the attribute is present but not numeric
	 */
	private static Double optional(Map<String, String> parameters, String key, String attribute) {
		String value = parameters.get(attribute);
		if (value == null || value.trim().isEmpty())
			return null;
		return parseDouble(key, attribute, value);
	}

	/**
	 * Reads a mandatory comma separated list of numbers.
	 *
	 * @param parameters the attribute values
	 * @param key        the distribution name, used in the error message
	 * @param attribute  the attribute name
	 * @return the parsed values
	 * @throws IllegalArgumentException when the attribute is absent or malformed
	 */
	private static double[] parseList(Map<String, String> parameters, String key, String attribute) {
		String value = parameters.get(attribute);
		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Distribution \"" + key + "\": the attribute \"" + attribute
					+ "\" is required and must be a comma separated list of numbers");
		return toList(key, attribute, value);
	}

	/**
	 * Reads an optional comma separated list of numbers.
	 *
	 * @param parameters the attribute values
	 * @param key        the distribution name, used in the error message
	 * @param attribute  the attribute name
	 * @return the parsed values, or {@code null} when the attribute is absent
	 */
	private static double[] parseOptionalList(Map<String, String> parameters, String key, String attribute) {
		String value = parameters.get(attribute);
		if (value == null || value.trim().isEmpty())
			return null;
		return toList(key, attribute, value);
	}

	/**
	 * Splits and parses a comma separated list.
	 *
	 * @param key       the distribution name, used in the error message
	 * @param attribute the attribute name
	 * @param value     the raw list
	 * @return the parsed values
	 */
	private static double[] toList(String key, String attribute, String value) {
		List<Double> values = new ArrayList<>();
		for (String part : value.split(","))
			values.add(parseDouble(key, attribute, part));
		double[] result = new double[values.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = values.get(i);
		return result;
	}

	/**
	 * Parses a numeric attribute.
	 *
	 * @param key       the distribution name, used in the error message
	 * @param attribute the attribute name
	 * @param value     the raw value
	 * @return the parsed value
	 * @throws IllegalArgumentException when the value is not a number
	 */
	private static double parseDouble(String key, String attribute, String value) {
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Distribution \"" + key + "\": the attribute \"" + attribute
					+ "\" must be a number, found \"" + value.trim() + "\"");
		}
	}

	/**
	 * Builds the table of supported distributions and their required attributes.
	 *
	 * @return an unmodifiable map, iterated in a stable order
	 */
	private static Map<String, String> buildKnownDistributions() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("constant", "value");
		map.put("uniform", MIN_ATTRIBUTE + ", " + MAX_ATTRIBUTE);
		map.put("triangular", MIN_ATTRIBUTE + ", mode, " + MAX_ATTRIBUTE);
		map.put("normal", "mean, std");
		map.put("lognormal", "mean, std");
		map.put("exponential", "mean");
		map.put("gamma", "shape, scale");
		map.put("erlang", "shape, scale");
		map.put("weibull", "shape, scale");
		map.put("pareto", "scale, shape");
		map.put("beta", "alpha, beta");
		map.put("poisson", "mean");
		map.put("bernoulli", "probability");
		map.put("geometric", "probability");
		map.put("binomial", "trials, probability");
		map.put("categorical", "values, weights (optional)");
		return Collections.unmodifiableMap(map);
	}

}
