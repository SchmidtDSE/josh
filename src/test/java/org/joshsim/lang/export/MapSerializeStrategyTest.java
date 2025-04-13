/**
 * Test for MapSerializeStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.value.type.EngineValue;
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

    when(entity.getAttributeValue("export.name")).thenReturn(Optional.of(nameValue));
    when(entity.getAttributeValue("export.description")).thenReturn(
        Optional.of(descriptionValue)
    );
    when(entity.getAttributeValue("other.attribute")).thenReturn(Optional.empty());

    // Act
    Map<String, String> result = mapSerializeStrategy.getRecord(entity);

    // Assert
    assertEquals(2, result.size());
    assertEquals("Sample Name", result.get("export.name"));
    assertEquals("Sample Description", result.get("export.description"));
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
}
