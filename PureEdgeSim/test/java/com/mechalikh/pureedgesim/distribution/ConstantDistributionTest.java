package com.mechalikh.pureedgesim.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ConstantDistributionTest {

	@Test
	void alwaysReturnsTheSameValue() {
		ConstantDistribution distribution = new ConstantDistribution(3.75);
		for (int draw = 0; draw < 200; draw++)
			assertEquals(3.75, distribution.sample(), 0.0);
	}

	@Test
	void theIntegerDrawIsTheFloorOfTheConstant() {
		assertEquals(2, new ConstantDistribution(2.9).sampleInt());
		assertEquals(-3, new ConstantDistribution(-2.5).sampleInt());
		assertEquals(7, new ConstantDistribution(7.0).sampleInt());
	}

	@Test
	void meanAndConstantAgree() {
		assertEquals(3.75, new ConstantDistribution(3.75).mean(), 0.0);
	}

	@Test
	void theSupportCollapsesOntoTheValue() {
		ConstantDistribution distribution = new ConstantDistribution(4.0);
		assertEquals(4.0, distribution.getMin(), 0.0);
		assertEquals(4.0, distribution.getMax(), 0.0);
	}

	@Test
	void theNameIsConstantAndTheTypeIsContinuous() {
		ConstantDistribution distribution = new ConstantDistribution(1);
		assertEquals("constant", distribution.getName());
		assertFalse(distribution.isDiscrete());
	}

	@Test
	void toStringShowsTheValue() {
		assertEquals("constant(2.5)", new ConstantDistribution(2.5).toString());
		assertEquals("constant(7.0)", new ConstantDistribution(7).toString());
	}

	@Test
	void negativeAndZeroConstantsAreAccepted() {
		assertEquals(0.0, new ConstantDistribution(0).sample(), 0.0);
		assertEquals(-12.5, new ConstantDistribution(-12.5).mean(), 0.0);
	}
}
