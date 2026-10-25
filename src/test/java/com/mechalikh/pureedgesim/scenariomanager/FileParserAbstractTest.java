package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class FileParserAbstractTest {

	private static class DummyParser extends FileParserAbstract {
		DummyParser(String file) {
			super(file);
		}

		@Override
		public boolean parse() {
			return true;
		}
	}

	private static class DummyXmlParser extends XmlFileParser {
		DummyXmlParser(String file) {
			super(file);
		}

		@Override
		public boolean parse() {
			return false;
		}
	}

	private Element element() throws Exception {
		String xml = "<root attr=\"attributeValue\">" + "<present>content</present>" + "<empty></empty>"
				+ "<value>5</value>" + "<zero>0</zero>" + "</root>";
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		return document.getDocumentElement();
	}

	@Test
	void fileParserStoresTheFileAndDelegatesParse() {
		DummyParser parser = new DummyParser("some/file.xml");
		assertEquals("some/file.xml", parser.file);
		assertTrue(parser.parse());
	}

	@Test
	void xmlParserParseIsImplementedBySubclasses() {
		assertFalse(new DummyXmlParser("anything").parse());
	}

	@Test
	void assertDoubleReturnsValidValues() throws Exception {
		Element element = element();
		Predicate<Double> positive = value -> value > 0;
		assertEquals(5.0, new DummyXmlParser("f").assertDouble(element, "value", positive, "> 0"), 0.0001);
	}

	@Test
	void assertDoubleRejectsInvalidValues() throws Exception {
		Element element = element();
		DummyXmlParser parser = new DummyXmlParser("f");
		Predicate<Double> positive = value -> value > 0;
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> parser.assertDouble(element, "zero", positive, "> 0"));
		assertTrue(exception.getMessage().contains("zero"));
		assertTrue(exception.getMessage().contains("> 0"));
	}

	@Test
	void assertDoubleFailsWhenTheElementIsMissing() throws Exception {
		Element element = element();
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new DummyXmlParser("f").assertDouble(element, "missing", value -> true, "anything"));
		assertTrue(exception.getMessage().contains("\"missing\""));
		assertTrue(exception.getMessage().contains("is missing"));
	}

	@Test
	void elementPresenceAcceptsExistingElements() throws Exception {
		Element element = element();
		assertDoesNotThrow(() -> new DummyXmlParser("f").isElementPresent(element, "present"));
	}

	@Test
	void elementPresenceRejectsMissingAndEmptyElements() throws Exception {
		Element element = element();
		DummyXmlParser parser = new DummyXmlParser("f");
		IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
				() -> parser.isElementPresent(element, "missing"));
		assertTrue(missing.getMessage().contains("\"missing\""), missing.getMessage());
		assertTrue(missing.getMessage().contains("not found"), missing.getMessage());
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> parser.isElementPresent(element, "empty"));
		assertTrue(exception.getMessage().contains("\"empty\""), exception.getMessage());
	}

	@Test
	void missingElementsCloseToAKnownNameGetASuggestion() throws Exception {
		Element element = element();
		DummyXmlParser parser = new DummyXmlParser("f");
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> parser.isElementPresent(element, "presentt"));
		assertTrue(exception.getMessage().contains("did you mean \"present\""), exception.getMessage());
	}

	@Test
	void attributePresenceAcceptsExistingAttributes() throws Exception {
		Element element = element();
		assertDoesNotThrow(() -> new DummyXmlParser("f").isAttributePresent(element, "attr"));
	}

	@Test
	void attributePresenceRejectsMissingAttributes() throws Exception {
		Element element = element();
		DummyXmlParser parser = new DummyXmlParser("f");
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> parser.isAttributePresent(element, "unknown"));
		assertTrue(exception.getMessage().contains("attribute \"unknown\""), exception.getMessage());
	}

	@Test
	void checkArgumentRejectsNullAndEmptyValues() throws Exception {
		Element element = element();
		DummyXmlParser parser = new DummyXmlParser("f");
		assertThrows(IllegalArgumentException.class, () -> parser.checkArgument("Element", "x", element, null));
		assertThrows(IllegalArgumentException.class, () -> parser.checkArgument("Element", "x", element, ""));
		assertDoesNotThrow(() -> parser.checkArgument("Element", "x", element, "value"));
	}
}
