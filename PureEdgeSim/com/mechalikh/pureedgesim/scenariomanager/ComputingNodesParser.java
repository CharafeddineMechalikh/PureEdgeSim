package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;

public abstract class ComputingNodesParser extends XmlFileParser {
	protected TYPES type;

	public ComputingNodesParser(String file, TYPES type) {
		super(file);
		this.type = type;
	}

	@Override
	public boolean parse() {
		return checkComputingNodesFile();
	}

	private boolean checkComputingNodesFile() {
		SimLog.println(this.getClass().getSimpleName() + " - Checking file: " + file);
	    InputStream computingNodesFile;
		try { 
		    computingNodesFile = new FileInputStream(file);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document xmlDoc = dBuilder.parse(computingNodesFile);
			xmlDoc.getDocumentElement().normalize(); 
			typeSpecificChecking(xmlDoc);
			computingNodesFile.close();
		} catch (Exception e) {
			SimLog.println(getClass().getSimpleName() + " - Failed to load " + file + " file!");
			e.printStackTrace();
			return false;
		}
		SimLog.println(getClass().getSimpleName() + " - " + file + " file successfully Loaded!");
		return true;
	}

	protected abstract boolean typeSpecificChecking(Document xmlDoc);

}
