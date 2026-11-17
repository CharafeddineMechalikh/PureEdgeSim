import com.mechalikh.pureedgesim.simulationmanager.Simulation;

/**
 * Minimal parity runner: it loads an explicit settings folder and writes the
 * standard PureEdgeSim CSV to an explicit output folder.  Keeping the runner
 * trivial guarantees that any Java/Python difference comes from the simulators,
 * not from the driver.
 */
public final class ParityRunner {
    private ParityRunner() {
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: ParityRunner <settings> <output>");
            System.exit(2);
        }
        Simulation simulation = new Simulation();
        simulation.setCustomSettingsFolder(args[0]);
        simulation.setCustomOutputFolder(args[1]);
        simulation.launchSimulation();
    }
}
