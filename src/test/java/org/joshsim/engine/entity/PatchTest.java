package org.joshsim.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Patch} class.
 */
public class PatchTest {

  private Geometry mockGeometry;
  private String patchName;
  private HashMap<EventKey, EventHandlerGroup> eventHandlerGroups;
  private HashMap<String, EngineValue> attributes;
  private Patch patch;

  /**
   * Setup common test objects before each test.
   */
  @BeforeEach
  public void setUp() {
    mockGeometry = mock(Geometry.class);
    patchName = "testPatch";
    eventHandlerGroups = new HashMap<>();
    attributes = new HashMap<>();
    EngineValue mockValue = mock(EngineValue.class);
    attributes.put("testAttribute", mockValue);
    
    patch = new Patch(mockGeometry, patchName, eventHandlerGroups, attributes);
  }

  /**
   * Test that the constructor correctly initializes the patch.
   */
  @Test
  public void testConstructor() {
    assertEquals(patchName, patch.getName());
    assertEquals(mockGeometry, patch.getGeometry());
    assertTrue(patch.getAttributeValue("testAttribute").isPresent());
  }

  /**
   * Test that the constructor handles null maps properly.
   */
  @Test
  public void testConstructorWithNullMaps() {
    Patch nullMapPatch = new Patch(mockGeometry, patchName, null, null);
    
    assertNotNull(nullMapPatch.getEventHandlers());
    assertTrue(nullMapPatch.getEventHandlers().isEmpty());
    
    assertNotNull(nullMapPatch.attributes);
    assertTrue(nullMapPatch.attributes.isEmpty());
  }

  /**
   * Test that getPatchGeometry returns the correct geometry.
   */
  @Test
  public void testGetPatchGeometry() {
    assertEquals(mockGeometry, patch.getPatchGeometry());
    assertEquals(patch.getGeometry(), patch.getPatchGeometry());
  }

  /**
   * Test inherited attribute management functionality.
   */
  @Test
  public void testAttributeManagement() {
    // Test getting an existing attribute
    Optional<EngineValue> attributeValue = patch.getAttributeValue("testAttribute");
    assertTrue(attributeValue.isPresent());
    
    // Test getting a non-existent attribute
    attributeValue = patch.getAttributeValue("nonExistentAttribute");
    assertFalse(attributeValue.isPresent());
    
    // Test setting a new attribute
    EngineValue newValue = mock(EngineValue.class);
    patch.setAttributeValue("newAttribute", newValue);
    attributeValue = patch.getAttributeValue("newAttribute");
    assertTrue(attributeValue.isPresent());
    assertEquals(newValue, attributeValue.get());
  }

  /**
   * Test the locking functionality inherited from Entity.
   */
  @Test
  public void testLockFunctionality() {
    // Test that setting attributes works when not locked
    EngineValue mockValue = mock(EngineValue.class);
    patch.setAttributeValue("testLock", mockValue);
    
    // Lock the entity
    patch.lock();
    
    // Setting attributes should throw an exception when locked
    EngineValue anotherMockValue = mock(EngineValue.class);
    assertThrows(IllegalStateException.class, () -> 
        patch.setAttributeValue("anotherAttribute", anotherMockValue));
    
    // Unlock and verify setting works again
    patch.unlock();
    patch.setAttributeValue("unlockedAttribute", mockValue);
    assertTrue(patch.getAttributeValue("unlockedAttribute").isPresent());
  }

  /**
   * Test the event handler functionality.
   */
  @Test
  public void testEventHandlerManagement() {
    // Create and add an event handler group
    EventKey testKey = new EventKey("testState", "testAttribute", "testEvent");
    EventHandlerGroup handlerGroup = mock(EventHandlerGroup.class);
    patch.eventHandlerGroups.put(testKey, handlerGroup);
    
    // Test getEventHandlers()
    HashMap<EventKey, EventHandlerGroup> handlers = patch.getEventHandlers();
    assertEquals(1, handlers.size());
    assertTrue(handlers.containsKey(testKey));
    assertEquals(handlerGroup, handlers.get(testKey));
    
    // Test getEventHandlers with specific parameters
    Optional<EventHandlerGroup> retrievedGroup = patch.getEventHandlers(
        new EventKey("testState", "testAttribute", "testEvent")
    );
    assertTrue(retrievedGroup.isPresent());
    assertEquals(handlerGroup, retrievedGroup.get());
    
    // Test with non-existent event key
    Optional<EventHandlerGroup> nonExistentGroup = patch.getEventHandlers(
        new EventKey("nonExistent", "nonExistent", "nonExistent")
    );
    assertEquals(Optional.empty(), nonExistentGroup);
  }
}