/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Fault tolerant, robust, adaptive workload orchestration on a pure edge fleet.
 * <p>
 * Three real-time classes share a fleet of battery powered devices with no
 * server to fall back on, and two of those devices carry the orchestration.
 * Halfway through the run the primary orchestrator crashes: the requests in
 * flight with it are lost until a cold standby boots and takes the fleet over.
 * Between those two moments the remaining mechanisms keep the traffic alive.
 * {@link Example26FaultTolerantOrchestrator} learns which way of ranking the
 * fleet returns the most tasks for each class,
 * {@link Example26OrchestratedDevice} puts hard real-time tasks at the head of
 * the queue of their destination and pushes elastic work away from a device that
 * is almost out of battery.
 * <p>
 * The scenario is the one of Safavifar, Mechalikh and Golpayegani, "Fault
 * tolerant robust adaptive workload orchestration in pure edge computing",
 * ICAART 2023 (published in the Springer Lecture Notes in Computer Science
 * series, pages 370-386). The three application classes, their shares of the
 * fleet, their latency budgets, container sizes and task lengths, the fleet of
 * one hundred devices, the two orchestrating devices, the crash in the middle of
 * the run and the class weighted penalties of the learning reward all come from
 * that paper. Four things are scaled down so that a run of five minutes fits on
 * one machine and reads on one screen: the arrival rate is a quarter of the
 * paper's, because the generator spaces the arrivals of an application by a whole
 * number of seconds; each device has the single core of the settings files
 * rather than the eight cores of the paper's hardware; the run lasts five
 * simulated minutes instead of thirty; and the wireless link is given one
 * millisecond per hop instead of the five of the framework default, because an
 * offloaded request crosses four hops and would otherwise spend the whole budget
 * of the health monitor before reaching any device. The placement policy is a
 * tabular Q-learning agent over a small set of ranking rules, not the fuzzy
 * decision tree the paper grows out of its Q-table, because that tree needs a
 * fuzzy-logic library and only pays off on a much larger state space.
 * <p>
 * Each mechanism can be switched off, so the run compares with itself:
 * 
 * <pre>
 * mvn exec:java -Dexec.mainClass="examples.Example26"
 * mvn exec:java -Dexec.mainClass="examples.Example26" -Dexample26.fault=false
 * mvn exec:java -Dexec.mainClass="examples.Example26" -Dexample26.backup=false
 * mvn exec:java -Dexec.mainClass="examples.Example26" -Dexample26.priority=false
 * mvn exec:java -Dexec.mainClass="examples.Example26" -Dexample26.reallocation=false
 * </pre>
 * 
 * The last four lines are the ablation of the paper: a fault that is never
 * injected, a crash that nobody recovers from, a fleet that treats every class
 * the same, and devices that keep work they can no longer afford. The summary
 * printed at the end of each run gives the success rate and the mean end to end
 * delay of every class, the number of hard real-time tasks that jumped a queue,
 * the number of tasks that were pushed away from a device that could no longer
 * carry them, and the number of requests the crash cost.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see Example26FaultTolerantOrchestrator
 * @see Example26OrchestratedDevice
 * @see Example26FaultTolerantSimulationManager
 */
public class Example26 {

	private static final String SETTINGS = "PureEdgeSim/examples/Example26/settings/";
	private static final String OUTPUT = "PureEdgeSim/examples/Example26/output/";

	/** Whether the primary orchestrator crashes in the middle of the run. */
	public static final boolean FAULT = enabled("example26.fault");
	/** Whether a cold standby takes the fleet over after the crash. */
	public static final boolean BACKUP = enabled("example26.backup");
	/** Whether hard real-time tasks jump the queue of their destination. */
	public static final boolean PRIORITY = enabled("example26.priority");
	/** Whether a device may push work away when it can no longer afford it. */
	public static final boolean REALLOCATION = enabled("example26.reallocation");

	/**
	 * Reads one of the ablation switches. Every mechanism is on unless the switch
	 * is present on the command line and set to false.
	 *
	 * @param key the name of the system property
	 * @return whether the mechanism is enabled
	 */
	private static boolean enabled(String key) {
		return !"false".equals(System.getProperty(key));
	}

	/**
	 * Runs the scenario with the learning orchestrator, the devices that protect
	 * their real-time traffic, and the manager that crashes the primary
	 * orchestrator and brings the standby up.
	 */
	public Example26() {
		Simulation sim = new Simulation();

		sim.setCustomEdgeOrchestrator(Example26FaultTolerantOrchestrator.class);
		sim.setCustomComputingNode(Example26OrchestratedDevice.class);
		sim.setCustomSimulationManager(Example26FaultTolerantSimulationManager.class);

		sim.setCustomSettingsFolder(SETTINGS);
		sim.setCustomOutputFolder(OUTPUT);

		sim.launchSimulation();
	}

	/**
	 * Starts the example.
	 *
	 * @param args the ablation switches, as {@code -Dexample26.backup=false}
	 *             system properties; pass them before the main class, on the
	 *             JVM arguments, not after it
	 */
	public static void main(String[] args) {
		new Example26();
	}
}
