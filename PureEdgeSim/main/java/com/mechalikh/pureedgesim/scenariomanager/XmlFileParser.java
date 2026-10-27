package com.mechalikh.pureedgesim.scenariomanager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.distribution.BoundedDistribution;
import com.mechalikh.pureedgesim.distribution.Distribution;

/**
 * Base class for parsers of the XML configuration files. It provides helpers
 * that read and validate values from DOM elements and fail with a clear
 * message when a required value is missing or out of range.
 * <p>
 * Numeric values may be declared either as a plain number or as a probability
 * distribution, as described by {@link DistributionReader}. When a distribution
 * is used, its mean is checked against the condition of the parameter.
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
	 * @return the parsed value, or the mean of the configured distribution
	 * @throws IllegalArgumentException if the value does not satisfy the condition
	 */
	protected double assertDouble(Element element, String parameter, Predicate<Double> p, String message) {
		Distribution distribution = getDistribution(element, parameter);
		double mean = distribution.mean();
		if (!Double.isNaN(mean) && !p.test(mean))
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - Error, the value of \"" + parameter + "\" must be " + message);
		return mean;
	}

	/**
	 * Reads a value that must stay inside a range, such as a size or a delay that
	 * can never be negative.
	 * <p>
	 * The mean is checked the same way as with {@link #assertDouble}, then a
	 * distribution able to generate values outside the range is bounded, so that a
	 * scenario using, for example, a normal distribution for a message size never
	 * produces a negative one.
	 *
	 * @param element   the parent element that contains the value
	 * @param parameter the tag name of the value
	 * @param min       the smallest accepted value
	 * @param max       the largest accepted value
	 * @param p         the condition the mean must satisfy
	 * @param message   description of the expected condition, used in the error
	 *                  message
	 * @return the distribution of the value, bounded when necessary
	 * @throws IllegalArgumentException if the mean does not satisfy the condition
	 */
	protected Distribution assertDistribution(Element element, String parameter, double min, double max,
			Predicate<Double> p, String message) {
		Distribution distribution = getDistribution(element, parameter);
		double mean = distribution.mean();
		if (!Double.isNaN(mean) && !p.test(mean))
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - Error, the value of \"" + parameter + "\" must be " + message);
		if (distribution.getMin() >= min && distribution.getMax() <= max)
			return distribution;
		return new BoundedDistribution(distribution, min, max);
	}

	/**
	 * Reads the distribution declared by a numeric child element. When the element
	 * only holds a number, a constant distribution is returned, so that the caller
	 * can sample it the same way in both cases.
	 *
	 * @param element   the parent element that contains the value
	 * @param parameter the tag name of the value
	 * @return the distribution of the value
	 * @throws IllegalArgumentException if the element is missing, empty, or declares
	 *                                  an invalid distribution
	 */
	protected Distribution getDistribution(Element element, String parameter) {
		Element valueElement = (Element) element.getElementsByTagName(parameter).item(0);
		return DistributionReader.read(valueElement, describe(element, parameter));
	}

	/**
	 * Reads the mean of a numeric child element without checking it. Used when the
	 * value was already validated, or when it only serves to build an error message.
	 *
	 * @param element   the parent element that contains the value
	 * @param parameter the tag name of the value
	 * @return the value, or the mean of the configured distribution
	 * @throws IllegalArgumentException if the element is missing, empty, or declares
	 *                                  an invalid distribution
	 */
	protected double getMean(Element element, String parameter) {
		return getDistribution(element, parameter).mean();
	}

	/**
	 * Builds the description of a value used inside error messages.
	 *
	 * @param element   the parent element
	 * @param parameter the tag name of the value
	 * @return a description naming the value, its parent and the file
	 */
	protected String describe(Element element, String parameter) {
		return "\"" + parameter + "\" of \"" + element.getNodeName() + "\" in the \"" + file + "\" file";
	}

	/**
	 * Ensures that a child element exists and has a non-empty value.
	 *
	 * @param element the parent element
	 * @param key     the tag name of the child element
	 * @throws IllegalArgumentException if the element is missing or empty
	 */
	protected void isElementPresent(Element element, String key) {
		Element valueElement = (Element) element.getElementsByTagName(key).item(0);
		if (valueElement == null) {
			checkArgument("Element", key, element, null);
			return;
		}
		String value = valueElement.getTextContent();
		if (DistributionReader.isDistribution(valueElement))
			return;
		checkArgument("Element", key, element, value);
	}

	/**
	 * Ensures that an attribute exists and has a non-empty value.
	 *
	 * @param element the element that should contain the attribute
	 * @param key     the attribute name
	 * @throws IllegalArgumentException if the attribute is missing or empty
	 */
	protected void isAttributePresent(Element element, String key) {
		String value = element.getAttribute(key);
		checkArgument("Attribute", key, element, value);
	}

	/**
	 * Throws when a required value is null or empty. When the file holds a name of
	 * the same kind that differs from the expected one by a small edit, the error
	 * message points to it as a likely misspelling.
	 *
	 * @param name    the kind of value, such as {@code Element} or {@code Attribute}
	 * @param key     the value name
	 * @param element the element being parsed
	 * @param value   the value to check
	 * @throws IllegalArgumentException if the value is null or empty
	 */
	protected void checkArgument(String name, String key, Element element, String value) {
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the " + name.toLowerCase() + " \""
					+ key + "\" is not found in \"" + element.getNodeName() + "\" in the \"" + file + "\" file"
					+ misspellingHint(name, key, element) + ".");
		}
	}

	/**
	 * Builds the misspelling hint appended to the error of a missing element or
	 * attribute.
	 *
	 * @param name    the kind of value, such as {@code Element} or {@code Attribute}
	 * @param key     the value name that was not found
	 * @param element the element being parsed
	 * @return an empty string, or a hint naming the closest known name
	 */
	private String misspellingHint(String name, String key, Element element) {
		List<String> known = new ArrayList<>();
		if ("Element".equals(name)) {
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
				if (children.item(i).getNodeType() == Node.ELEMENT_NODE)
					known.add(children.item(i).getNodeName());
		} else {
			for (int i = 0; i < element.getAttributes().getLength(); i++)
				known.add(element.getAttributes().item(i).getNodeName());
		}
		String suggestion = suggest(key, known);
		return suggestion == null ? "" : " (did you mean \"" + suggestion + "\"?)";
	}

}
