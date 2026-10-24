package com.mechalikh.pureedgesim.scenariomanager;

/**
 * Base class for the configuration file parsers. A parser reads one file,
 * validates its content, and stores the result in {@link SimulationParameters}.
 */
public abstract class FileParserAbstract {
	/** Path of the file being parsed. */
	protected String file;

	/**
	 * Creates a parser for the given file.
	 *
	 * @param file path of the file to parse
	 */
	protected FileParserAbstract(String file) {
		this.file = file;
	}

	/**
	 * Reads and validates the configuration file.
	 *
	 * @return {@code true} if the file was parsed successfully, {@code false}
	 *         otherwise.
	 */
	public abstract boolean parse();
}
