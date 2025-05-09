package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridShape;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.geometry.EarthShape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class PatchKeyConverterTest {
  
  private PatchKeyConverter converter;
  private static final BigDecimal PATCH_WIDTH = new BigDecimal("5000"); // 5km patches

  @BeforeEach
  void setUp() {
    // Using LA and SF coordinates from HaversineUtilTest
    BigDecimal laLong = new BigDecimal("-118.24");
    BigDecimal laLat = new BigDecimal("34.05");
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    
    PatchBuilderExtents extents = new PatchBuilderExtents(
        sfLong,  // topLeftX (west)
        sfLat,   // topLeftY (north)
        laLong,  // bottomRightX (east)
        laLat    // bottomRightY (south)
    );
    
    converter = new PatchKeyConverter(extents, PATCH_WIDTH);
  }

  @Test
  void convertShouldCalculateCorrectGridPosition() {
    // Create a GeoKey at LA's position
    GeoKey laKey = new GeoKey(new Entity() {
      @Override
      public Optional<EngineGeometry> getGeometry() {
        return Optional.of(new EngineGeometry() {
          @Override
          public BigDecimal getCenterX() {
            return new BigDecimal("-118.24");
          }
          
          @Override
          public BigDecimal getCenterY() {
            return new BigDecimal("34.05");
          }

          @Override
          public EngineGeometry getCenter() {
            return this;
          }

          @Override
          public boolean intersects(EngineGeometry other) {
            return false;
          }

          @Override
          public boolean intersects(BigDecimal locationX, BigDecimal locationY) {
            return false;
          }

          @Override
          public EarthShape getOnEarth() {
            return null;
          }

          @Override
          public GridShape getOnGrid() {
            return null;
          }
        });
      }
      
      @Override
      public String getName() {
        return "TestPatch";
      }

      @Override
      public Optional<EngineValue> getAttributeValue(String name) {
        return Optional.empty();
      }

      @Override
      public Set<String> getAttributeNames() {
        return Set.of();
      }

      @Override
      public EntityType getEntityType() {
        return null;
      }

      @Override
      public Entity freeze() {
        return null;
      }

      @Override
      public Optional<GeoKey> getKey() {
        return Optional.empty();
      }
    });

    PatchKeyConverter.ProjectedValue result = converter.convert(laKey, BigDecimal.ONE);
    BigDecimal expectedX = new BigDecimal("74");
    BigDecimal expectedY = new BigDecimal("81");
    
    assertEquals(0, result.getX().compareTo(expectedX));
    assertEquals(0, result.getY().compareTo(expectedY));
    assertEquals(0, result.getValue().compareTo(BigDecimal.ONE));
  }
}
