package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;


class MapToMemoryStringConverterTest {

  /**
   * Tests for MapToMemoryStringConverter's `convert` method.
   * The `convert` method takes a name and a map, converts map entries to a formatted string,
   * and ensures replacing tabs or newlines within map values for safety.
   * The result combines the name and map entries into a structured output.
   */

  @Test
  void testConvertWithValidNameAndMap() {
    // Arrange
    String name = "TestName";
    Map<String, String> target = Map.of("key1", "value1", "key2", "value2");

    // Act
    String result = MapToMemoryStringConverter.convert(name, target);

    // Assert
    assertEquals("TestName:key1=value1\tkey2=value2", result);
  }

  @Test
  void testConvertHandlesTabsAndNewlinesInValues() {
    // Arrange
    String name = "FormattedName";
    Map<String, String> target = Map.of(
        "key1",
        "value\tWith\tTabs",
        "key2",
        "value\nWith\nNewlines"
    );

    // Act
    String result = MapToMemoryStringConverter.convert(name, target);

    // Assert
    assertEquals(
        "FormattedName:key1=value    With    Tabs\tkey2=value    With    Newlines",
        result
    );
  }

  @Test
  void testConvertEmptyMap() {
    // Arrange
    String name = "EmptyMapTest";
    Map<String, String> target = Map.of();

    // Act
    String result = MapToMemoryStringConverter.convert(name, target);

    // Assert
    assertEquals("EmptyMapTest:", result);
  }

  @Test
  void testConvertSingleEntryMap() {
    // Arrange
    String name = "SingleEntry";
    Map<String, String> target = Map.of("keyOnly", "valueOnly");

    // Act
    String result = MapToMemoryStringConverter.convert(name, target);

    // Assert
    assertEquals("SingleEntry:keyOnly=valueOnly", result);
  }
}
