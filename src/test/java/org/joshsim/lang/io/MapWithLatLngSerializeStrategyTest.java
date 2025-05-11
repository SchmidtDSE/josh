
package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapWithLatLngSerializeStrategyTest {

  private MapWithLatLngSerializeStrategy strategy;
  private MapSerializeStrategy innerStrategy;
  private PatchBuilderExtents extents;

  @BeforeEach
  void setUp() {
    // Create real instances with test coordinates similar to GridBuilderExtentsBuilderTest
    BigDecimal topLeftX = new BigDecimal("-115.55");
    BigDecimal topLeftY = new BigDecimal("33.55");
    BigDecimal bottomRightX = new BigDecimal("-115.5");
    BigDecimal bottomRightY = new BigDecimal("33.5");
    
    extents = new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
    innerStrategy = new MapSerializeStrategy();
    strategy = new MapWithLatLngSerializeStrategy(extents, innerStrategy);
  }

  @Test
  @DisplayName("Should add latitude and longitude to record with geometry")
  void testGetRecordWithGeometry() {
    // Arrange
    Entity entity = mock(Entity.class);
    EngineGeometry geometry = mock(EngineGeometry.class);
    
    // Set x,y to values that should map to middle of the extents
    when(geometry.getCenterX()).thenReturn(new BigDecimal("0.5")); // 50% across grid
    when(geometry.getCenterY()).thenReturn(new BigDecimal("0.5")); // 50% down grid
    when(entity.getGeometry()).thenReturn(Optional.of(geometry));
    
    // Act
    Map<String, String> result = strategy.getRecord(entity);
    
    // Assert - expect coordinates halfway between extents bounds
    BigDecimal expectedLon = new BigDecimal("-115.525"); // halfway between -115.55 and -115.5
    BigDecimal expectedLat = new BigDecimal("33.525"); // halfway between 33.55 and 33.5
    
    assertEquals(expectedLon.toString(), result.get("position.longitude"));
    assertEquals(expectedLat.toString(), result.get("position.latitude"));
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
    
    when(geometry.getCenterX()).thenReturn(BigDecimal.ZERO);
    when(geometry.getCenterY()).thenReturn(BigDecimal.ZERO);
    when(entity.getGeometry()).thenReturn(Optional.of(geometry));
    
    // Add an export attribute that inner strategy should handle
    when(entity.getAttributeNames()).thenReturn(java.util.Set.of("export.name"));
    when(entity.getAttributeValue("export.name"))
        .thenReturn(Optional.of(mock(org.joshsim.engine.value.type.EngineValue.class)));
    
    // Act
    Map<String, String> result = strategy.getRecord(entity);
    
    // Assert - should contain both inner strategy and lat/lng fields
    assertEquals(true, result.containsKey("name")); // From inner strategy
    assertEquals(true, result.containsKey("position.longitude")); // From our strategy
    assertEquals(true, result.containsKey("position.latitude")); // From our strategy
  }
}
