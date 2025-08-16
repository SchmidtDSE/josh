/**
 * Unit tests for DiscoveredConfigVar class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for the DiscoveredConfigVar class.
 */
class DiscoveredConfigVarTest {

  @Test
  void testConstructorWithoutDefault() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar");

    assertEquals("testVar", var.getName());
    assertEquals(Optional.empty(), var.getDefaultValue());
    assertFalse(var.getDefaultValue().isPresent());
  }

  @Test
  void testConstructorWithDefault() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar", "5 m");

    assertEquals("testVar", var.getName());
    assertEquals(Optional.of("5 m"), var.getDefaultValue());
    assertTrue(var.getDefaultValue().isPresent());
    assertEquals("5 m", var.getDefaultValue().get());
  }

  @Test
  void testConstructorWithEmptyName() {
    assertThrows(IllegalArgumentException.class, () -> {
      new DiscoveredConfigVar("");
    });
  }

  @Test
  void testConstructorWithWhitespaceName() {
    assertThrows(IllegalArgumentException.class, () -> {
      new DiscoveredConfigVar("   ");
    });
  }

  @Test
  void testDescribeWithoutDefault() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar");
    assertEquals("testVar", var.describe());
  }

  @Test
  void testDescribeWithDefault() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar", "5 m");
    assertEquals("testVar(5 m)", var.describe());
  }

  @Test
  void testDescribeWithComplexDefault() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("complexVar", "10.5 meters");
    assertEquals("complexVar(10.5 meters)", var.describe());
  }

  @Test
  void testToStringWithoutDefault() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar");
    assertEquals("DiscoveredConfigVar: testVar", var.toString());
  }

  @Test
  void testToStringWithDefault() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar", "5 m");
    assertEquals("DiscoveredConfigVar: testVar(5 m)", var.toString());
  }

  @Test
  void testEqualsAndHashCode() {
    DiscoveredConfigVar var1 = new DiscoveredConfigVar("testVar", "5 m");
    DiscoveredConfigVar var2 = new DiscoveredConfigVar("testVar", "5 m");
    DiscoveredConfigVar var3 = new DiscoveredConfigVar("testVar", "10 m");
    DiscoveredConfigVar var4 = new DiscoveredConfigVar("otherVar", "5 m");

    // Test equals
    assertEquals(var1, var2);
    assertNotEquals(var1, var3);
    assertNotEquals(var1, var4);

    DiscoveredConfigVar var5 = new DiscoveredConfigVar("testVar");
    assertNotEquals(var1, var5);
    assertNotEquals(var1, null);
    assertNotEquals(var1, "string");

    // Test hash code consistency
    assertEquals(var1.hashCode(), var2.hashCode());
  }

  @Test
  void testEqualsWithoutDefaults() {
    DiscoveredConfigVar var1 = new DiscoveredConfigVar("testVar");
    DiscoveredConfigVar var2 = new DiscoveredConfigVar("testVar");
    DiscoveredConfigVar var3 = new DiscoveredConfigVar("otherVar");

    assertEquals(var1, var2);
    assertNotEquals(var1, var3);
    assertEquals(var1.hashCode(), var2.hashCode());
  }

  @Test
  void testNameTrimming() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("  testVar  ");
    assertEquals("testVar", var.getName());
    assertEquals("testVar", var.describe());
  }

  @Test
  void testDefaultValueTrimming() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar", "  5 m  ");
    assertEquals("5 m", var.getDefaultValue().get());
    assertEquals("testVar(5 m)", var.describe());
  }

  @Test
  void testSameObjectEquals() {
    DiscoveredConfigVar var = new DiscoveredConfigVar("testVar", "5 m");
    assertEquals(var, var);
  }
}
