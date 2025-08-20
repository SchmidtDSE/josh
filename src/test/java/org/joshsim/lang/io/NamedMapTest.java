package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;


class NamedMapTest {

  /**
   * Tests for NamedMap value object functionality.
   * Tests constructor validation, immutability, getters, and defensive copying.
   */

  @Test
  void testConstructorWithValidInputs() {
    // Arrange
    String name = "TestName";
    Map<String, String> target = Map.of("key1", "value1", "key2", "value2");

    // Act
    NamedMap namedMap = new NamedMap(name, target);

    // Assert
    assertEquals("TestName", namedMap.getName());
    assertEquals(2, namedMap.getTarget().size());
    assertEquals("value1", namedMap.getTarget().get("key1"));
    assertEquals("value2", namedMap.getTarget().get("key2"));
  }

  @Test
  void testConstructorWithEmptyMap() {
    // Arrange
    String name = "EmptyTest";
    Map<String, String> target = Map.of();

    // Act
    NamedMap namedMap = new NamedMap(name, target);

    // Assert
    assertEquals("EmptyTest", namedMap.getName());
    assertEquals(0, namedMap.getTarget().size());
  }

  @Test
  void testConstructorThrowsOnNullName() {
    // Arrange
    Map<String, String> target = Map.of("key", "value");

    // Assert
    assertThrows(NullPointerException.class, () -> {
      new NamedMap(null, target);
    });
  }

  @Test
  void testConstructorThrowsOnEmptyName() {
    // Arrange
    Map<String, String> target = Map.of("key", "value");

    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      new NamedMap("", target);
    });
    
    assertThrows(IllegalArgumentException.class, () -> {
      new NamedMap("   ", target);
    });
  }

  @Test
  void testConstructorThrowsOnNullTarget() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      new NamedMap("TestName", null);
    });
  }

  @Test
  void testTargetMapIsImmutable() {
    // Arrange
    Map<String, String> originalMap = new HashMap<>();
    originalMap.put("key1", "value1");
    NamedMap namedMap = new NamedMap("TestName", originalMap);

    // Act & Assert
    assertThrows(UnsupportedOperationException.class, () -> {
      namedMap.getTarget().put("key2", "value2");
    });
  }

  @Test
  void testDefensiveCopying() {
    // Arrange
    Map<String, String> mutableMap = new HashMap<>();
    mutableMap.put("key1", "value1");
    NamedMap namedMap = new NamedMap("TestName", mutableMap);

    // Act - Modify original map after NamedMap creation
    mutableMap.put("key2", "value2");

    // Assert - NamedMap should not be affected
    assertEquals(1, namedMap.getTarget().size());
    assertEquals("value1", namedMap.getTarget().get("key1"));
    assertTrue(!namedMap.getTarget().containsKey("key2"));
  }

  @Test
  void testGetTargetReturnsSameImmutableInstance() {
    // Arrange
    Map<String, String> target = Map.of("key", "value");
    NamedMap namedMap = new NamedMap("TestName", target);

    // Act
    Map<String, String> target1 = namedMap.getTarget();
    Map<String, String> target2 = namedMap.getTarget();

    // Assert - Should return same immutable instance for efficiency
    assertEquals(target1, target2);
    assertEquals(target1, target2);
  }

  @Test
  void testEqualsAndHashCode() {
    // Arrange
    Map<String, String> target1 = Map.of("key1", "value1", "key2", "value2");
    Map<String, String> target2 = Map.of("key1", "value1", "key2", "value2");
    Map<String, String> target3 = Map.of("key1", "value1");

    NamedMap namedMap1 = new NamedMap("TestName", target1);
    NamedMap namedMap2 = new NamedMap("TestName", target2);
    NamedMap namedMap3 = new NamedMap("DifferentName", target1);
    NamedMap namedMap4 = new NamedMap("TestName", target3);

    // Assert equals
    assertEquals(namedMap1, namedMap2);
    assertTrue(!namedMap1.equals(namedMap3));
    assertTrue(!namedMap1.equals(namedMap4));
    assertTrue(!namedMap1.equals(null));
    assertTrue(!namedMap1.equals("string"));

    // Assert hashCode consistency
    assertEquals(namedMap1.hashCode(), namedMap2.hashCode());
  }

  @Test
  void testToString() {
    // Arrange
    Map<String, String> target = Map.of("key1", "value1");
    NamedMap namedMap = new NamedMap("TestName", target);

    // Act
    String result = namedMap.toString();

    // Assert
    assertTrue(result.contains("TestName"));
    assertTrue(result.contains("key1"));
    assertTrue(result.contains("value1"));
    assertTrue(result.startsWith("NamedMap{"));
  }
}