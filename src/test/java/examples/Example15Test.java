package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.ParametersParser;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

class Example15Test extends AbstractTest {

	@Test
	void theExampleSettingsDelegateTheDecisionToTheEdgeTier() {
		assertTrue(new ParametersParser("examples/Example15/settings/simulation_parameters.properties").parse());
		assertEquals("ALL", SimulationParameters.orchestrationArchitectures[0]);
		assertEquals("Q_LEARNING", SimulationParameters.orchestrationAlgorithms[0]);
		assertTrue(SimulationParameters.enableOrchestrators);
		assertEquals("EDGE", SimulationParameters.deployOrchestrators);
		assertEquals(500, SimulationParameters.maxNumberOfEdgeDevices);
	}
}
