/* ScenarioFactory wiring the P1 common-benchmark components. */
package crosssim;

import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.cloud_server.DefaultCloudServerManager;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.DefaultEdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.DefaultMobileServerManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.applications.tutorial1.SampleMobileDeviceManager;
import edu.boun.edgecloudsim.applications.tutorial1.SampleNetworkModel;

public class CommonScenarioFactory implements ScenarioFactory {
    private final int devices;
    private final double simulationTime;
    private final String policy;
    private final String scenario;

    public CommonScenarioFactory(int devices, double simulationTime, String policy, String scenario) {
        this.devices = devices;
        this.simulationTime = simulationTime;
        this.policy = policy;
        this.scenario = scenario;
    }

    @Override public LoadGeneratorModel getLoadGeneratorModel() {
        return new CommonLoadGenerator(devices, simulationTime, scenario);
    }
    @Override public EdgeOrchestrator getEdgeOrchestrator() {
        return new CommonEdgeOrchestrator(policy, scenario);
    }
    @Override public MobilityModel getMobilityModel() {
        return new CommonMobilityModel(devices, simulationTime);
    }
    @Override public NetworkModel getNetworkModel() {
        return new SampleNetworkModel(devices, scenario);
    }
    @Override public EdgeServerManager getEdgeServerManager() {
        return new DefaultEdgeServerManager();
    }
    @Override public CloudServerManager getCloudServerManager() {
        return new DefaultCloudServerManager();
    }
    @Override public MobileDeviceManager getMobileDeviceManager() throws Exception {
        return new SampleMobileDeviceManager();
    }
    @Override public MobileServerManager getMobileServerManager() {
        return new DefaultMobileServerManager();
    }
}
