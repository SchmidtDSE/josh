package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapWithLatLngSerializeStrategyTest {

  private MapWithLatLngSerializeStrategy strategy;
  private MapSerializeStrategy innerStrategy;
  private PatchBuilderExtents extents;
  private BigDecimal width;

  @BeforeEach
  void setUp() {
    BigDecimal topLeftX = new BigDecimal("-123");
    BigDecimal topLeftY = new BigDecimal("45");
    BigDecimal bottomRightX = new BigDecimal("-124");
    BigDecimal bottomRightY = new BigDecimal("46");
    width = new BigDecimal("1000");
    extents = new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
    innerStrategy = new MapSerializeStrategy();
    strategy = new MapWithLatLngSerializeStrategy(extents, width, innerStrategy);
  }

  @Test
  @DisplayName("Should add latitude and longitude to record with geometry")
  void testGetRecordWithGeometry() {
    // Arrange
    Entity entity = mock(Entity.class);
    EngineGeometry geometry = mock(EngineGeometry.class);

    // Set up mock geometry with grid coordinates (1, 1)
    when(geometry.getCenterX()).thenReturn(BigDecimal.ONE);
    when(geometry.getCenterY()).thenReturn(BigDecimal.ONE);
    when(entity.getGeometry()).thenReturn(Optional.of(geometry));

    MapSerializeStrategy innerStrategy = new MapSerializeStrategy();
    MapWithLatLngSerializeStrategy strategy = new MapWithLatLngSerializeStrategy(
        extents,
        width,
        innerStrategy
    );

    // Act
    Map<String, String> result = strategy.getRecord(entity);

    // Assert
    assertTrue(result.containsKey("position.longitude"));
    assertTrue(result.containsKey("position.latitude"));
    // Values should be transformed from grid space to geo space
    assertNotEquals("1", result.get("position.longitude"));
    assertNotEquals("1", result.get("position.latitude"));
  }

  @Test
  @DisplayName("Should handle entity without geometry")
  void testGetRecordWithoutGeometry() {
    // Arrange
    Entity entity = mock(Entity.class);
    when(entity.getGeometry()).thenReturn(Optional.empty());

    // Act
    Map<String, String> result = strategy.getRecord(entity);

    // Assert - should not have lat/lng fields
    assertEquals(false, result.containsKey("position.longitude"));
    assertEquals(false, result.containsKey("position.latitude"));
  }

  @Test
  @DisplayName("Should preserve inner strategy records")
  void testPreservesInnerStrategyRecords() {
    // Arrange
    Entity entity = mock(Entity.class);
    EngineGeometry geometry = mock(EngineGeometry.class);
    EngineValue nameValue = mock(EngineValue.class);

    when(geometry.getCenterX()).thenReturn(BigDecimal.ZERO);
    when(geometry.getCenterY()).thenReturn(BigDecimal.ZERO);
    when(entity.getGeometry()).thenReturn(Optional.of(geometry));
    when(nameValue.getAsString()).thenReturn("Test Name");

    // Add an export attribute that inner strategy should handle
    when(entity.getAttributeNames()).thenReturn(java.util.Set.of("export.name"));
    when(entity.getAttributeValue("export.name")).thenReturn(Optional.of(nameValue));

    // Act
    Map<String, String> result = strategy.getRecord(entity);

    // Assert - should contain both inner strategy and lat/lng fields
    assertTrue(result.containsKey("name")); // From inner strategy
    assertTrue(result.containsKey("position.longitude")); // From our strategy
    assertTrue(result.containsKey("position.latitude")); // From our strategy
    assertEquals("Test Name", result.get("name")); // Verify inner strategy value
  }
}
