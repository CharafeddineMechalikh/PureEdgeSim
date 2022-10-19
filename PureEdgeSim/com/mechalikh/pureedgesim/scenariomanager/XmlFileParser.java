package com.mechalikh.pureedgesim.scenariomanager;

import java.util.function.Predicate;

import org.w3c.dom.Element;

public abstract class XmlFileParser extends FileParserAbstract {

	protected XmlFileParser(String file) {
		super(file); 
	}

	protected double assertDouble(Element element, String parameter, Predicate<Double> p, String message) {
		double number = Double.parseDouble(element.getElementsByTagName(parameter).item(0).getTextContent());
		if (!p.test(number))
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - Error, the value of \"" + parameter + "\" must be " + message);
		return number;
	}
	

	protected void isElementPresent(Element element, String key) {
		String value = element.getElementsByTagName(key).item(0).getTextContent();
		checkArgument("Element", key, element, value);
	}

	protected void isAttribtuePresent(Element element, String key) {
		String value = element.getAttribute(key);
		checkArgument("Attribure", key, element, value);

	}

	protected void checkArgument(String name, String key, Element element, String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException(name + " " + key + "' is not found in '" + element.getNodeName() + "'");
		}
	}

}
