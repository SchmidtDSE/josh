/**
 * Tests for structures to help build event handler groups.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for structures to help build event handler groups.
 */
public class EventHandlerGroupBuilderTest {
  private EventHandlerGroupBuilder builder;
  private EventHandler mockHandler;
  private static final String TEST_STATE = "testState";
  private static final String TEST_ATTRIBUTE = "testAttribute";
  private static final String TEST_EVENT = "testEvent";

  /**
   * Create a test event handler group builder and mock handler.
   */
  @BeforeEach
  public void setUp() {
    builder = new EventHandlerGroupBuilder();
    mockHandler = mock(EventHandler.class);
  }

  @Test
  public void testBuildWithValidData() {
    builder.setState(TEST_STATE);
    builder.setAttribute(TEST_ATTRIBUTE);
    builder.setEvent(TEST_EVENT);
    builder.addEventHandler(mockHandler);

    EventHandlerGroup group = builder.build();

    assertNotNull(group);
    assertTrue(group.getEventHandlers().iterator().hasNext());
    assertEquals(TEST_STATE, group.getEventKey().getState());
    assertEquals(TEST_ATTRIBUTE, group.getEventKey().getAttribute());
    assertEquals(TEST_EVENT, group.getEventKey().getEvent());
  }

  @Test
  public void testAddMultipleEventHandlers() {
    List<EventHandler> handlers = new ArrayList<>();
    handlers.add(mock(EventHandler.class));
    handlers.add(mock(EventHandler.class));

    builder.setState(TEST_STATE);
    builder.setAttribute(TEST_ATTRIBUTE);
    builder.setEvent(TEST_EVENT);
    builder.addEventHandler(handlers);

    EventHandlerGroup group = builder.build();
    int count = 0;
    for (EventHandler handler : group.getEventHandlers()) {
      handler.toString(); // To avoid style warning
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void testClear() {
    builder.setState(TEST_STATE);
    builder.setAttribute(TEST_ATTRIBUTE);
    builder.setEvent(TEST_EVENT);
    builder.addEventHandler(mockHandler);

    builder.clear();

    assertThrows(IllegalStateException.class, () -> builder.build());
  }

  @Test
  public void testBuildWithoutState() {
    builder.setAttribute(TEST_ATTRIBUTE);
    builder.setEvent(TEST_EVENT);

    assertThrows(IllegalStateException.class, () -> builder.build());
  }

  @Test
  public void testBuildWithoutAttribute() {
    builder.setState(TEST_STATE);
    builder.setEvent(TEST_EVENT);

    assertThrows(IllegalStateException.class, () -> builder.build());
  }

  @Test
  public void testBuildWithoutEvent() {
    builder.setState(TEST_STATE);
    builder.setAttribute(TEST_ATTRIBUTE);

    assertThrows(IllegalStateException.class, () -> builder.build());
  }
}
