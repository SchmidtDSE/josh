/**
 * Tests for the Agent class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for the Agent class functionality including inherited behavior.
 */
public class AgentTest {

  private Entity mockParent;
  private EngineGeometry mockGeometry;
  private EngineValue mockValue;
  private HashMap<EventKey, EventHandlerGroup> eventHandlers;
  private HashMap<String, EngineValue> attributes;
  private Agent agent;
  private static final String AGENT_NAME = "TestAgent";
  private static final String ATTR_NAME = "TestAttribute";
  private static final EventKey EVENT_KEY = new EventKey("state", "attribute", "event");

  /**
   * Sets up the test environment by initializing mocks and creating the Agent instance.
   */
  @BeforeEach
  public void setUp() {
    // Set up mocks
    mockParent = mock(Entity.class);
    mockGeometry = mock(EngineGeometry.class);
    mockValue = mock(EngineValue.class);

    when(mockParent.getGeometry()).thenReturn(Optional.of(mockGeometry));

    // Initialize maps
    eventHandlers = new HashMap<>();
    EventHandlerGroup handlerGroup = mock(EventHandlerGroup.class);
    eventHandlers.put(EVENT_KEY, handlerGroup);

    attributes = new HashMap<>();
    attributes.put(ATTR_NAME, mockValue);

    // Create agent instance
    agent = new Agent(mockParent, AGENT_NAME, eventHandlers, attributes);
  }

  /**
   * Verify that getName returns the correct name.
   */
  @Test
  public void testGetName() {
    assertEquals(AGENT_NAME, agent.getName(), "Agent name should match constructor argument");
  }

  /**
   * Test that getParent returns the parent provided in constructor.
   */
  @Test
  public void testGetParent() {
    assertEquals(mockParent, agent.getParent(), "Parent should match constructor argument");
  }

  /**
   * Test that getGeometry returns the parent's geometry.
   */
  @Test
  public void testGetGeometry() {
    assertEquals(mockGeometry, agent.getGeometry().get(), "Geometry should come from parent");
  }

  /**
   * Test attribute retrieval functionality.
   */
  @Test
  public void testGetAttributeValue() {
    Optional<EngineValue> result = agent.getAttributeValue(ATTR_NAME);
    assertTrue(result.isPresent(), "Attribute should be present");
    assertEquals(mockValue, result.get(), "Retrieved value should match original");
  }

  /**
   * Test that non-existent attributes return empty Optional.
   */
  @Test
  public void testGetNonExistentAttribute() {
    Optional<EngineValue> result = agent.getAttributeValue("nonexistent");
    assertFalse(result.isPresent(), "Non-existent attribute should return empty Optional");
  }

  /**
   * Test setting attribute values.
   */
  @Test
  public void testSetAttributeValue() {
    EngineValue newValue = mock(EngineValue.class);
    agent.setAttributeValue("newAttr", newValue);

    Optional<EngineValue> result = agent.getAttributeValue("newAttr");
    assertTrue(result.isPresent(), "New attribute should be present");
    assertEquals(newValue, result.get(), "Retrieved value should match set value");
  }

  /**
   * Test that unlocking allows setting attributes again.
   */
  @Test
  public void testLockUnlockCycle() {
    agent.lock();
    agent.unlock();

    EngineValue newValue = mock(EngineValue.class);
    // Should not throw exception
    agent.setAttributeValue("newAttr", newValue);

    assertEquals(newValue, agent.getAttributeValue("newAttr").get());
  }

  /**
   * Test retrieving event handlers.
   */
  @Test
  public void testGetEventHandlers() {
    EventHandlerGroup result = agent.getEventHandlers(
        new EventKey("state", "attribute", "event")
    ).get();
    assertEquals(eventHandlers.get(EVENT_KEY), result);
  }

  /**
   * Test null handling in constructor.
   */
  @Test
  public void testNullMapsInConstructor() {
    Agent nullMapAgent = new Agent(mockParent, AGENT_NAME, null, null);

    Iterable<EventHandlerGroup> groups = nullMapAgent.getEventHandlers();
    assertFalse(groups.iterator().hasNext(),
        "Event handlers should return empty list with null map");
    assertFalse(nullMapAgent.getAttributeValue("any").isPresent(),
        "Attributes should be initialized as empty map when null is provided");
  }

  /**
   * Test the complete event handlers map is retrievable.
   */
  @Test
  public void testGetAllEventHandlers() {
    // Convert both collections to lists for proper comparison
    List<EventHandlerGroup> expectedHandlers = new ArrayList<>(eventHandlers.values());
    List<EventHandlerGroup> actualHandlers = new ArrayList<>();
    agent.getEventHandlers().forEach(actualHandlers::add);

    // Check they contain the same elements
    assertEquals(expectedHandlers.size(), actualHandlers.size(),
        "Event handlers should have the same number of elements");
    assertTrue(expectedHandlers.containsAll(actualHandlers),
        "Event handlers should contain the same elements");
  }
}
