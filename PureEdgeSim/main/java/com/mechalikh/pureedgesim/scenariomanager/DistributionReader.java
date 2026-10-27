package com.mechalikh.pureedgesim.scenariomanager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.mechalikh.pureedgesim.distribution.ConstantDistribution;
import com.mechalikh.pureedgesim.distribution.Distribution;
import com.mechalikh.pureedgesim.distribution.DistributionFactory;
import com.mechalikh.pureedgesim.distribution.Randomness;

/**
 * Reads a numeric value that may be declared either as a plain number or as a
 * probability distribution, from any scenario XML file.
 * <p>
 * A plain element keeps the historical behavior:
 * 
 * <pre>
 * &lt;request_size&gt;25&lt;/request_size&gt;
 * </pre>
 * 
 * The same element may declare a distribution instead, in which case its text is
 * left empty and the parameters are given as attributes:
 * 
 * <pre>
 * &lt;request_size distribution="lognormal" mean="25" std="15"/&gt;
 * </pre>
 * 
 * Every numeric value that this class can read accepts an additional
 * {@code min} and {@code max} attribute to restrict the generated values,
 * whatever the distribution. See {@link DistributionFactory} for the attributes
 * expected by each distribution.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.7
 */
public final class DistributionReader {

	/** Attribute that carries the distribution name. */
	public static final String NAME_ATTRIBUTE = DistributionFactory.NAME_ATTRIBUTE;

	private DistributionReader() {
	}

	/**
	 * Returns whether an element declares a distribution rather than a plain value.
	 *
	 * @param element the element to check, may be {@code null}
	 * @return {@code true} when the element carries a {@code distribution} attribute
	 */
	public static boolean isDistribution(Element element) {
		return element != null && element.hasAttribute(NAME_ATTRIBUTE);
	}

	/**
	 * Returns the distribution declared by an element, or the constant it holds.
	 *
	 * @param element the element carrying the value
	 * @param context description of the value, used in error messages
	 * @return a distribution that returns the value of the element
	 * @throws IllegalArgumentException when the element holds neither a number nor a
	 *                                  valid distribution
	 */
	public static Distribution read(Element element, String context) {
		if (element == null)
			throw new IllegalArgumentException(context + " is missing");
		if (!isDistribution(element))
			return new ConstantDistribution(parseValue(element, context));
		try {
			return DistributionFactory.create(element.getAttribute(NAME_ATTRIBUTE), attributes(element),
					Randomness.getRandom());
		} catch (IllegalArgumentException e) {
			// Keeping the location of the value, since the factory only knows the name.
			throw new IllegalArgumentException(context + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Returns the value of an element: its content when it holds a number, or the
	 * mean of its distribution otherwise. Used to report a single number where a
	 * distribution was configured.
	 *
	 * @param element the element carrying the value
	 * @param context description of the value, used in error messages
	 * @return the value of the element
	 * @throws IllegalArgumentException when the element holds neither a number nor a
	 *                                  valid distribution
	 */
	public static double readMean(Element element, String context) {
		return read(element, context).mean();
	}

	/**
	 * Collects the attributes of an element, excluding the {@code distribution}
	 * attribute itself.
	 *
	 * @param element the element to read
	 * @return a map of attribute name to value, with lower-case keys
	 */
	public static Map<String, String> attributes(Element element) {
		Map<String, String> attributes = new HashMap<>();
		NamedNodeMap named = element.getAttributes();
		for (int i = 0; i < named.getLength(); i++) {
			Node attribute = named.item(i);
			if (!NAME_ATTRIBUTE.equals(attribute.getNodeName()))
				attributes.put(attribute.getNodeName().toLowerCase(Locale.ROOT), attribute.getNodeValue());
		}
		return attributes;
	}

	/**
	 * Parses the text content of an element as a number.
	 *
	 * @param element the element carrying the value
	 * @param context description of the value, used in error messages
	 * @return the parsed value
	 * @throws IllegalArgumentException when the content is empty or not a number
	 */
	private static double parseValue(Element element, String context) {
		String value = element.getTextContent().trim();
		if (value.isEmpty())
			throw new IllegalArgumentException(context
					+ " is empty: give a value, or a distribution with the \"" + NAME_ATTRIBUTE + "\" attribute");
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(context + " must be a number or a distribution, found \"" + value + "\"");
		}
	}

}
