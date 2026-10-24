package com.mechalikh.pureedgesim.taskgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
		assertTrue(text.contains("rate=10"));
		assertTrue(text.contains("latency=0.5"));
		assertTrue(text.contains("containerSize=8000000"));
		assertTrue(text.contains("requestSize=4000000"));
		assertTrue(text.contains("resultsSize=2000000"));
		assertTrue(text.contains("taskLength=1000.0"));
		assertTrue(text.contains("usagePercentage=25.0"));
		assertTrue(text.startsWith("Application ["));
		assertTrue(text.endsWith("]"));
	}
}
