package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.distribution.ConstantDistribution;
import com.mechalikh.pureedgesim.distribution.Distribution;
import com.mechalikh.pureedgesim.distribution.ExponentialDistribution;
import com.mechalikh.pureedgesim.distribution.ScaledDistribution;
import com.mechalikh.pureedgesim.distribution.UniformDistribution;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.Subtask;

/**
 * Reads the applications XML file and loads the application profiles into
 * {@link SimulationParameters#applicationList}.
 * <p>
 * Every numeric parameter of an application may be a fixed value or a probability
 * distribution, and the file may add an {@code arrival} element to describe how
 * the tasks of a device are spread in time. See {@link DistributionReader} for
 * the syntax.
 * <p>
 * An application may also declare {@code subtask} elements to turn each request
 * into a service graph: the subtasks without dependencies are offloaded when the
 * request arrives, and the others once their dependencies have returned their
 * results. An application without subtasks keeps generating one independent
 * task per request.
 */
public class ApplicationFileParser extends XmlFileParser {

	/** Number of bits in one kilobyte, as used by the scenario files. */
	private static final double BITS_PER_KILOBYTE = 8000;

	/** Attributes accepted on a {@code subtask} element. */
	private static final Set<String> SUBTASK_ATTRIBUTES = new LinkedHashSet<>(List.of("name", "depends_on",
			"task_length", "request_size", "results_size", "container_size", "latency", "optional"));

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
				Application application = new Application(type, (int) rate.mean(), usagePercentage, latency.mean(),
						(long) containerSize.mean(), (long) requestSize.mean(), (long) resultsSize.mean(),
						taskLength.mean(), requestSize, resultsSize, containerSize, taskLength, latency, rate, arrival);
				application.setSubtasks(readSubtasks(appElement, application));
				SimulationParameters.applicationList.add(application);
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
	 * Reads the optional {@code subtask} elements of an application, which describe
	 * the service graph generated for every request.
	 * <p>
	 * A subtask is an empty element whose parameters are attributes. All of them
	 * are optional except {@code name}: a missing numeric attribute inherits the
	 * distribution of the application, while a value replaces it with a constant.
	 * Sizes are expressed in kilobytes, like the application parameters.
	 * 
	 * <pre>
	 * &lt;subtask name="detect"/&gt;
	 * &lt;subtask name="classify" depends_on="detect" task_length="4000"/&gt;
	 * &lt;subtask name="alert" depends_on="classify" request_size="2" results_size="1"/&gt;
	 * </pre>
	 *
	 * @param appElement  the application element
	 * @param application the application being built, used for the inherited values
	 * @return the subtasks in declaration order, empty when the application is a
	 *         single independent task
	 */
	protected List<Subtask> readSubtasks(Element appElement, Application application) {
		NodeList list = appElement.getElementsByTagName("subtask");
		if (list.getLength() == 0)
			return List.of();

		String appName = appElement.getAttribute("name");
		List<Subtask> subtasks = new ArrayList<>();
		Map<String, Subtask> byName = new HashMap<>();
		for (int i = 0; i < list.getLength(); i++) {
			Element element = (Element) list.item(i);
			String name = element.getAttribute("name").trim();
			if (name.isEmpty())
				throw subtaskError(appName, "subtask " + (i + 1), "has no \"name\" attribute.");
			if (byName.containsKey(name))
				throw subtaskError(appName, name, "is declared more than once.");
			checkSubtaskAttributes(element, appName, name);

			List<String> dependencies = readDependencies(element, appName, name);
			boolean optional = readOptional(element, appName, name);
			if (optional && dependencies.isEmpty())
				throw subtaskError(appName, name, "is optional but has no dependency; an optional subtask is "
						+ "skipped only when one of its dependencies fails, so it needs at least one.");

			Subtask subtask = new Subtask(name, dependencies,
					subtaskOverride(element, "task_length", application.getTaskLengthDistribution(), 1, appName, name),
					subtaskOverride(element, "request_size", application.getRequestSizeDistribution(), BITS_PER_KILOBYTE,
							appName, name),
					subtaskOverride(element, "results_size", application.getResultsSizeDistribution(), BITS_PER_KILOBYTE,
							appName, name),
					subtaskOverride(element, "container_size", application.getContainerSizeDistribution(),
							BITS_PER_KILOBYTE, appName, name),
					subtaskOverride(element, "latency", application.getLatencyDistribution(), 1, appName, name),
					optional);
			byName.put(name, subtask);
			subtasks.add(subtask);
		}

		checkGraph(subtasks, byName, appName);
		return subtasks;
	}

	/**
	 * Reads the dependencies of a subtask from its {@code depends_on} attribute, a
	 * comma separated list of subtask names.
	 *
	 * @param element the subtask element
	 * @param appName the name of the application, used in error messages
	 * @param name    the name of the subtask, used in error messages
	 * @return the dependency names, empty for a root subtask
	 */
	protected List<String> readDependencies(Element element, String appName, String name) {
		String raw = element.getAttribute("depends_on").trim();
		if (raw.isEmpty())
			return List.of();
		Set<String> dependencies = new LinkedHashSet<>();
		for (String part : raw.split(",", -1)) {
			String dependency = part.trim();
			if (dependency.isEmpty())
				throw subtaskError(appName, name, "has an empty name in its \"depends_on\" attribute.");
			if (dependency.equals(name))
				throw subtaskError(appName, name, "cannot depend on itself.");
			dependencies.add(dependency);
		}
		return new ArrayList<>(dependencies);
	}

	/**
	 * Reads the optional {@code optional} attribute of a subtask.
	 * <p>
	 * An optional subtask is skipped, rather than failed, when one of its
	 * dependencies fails, and the request continues with the results of the other
	 * branches. The attribute is absent by default, which makes the subtask
	 * required.
	 *
	 * @param element the subtask element
	 * @param appName the name of the application, used in error messages
	 * @param name    the name of the subtask, used in error messages
	 * @return {@code true} when the subtask is optional
	 */
	protected boolean readOptional(Element element, String appName, String name) {
		String raw = element.getAttribute("optional").trim();
		if (raw.isEmpty())
			return false;
		if (raw.equalsIgnoreCase("true"))
			return true;
		if (raw.equalsIgnoreCase("false"))
			return false;
		throw subtaskError(appName, name,
				"has an \"optional\" attribute of \"" + raw + "\", expected \"true\" or \"false\".");
	}

	/**
	 * Rejects an unknown attribute of a subtask, suggesting the closest valid one.
	 *
	 * @param element the subtask element
	 * @param appName the name of the application, used in error messages
	 * @param name    the name of the subtask, used in error messages
	 */
	protected void checkSubtaskAttributes(Element element, String appName, String name) {
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			String attribute = attributes.item(i).getNodeName();
			if (SUBTASK_ATTRIBUTES.contains(attribute))
				continue;
			String suggestion = suggest(attribute, SUBTASK_ATTRIBUTES);
			throw subtaskError(appName, name,
					"has an unknown attribute \"" + attribute + "\"" + (suggestion == null ? "" : " (did you mean \""
							+ suggestion + "\"?)") + ". Expected " + String.join(", ", SUBTASK_ATTRIBUTES) + ".");
		}
	}

	/**
	 * Reads one optional numeric override of a subtask.
	 * <p>
	 * Without the attribute the subtask inherits the distribution of the
	 * application, so its values keep varying from request to request.
	 *
	 * @param element   the subtask element
	 * @param attribute the name of the attribute
	 * @param inherited the distribution of the application, used when absent
	 * @param scale     multiplier applied to the value, {@code 1} or
	 *                  {@link #BITS_PER_KILOBYTE} for sizes
	 * @param appName   the name of the application, used in error messages
	 * @param name      the name of the subtask, used in error messages
	 * @return the inherited distribution, or a constant with the declared value
	 */
	protected Distribution subtaskOverride(Element element, String attribute, Distribution inherited, double scale,
			String appName, String name) {
		String raw = element.getAttribute(attribute).trim();
		if (raw.isEmpty())
			return inherited;
		double value;
		try {
			value = Double.parseDouble(raw);
		} catch (NumberFormatException e) {
			throw subtaskError(appName, name,
					"has a non-numeric \"" + attribute + "\" attribute \"" + raw + "\".");
		}
		if (!(value > 0) || Double.isInfinite(value))
			throw subtaskError(appName, name,
					"has a \"" + attribute + "\" attribute of \"" + raw + "\", which must be greater than zero.");
		return new ConstantDistribution(value * scale);
	}

	/**
	 * Checks that every dependency names a declared subtask and that the graph has
	 * no cycle, so that at least one subtask can start when a request arrives.
	 *
	 * @param subtasks the declared subtasks
	 * @param byName   the same subtasks, indexed by name
	 * @param appName  the name of the application, used in error messages
	 */
	protected void checkGraph(List<Subtask> subtasks, Map<String, Subtask> byName, String appName) {
		Map<String, Integer> remaining = new HashMap<>();
		Map<String, List<String>> dependents = new HashMap<>();
		Deque<String> ready = new ArrayDeque<>();
		for (Subtask subtask : subtasks) {
			for (String dependency : subtask.getDependencies())
				if (!byName.containsKey(dependency))
					throw subtaskError(appName, subtask.getName(), "depends on the unknown subtask \"" + dependency
							+ "\". Declared subtasks are " + String.join(", ", byName.keySet()) + ".");
			remaining.put(subtask.getName(), subtask.getDependencies().size());
			if (subtask.isRoot())
				ready.add(subtask.getName());
			for (String dependency : subtask.getDependencies())
				dependents.computeIfAbsent(dependency, key -> new ArrayList<>()).add(subtask.getName());
		}

		int ordered = 0;
		while (!ready.isEmpty()) {
			String name = ready.poll();
			ordered++;
			for (String dependent : dependents.getOrDefault(name, List.of()))
				if (remaining.merge(dependent, -1, Integer::sum) == 0)
					ready.add(dependent);
		}
		if (ordered != subtasks.size()) {
			List<String> cycle = new ArrayList<>();
			for (Map.Entry<String, Integer> entry : remaining.entrySet())
				if (entry.getValue() > 0)
					cycle.add(entry.getKey());
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Error, the subtasks of the \"" + appName
					+ "\" application contain a dependency cycle involving " + String.join(", ", cycle)
					+ " in the \"" + file + "\" file.");
		}
	}

	/**
	 * Builds the description of an invalid subtask, naming the application, the
	 * subtask, and the file.
	 *
	 * @param appName the name of the application
	 * @param name    the name of the subtask
	 * @param reason  what is wrong with this subtask
	 * @return the exception describing the problem
	 */
	protected IllegalArgumentException subtaskError(String appName, String name, String reason) {
		return new IllegalArgumentException(getClass().getSimpleName() + " - Error, the \"" + name
				+ "\" subtask of the \"" + appName + "\" application in the \"" + file + "\" file " + reason);
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
