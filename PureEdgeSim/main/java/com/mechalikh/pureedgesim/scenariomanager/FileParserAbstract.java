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

	/**
	 * Finds the known name closest to the given one, to point out misspellings in
	 * error messages.
	 *
	 * @param name       the unknown name found in the file
	 * @param candidates the known names of the same kind
	 * @return the closest candidate when it differs from the name by at most two
	 *         edits, or {@code null} when no candidate looks like a misspelling of
	 *         the name
	 */
	protected static String suggest(String name, Iterable<String> candidates) {
		String closest = null;
		int closestDistance = Integer.MAX_VALUE;
		for (String candidate : candidates) {
			int distance = editDistance(name.toLowerCase(), candidate.toLowerCase());
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = candidate;
			}
		}
		return closestDistance <= 2 ? closest : null;
	}

	/**
	 * Computes the Levenshtein edit distance between two names.
	 *
	 * @param first  the first name
	 * @param second the second name
	 * @return the smallest number of insertions, deletions and replacements that
	 *         turn the first name into the second one
	 */
	private static int editDistance(String first, String second) {
		int[] previous = new int[second.length() + 1];
		int[] current = new int[second.length() + 1];
		for (int j = 0; j <= second.length(); j++)
			previous[j] = j;
		for (int i = 1; i <= first.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= second.length(); j++) {
				int substitution = previous[j - 1] + (first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1);
				current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), substitution);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[second.length()];
	}
}
