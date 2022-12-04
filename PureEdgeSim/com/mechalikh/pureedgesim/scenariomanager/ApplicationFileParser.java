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

	protected boolean checkAppFile() {
		String condition = "> 0. Check the \"";
		String application = "\" application in \"";
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
				isAttribtuePresent(appElement, "name");

				for (String element : List.of("type", "latency", "usage_percentage", "container_size", "request_size",
						"results_size", "task_length", "rate"))
					isElementPresent(appElement, element);

				// Latency-sensitivity in seconds.
				double latency = assertDouble(appElement, "latency", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file");

				// The size of the container (bits).
				long containerSize = (long) (8000 * assertDouble(appElement, "container_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average request size (bits).
				long requestSize = (long) (8000 * assertDouble(appElement, "request_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average downloaded results size (bits).
				long resultsSize = (long) (8000 * assertDouble(appElement, "results_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average task length (MI).
				double taskLength = assertDouble(appElement, "task_length", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The generation rate (tasks per minute)
				int rate = (int) assertDouble(appElement, "rate", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The percentage of devices using this type of applications.
				int usagePercentage = (int) assertDouble(appElement, "usage_percentage", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The type of application.
				String type = appElement.getElementsByTagName("type").item(0).getTextContent();

				// Save applications parameters.
				SimulationParameters.applicationList.add(new Application(type, rate, usagePercentage, latency,
						containerSize, requestSize, resultsSize, taskLength));
			}

		} catch (Exception e) {
			SimLog.println("%s - Applications XML file cannot be parsed!",this.getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}

		SimLog.println("%s - Applications XML file successfully loaded!",this.getClass().getSimpleName());
		return true;
	}

}
