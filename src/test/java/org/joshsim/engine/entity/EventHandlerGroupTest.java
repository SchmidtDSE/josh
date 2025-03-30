
package org.joshsim.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for a group of event handlers related by mutual conditons.
 */
public class EventHandlerGroupTest {
  private List<EventHandler> eventHandlers;
  private EventKey eventKey;
  private EventHandlerGroup group;
  private static final String TEST_STATE = "testState";
  private static final String TEST_ATTRIBUTE = "testAttribute";
  private static final String TEST_EVENT = "testEvent";

  /**
   * Setup mock event handler groups.
   */
  @BeforeEach
  public void setUp() {
    eventHandlers = new ArrayList<>();
    eventHandlers.add(mock(EventHandler.class));
    eventKey = new EventKey(TEST_STATE, TEST_ATTRIBUTE, TEST_EVENT);
    group = new EventHandlerGroup(eventHandlers, eventKey);
  }

  @Test
  public void testConstructor() {
    assertNotNull(group);
    assertEquals(eventHandlers, group.getEventHandlers());
    assertEquals(eventKey, group.getEventKey());
  }

  @Test
  public void testGetEventHandlers() {
    Iterable<EventHandler> handlers = group.getEventHandlers();
    assertNotNull(handlers);
    assertTrue(handlers.iterator().hasNext());
    assertEquals(eventHandlers.get(0), handlers.iterator().next());
  }

  @Test
  public void testGetEventKey() {
    EventKey key = group.getEventKey();
    assertNotNull(key);
    assertEquals(TEST_STATE, key.getState());
    assertEquals(TEST_ATTRIBUTE, key.getAttribute());
    assertEquals(TEST_EVENT, key.getEvent());
  }

  @Test
  public void testConstructorWithEmptyHandlers() {
    group = new EventHandlerGroup(new ArrayList<>(), eventKey);
    assertNotNull(group);
    assertFalse(group.getEventHandlers().iterator().hasNext());
  }
}
