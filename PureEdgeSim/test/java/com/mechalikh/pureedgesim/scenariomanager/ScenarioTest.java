package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;

class ScenarioTest extends AbstractTest {

	@Test
	void gettersAndSettersRoundTrip() {
		Scenario scenario = new Scenario(10, 0, 0);
		assertEquals(10, scenario.getDevicesCount());
		assertEquals(0, scenario.getOrchAlgorithm());
		assertEquals(0, scenario.getOrchArchitecture());

		scenario.setDevicesCount(42);
		scenario.setOrchAlgorithm(0);
		scenario.setOrchArchitecture(0);

		assertEquals(42, scenario.getDevicesCount());
		assertEquals(0, scenario.getOrchAlgorithm());
		assertEquals(0, scenario.getOrchArchitecture());
	}

	@Test
	void stringRepresentationsUseTrimmedParameterNames() {
		SimulationParameters.orchestrationAlgorithms = new String[] { " TRADE_OFF " };
		SimulationParameters.orchestrationArchitectures = new String[] { " ALL " };

		Scenario scenario = new Scenario(7, 0, 0);

		assertEquals("TRADE_OFF", scenario.getStringOrchAlgorithm());
		assertEquals("ALL", scenario.getStringOrchArchitecture());
		assertEquals("Orchestration algorithm= TRADE_OFF -  Architecture= ALL -  number of edge devices= 7",
				scenario.toString());
	}

	@Test
	void stringRepresentationSupportsSeveralAlgorithmsAndArchitectures() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF", "ROUND_ROBIN" };
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL", "EDGE_ONLY" };

		Scenario scenario = new Scenario(3, 1, 1);

		assertEquals("ROUND_ROBIN", scenario.getStringOrchAlgorithm());
		assertEquals("EDGE_ONLY", scenario.getStringOrchArchitecture());
		assertNotNull(scenario.toString());
	}

	@Test
	void invalidIndicesAreReportedByTheRuntime() {
		SimulationParameters.orchestrationAlgorithms = new String[] { "TRADE_OFF" };
		SimulationParameters.orchestrationArchitectures = new String[] { "ALL" };
		Scenario scenario = new Scenario(1, 5, 5);

		assertThrows(ArrayIndexOutOfBoundsException.class, scenario::getStringOrchAlgorithm);
		assertThrows(ArrayIndexOutOfBoundsException.class, scenario::getStringOrchArchitecture);
		assertThrows(ArrayIndexOutOfBoundsException.class, scenario::toString);
		assertEquals(1, scenario.getDevicesCount());
	}
}
