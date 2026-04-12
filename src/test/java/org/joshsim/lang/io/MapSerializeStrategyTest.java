/**
 * Test for MapSerializeStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


/**
 * Tests the getRecord method of MapSerializeStrategy, which filters attributes of an entity
 * starting with "export." and maps those attributes and their values into a Map (String, String).
 */
class MapSerializeStrategyTest {

  @Test
  void testGetRecordWithExportAttributes() {
    // Arrange
    Entity entity = Mockito.mock(Entity.class);
    MapSerializeStrategy mapSerializeStrategy = new MapSerializeStrategy();

    Set<String> attributeNames = Set.of("export.name", "export.description", "other.attribute");
    when(entity.getAttributeNames()).thenReturn(attributeNames);

    EngineValue nameValue = Mockito.mock(EngineValue.class);
    EngineValue descriptionValue = Mockito.mock(EngineValue.class);
    when(nameValue.getAsString()).thenReturn("Sample Name");
    when(descriptionValue.getAsString()).thenReturn("Sample Description");

    when(entity.getGeometry()).thenReturn(Optional.empty());
    when(entity.getAttributeValue("export.name")).thenReturn(Optional.of(nameValue));
    when(entity.getAttributeValue("export.description")).thenReturn(
        Optional.of(descriptionValue)
    );
    when(entity.getAttributeValue("other.attribute")).thenReturn(Optional.empty());

    // Act
    Map<String, String> result = mapSerializeStrategy.getRecord(entity);

    // Assert
    assertEquals("Sample Name", result.get("name"));
    assertEquals("Sample Description", result.get("description"));
  }

  @Test
  void testGetRecordWithGeometry() {
    // Arrange
    Entity entity = Mockito.mock(Entity.class);
    EngineGeometry geometry = Mockito.mock(EngineGeometry.class);
    MapSerializeStrategy mapSerializeStrategy = new MapSerializeStrategy();

    when(geometry.getCenterX()).thenReturn(BigDecimal.valueOf(12));
    when(geometry.getCenterY()).thenReturn(BigDecimal.valueOf(34));

    Set<String> attributeNames = Set.of("export.name", "export.description", "other.attribute");
    when(entity.getAttributeNames()).thenReturn(attributeNames);

    EngineValue nameValue = Mockito.mock(EngineValue.class);
    EngineValue descriptionValue = Mockito.mock(EngineValue.class);
    when(nameValue.getAsString()).thenReturn("Sample Name");
    when(descriptionValue.getAsString()).thenReturn("Sample Description");

    when(entity.getGeometry()).thenReturn(Optional.of(geometry));
    when(entity.getAttributeValue("export.name")).thenReturn(Optional.of(nameValue));
    when(entity.getAttributeValue("export.description")).thenReturn(
        Optional.of(descriptionValue)
    );
    when(entity.getAttributeValue("other.attribute")).thenReturn(Optional.empty());

    // Act
    Map<String, String> result = mapSerializeStrategy.getRecord(entity);

    // Assert
    assertTrue(result.containsKey("position.x"));
    assertTrue(result.containsKey("position.y"));
  }

  @Test
  void testGetRecordWithNoExportAttributes() {
    // Arrange
    Entity entity = Mockito.mock(Entity.class);
    MapSerializeStrategy mapSerializeStrategy = new MapSerializeStrategy();

    Set<String> attributeNames = Set.of("other.attribute1", "other.attribute2");
    when(entity.getAttributeNames()).thenReturn(attributeNames);

    when(entity.getAttributeValue(anyString())).thenReturn(Optional.empty());

    // Act
    Map<String, String> result = mapSerializeStrategy.getRecord(entity);

    // Assert
    assertEquals(0, result.size());
  }

  @Test
  void testGetRecordWithEmptyAttributes() {
    // Arrange
    Entity entity = Mockito.mock(Entity.class);
    MapSerializeStrategy mapSerializeStrategy = new MapSerializeStrategy();

    when(entity.getAttributeNames()).thenReturn(Set.of());

    // Act
    Map<String, String> result = mapSerializeStrategy.getRecord(entity);

    // Assert
    assertEquals(0, result.size());
  }

  @Nested
  class PrecisionTests {

    @Test
    void testDefaultPrecisionTruncatesLongDecimals() {
      MapSerializeStrategy strategy = new MapSerializeStrategy();

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      when(value.getAsString()).thenReturn("3.141592653589793238462643383279");
      when(entity.getAttributeNames()).thenReturn(Set.of("export.pi"));
      when(entity.getAttributeValue("export.pi")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("3.141593", result.get("pi"));
    }

    @Test
    void testCustomPrecision() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(2);

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      when(value.getAsString()).thenReturn("3.14159");
      when(entity.getAttributeNames()).thenReturn(Set.of("export.pi"));
      when(entity.getAttributeValue("export.pi")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("3.14", result.get("pi"));
    }

    @Test
    void testUnlimitedPrecision() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(
          MapSerializeStrategy.UNLIMITED_PRECISION);

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      String longDecimal = "3.141592653589793238462643383279";
      when(value.getAsString()).thenReturn(longDecimal);
      when(entity.getAttributeNames()).thenReturn(Set.of("export.pi"));
      when(entity.getAttributeValue("export.pi")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals(longDecimal, result.get("pi"));
    }

    @Test
    void testIntegerValuesUnchanged() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(2);

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      when(value.getAsString()).thenReturn("42");
      when(entity.getAttributeNames()).thenReturn(Set.of("export.count"));
      when(entity.getAttributeValue("export.count")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("42", result.get("count"));
    }

    @Test
    void testStringValuesUnchanged() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(2);

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      when(value.getAsString()).thenReturn("hello world");
      when(entity.getAttributeNames()).thenReturn(Set.of("export.label"));
      when(entity.getAttributeValue("export.label")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("hello world", result.get("label"));
    }

    @Test
    void testValuesWithinPrecisionUnchanged() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(6);

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      when(value.getAsString()).thenReturn("1.5");
      when(entity.getAttributeNames()).thenReturn(Set.of("export.height"));
      when(entity.getAttributeValue("export.height")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("1.5", result.get("height"));
    }

    @Test
    void testGeometryPositionRounded() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(3);

      Entity entity = Mockito.mock(Entity.class);
      EngineGeometry geometry = Mockito.mock(EngineGeometry.class);
      when(geometry.getCenterX()).thenReturn(new BigDecimal("12.123456789"));
      when(geometry.getCenterY()).thenReturn(new BigDecimal("34.987654321"));
      when(entity.getAttributeNames()).thenReturn(Set.of());
      when(entity.getGeometry()).thenReturn(Optional.of(geometry));

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("12.123", result.get("position.x"));
      assertEquals("34.988", result.get("position.y"));
    }

    @Test
    void testTrailingZerosStripped() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(6);

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      when(value.getAsString()).thenReturn("1.10000000000000000");
      when(entity.getAttributeNames()).thenReturn(Set.of("export.val"));
      when(entity.getAttributeValue("export.val")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("1.1", result.get("val"));
    }

    @Test
    void testRoundingHalfUp() {
      MapSerializeStrategy strategy = new MapSerializeStrategy(2);

      Entity entity = Mockito.mock(Entity.class);
      EngineValue value = Mockito.mock(EngineValue.class);
      when(value.getAsString()).thenReturn("1.005");
      when(entity.getAttributeNames()).thenReturn(Set.of("export.val"));
      when(entity.getAttributeValue("export.val")).thenReturn(Optional.of(value));
      when(entity.getGeometry()).thenReturn(Optional.empty());

      Map<String, String> result = strategy.getRecord(entity);
      assertEquals("1.01", result.get("val"));
    }
  }
}
