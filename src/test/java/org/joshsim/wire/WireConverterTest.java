package org.joshsim.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;


class WireConverterTest {

  /**
   * Tests for WireConverter's serialization and deserialization methods.
   * The serialization method converts NamedMap objects to wire format strings,
   * and the deserialization method converts wire format strings back to NamedMap objects.
   * Special characters in values are handled safely for round-trip compatibility.
   */

  @Test
  void testSerializeToStringWithValidNamedMap() {
    // Arrange
    Map<String, String> target = Map.of("key1", "value1", "key2", "value2");
    NamedMap namedMap = new NamedMap("TestName", target);

    // Act
    String result = WireConverter.serializeToString(namedMap);

    // Assert
    assertTrue(result.startsWith("TestName:"));
    assertTrue(result.contains("key1=value1"));
    assertTrue(result.contains("key2=value2"));
  }

  @Test
  void testSerializeToStringHandlesTabsAndNewlinesInValues() {
    // Arrange
    Map<String, String> target = Map.of(
        "key1", "value\tWith\tTabs",
        "key2", "value\nWith\nNewlines"
    );
    NamedMap namedMap = new NamedMap("FormattedName", target);

    // Act
    String result = WireConverter.serializeToString(namedMap);

    // Assert
    assertTrue(result.startsWith("FormattedName:"));
    assertTrue(result.contains("key1=value    With    Tabs"));
    assertTrue(result.contains("key2=value    With    Newlines"));
  }

  @Test
  void testSerializeToStringEmptyMap() {
    // Arrange
    Map<String, String> target = Map.of();
    NamedMap namedMap = new NamedMap("EmptyMapTest", target);

    // Act
    String result = WireConverter.serializeToString(namedMap);

    // Assert
    assertEquals("EmptyMapTest:", result);
  }

  @Test
  void testSerializeToStringSingleEntryMap() {
    // Arrange
    Map<String, String> target = Map.of("keyOnly", "valueOnly");
    NamedMap namedMap = new NamedMap("SingleEntry", target);

    // Act
    String result = WireConverter.serializeToString(namedMap);

    // Assert
    assertEquals("SingleEntry:keyOnly=valueOnly", result);
  }

  @Test
  void testSerializeToStringThrowsOnNullNamedMap() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.serializeToString(null);
    });
  }

  @Test
  void testDeserializeFromStringWithValidFormat() {
    // Arrange
    String wireFormat = "TestName:key1=value1\tkey2=value2";

    // Act
    NamedMap result = WireConverter.deserializeFromString(wireFormat);

    // Assert
    assertEquals("TestName", result.getName());
    assertEquals(2, result.getTarget().size());
    assertEquals("value1", result.getTarget().get("key1"));
    assertEquals("value2", result.getTarget().get("key2"));
  }

  @Test
  void testDeserializeFromStringEmptyData() {
    // Arrange
    String wireFormat = "EmptyData:";

    // Act
    NamedMap result = WireConverter.deserializeFromString(wireFormat);

    // Assert
    assertEquals("EmptyData", result.getName());
    assertEquals(0, result.getTarget().size());
  }

  @Test
  void testDeserializeFromStringSingleEntry() {
    // Arrange
    String wireFormat = "SingleEntry:onlyKey=onlyValue";

    // Act
    NamedMap result = WireConverter.deserializeFromString(wireFormat);

    // Assert
    assertEquals("SingleEntry", result.getName());
    assertEquals(1, result.getTarget().size());
    assertEquals("onlyValue", result.getTarget().get("onlyKey"));
  }

  @Test
  void testDeserializeFromStringHandlesEmptyValues() {
    // Arrange
    String wireFormat = "TestName:key1=\tkey2=value2";

    // Act
    NamedMap result = WireConverter.deserializeFromString(wireFormat);

    // Assert
    assertEquals("TestName", result.getName());
    assertEquals(2, result.getTarget().size());
    assertEquals("", result.getTarget().get("key1"));
    assertEquals("value2", result.getTarget().get("key2"));
  }

  @Test
  void testDeserializeFromStringThrowsOnNullInput() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.deserializeFromString(null);
    });
  }

  @Test
  void testDeserializeFromStringThrowsOnEmptyInput() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.deserializeFromString("");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.deserializeFromString("   ");
    });
  }

  @Test
  void testDeserializeFromStringThrowsOnMissingColon() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.deserializeFromString("InvalidFormatNoColon");
    });
  }

  @Test
  void testDeserializeFromStringThrowsOnEmptyName() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.deserializeFromString(":key=value");
    });
  }

  @Test
  void testDeserializeFromStringThrowsOnInvalidKeyValuePair() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.deserializeFromString("TestName:invalidpair");
    });
  }

  @Test
  void testDeserializeFromStringThrowsOnEmptyKey() {
    // Assert
    assertThrows(IllegalArgumentException.class, () -> {
      WireConverter.deserializeFromString("TestName:=value");
    });
  }

  @Test
  void testRoundTripSerializationDeserialization() {
    // Arrange
    Map<String, String> originalTarget = new HashMap<>();
    originalTarget.put("key1", "value1");
    originalTarget.put("key2", "value with spaces");
    originalTarget.put("key3", "");
    originalTarget.put("specialChars", "value\twith\ttabs\nand\nnewlines");

    NamedMap originalNamedMap = new NamedMap("RoundTripTest", originalTarget);

    // Act
    String serialized = WireConverter.serializeToString(originalNamedMap);
    NamedMap deserialized = WireConverter.deserializeFromString(serialized);

    // Assert
    assertEquals(originalNamedMap.getName(), deserialized.getName());
    assertEquals(originalTarget.size(), deserialized.getTarget().size());

    // Special characters should be replaced in round trip
    assertEquals("value1", deserialized.getTarget().get("key1"));
    assertEquals("value with spaces", deserialized.getTarget().get("key2"));
    assertEquals("", deserialized.getTarget().get("key3"));
    assertEquals("value    with    tabs    and    newlines",
                 deserialized.getTarget().get("specialChars"));
  }

  @Test
  void testRoundTripWithComplexNames() {
    // Arrange
    Map<String, String> target = Map.of("test", "value");
    NamedMap namedMap = new NamedMap("Complex-Name_With.Special123", target);

    // Act
    String serialized = WireConverter.serializeToString(namedMap);
    NamedMap deserialized = WireConverter.deserializeFromString(serialized);

    // Assert
    assertEquals("Complex-Name_With.Special123", deserialized.getName());
    assertEquals("value", deserialized.getTarget().get("test"));
  }

}
