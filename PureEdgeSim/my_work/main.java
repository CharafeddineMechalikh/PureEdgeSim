package my_work;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;

public class main{
    private static String settingsPath = "CharafeddineMechalikh/PureEdgeSim/PureEdgeSim/my_work";
    private static String outputPath = "CharafeddineMechalikh/PureEdgeSim/PureEdgeSim/my_work/output";

    public static void main(String[] args){
        
        Simulation sim = new Simulation();
        sim.setCustomOutputFolder(outputPath);
        sim.setCustomSettingsFolder(settingsPath);
        sim.launchSimulation();
    }
}