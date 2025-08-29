package org.joshsim.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;


class NamedMapTest {

  @Test
  void testConstructorWithValidInputs() {
    // Arrange
    Map<String, String> target = Map.of("key1", "value1", "key2", "value2");

    // Act
    NamedMap namedMap = new NamedMap("TestName", target);

    // Assert
    assertEquals("TestName", namedMap.getName());
    assertEquals(2, namedMap.getTarget().size());
    assertEquals("value1", namedMap.getTarget().get("key1"));
    assertEquals("value2", namedMap.getTarget().get("key2"));
  }

  @Test
  void testConstructorWithEmptyMap() {
    // Arrange
    Map<String, String> target = Map.of();

    // Act
    NamedMap namedMap = new NamedMap("EmptyTest", target);

    // Assert
    assertEquals("EmptyTest", namedMap.getName());
    assertEquals(0, namedMap.getTarget().size());
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
    Map<String, String> originalTarget = new HashMap<>();
    originalTarget.put("key1", "value1");
    NamedMap namedMap = new NamedMap("TestName", originalTarget);

    // Act - modify original map
    originalTarget.put("key2", "value2");

    // Assert - NamedMap should not be affected
    assertEquals(1, namedMap.getTarget().size());

    // Act - try to modify returned map
    assertThrows(UnsupportedOperationException.class, () -> {
      namedMap.getTarget().put("key3", "value3");
    });
  }

  @Test
  void testEqualsAndHashCode() {
    // Arrange
    Map<String, String> target1 = Map.of("key1", "value1");
    Map<String, String> target2 = Map.of("key1", "value1");
    Map<String, String> target3 = Map.of("key2", "value2");

    NamedMap namedMap1 = new NamedMap("TestName", target1);
    NamedMap namedMap2 = new NamedMap("TestName", target2);
    NamedMap namedMap3 = new NamedMap("TestName", target3);
    NamedMap namedMap4 = new NamedMap("DifferentName", target1);

    // Assert equals
    assertEquals(namedMap1, namedMap2);
    assertNotEquals(namedMap1, namedMap3);
    assertNotEquals(namedMap1, namedMap4);

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
  }

}
