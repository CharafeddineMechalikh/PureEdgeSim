package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;

/**
 * Base parser for the XML files that describe computing nodes (cloud data
 * centers, edge data centers, and edge devices). Subclasses validate the
 * elements that are specific to their node type.
 */
public abstract class ComputingNodesParser extends XmlFileParser {
	/** The type of computing nodes described by the parsed file. */
	protected TYPES type;

	/**
	 * Creates a parser for a computing nodes file.
	 *
	 * @param file path to the XML file.
	 * @param type the type of the nodes contained in the file.
	 */
	protected ComputingNodesParser(String file, TYPES type) {
		super(file);
		this.type = type;
	}

	/**
	 * Parses and validates the file.
	 *
	 * @return {@code true} if the file is valid, {@code false} otherwise.
	 */
	@Override
	public boolean parse() {
		return checkComputingNodesFile();
	}

	/**
	 * Opens the XML file and delegates the type-specific validation.
	 *
	 * @return {@code true} if the file exists and passes validation, {@code false}
	 *         otherwise.
	 */
	protected boolean checkComputingNodesFile() {
		SimLog.println("%s - Checking file: %s ",this.getClass().getSimpleName(), file);
		try (InputStream computingNodesFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document xmlDoc = dBuilder.parse(computingNodesFile);
			xmlDoc.getDocumentElement().normalize();
			typeSpecificChecking(xmlDoc);
		} catch (IllegalArgumentException e) {
			SimLog.println("%s", e.getMessage());
			return false;
		} catch (Exception e) {
			SimLog.println("%s - Failed to load the file \"%s\": %s", getClass().getSimpleName(), file,
					e.getMessage());
			return false;
		}
		SimLog.println("%s - %s file successfully Loaded!",getClass().getSimpleName(),file);
		return true;
	}

	/**
	 * Validates the elements that depend on the node type.
	 *
	 * @param xmlDoc the parsed XML document.
	 * @return {@code true} if the document is valid, {@code false} otherwise.
	 */
	protected abstract boolean typeSpecificChecking(Document xmlDoc);

}
