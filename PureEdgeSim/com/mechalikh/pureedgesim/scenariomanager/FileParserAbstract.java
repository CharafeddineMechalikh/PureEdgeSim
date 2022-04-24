package com.mechalikh.pureedgesim.scenariomanager;

public abstract class FileParserAbstract {
	protected String file;

	public FileParserAbstract(String file) {
		this.file = file;
	}

	public abstract boolean parse();
}
