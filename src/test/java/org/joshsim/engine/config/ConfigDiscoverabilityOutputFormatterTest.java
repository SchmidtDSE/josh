/**
 * Unit tests for ConfigDiscoverabilityOutputFormatter class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ConfigDiscoverabilityOutputFormatter class.
 */
class ConfigDiscoverabilityOutputFormatterTest {

  @Test
  void testFormatEmptySet() {
    Set<DiscoveredConfigVar> emptySet = Collections.emptySet();
    String result = ConfigDiscoverabilityOutputFormatter.format(emptySet);
    assertEquals("", result);
  }

  @Test
  void testFormatSingleVariableWithoutDefault() {
    Set<DiscoveredConfigVar> vars = Set.of(
        new DiscoveredConfigVar("testVar")
    );
    String result = ConfigDiscoverabilityOutputFormatter.format(vars);
    assertEquals("testVar", result);
  }

  @Test
  void testFormatSingleVariableWithDefault() {
    Set<DiscoveredConfigVar> vars = Set.of(
        new DiscoveredConfigVar("testVar", "5 m")
    );
    String result = ConfigDiscoverabilityOutputFormatter.format(vars);
    assertEquals("testVar(5 m)", result);
  }

  @Test
  void testFormatMultipleVariablesMixed() {
    Set<DiscoveredConfigVar> vars = new HashSet<>(Arrays.asList(
        new DiscoveredConfigVar("testVar1", "5 m"),
        new DiscoveredConfigVar("testVar2"),
        new DiscoveredConfigVar("anotherVar", "10 count")
    ));

    String result = ConfigDiscoverabilityOutputFormatter.format(vars);

    // Variables should be sorted alphabetically
    String expected = "anotherVar(10 count)\ntestVar1(5 m)\ntestVar2";
    assertEquals(expected, result);
  }

  @Test
  void testFormatSortingOrder() {
    Set<DiscoveredConfigVar> vars = new HashSet<>(Arrays.asList(
        new DiscoveredConfigVar("zVariable"),
        new DiscoveredConfigVar("aVariable", "1 m"),
        new DiscoveredConfigVar("mVariable", "5 count")
    ));

    String result = ConfigDiscoverabilityOutputFormatter.format(vars);

    // Should be sorted alphabetically: aVariable, mVariable, zVariable
    String expected = "aVariable(1 m)\nmVariable(5 count)\nzVariable";
    assertEquals(expected, result);
  }

  @Test
  void testFormatAsLinesEmptySet() {
    Set<DiscoveredConfigVar> emptySet = Collections.emptySet();
    List<String> result = ConfigDiscoverabilityOutputFormatter.formatAsLines(emptySet);
    assertTrue(result.isEmpty());
  }

  @Test
  void testFormatAsLinesSingleVariable() {
    Set<DiscoveredConfigVar> vars = Set.of(
        new DiscoveredConfigVar("testVar", "5 m")
    );
    List<String> result = ConfigDiscoverabilityOutputFormatter.formatAsLines(vars);
    assertEquals(1, result.size());
    assertEquals("testVar(5 m)", result.get(0));
  }

  @Test
  void testFormatAsLinesMultipleVariables() {
    Set<DiscoveredConfigVar> vars = new HashSet<>(Arrays.asList(
        new DiscoveredConfigVar("testVar1", "5 m"),
        new DiscoveredConfigVar("testVar2"),
        new DiscoveredConfigVar("anotherVar", "10 count")
    ));

    List<String> result = ConfigDiscoverabilityOutputFormatter.formatAsLines(vars);

    assertEquals(3, result.size());
    // Should be sorted alphabetically
    assertEquals("anotherVar(10 count)", result.get(0));
    assertEquals("testVar1(5 m)", result.get(1));
    assertEquals("testVar2", result.get(2));
  }

  @Test
  void testFormatNullIterable() {
    assertThrows(IllegalArgumentException.class, () -> {
      ConfigDiscoverabilityOutputFormatter.format(null);
    });
  }

  @Test
  void testFormatAsLinesNullIterable() {
    assertThrows(IllegalArgumentException.class, () -> {
      ConfigDiscoverabilityOutputFormatter.formatAsLines(null);
    });
  }

  @Test
  void testFormatComplexVariableNames() {
    Set<DiscoveredConfigVar> vars = new HashSet<>(Arrays.asList(
        new DiscoveredConfigVar("example.testVar1", "5 meters"),
        new DiscoveredConfigVar("config.value2", "10.5%"),
        new DiscoveredConfigVar("simple")
    ));

    String result = ConfigDiscoverabilityOutputFormatter.format(vars);

    // Should handle dotted names correctly and sort them
    String expected = "config.value2(10.5%)\nexample.testVar1(5 meters)\nsimple";
    assertEquals(expected, result);
  }

  @Test
  void testFormatSpecialCharactersInDefaults() {
    Set<DiscoveredConfigVar> vars = new HashSet<>(Arrays.asList(
        new DiscoveredConfigVar("var1", "5.5 m/s"),
        new DiscoveredConfigVar("var2", "\"quoted string\""),
        new DiscoveredConfigVar("var3", "100%")
    ));

    String result = ConfigDiscoverabilityOutputFormatter.format(vars);

    String expected = "var1(5.5 m/s)\nvar2(\"quoted string\")\nvar3(100%)";
    assertEquals(expected, result);
  }

  @Test
  void testFormatConsistentOrdering() {
    Set<DiscoveredConfigVar> vars = new HashSet<>(Arrays.asList(
        new DiscoveredConfigVar("testVar1", "5 m"),
        new DiscoveredConfigVar("testVar2"),
        new DiscoveredConfigVar("anotherVar", "10 count")
    ));

    // Format multiple times to ensure consistent ordering
    String result1 = ConfigDiscoverabilityOutputFormatter.format(vars);
    String result2 = ConfigDiscoverabilityOutputFormatter.format(vars);
    String result3 = ConfigDiscoverabilityOutputFormatter.format(vars);

    assertEquals(result1, result2);
    assertEquals(result2, result3);
  }
}
