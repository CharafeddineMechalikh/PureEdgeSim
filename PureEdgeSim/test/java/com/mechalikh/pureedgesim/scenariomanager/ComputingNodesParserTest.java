package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

class ComputingNodesParserTest {

	private static class TestComputingNodesParser extends ComputingNodesParser {
		private final boolean typeSpecificResult;

		TestComputingNodesParser(String file, TYPES type, boolean typeSpecificResult) {
			super(file, type);
			this.typeSpecificResult = typeSpecificResult;
		}

		@Override
		protected boolean typeSpecificChecking(Document xmlDoc) {
			return typeSpecificResult;
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		TempSettingsFiles.resetDirectory();
	}

	@AfterEach
	void cleanUp() throws Exception {
		TempSettingsFiles.resetDirectory();
	}

	@Test
	void typeIsStoredAndParsingDelegatesToTypeSpecificChecking() throws Exception {
		Path file = TempSettingsFiles.write("nodes.xml", "<nodes><node/></nodes>");
		TestComputingNodesParser parser = new TestComputingNodesParser(file.toString(), TYPES.EDGE_DATACENTER, false);
		assertTrue(parser.type == TYPES.EDGE_DATACENTER);
		assertTrue(parser.parse());
	}

	@Test
	void returnsFalseWhenTheFileDoesNotExist() {
		assertFalse(new TestComputingNodesParser(TempSettingsFiles.DIR.resolve("missing.xml").toString(), TYPES.CLOUD,
				true).parse());
	}

	@Test
	void returnsFalseForMalformedXml() throws Exception {
		Path file = TempSettingsFiles.write("malformed.xml", "<nodes><node></nodes>");
		assertFalse(new TestComputingNodesParser(file.toString(), TYPES.CLOUD, true).parse());
	}

	@Test
	void returnsFalseWhenDoctypeIsDeclared() throws Exception {
		Path file = TempSettingsFiles.write("doctype.xml",
				"<?xml version=\"1.0\"?><!DOCTYPE nodes [<!ENTITY x \"y\">]><nodes/>");
		assertFalse(new TestComputingNodesParser(file.toString(), TYPES.CLOUD, true).parse());
	}
}
