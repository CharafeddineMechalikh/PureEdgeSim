package com.mechalikh.pureedgesim.simulationengine;

/**
 * An element that can be ordered inside a {@link FutureQueue}. The serial
 * number defines the position among elements with the same sort key.
 */
public interface QueueElement {

	/**
	 * Returns the insertion serial.
	 *
	 * @return the serial number.
	 */
	long getSerial();

	/**
	 * Sets the insertion serial.
	 *
	 * @param l the serial number.
	 */
	void setSerial(long l);
}
