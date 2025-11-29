package org.joshsim.engine.entity.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.EntityInitializationInfo;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Patch} class.
 */
public class PatchTest {

  private EngineGeometry mockGeometry;
  private String patchName;
  private HashMap<EventKey, EventHandlerGroup> eventHandlerGroups;
  private HashMap<String, EngineValue> attributes;
  private Patch patch;

  /**
   * Setup common test objects before each test.
   */
  @BeforeEach
  public void setUp() {
    mockGeometry = mock(EngineGeometry.class);
    patchName = "testPatch";
    eventHandlerGroups = new HashMap<>();
    attributes = new HashMap<>();
    EngineValue mockValue = mock(EngineValue.class);
    attributes.put("testAttribute", mockValue);

    EventKey stateKey = EventKey.of("testState", "testAttribute", "testEvent");
    EventHandlerGroup stateHandlerGroup = mock(EventHandlerGroup.class);
    eventHandlerGroups.put(stateKey, stateHandlerGroup);

    patch = new Patch(mockGeometry, createInitInfo(patchName, eventHandlerGroups, attributes));
  }

  /**
   * Convert attributes map to array using alphabetical ordering.
   */
  private static EngineValue[] toAttributesArray(
      Map<EventKey, EventHandlerGroup> handlers,
      Map<String, EngineValue> attributes) {
    Map<String, Integer> indexMap = toAttributeIndex(handlers, attributes);
    EngineValue[] result = new EngineValue[indexMap.size()];
    if (attributes != null) {
      for (Map.Entry<String, EngineValue> entry : attributes.entrySet()) {
        Integer index = indexMap.get(entry.getKey());
        if (index != null) {
          result[index] = entry.getValue();
        }
      }
    }
    return result;
  }

  /**
   * Create index map from attribute names using alphabetical ordering.
   */
  private static Map<String, Integer> toAttributeIndex(
      Map<EventKey, EventHandlerGroup> handlers,
      Map<String, EngineValue> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      if (handlers == null || handlers.isEmpty()) {
        return Collections.emptyMap();
      }
    }

    // Collect all attribute names
    Set<String> allNames = new HashSet<>();
    if (attributes != null) {
      allNames.addAll(attributes.keySet());
    }
    if (handlers != null) {
      for (EventHandlerGroup group : handlers.values()) {
        if (group != null) {
          for (EventHandler handler : group.getEventHandlers()) {
            allNames.add(handler.getAttributeName());
          }
        }
      }
    }

    // For test purposes, also include "newAttribute" which tests try to set dynamically
    allNames.add("newAttribute");

    if (allNames.isEmpty()) {
      return Collections.emptyMap();
    }

    // Sort alphabetically
    List<String> sortedNames = new ArrayList<>(allNames);
    Collections.sort(sortedNames);

    // Build index map
    Map<String, Integer> result = new HashMap<>();
    for (int i = 0; i < sortedNames.size(); i++) {
      result.put(sortedNames.get(i), i);
    }
    return Collections.unmodifiableMap(result);
  }

  /**
   * Create index-to-name array from attribute names using alphabetical ordering.
   */
  private static String[] toIndexToAttributeName(
      Map<EventKey, EventHandlerGroup> handlers,
      Map<String, EngineValue> attributes) {
    Map<String, Integer> indexMap = toAttributeIndex(handlers, attributes);
    String[] result = new String[indexMap.size()];
    for (Map.Entry<String, Integer> entry : indexMap.entrySet()) {
      result[entry.getValue()] = entry.getKey();
    }
    return result;
  }

  /**
   * Create EntityInitializationInfo from test parameters.
   */
  private static EntityInitializationInfo createInitInfo(
      String name,
      Map<EventKey, EventHandlerGroup> handlers,
      Map<String, EngineValue> attributes) {
    final EngineValue[] attributesArray = toAttributesArray(handlers, attributes);
    final Map<String, Integer> attributeIndex = toAttributeIndex(handlers, attributes);
    final String[] indexToAttributeName = toIndexToAttributeName(handlers, attributes);

    return new EntityInitializationInfo() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Map<EventKey, EventHandlerGroup> getEventHandlerGroups() {
        return handlers != null ? handlers : Collections.emptyMap();
      }

      @Override
      public EngineValue[] createAttributesArray() {
        return attributesArray;
      }

      @Override
      public Map<String, Integer> getAttributeNameToIndex() {
        return attributeIndex;
      }

      @Override
      public String[] getIndexToAttributeName() {
        return indexToAttributeName;
      }

      @Override
      public Map<String, boolean[]> getAttributesWithoutHandlersBySubstep() {
        return Collections.emptyMap();
      }

      @Override
      public Map<String, List<EventHandlerGroup>> getCommonHandlerCache() {
        return Collections.emptyMap();
      }

      @Override
      public Set<String> getSharedAttributeNames() {
        return Collections.emptySet();
      }

      @Override
      public boolean getUsesState() {
        return false;
      }

      @Override
      public int getStateIndex() {
        return -1;
      }
    };
  }

  /**
   * Test that the constructor correctly initializes the patch.
   */
  @Test
  public void testConstructor() {
    assertEquals(patchName, patch.getName());
    assertEquals(mockGeometry, patch.getGeometry().get());
    assertTrue(patch.getAttributeValue("testAttribute").isPresent());
  }

  /**
   * Test that the constructor handles null maps properly.
   */
  @Test
  public void testConstructorWithNullMaps() {
    Patch nullMapPatch = new Patch(mockGeometry, createInitInfo(patchName, null, null));

    assertNotNull(nullMapPatch.getEventHandlers());
    assertFalse(nullMapPatch.getEventHandlers().iterator().hasNext());
  }

  /**
   * Test that getPatchGeometry returns the correct geometry.
   */
  @Test
  public void testGetPatchGeometry() {
    assertEquals(mockGeometry, patch.getGeometry().get());
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
   * Test the event handler functionality.
   */
  @Test
  public void testEventHandlerManagement() {
    // Test getEventHandlers()
    Iterable<EventHandlerGroup> handlers = patch.getEventHandlers();
    assertTrue(handlers.iterator().hasNext());
    assertTrue(patch.getEventHandlers(
        EventKey.of("testState", "testAttribute", "testEvent")
    ).isPresent());

    // Test getEventHandlers with specific parameters
    Optional<EventHandlerGroup> retrievedGroup = patch.getEventHandlers(
        EventKey.of("testState", "testAttribute", "testEvent")
    );
    assertTrue(retrievedGroup.isPresent());

    // Test with non-existent event key
    Optional<EventHandlerGroup> nonExistentGroup = patch.getEventHandlers(
        EventKey.of("nonExistent", "nonExistent", "nonExistent")
    );
    assertEquals(Optional.empty(), nonExistentGroup);
  }
}
