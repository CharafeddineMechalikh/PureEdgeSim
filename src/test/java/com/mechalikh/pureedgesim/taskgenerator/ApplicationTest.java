package com.mechalikh.pureedgesim.taskgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.distribution.ConstantDistribution;
import com.mechalikh.pureedgesim.distribution.ExponentialDistribution;
import com.mechalikh.pureedgesim.distribution.UniformDistribution;

class ApplicationTest {

	private Application newApplication() {
		return new Application("Type", 10, 25.0, 0.5, 8_000_000L, 4_000_000L, 2_000_000L, 1000.0);
	}

	@Test
	void constructorInitializesEveryProperty() {
		Application application = newApplication();
		assertEquals("Type", application.getType());
		assertEquals(10, application.getRate());
		assertEquals(25.0, application.getUsagePercentage(), 0.0001);
		assertEquals(0.5, application.getLatency(), 0.0001);
		assertEquals(8_000_000L, application.getContainerSizeInBits());
		assertEquals(4_000_000L, application.getRequestSize());
		assertEquals(2_000_000L, application.getResultsSize());
		assertEquals(1000.0, application.getTaskLength(), 0.0001);
	}

	@Test
	void settersUpdateEveryProperty() {
		Application application = newApplication();
		application.setType("Other");
		application.setRate(3);
		application.setUsagePercentage(50.0);
		application.setLatency(12.5);
		application.setContainerSize(1_000_000L);
		application.setRequestSize(2_000_000L);
		application.setResultsSize(3_000_000L);
		application.setTaskLength(42.0);

		assertEquals("Other", application.getType());
		assertEquals(3, application.getRate());
		assertEquals(50.0, application.getUsagePercentage(), 0.0001);
		assertEquals(12.5, application.getLatency(), 0.0001);
		assertEquals(1_000_000L, application.getContainerSizeInBits());
		assertEquals(2_000_000L, application.getRequestSize());
		assertEquals(3_000_000L, application.getResultsSize());
		assertEquals(42.0, application.getTaskLength(), 0.0001);
	}

	@Test
	void containerSizeIsConvertedFromBitsToMegabytes() {
		Application application = newApplication();
		assertEquals(1.0, application.getContainerSizeInMBytes(), 0.0001);
		application.setContainerSize(4_000_000L);
		assertEquals(0.5, application.getContainerSizeInMBytes(), 0.0001);
		application.setContainerSize(0);
		assertEquals(0.0, application.getContainerSizeInMBytes(), 0.0001);
	}

	@Test
	void toStringContainsAllFields() {
		String text = newApplication().toString();
		assertNotNull(text);
		assertTrue(text.contains("type=Type"));
		assertTrue(text.contains("rate=constant(10.0)"));
		assertTrue(text.contains("latency=constant(0.5)"));
		assertTrue(text.contains("containerSize=constant(8000000.0)"));
		assertTrue(text.contains("requestSize=constant(4000000.0)"));
		assertTrue(text.contains("resultsSize=constant(2000000.0)"));
		assertTrue(text.contains("taskLength=constant(1000.0)"));
		assertTrue(text.contains("usagePercentage=25.0"));
		assertTrue(text.contains("arrival=null"));
		assertTrue(text.startsWith("Application ["));
		assertTrue(text.endsWith("]"));
	}

	@Test
	void plainParametersAreSampledWithoutVariation() {
		Application application = newApplication();
		assertEquals(4_000_000L, application.sampleRequestSize());
		assertEquals(2_000_000L, application.sampleResultsSize());
		assertEquals(8_000_000L, application.sampleContainerSize());
		assertEquals(1000L, application.sampleTaskLength());
		assertEquals(0.5, application.sampleLatency(), 0.0001);
		assertEquals(10, application.sampleTasksPerMinute());
		assertFalse(application.hasRandomArrival());
		assertNull(application.getArrivalDistribution());
	}

	@Test
	void distributedParametersAreSampledAroundTheirMean() {
		Application application = new Application("Type", 10, 25.0, 0.5, 8_000_000L, 1_500_000L, 2_000_000L, 1000.0,
				new UniformDistribution(1_000_000, 2_000_000), new ConstantDistribution(500),
				new ConstantDistribution(1_000_000), new UniformDistribution(500, 1500),
				new UniformDistribution(0.1, 0.9), new ConstantDistribution(7), new ExponentialDistribution(3));

		assertEquals(1_500_000L, application.getRequestSize());
		assertTrue(application.hasRandomArrival());
		assertNotNull(application.getArrivalDistribution());

		for (int i = 0; i < 100; i++) {
			long requestSize = application.sampleRequestSize();
			assertTrue(requestSize >= 1_000_000L && requestSize < 2_000_000L,
					"request size out of range: " + requestSize);
			long taskLength = application.sampleTaskLength();
			assertTrue(taskLength >= 500 && taskLength <= 1500, "task length out of range: " + taskLength);
			double latency = application.sampleLatency();
			assertTrue(latency >= 0.1 && latency < 0.9, "latency out of range: " + latency);
			assertEquals(500L, application.sampleResultsSize());
			assertEquals(1_000_000L, application.sampleContainerSize());
			assertEquals(7, application.sampleTasksPerMinute());
		}
	}

	@Test
	void settersReplaceTheDistributionOfTheirParameter() {
		Application application = new Application("Type", 10, 25.0, 0.5, 8_000_000L, 4_000_000L, 2_000_000L, 1000.0,
				new UniformDistribution(1_000_000, 2_000_000), null, null, null, null,
				new UniformDistribution(5, 15), null);
		assertInstanceOf(UniformDistribution.class, application.getRequestSizeDistribution());
		assertInstanceOf(UniformDistribution.class, application.getRateDistribution());

		application.setRequestSize(3_000_000L);
		application.setRate(4);

		assertEquals(3_000_000L, application.sampleRequestSize());
		assertEquals(4, application.sampleTasksPerMinute());
		assertInstanceOf(ConstantDistribution.class, application.getRequestSizeDistribution());
		assertInstanceOf(ConstantDistribution.class, application.getRateDistribution());
	}

	@Test
	void sampledValuesNeverReachZero() {
		Application application = new Application("Type", 0, 25.0, 0, 0L, 0L, 0L, 0.0,
				new ConstantDistribution(0), new ConstantDistribution(0), new ConstantDistribution(0),
				new ConstantDistribution(0), new ConstantDistribution(0), new ConstantDistribution(0),
				new ConstantDistribution(0));
		assertEquals(1L, application.sampleRequestSize());
		assertEquals(1L, application.sampleResultsSize());
		assertEquals(1L, application.sampleContainerSize());
		assertEquals(1L, application.sampleTaskLength());
		assertTrue(application.sampleLatency() > 0);
		assertEquals(0, application.sampleTasksPerMinute());
	}
}
