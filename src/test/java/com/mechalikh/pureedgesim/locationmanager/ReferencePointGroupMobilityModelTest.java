package com.mechalikh.pureedgesim.locationmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mechalikh.pureedgesim.AbstractTest;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DataCentersManager;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Tests the reference point group mobility model: the path of the anchor, the
 * fact that members of a group agree on it without sharing state, and that each
 * member keeps the position it had next to the anchor at simulation start.
 */
class ReferencePointGroupMobilityModelTest extends AbstractTest {

	private SimulationManager simulationManager;

	@BeforeEach
	void setUp() {
		simulationManager = mock(SimulationManager.class);
		DataCentersManager dataCentersManager = mock(DataCentersManager.class);
		ComputingNodesGenerator generator = mock(ComputingNodesGenerator.class);
		when(simulationManager.getDataCentersManager()).thenReturn(dataCentersManager);
		when(dataCentersManager.getComputingNodesGenerator()).thenReturn(generator);
		when(generator.getEdgeOnlyList()).thenReturn(new ArrayList<ComputingNode>());
		SimulationParameters.updateInterval = 1;
		SimulationParameters.simulationDuration = 60;
		SimulationParameters.simulationMapLength = 200;
		SimulationParameters.simulationMapWidth = 200;
		SimulationParameters.edgeDataCentersRange = 50;
	}

	private static Map<String, String> options(String... keyValues) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < keyValues.length; i += 2)
			map.put(keyValues[i], keyValues[i + 1]);
		return map;
	}

	private ReferencePointGroupMobilityModel groupModel(String group, Location start, String... keyValues) {
		ReferencePointGroupMobilityModel model = new ReferencePointGroupMobilityModel(simulationManager, start);
		model.setSpeed(10);
		model.setMobile(true);
		List<String> values = new ArrayList<>(List.of(keyValues));
		values.add(0, "group");
		values.add(1, group);
		model.setParameters(options(values.toArray(new String[0])));
		return model;
	}

	@Test
	void groupDefaultsToOne() {
		ReferencePointGroupMobilityModel model = new ReferencePointGroupMobilityModel(simulationManager,
				new Location(50, 50));

		assertEquals("1", model.getGroup());
	}

	@Test
	void groupNameIsReadFromTheOptions() {
		ReferencePointGroupMobilityModel model = groupModel("buses", new Location(50, 50));

		assertEquals("buses", model.getGroup());
	}

	@Test
	void anchorPathHasOneLocationPerUpdateAndStaysInsideTheMap() {
		ReferencePointGroupMobilityModel model = groupModel("buses", new Location(50, 50));

		List<Location> path = model.generateAnchorPath();

		assertEquals(61, path.size(), "one location per update, plus the initial one");
		for (Location anchor : path) {
			assertTrue(anchor.getXPos() >= 0 && anchor.getXPos() <= SimulationParameters.simulationMapWidth,
					"the anchor left the simulation width: " + anchor.getXPos());
			assertTrue(anchor.getYPos() >= 0 && anchor.getYPos() <= SimulationParameters.simulationMapLength,
					"the anchor left the simulation length: " + anchor.getYPos());
		}
	}

	@Test
	void membersOfAGroupGenerateTheSameAnchorPath() {
		ReferencePointGroupMobilityModel first = groupModel("buses", new Location(50, 50), "anchor_speed", "15");
		ReferencePointGroupMobilityModel second = groupModel("buses", new Location(80, 120), "anchor_speed", "15");

		List<Location> firstPath = first.generateAnchorPath();
		List<Location> secondPath = second.generateAnchorPath();

		assertEquals(firstPath.size(), secondPath.size());
		for (int i = 0; i < firstPath.size(); i++)
			assertEquals(firstPath.get(i), secondPath.get(i), "the two members disagree at tick " + i);
	}

	@Test
	void twoGroupsFollowTwoDifferentAnchors() {
		List<Location> buses = groupModel("buses", new Location(50, 50)).generateAnchorPath();
		List<Location> taxis = groupModel("taxis", new Location(50, 50)).generateAnchorPath();

		boolean different = false;
		for (int i = 0; i < buses.size(); i++)
			if (!buses.get(i).equals(taxis.get(i)))
				different = true;
		assertTrue(different, "the two groups replayed the same path");
	}

	@Test
	void aMemberFollowsItsAnchorAndKeepsItsOffset() {
		ReferencePointGroupMobilityModel model = groupModel("buses", new Location(50, 50), "anchor_speed", "15");
		List<Location> path = model.generateAnchorPath();
		Location offset = new Location(50 - path.get(0).getXPos(), 50 - path.get(0).getYPos());

		Location current = new Location(50, 50);
		for (int tick = 1; tick < path.size(); tick++) {
			current = model.getNextLocation(current);
			Location expected = new Location(path.get(tick).getXPos() + offset.getXPos(),
					path.get(tick).getYPos() + offset.getYPos());
			assertTrue(current.getXPos() >= 0 && current.getXPos() <= SimulationParameters.simulationMapWidth,
					"x=" + current.getXPos() + " left the map at tick " + tick);
			assertTrue(current.getYPos() >= 0 && current.getYPos() <= SimulationParameters.simulationMapLength,
					"y=" + current.getYPos() + " left the map at tick " + tick);
			if (isStrictlyInside(expected)) {
				assertEquals(expected.getXPos(), current.getXPos(), 1e-9,
						"the offset next to the anchor changed at tick " + tick);
				assertEquals(expected.getYPos(), current.getYPos(), 1e-9,
						"the offset next to the anchor changed at tick " + tick);
			} else {
				Location bounced = model.reflectLocation(expected);
				assertEquals(bounced.getXPos(), current.getXPos(), 1e-9,
						"the device was not bounced off the border at tick " + tick);
				assertEquals(bounced.getYPos(), current.getYPos(), 1e-9,
						"the device was not bounced off the border at tick " + tick);
			}
		}
	}

	@Test
	void twoMembersMoveTogether() {
		ReferencePointGroupMobilityModel front = groupModel("buses", new Location(60, 60), "anchor_speed", "15");
		ReferencePointGroupMobilityModel rear = groupModel("buses", new Location(50, 40), "anchor_speed", "15");
		Location gap = new Location(10, 20);
		Location frontPosition = new Location(60, 60);
		Location rearPosition = new Location(50, 40);
		int compared = 0;

		for (int tick = 0; tick < 40; tick++) {
			frontPosition = front.getNextLocation(frontPosition);
			rearPosition = rear.getNextLocation(rearPosition);
			// A member that hits a border is bounced back, which flips its offset; the
			// group only stays rigid while both of them are inside the map.
			if (isStrictlyInside(rawPosition(front)) && isStrictlyInside(rawPosition(rear))) {
				assertEquals(gap.getXPos(), frontPosition.getXPos() - rearPosition.getXPos(), 1e-9,
						"the two members drifted apart at tick " + tick);
				assertEquals(gap.getYPos(), frontPosition.getYPos() - rearPosition.getYPos(), 1e-9,
						"the two members drifted apart at tick " + tick);
				compared++;
			}
		}
		assertTrue(compared > 20, "the members were almost always bounced: " + compared);
		assertTrue(Math.abs(frontPosition.getXPos() - 60) > 1 || Math.abs(frontPosition.getYPos() - 60) > 1,
				"the group never moved");
	}

	private static Location rawPosition(ReferencePointGroupMobilityModel model) {
		Location anchor = model.anchorPath.get(model.tick);
		return new Location(anchor.getXPos() + model.offset.getXPos(), anchor.getYPos() + model.offset.getYPos());
	}

	@Test
	void aStationaryAnchorLeavesTheDeviceWhereItIs() {
		ReferencePointGroupMobilityModel model = groupModel("parked", new Location(50, 50), "anchor_speed", "0");

		Location current = new Location(50, 50);
		for (int tick = 0; tick < 20; tick++)
			current = model.getNextLocation(current);

		assertEquals(50, current.getXPos(), 1e-9);
		assertEquals(50, current.getYPos(), 1e-9);
	}

	@Test
	void theAnchorPausesAtEachWaypoint() {
		ReferencePointGroupMobilityModel withoutPause = groupModel("fast", new Location(50, 50), "anchor_speed",
				"5000");
		ReferencePointGroupMobilityModel withPause = groupModel("fast", new Location(50, 50), "anchor_speed", "5000",
				"anchor_pause", "5");

		assertEquals(61, distinctLocations(withoutPause.generateAnchorPath()),
				"without a pause the anchor moves at every update");
		List<Location> paused = withPause.generateAnchorPath();
		assertTrue(distinctLocations(paused) < 15, "the anchor did not wait at its waypoints: " + paused.size());
		assertTrue(distinctLocations(paused) > 5, "the anchor never moved: " + distinctLocations(paused));
	}

	@Test
	void thePathIsNotReplayedOnceItIsFinished() {
		ReferencePointGroupMobilityModel model = groupModel("buses", new Location(50, 50), "anchor_speed", "15");
		Location current = new Location(50, 50);
		for (int tick = 0; tick < 61; tick++)
			current = model.getNextLocation(current);

		Location endOfPath = current;
		for (int tick = 0; tick < 10; tick++)
			current = model.getNextLocation(current);

		assertEquals(endOfPath, current, "the device kept moving after the end of the simulation");
	}

	@Test
	void aNonNumericAnchorSpeedIsRejected() {
		ReferencePointGroupMobilityModel model = new ReferencePointGroupMobilityModel(simulationManager,
				new Location(50, 50));
		model.setParameters(options("anchor_speed", "slow"));

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, model::generateAnchorPath);
		assertTrue(error.getMessage().contains("the \"anchor_speed\" option must be a number"),
				"unexpected message: " + error.getMessage());
	}

	@Test
	void theFactoryBuildsTheGroupModel() {
		MobilityModel model = MobilityModelFactory.create("rpgm", simulationManager, new Location(50, 50));

		assertInstanceOf(ReferencePointGroupMobilityModel.class, model);
		assertTrue(model.getParameters().isEmpty(), "the factory must not set any option");
	}

	private static boolean isStrictlyInside(Location location) {
		return location.getXPos() > 0 && location.getXPos() < SimulationParameters.simulationMapWidth
				&& location.getYPos() > 0 && location.getYPos() < SimulationParameters.simulationMapLength;
	}

	private static int distinctLocations(List<Location> path) {
		int count = 0;
		for (int i = 0; i < path.size(); i++)
			if (i == 0 || !path.get(i).equals(path.get(i - 1)))
				count++;
		return count;
	}
}
