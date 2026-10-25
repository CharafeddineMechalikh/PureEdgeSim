package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class DistributionFactoryTest {

	private static Map<String, String> attrs(String... pairs) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
			map.put(pairs[i], pairs[i + 1]);
		return map;
	}

	private static Distribution create(String name, String... pairs) {
		return DistributionFactory.create(name, attrs(pairs));
	}

	@Test
	void mapsEveryNameToTheRightClassAndParameters() {
		assertInstanceOf(ConstantDistribution.class, create("constant", "value", "7"));
		assertInstanceOf(UniformDistribution.class, create("uniform", "min", "1", "max", "5"));
		assertInstanceOf(TriangularDistribution.class, create("triangular", "min", "1", "mode", "2", "max", "6"));
		assertInstanceOf(NormalDistribution.class, create("normal", "mean", "10", "std", "2"));
		assertInstanceOf(LogNormalDistribution.class, create("lognormal", "mean", "10", "std", "2"));
		assertInstanceOf(ExponentialDistribution.class, create("exponential", "mean", "4"));
		assertInstanceOf(GammaDistribution.class, create("gamma", "shape", "2", "scale", "3"));
		assertInstanceOf(ErlangDistribution.class, create("erlang", "shape", "3", "scale", "2"));
		assertInstanceOf(WeibullDistribution.class, create("weibull", "shape", "2", "scale", "5"));
		assertInstanceOf(ParetoDistribution.class, create("pareto", "scale", "1", "shape", "4"));
		assertInstanceOf(BetaDistribution.class, create("beta", "alpha", "2", "beta", "5"));
		assertInstanceOf(PoissonDistribution.class, create("poisson", "mean", "5"));
		assertInstanceOf(BernoulliDistribution.class, create("bernoulli", "probability", "0.3"));
		assertInstanceOf(GeometricDistribution.class, create("geometric", "probability", "0.3"));
		assertInstanceOf(BinomialDistribution.class, create("binomial", "trials", "20", "probability", "0.4"));
		assertInstanceOf(CategoricalDistribution.class, create("categorical", "values", "1,2,3"));
	}

	@Test
	void eachBuiltDistributionCarriesTheExpectedMean() {
		assertEquals(7.0, create("constant", "value", "7").mean(), 1e-9);
		assertEquals(3.0, create("uniform", "min", "1", "max", "5").mean(), 1e-9);
		assertEquals(3.0, create("triangular", "min", "1", "mode", "2", "max", "6").mean(), 1e-9);
		assertEquals(10.0, create("normal", "mean", "10", "std", "2").mean(), 1e-9);
		assertEquals(10.0, create("lognormal", "mean", "10", "std", "2").mean(), 1e-9);
		assertEquals(4.0, create("exponential", "mean", "4").mean(), 1e-9);
		assertEquals(6.0, create("gamma", "shape", "2", "scale", "3").mean(), 1e-9);
		assertEquals(6.0, create("erlang", "shape", "3", "scale", "2").mean(), 1e-9);
		assertEquals(4.0 / 3.0, create("pareto", "scale", "1", "shape", "4").mean(), 1e-9);
		assertEquals(2.0 / 7.0, create("beta", "alpha", "2", "beta", "5").mean(), 1e-9);
		assertEquals(5.0, create("poisson", "mean", "5").mean(), 1e-9);
		assertEquals(0.3, create("bernoulli", "probability", "0.3").mean(), 1e-9);
		assertEquals(1 / 0.3, create("geometric", "probability", "0.3").mean(), 1e-9);
		assertEquals(8.0, create("binomial", "trials", "20", "probability", "0.4").mean(), 1e-9);
		assertEquals(2.25, create("categorical", "values", "1,2,3", "weights", "1,1,2").mean(), 1e-9);
	}

	@Test
	void theSuppliedGeneratorIsForwardedToTheBuiltDistribution() {
		Random random = new Random(1);
		UniformDistribution uniform = (UniformDistribution) DistributionFactory.create("uniform", attrs("min", "0",
				"max", "10"), random);
		assertSame(random, uniform.getRandom());
	}

	@Test
	void namesAreCaseInsensitiveAndTrimmed() {
		assertInstanceOf(NormalDistribution.class, create("  NORMAL ", "mean", "1", "std", "1"));
		assertInstanceOf(UniformDistribution.class, create("Uniform", "min", "0", "max", "1"));
	}

	@Test
	void minAndMaxWrapEverythingExceptUniformAndTriangular() {
		Distribution normal = create("normal", "mean", "10", "std", "2", "min", "0", "max", "5");
		BoundedDistribution bounded = assertInstanceOf(BoundedDistribution.class, normal);
		assertEquals(0.0, bounded.getMin(), 0.0);
		assertEquals(5.0, bounded.getMax(), 0.0);
		assertInstanceOf(NormalDistribution.class, bounded.getDelegate());

		assertInstanceOf(UniformDistribution.class, create("uniform", "min", "1", "max", "5"));
		assertInstanceOf(TriangularDistribution.class, create("triangular", "min", "1", "mode", "2", "max", "6"));
	}

	@Test
	void aBoundWithOnlyOneLimitUsesInfinityForTheMissingOne() {
		BoundedDistribution withMax = assertInstanceOf(BoundedDistribution.class,
				create("exponential", "mean", "4", "max", "10"));
		assertEquals(Double.NEGATIVE_INFINITY, withMax.getMin(), 0.0);
		assertEquals(10.0, withMax.getMax(), 0.0);

		BoundedDistribution withMin = assertInstanceOf(BoundedDistribution.class,
				create("exponential", "mean", "4", "min", "1"));
		assertEquals(1.0, withMin.getMin(), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, withMin.getMax(), 0.0);

		assertInstanceOf(ExponentialDistribution.class, create("exponential", "mean", "4"));
	}

	@Test
	void theConstantFactoryHelperReturnsAConstantDistribution() {
		ConstantDistribution distribution = assertInstanceOf(ConstantDistribution.class,
				DistributionFactory.constant(42));
		assertEquals(42.0, distribution.sample(), 0.0);
	}

	@Test
	void aMissingNameIsRejectedWithTheListOfKnownNames() {
		IllegalArgumentException nullName = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create(null, attrs()));
		IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("   ", attrs()));
		assertTrue(nullName.getMessage().contains("No distribution name given, expected one of:"));
		assertTrue(blank.getMessage().contains("No distribution name given, expected one of:"));
	}

	@Test
	void anUnknownNameIsRejectedWithTheListOfKnownNames() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("  Foo ", attrs()));
		assertTrue(error.getMessage().contains("Distribution \"foo\" is not supported"));
		assertTrue(error.getMessage().contains("expected one of:"));
		// The normalised key is used in the message and every known name is listed.
		assertTrue(error.getMessage().contains("uniform"));
		assertTrue(error.getMessage().contains("weibull"));
	}

	@Test
	void aMissingRequiredAttributeNamesTheAttributeAndTheExpectation() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("normal", attrs()));
		assertTrue(error.getMessage().contains("Distribution \"normal\""));
		assertTrue(error.getMessage().contains("the attribute \"mean\" is required"));
		assertTrue(error.getMessage().contains("expected: mean, std"));
	}

	@Test
	void aBlankAttributeIsTreatedAsMissing() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("normal", attrs("mean", "   ", "std", "2")));
		assertTrue(error.getMessage().contains("the attribute \"mean\" is required"));
	}

	@Test
	void aNonNumericAttributeIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("normal", attrs("mean", "abc", "std", "2")));
		assertTrue(error.getMessage().contains("the attribute \"mean\" must be a number, found \"abc\""));
	}

	@Test
	void aNonNumericIntegerAttributeIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("binomial", attrs("trials", "x", "probability", "0.5")));
		assertTrue(error.getMessage().contains("the attribute \"trials\" must be a number, found \"x\""));
	}

	@Test
	void aMissingCategoricalValueListIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("categorical", attrs()));
		assertTrue(error.getMessage().contains("the attribute \"values\" is required"));
		assertTrue(error.getMessage().contains("comma separated list of numbers"));
	}

	@Test
	void aFractionalErlangShapeRejectedByTheDistributionPropagates() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("erlang", attrs("shape", "2.5", "scale", "1")));
		assertTrue(error.getMessage().contains("must be a whole number of phases"));
	}

	@Test
	void aDistributionLevelValidationErrorPropagates() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionFactory.create("exponential", attrs("mean", "-1")));
		assertTrue(error.getMessage().contains("the parameter \"mean\" must be > 0.0"));
	}

	@Test
	void knownNamesListsEverySupportedDistributionSorted() {
		TreeSet<String> names = DistributionFactory.knownNames();
		assertEquals(16, names.size());
		assertEquals("bernoulli", names.first());
		assertEquals("weibull", names.last());
		assertTrue(names.contains("gamma"));
		assertTrue(names.contains("categorical"));
	}

	@Test
	void requiredAttributesDescribeTheExpectedParameterLists() {
		Map<String, String> required = DistributionFactory.requiredAttributes();
		assertEquals("min, max", required.get("uniform"));
		assertEquals("trials, probability", required.get("binomial"));
		assertEquals("values, weights (optional)", required.get("categorical"));
		assertEquals("alpha, beta", required.get("beta"));
	}

	@Test
	void theRequiredAttributesMapIsUnmodifiable() {
		assertThrows(UnsupportedOperationException.class,
				() -> DistributionFactory.requiredAttributes().put("x", "y"));
	}
}
