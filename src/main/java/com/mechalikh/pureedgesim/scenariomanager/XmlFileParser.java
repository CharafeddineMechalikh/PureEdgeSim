package com.mechalikh.pureedgesim.scenariomanager;

import java.util.function.Predicate;

import org.w3c.dom.Element;

/**
 * Base class for parsers of the XML configuration files. It provides helpers
 * that read and validate values from DOM elements and fail with a clear
 * message when a required value is missing or out of range.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public abstract class XmlFileParser extends FileParserAbstract {

	/**
	 * Creates a parser for the given XML file.
	 *
	 * @param file path to the XML file
	 */
	protected XmlFileParser(String file) {
		super(file); 
	}

	/**
	 * Reads a numeric element and checks it against a predicate.
	 *
	 * @param element   the parent element that contains the value
	 * @param parameter the tag name of the value
	 * @param p         the condition the value must satisfy
	 * @param message   description of the expected condition, used in the error
	 *                  message
	 * @return the parsed value
	 * @throws IllegalArgumentException if the value does not satisfy the condition
	 */
	protected double assertDouble(Element element, String parameter, Predicate<Double> p, String message) {
		double number = Double.parseDouble(element.getElementsByTagName(parameter).item(0).getTextContent());
		if (!p.test(number))
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - Error, the value of \"" + parameter + "\" must be " + message);
		return number;
	}
	

	/**
	 * Ensures that a child element exists and has a non-empty value.
	 *
	 * @param element the parent element
	 * @param key     the tag name of the child element
	 * @throws IllegalArgumentException if the element is missing or empty
	 */
	protected void isElementPresent(Element element, String key) {
		String value = element.getElementsByTagName(key).item(0).getTextContent();
		checkArgument("Element", key, element, value);
	}

	/**
	 * Ensures that an attribute exists and has a non-empty value.
	 *
	 * @param element the element that should contain the attribute
	 * @param key     the attribute name
	 * @throws IllegalArgumentException if the attribute is missing or empty
	 */
	protected void isAttribtuePresent(Element element, String key) {
		String value = element.getAttribute(key);
		checkArgument("Attribure", key, element, value);

	}

	/**
	 * Throws when a required value is null or empty.
	 *
	 * @param name    the kind of value, such as {@code Element} or {@code Attribute}
	 * @param key     the value name
	 * @param element the element being parsed
	 * @param value   the value to check
	 * @throws IllegalArgumentException if the value is null or empty
	 */
	protected void checkArgument(String name, String key, Element element, String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException(name + " " + key + "' is not found in '" + element.getNodeName() + "'");
		}
	}

}
