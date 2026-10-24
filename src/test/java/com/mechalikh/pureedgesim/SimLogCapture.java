package com.mechalikh.pureedgesim;

import java.io.OutputStream;
import java.io.PrintStream;

/** Silences console output produced by the simulation while tests run. */
public final class SimLogCapture {

	private static final PrintStream ORIGINAL_OUT = System.out;
	private static final PrintStream ORIGINAL_ERR = System.err;
	private static final PrintStream NULL_STREAM = new PrintStream(OutputStream.nullOutputStream());

	private SimLogCapture() {
	}

	public static void mute() {
		System.setOut(NULL_STREAM);
		System.setErr(NULL_STREAM);
	}

	public static void unmute() {
		System.setOut(ORIGINAL_OUT);
		System.setErr(ORIGINAL_ERR);
	}
}
