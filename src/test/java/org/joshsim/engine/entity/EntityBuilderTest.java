package org.joshsim.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
   * Test setting and getting the name of the entity.
   */
  @Test
  public void testSetAndGetName() {
    String testName = "TestEntity";
    builder.setName(testName);
    assertEquals(testName, builder.getName());
  }

  /**
   * Test that getName throws exception when name is not set.
   */
  @Test
  public void testGetNameThrowsWhenNameNotSet() {
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> builder.getName()
    );
    assertEquals("Name not set", exception.getMessage());
  }

  /**
   * Test clearing the builder state.
   */
  @Test
  public void testClear() {
    // Set up builder with values
    builder.setName("TestName")
        .addEventHandlerGroup(mockEventKey, mockHandlerGroup)
        .addAttribute("testAttribute", mockValue);
    
    // Clear the builder
    builder.clear();
    
    // Verify name is reset
    assertThrows(IllegalStateException.class, () -> builder.getName());
    
    // Verify collections are cleared
    assertTrue(builder.eventHandlerGroups.isEmpty());
    assertTrue(builder.attributes.isEmpty());
  }

  /**
   * Test adding event handler groups.
   */
  @Test
  public void testAddEventHandlerGroup() {
    builder.addEventHandlerGroup(mockEventKey, mockHandlerGroup);
    
    assertEquals(1, builder.eventHandlerGroups.size());
    assertTrue(builder.eventHandlerGroups.containsKey(mockEventKey));
    assertEquals(mockHandlerGroup, builder.eventHandlerGroups.get(mockEventKey));
  }

  /**
   * Test adding attributes.
   */
  @Test
  public void testAddAttribute() {
    String attributeName = "testAttribute";
    builder.addAttribute(attributeName, mockValue);
    
    assertEquals(1, builder.attributes.size());
    assertTrue(builder.attributes.containsKey(attributeName));
    assertEquals(mockValue, builder.attributes.get(attributeName));
  }

  /**
   * Test method chaining functionality.
   */
  @Test
  public void testMethodChaining() {
    builder.setName("ChainTest")
        .addEventHandlerGroup(mockEventKey, mockHandlerGroup)
        .addAttribute("attr1", mockValue)
        .addAttribute("attr2", mockValue);
    
    assertEquals("ChainTest", builder.getName());
    assertEquals(1, builder.eventHandlerGroups.size());
    assertEquals(2, builder.attributes.size());
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
    assertEquals(1, agent.getEventHandlers().size());
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
    assertEquals(1, sim.getEventHandlers().size());
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
    assertEquals(1, agent.getEventHandlers().size());
    assertThrows(ClassCastException.class, () -> agent.getEventHandlers(
        new EventKey("newState", "newAttribute", "newEvent")
    ));
  }
}