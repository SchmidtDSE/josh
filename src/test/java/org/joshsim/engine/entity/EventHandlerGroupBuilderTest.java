package org.joshsim.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link EventHandlerGroupBuilder} class.
 */
public class EventHandlerGroupBuilderTest {

  private EventHandlerGroupBuilder builder;
  private EventHandler mockHandler1;
  private EventHandler mockHandler2;

  /**
   * Setup common test objects before each test.
   */
  @BeforeEach
  public void setUp() {
    builder = new EventHandlerGroupBuilder();
    mockHandler1 = mock(EventHandler.class);
    mockHandler2 = mock(EventHandler.class);
  }

  /**
   * Test building an EventHandlerGroup with all properties set.
   */
  @Test
  public void testBuildCompleteGroup() {
    // Setup the builder with all properties
    builder.setState("testState");
    builder.setAttribute("testAttribute");
    builder.setEvent("testEvent");
    builder.addEventHandler(mockHandler1);
    
    // Build the group
    EventHandlerGroup group = builder.build();
    
    // Verify the group properties
    assertNotNull(group);
    assertEquals("testState", group.getState());
    assertEquals("testAttribute", group.getAttribute());
    assertEquals("testEvent", group.getEvent());
    assertEquals(1, group.getEventHandlers().size());
    assertEquals(mockHandler1, group.getEventHandlers().get(0));
  }

  /**
   * Test setting and getting state.
   */
  @Test
  public void testStateManagement() {
    // Initially, state should be empty
    assertFalse(builder.getState().isPresent());
    
    // Set state and verify
    String testState = "testState";
    builder.setState(testState);
    
    Optional<String> retrievedState = builder.getState();
    assertTrue(retrievedState.isPresent());
    assertEquals(testState, retrievedState.get());
  }

  /**
   * Test setting and getting attribute with error handling.
   */
  @Test
  public void testAttributeManagement() {
    // Initially, getAttribute should throw exception
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> builder.getAttribute()
    );
    assertEquals("Attribute not set", exception.getMessage());
    
    // Set attribute and verify
    String testAttribute = "testAttribute";
    builder.setAttribute(testAttribute);
    assertEquals(testAttribute, builder.getAttribute());
  }

  /**
   * Test setting and getting event with error handling.
   */
  @Test
  public void testEventManagement() {
    // Initially, getEvent should throw exception
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> builder.getEvent()
    );
    assertEquals("Event not set", exception.getMessage());
    
    // Set event and verify
    String testEvent = "testEvent";
    builder.setEvent(testEvent);
    assertEquals(testEvent, builder.getEvent());
  }

  /**
   * Test adding a single event handler.
   */
  @Test
  public void testAddSingleEventHandler() {
    builder.addEventHandler(mockHandler1);
    
    EventHandlerGroup group = builder.build();
    assertEquals(1, group.getEventHandlers().size());
    assertEquals(mockHandler1, group.getEventHandlers().get(0));
  }

  /**
   * Test adding multiple event handlers at once.
   */
  @Test
  public void testAddMultipleEventHandlers() {
    List<EventHandler> handlers = Arrays.asList(mockHandler1, mockHandler2);
    builder.addEventHandler(handlers);
    
    EventHandlerGroup group = builder.build();
    assertEquals(2, group.getEventHandlers().size());
    assertEquals(mockHandler1, group.getEventHandlers().get(0));
    assertEquals(mockHandler2, group.getEventHandlers().get(1));
  }

  /**
   * Test clearing the builder.
   */
  @Test
  public void testClear() {
    // Setup the builder with all properties
    builder.setState("testState");
    builder.setAttribute("testAttribute");
    builder.setEvent("testEvent");
    builder.addEventHandler(mockHandler1);
    
    // Clear the builder
    builder.clear();
    
    // Verify all properties are reset
    assertFalse(builder.getState().isPresent());
    
    assertThrows(IllegalStateException.class, () -> builder.getAttribute());
    assertThrows(IllegalStateException.class, () -> builder.getEvent());
    
    EventHandlerGroup clearedGroup = builder.build();
    assertTrue(clearedGroup.getEventHandlers().isEmpty());
  }

  /**
   * Test building with minimal required properties.
   */
  @Test
  public void testBuildWithMinimalProperties() {
    // Only set the required properties (attribute and event are required)
    builder.setAttribute("testAttribute");
    builder.setEvent("testEvent");
    
    EventHandlerGroup group = builder.build();
    
    assertNotNull(group);
    assertFalse(group.getState().isPresent());
    assertEquals("testAttribute", group.getAttribute());
    assertEquals("testEvent", group.getEvent());
    assertTrue(group.getEventHandlers().isEmpty());
  }
}