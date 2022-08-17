package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Application;

public class ApplicationFileParser extends XmlFileParser {

	public ApplicationFileParser(String file) {
		super(file);
	}

	@Override
	public boolean parse() {
		return checkAppFile();
	}

	private boolean checkAppFile() {
		String condition = "> 0. Check the \"";
		String application = "\" application in \"";
		SimLog.println(this.getClass().getSimpleName() + " - Checking applications file.");
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
				isAttribtuePresent(appElement, "name");

				for (String element : List.of("latency", "usage_percentage", "container_size", "request_size",
						"results_size", "task_length", "rate"))
					isElementPresent(appElement, element);

				// Latency-sensitivity in seconds.
				double latency = assertDouble(appElement, "latency", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file");

				// The size of the container (bits).
				long container_size = (long) (8000 * assertDouble(appElement, "container_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average request size (bits).
				long request_size = (long) (8000 * assertDouble(appElement, "request_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average downloaded results size (bits).
				long results_size = (long) (8000 * assertDouble(appElement, "results_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average task length (MI).
				double task_length = assertDouble(appElement, "task_length", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The generation rate (tasks per minute)
				int rate = (int) assertDouble(appElement, "rate", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The percentage of devices using this type of applications.
				int usage_percentage = (int) assertDouble(appElement, "usage_percentage", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// Save applications parameters.
				SimulationParameters.applicationList.add(new Application(rate, usage_percentage, latency,
						container_size, request_size, results_size, task_length));
			}

		} catch (Exception e) {
			SimLog.println(this.getClass().getSimpleName() + " - Applications XML file cannot be parsed!");
			e.printStackTrace();
			return false;
		}

		SimLog.println(this.getClass().getSimpleName() + " - Applications XML file successfully loaded!");
		return true;
	}

}
