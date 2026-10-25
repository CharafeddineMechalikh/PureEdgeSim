package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.distribution.Distribution;
import com.mechalikh.pureedgesim.distribution.ExponentialDistribution;
import com.mechalikh.pureedgesim.distribution.ScaledDistribution;
import com.mechalikh.pureedgesim.distribution.UniformDistribution;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Application;

/**
 * Reads the applications XML file and loads the application profiles into
 * {@link SimulationParameters#applicationList}.
 * <p>
 * Every numeric parameter of an application may be a fixed value or a probability
 * distribution, and the file may add an {@code arrival} element to describe how
 * the tasks of a device are spread in time. See {@link DistributionReader} for
 * the syntax.
 */
public class ApplicationFileParser extends XmlFileParser {

	/** Number of bits in one kilobyte, as used by the scenario files. */
	private static final double BITS_PER_KILOBYTE = 8000;

	/**
	 * Creates a parser for the given applications file.
	 *
	 * @param file path to the applications XML file.
	 */
	public ApplicationFileParser(String file) {
		super(file);
	}

	/**
	 * Parses the applications file.
	 *
	 * @return {@code true} if the file was read and validated successfully,
	 *         {@code false} otherwise.
	 */
	@Override
	public boolean parse() {
		return checkAppFile();
	}

	/**
	 * Validates every application entry and adds it to the application list. Each
	 * size is expressed in kilobytes in the XML file and converted to bits here.
	 *
	 * @return {@code true} on success, {@code false} if the file cannot be read or
	 *         an entry is invalid.
	 */
	protected boolean checkAppFile() {
		SimLog.println("%s - Checking applications file.",this.getClass().getSimpleName());
		SimulationParameters.applicationList = new ArrayList<>();
		Document doc;
		try (InputStream applicationFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(applicationFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttributePresent(appElement, "name");

				for (String element : List.of("type", "latency", "usage_percentage", "container_size", "request_size",
						"results_size", "task_length", "rate"))
					isElementPresent(appElement, element);

				String name = appElement.getAttribute("name");
				String condition = condition(name);

				// Latency-sensitivity in seconds.
				Distribution latency = positive(appElement, "latency", condition);

				// The size of the container, written in kilobytes and converted to bits.
				Distribution containerSize = inBits(positive(appElement, "container_size", condition));

				// Average request size (bits).
				Distribution requestSize = inBits(positive(appElement, "request_size", condition));

				// Average downloaded results size (bits).
				Distribution resultsSize = inBits(positive(appElement, "results_size", condition));

				// Average task length (MI).
				Distribution taskLength = positive(appElement, "task_length", condition);

				// The generation rate (tasks per minute).
				Distribution rate = positive(appElement, "rate", condition);

				// The percentage of devices using this type of applications.
				int usagePercentage = (int) assertDouble(appElement, "usage_percentage", value -> (value > 0),
						condition);

				// The type of application.
				String type = appElement.getElementsByTagName("type").item(0).getTextContent();

				// The optional arrival pattern: the time between two tasks, in seconds. Evenly
				// spaced tasks when the element is missing.
				Distribution arrival = readArrival(appElement, rate.mean());

				// Save applications parameters.
				SimulationParameters.applicationList.add(new Application(type, (int) rate.mean(), usagePercentage,
						latency.mean(), (long) containerSize.mean(), (long) requestSize.mean(),
						(long) resultsSize.mean(), taskLength.mean(), requestSize, resultsSize, containerSize,
						taskLength, latency, rate, arrival));
			}

		} catch (IllegalArgumentException e) {
			SimLog.println("%s", e.getMessage());
			return false;
		} catch (Exception e) {
			SimLog.println("%s - Failed to load the file \"%s\": %s", getClass().getSimpleName(), file,
					e.getMessage());
			return false;
		}

		SimLog.println("%s - Applications XML file successfully loaded!",this.getClass().getSimpleName());
		return true;
	}

	/**
	 * Reads an application parameter that must be strictly positive. A distribution
	 * able to generate negative values, like the normal one, is truncated at zero so
	 * that no task is created with an impossible size or delay.
	 *
	 * @param appElement the application element
	 * @param parameter  the tag name of the value
	 * @param condition  description of the expected condition
	 * @return the distribution of the value
	 */
	protected Distribution positive(Element appElement, String parameter, String condition) {
		return assertDistribution(appElement, parameter, 0, Double.POSITIVE_INFINITY, value -> (value > 0), condition);
	}

	/**
	 * Converts a distribution written in kilobytes into bits.
	 *
	 * @param kilobytes the distribution in kilobytes
	 * @return the same distribution, in bits
	 */
	protected Distribution inBits(Distribution kilobytes) {
		return new ScaledDistribution(kilobytes, BITS_PER_KILOBYTE);
	}

	/**
	 * Reads the optional {@code arrival} element of an application, which describes
	 * the time between two consecutive tasks, in seconds.
	 * <p>
	 * Without this element the tasks of a device are evenly spaced inside each
	 * minute, which is the behavior of the previous versions. The element accepts
	 * either a keyword, {@code poisson} (or {@code exponential}, the same thing) for
	 * the memoryless arrivals of a Poisson process, {@code uniform} for random gaps
	 * of the same average length, or {@code constant} for the evenly spaced tasks; or
	 * any distribution of the framework, in which case the mean of the distribution
	 * is the average gap:
	 * 
	 * <pre>
	 * &lt;arrival distribution="exponential" mean="3"/&gt;
	 * </pre>
	 *
	 * @param appElement the application element
	 * @param rate       the number of tasks generated each minute
	 * @return the distribution of the gap between two tasks, or {@code null} when the
	 *         tasks should be evenly spaced
	 */
	protected Distribution readArrival(Element appElement, double rate) {
		NodeList list = appElement.getElementsByTagName("arrival");
		if (list.getLength() == 0)
			return null;
		Element arrival = (Element) list.item(0);
		if (DistributionReader.isDistribution(arrival))
			return positive(appElement, "arrival",
					condition(appElement.getAttribute("name")) + ", or use \"constant\", \"poisson\", \"uniform\"");
		double meanGap = 60 / Math.max(1, rate);
		String value = arrival.getTextContent().trim().toLowerCase(Locale.ROOT);
		if (value.isEmpty() || value.equals("constant") || value.equals("even"))
			return null;
		if (value.equals("poisson") || value.equals("exponential"))
			return new ExponentialDistribution(meanGap);
		if (value.equals("uniform") || value.equals("random"))
			return new UniformDistribution(0, 2 * meanGap);
		throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the arrival pattern \"" + value
				+ "\" of the \"" + appElement.getAttribute("name") + "\" application is unknown, expected "
				+ "\"constant\", \"poisson\", \"uniform\", or a distribution.");
	}

	/**
	 * Builds the description of an invalid value, naming the application and the
	 * file.
	 *
	 * @param name the name of the application
	 * @return the description used in the error message
	 */
	protected String condition(String name) {
		return "> 0. Check the \"" + name + "\" application in the \"" + file + "\" file";
	}

}
