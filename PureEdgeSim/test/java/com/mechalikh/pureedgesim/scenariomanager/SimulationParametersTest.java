package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

class SimulationParametersTest extends AbstractTest {

	@Test
	void privateConstructorCannotBeUsedDirectly() throws Exception {
		Constructor<SimulationParameters> constructor = SimulationParameters.class.getDeclaredConstructor();
		assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
		assertTrue(exception.getCause() instanceof IllegalStateException);
		assertEquals("SimulationParameters class cannot be instantiated", exception.getCause().getMessage());
	}

	@Test
	void typesEnumContainsAllComputingNodeKinds() {
		TYPES[] values = TYPES.values();
		assertEquals(4, values.length);
		assertEquals(TYPES.CLOUD, TYPES.valueOf("CLOUD"));
		assertEquals(TYPES.EDGE_DATACENTER, TYPES.valueOf("EDGE_DATACENTER"));
		assertEquals(TYPES.EDGE_DEVICE, TYPES.valueOf("EDGE_DEVICE"));
		assertEquals(TYPES.NULL, TYPES.valueOf("NULL"));
		assertNotNull(TYPES.NULL);
	}
}
