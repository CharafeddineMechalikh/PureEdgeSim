package examples;

import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * A computing node that can crash and come back, for the crash scenario of
 * Example 30.
 * <p>
 * As in Example 26, the death flag is set directly instead of through
 * {@code setDeath}, because that counter reports devices that ran out of energy.
 * A crash is a different event, and the run keeps the two apart.
 */
public class Example30Device extends DefaultComputingNode {

	private boolean crashed;

	/**
	 * Creates the node, with the signature the nodes generator calls by
	 * reflection.
	 *
	 * @param simulationManager the simulation manager
	 * @param mipsPerCore       the capacity of one core, in MIPS
	 * @param numberOfCPUCores  the number of cores
	 * @param storage           the storage, in megabytes
	 * @param ram               the memory, in megabytes
	 */
	public Example30Device(SimulationManager simulationManager, double mipsPerCore, int numberOfCPUCores,
			double storage, double ram) {
		super(simulationManager, mipsPerCore, numberOfCPUCores, storage, ram);
	}

	/** Crashes the node: it stops accepting and finishing work. */
	public void crash() {
		crashed = true;
		isDead = true;
	}

	/** Brings a crashed node back. A node that ran out of energy stays dead. */
	public void restore() {
		if (crashed) {
			crashed = false;
			isDead = false;
		}
	}

	/**
	 * Returns whether the node is down because of a crash, as opposed to an empty
	 * battery.
	 *
	 * @return {@code true} while the node is crashed
	 */
	public boolean isCrashed() {
		return crashed;
	}
}
