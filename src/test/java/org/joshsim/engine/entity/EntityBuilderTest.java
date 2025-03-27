package org.joshsim.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link EntityBuilder} class.
 */
public class EntityBuilderTest {

  private EntityBuilder builder;
  private SpatialEntity mockParent;
  private Geometry mockGeometry;
  private EventKey mockEventKey;
  private EventHandlerGroup mockHandlerGroup;
  private EngineValue mockValue;

  /**
   * Setup common test objects before each test.
   */
  @BeforeEach
  public void setUp() {
    builder = new EntityBuilder();
    mockParent = mock(SpatialEntity.class);
    mockGeometry = mock(Geometry.class);
    mockEventKey = new EventKey("testState", "testAttribute", "testEvent");
    mockHandlerGroup = mock(EventHandlerGroup.class);
    mockValue = mock(EngineValue.class);
  }

  /**
   * Test building an Agent.
   */
  @Test
  public void testBuildAgent() {
    String agentName = "TestAgent";
    builder.setName(agentName)
        .addEventHandlerGroup(mockEventKey, mockHandlerGroup)
        .addAttribute("agentAttr", mockValue);

    Agent agent = builder.buildAgent(mockParent);

    assertNotNull(agent);
    assertEquals(agentName, agent.getName());
    assertEquals(mockParent, agent.getParent());
    assertTrue(agent.getEventHandlers().iterator().hasNext());
    assertTrue(agent.getAttributeValue("agentAttr").isPresent());
  }

  /**
   * Test building a Disturbance.
   */
  @Test
  public void testBuildDisturbance() {
    String disturbanceName = "TestDisturbance";
    builder.setName(disturbanceName);

    Disturbance disturbance = builder.buildDisturbance(mockParent);

    assertNotNull(disturbance);
    assertEquals(disturbanceName, disturbance.getName());
    assertEquals(mockParent, disturbance.getParent());
  }

  /**
   * Test building a Patch.
   */
  @Test
  public void testBuildPatch() {
    String patchName = "TestPatch";
    builder.setName(patchName)
        .addAttribute("patchAttr", mockValue);

    Patch patch = builder.buildPatch(mockGeometry);

    assertNotNull(patch);
    assertEquals(patchName, patch.getName());
    assertEquals(mockGeometry, patch.getGeometry());
    assertTrue(patch.getAttributeValue("patchAttr").isPresent());
  }

  /**
   * Test building a Simulation.
   */
  @Test
  public void testBuildSimulation() {
    String simName = "TestSimulation";
    builder.setName(simName)
        .addEventHandlerGroup(mockEventKey, mockHandlerGroup)
        .addAttribute("simAttr", mockValue);

    Simulation sim = builder.buildSimulation();

    assertNotNull(sim);
    assertEquals(simName, sim.getName());
    assertLengthEquals(1, sim.getEventHandlers());
    assertTrue(sim.getAttributeValue("simAttr").isPresent());
  }

  /**
   * Test that the built entities have independent copies of the maps.
   */
  @Test
  public void testMapsAreIndependent() {
    builder.setName("TestEntity")
        .addEventHandlerGroup(mockEventKey, mockHandlerGroup)
        .addAttribute("attr", mockValue);

    Agent agent = builder.buildAgent(mockParent);

    // Modify the builder's maps after building
    EventKey newKey = new EventKey("newState", "newAttribute", "newEvent");
    EventHandlerGroup newHandler = mock(EventHandlerGroup.class);
    builder.addEventHandlerGroup(newKey, newHandler);
    builder.addAttribute("newAttr", mockValue);

    // Agent should not have the new entries
    assertLengthEquals(1, agent.getEventHandlers());
    assertEquals(Optional.empty(), agent.getEventHandlers(newKey));

  }

  private void assertLengthEquals(int length, Iterable<EventHandlerGroup> groups) {
    List<EventHandlerGroup> groupList = new ArrayList<>();
    groups.forEach(groupList::add);
    assertEquals(length, groupList.size());
  }
}
