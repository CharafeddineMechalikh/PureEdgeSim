package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.LinkedList;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.taskgenerator.Task;

/**
 * A Singleton class that implements the Null Object Design Pattern for the
 * {@link ComputingNode} class. Needed to avoid {@link NullPointerException}
 * when using the NULL object instead of attributing null to ComputingNode
 * variables.
 */
public class ComputingNodeNull implements ComputingNode {

	/**
	 * 
	 * Represents a null computing node, used as a placeholder when a computing node
	 * is expected but none is available or applicable.
	 */
	private static final ComputingNodeNull instance = new ComputingNodeNull();

	/**
	 * 
	 * The list of tasks currently queued in this computing node. Stays empty since
	 * this node is supposed to be null.
	 */
	private LinkedList<Task> tasksQueue = new LinkedList<Task>();

	/**
	 * 
	 * Private constructor to prevent instantiation from outside the class.
	 */
	private ComputingNodeNull() {
	}

	/**
	 * Returns the singleton instance of the ComputingNodeNullObject.
	 * 
	 * @return the singleton instance of the ComputingNodeNullObject
	 */
	public static ComputingNodeNull getInstance() {
		return instance;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void submitTask(Task task) {
		// Do nothing.
	}

	/**
	 * Returns a NULL type since this Computing Node is supposed to be null.
	 */
	@Override
	public TYPES getType() {
		return TYPES.NULL;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setType(TYPES type) {
		// Do nothing.
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setName(String name) {
		// Do nothing.
	}

	/**
	 * Returns the name of the null Computing Node which is an empty string.
	 * 
	 * @return An empty string.
	 */
	@Override
	public String getName() {
		return "";
	}

	/**
	 * 
	 * Returns false since this Computing Node is not an orchestrator.
	 * 
	 * @return false
	 */
	@Override
	public boolean isOrchestrator() {
		return false;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setAsOrchestrator(boolean isOrchestrator) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns the singleton instance of ComputingNodeNull as the orchestrator.
	 * 
	 * @return The singleton instance of ComputingNodeNull.
	 */
	@Override
	public ComputingNode getOrchestrator() {
		return ComputingNode.NULL;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void enableTaskGeneration(boolean generateTasks) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns false since this Computing Node is not capable of generating tasks.
	 * 
	 * @return false
	 */
	@Override
	public boolean isGeneratingTasks() {
		return false;
	}

	/**
	 * 
	 * Returns false since this Computing Node is not dead.
	 * 
	 * @return false
	 */
	@Override
	public boolean isDead() {
		return false;
	}

	/**
	 * 
	 * Returns -1 since this Computing Node is not dead.
	 * 
	 * @return -1
	 */
	@Override
	public double getDeathTime() {
		return -1;
	}

	/**
	 * 
	 * Returns the instance of the null energy model for the null computing node.
	 * 
	 * @return the instance of the null energy model
	 */
	@Override
	public EnergyModelComputingNode getEnergyModel() {
		return EnergyModelComputingNode.NULL;
	}

	/**
	 * 
	 * Does nothing since the null computing node does not have an energy model.
	 */
	@Override
	public void setEnergyModel(EnergyModelComputingNode energyModel) {
	}

	/**
	 * 
	 * Returns the instance of the null mobility model for the null computing node.
	 * 
	 * @return the instance of the null mobility model
	 */
	@Override
	public MobilityModel getMobilityModel() {
		return MobilityModel.NULL;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setMobilityModel(MobilityModel mobilityModel) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns false since the null computing node is not a peripheral node.
	 * 
	 * @return false
	 */
	@Override
	public boolean isPeripheral() {
		return false;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setPeriphery(boolean periphery) {
		// Do nothing.
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setApplicationPlacementLocation(ComputingNode node) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns the null Computing Node since it has no application placed on another
	 * computing node.
	 * 
	 * @return The null Computing Node.
	 */
	@Override
	public ComputingNode getApplicationPlacementLocation() {
		return ComputingNode.NULL;
	}

	/**
	 * 
	 * Returns false since it cannot request application placements.
	 * 
	 * @return False.
	 */
	@Override
	public boolean isApplicationPlaced() {
		return false;
	}

	/**
	 * 
	 * Does nothing since it cannot request application placements.
	 */
	@Override
	public void setApplicationPlaced(boolean isApplicationPlaced) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns 0 since there are no CPU cores available on this Computing Node.
	 * 
	 * @return 0.
	 */
	@Override
	public double getNumberOfCPUCores() {
		return 0;
	}

	/**
	 * 
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setNumberOfCPUCores(int numberOfCPUCores) {
	}

	/**
	 * 
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public int getApplicationType() {
		return -1;
	}

	/**
	 * 
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setApplicationType(int applicationType) {
	}

	/**
	 * 
	 * Returns 0 since there is no storage available on this Computing Node.
	 * 
	 * @return 0.
	 */
	@Override
	public double getAvailableStorage() {
		return 0;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setAvailableStorage(double availableStorage) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns 0 since there is no computation available on this Computing Node.
	 * 
	 * @return 0.
	 */
	@Override
	public double getAvgCpuUtilization() {
		return 0;
	}

	/**
	 * 
	 * Returns 0 since there is no computation available on this Computing Node.
	 * 
	 * @return 0.
	 */
	@Override
	public double getCurrentCpuUtilization() {
		return 0;
	}

	/**
	 * 
	 * Returns false since this device is null and cannot be considered idle.
	 * 
	 * @return false.
	 */
	@Override
	public boolean isIdle() {
		return false;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setIdle(boolean isIdle) {
		// Do nothing.
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void addCpuUtilization(Task task) {
		// Do nothing.
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void removeCpuUtilization(Task task) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns false since this device is null and cannot be considered a sensor.
	 * 
	 * @return false.
	 */
	@Override
	public boolean isSensor() {
		return false;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setAsSensor(boolean isSensor) {
		// Do nothing.
	}

	/**
	 * 
	 * Gets the list of tasks currently queued in this computing node. Stays empty
	 * since this node is supposed to be null.
	 * 
	 * @return an empty task queue.
	 */
	@Override
	public LinkedList<Task> getTasksQueue() {
		return tasksQueue;
	}

	/**
	 * 
	 * Returns 0 since there is no storage on this Computing Node.
	 * 
	 * @return 0.
	 */
	@Override
	public double getTotalStorage() {
		return 0;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setStorage(double storage) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns 0 since this Computing Node has no computation capacity.
	 * 
	 * @return 0.
	 */
	@Override
	public double getTotalMipsCapacity() {
		return 0;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setTotalMipsCapacity(double totalMipsCapacity) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns 0 since this Computing Node has no computation capacity.
	 * 
	 * @return 0.
	 */
	@Override
	public double getMipsPerCore() {
		return 0;
	}

	/**
	 * 
	 * Returns -1 to avoid conflicting with other ids.
	 * 
	 * @return -1.
	 */
	@Override
	public int getId() {
		return -1;
	}

	/**
	 * 
	 * Returns 0 since this Computing Node has no RAM.
	 * 
	 * @return 0.
	 */
	@Override
	public double getRamCapacity() {
		return 0;
	}

	/**
	 * 
	 * Returns 0 since this Computing Node has no RAM.
	 * 
	 * @return 0.
	 */
	@Override
	public double getAvailableRam() {
		return 0;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setRam(double ram) {
		// Do nothing.
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setAvailableRam(double ram) {
		// Do nothing.
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setOrchestrator(ComputingNode orchestrator) {
		// Do nothing.
	}

	/**
	 * 
	 * Returns the null network link since this computing node is supposed to be
	 * null.
	 * 
	 * @param linkType the type of link to get.
	 * @return the null network link.
	 */
	@Override
	public NetworkLink getCurrentLink(LinkOrientation linkType) {
		return NetworkLink.NULL;
	}

	/**
	 * Does nothing since this Computing Node is supposed to be null.
	 */
	@Override
	public void setCurrentLink(NetworkLink link, LinkOrientation linkType) {
		// Do nothing.
	}

}
