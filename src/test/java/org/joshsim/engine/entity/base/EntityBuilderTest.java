package org.joshsim.engine.entity.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.Agent;
import org.joshsim.engine.entity.type.Disturbance;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link EntityBuilder} class.
 */
public class EntityBuilderTest {

  private EntityBuilder builder;
  private Entity mockParent;
  private EngineGeometry mockGeometry;
  private EventKey mockEventKey;
  private EventHandlerGroup mockHandlerGroup;
  private EngineValue mockValue;

  /**
   * Setup common test objects before each test.
   */
  @BeforeEach
  public void setUp() {
    builder = new EntityBuilder(new ValueSupportFactory());
    mockParent = mock(Entity.class);
    mockGeometry = mock(EngineGeometry.class);
    mockEventKey = EventKey.of("testState", "testAttribute", "testEvent");
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
    assertEquals(mockGeometry, patch.getGeometry().get());
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
    EventKey newKey = EventKey.of("newState", "newAttribute", "newEvent");
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

  /**
   * combineWith should keep a base handler the override does not redeclare.
   */
  @Test
  public void combineWithKeepsBaseHandlerNotRedeclaredByOverride() {
    EventKey ageKey = EventKey.of("age", "step");
    EventHandlerGroup ageGroup = mock(EventHandlerGroup.class);
    builder.setName("Tree").addEventHandlerGroup(ageKey, ageGroup);

    EntityBuilder override = new EntityBuilder(new ValueSupportFactory());
    override.setName("Tree");

    EntityBuilder combined = builder.combineWith(override);

    Agent agent = combined.buildAgent(mockParent);
    assertTrue(agent.getEventHandlers(ageKey).isPresent());
  }

  /**
   * combineWith should let the override's handler win for a key both declare.
   */
  @Test
  public void combineWithLetsOverrideWinOnSharedKey() {
    EventKey heightKey = EventKey.of("height", "step");
    EventHandlerGroup baseGroup = mock(EventHandlerGroup.class);
    EventHandlerGroup overrideGroup = mock(EventHandlerGroup.class);
    builder.setName("Tree").addEventHandlerGroup(heightKey, baseGroup);

    EntityBuilder override = new EntityBuilder(new ValueSupportFactory());
    override.setName("Tree").addEventHandlerGroup(heightKey, overrideGroup);

    EntityBuilder combined = builder.combineWith(override);

    Agent agent = combined.buildAgent(mockParent);
    assertEquals(overrideGroup, agent.getEventHandlers(heightKey).orElseThrow());
  }

  /**
   * combineWith should add a handler the override declares that the base never had.
   */
  @Test
  public void combineWithAddsHandlerOnlyTheOverrideDeclares() {
    EventKey widthKey = EventKey.of("width", "step");
    EventHandlerGroup widthGroup = mock(EventHandlerGroup.class);
    builder.setName("Tree");

    EntityBuilder override = new EntityBuilder(new ValueSupportFactory());
    override.setName("Tree").addEventHandlerGroup(widthKey, widthGroup);

    EntityBuilder combined = builder.combineWith(override);

    Agent agent = combined.buildAgent(mockParent);
    assertEquals(widthGroup, agent.getEventHandlers(widthKey).orElseThrow());
  }

  /**
   * combineWith should not mutate either source builder.
   */
  @Test
  public void combineWithDoesNotMutateEitherSourceBuilder() {
    EventKey ageKey = EventKey.of("age", "step");
    EventHandlerGroup ageGroup = mock(EventHandlerGroup.class);
    builder.setName("Tree").addEventHandlerGroup(ageKey, ageGroup);

    EntityBuilder override = new EntityBuilder(new ValueSupportFactory());
    override.setName("Tree");

    builder.combineWith(override);

    Agent baseAgent = builder.buildAgent(mockParent);
    Agent overrideAgent = override.buildAgent(mockParent);
    assertTrue(baseAgent.getEventHandlers(ageKey).isPresent());
    assertTrue(overrideAgent.getEventHandlers(ageKey).isEmpty());
  }
}
