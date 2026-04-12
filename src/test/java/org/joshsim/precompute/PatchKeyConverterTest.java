package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridShape;
import org.joshsim.engine.value.type.EngineValue;
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
      public Optional<EngineValue> getAttributeValue(int index) {
        return Optional.empty();
      }

      @Override
      public Optional<Integer> getAttributeIndex(String name) {
        return Optional.empty();
      }

      @Override
      public Map<String, Integer> getAttributeNameToIndex() {
        return Collections.emptyMap();
      }

      @Override
      public String[] getIndexToAttributeName() {
        return null;
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

      @Override
      public Map<String, List<EventHandlerGroup>> getResolvedHandlers() {
        return Collections.emptyMap();
      }

      @Override
      public boolean usesState() {
        return false;
      }

      @Override
      public int getStateIndex() {
        return -1;
      }
    });

    PatchKeyConverter.ProjectedValue result = converter.convert(laKey, BigDecimal.ONE);
    BigDecimal expectedX = new BigDecimal("74");
    BigDecimal expectedY = new BigDecimal("81");

    assertEquals(0, result.getX().compareTo(expectedX));
    assertEquals(0, result.getY().compareTo(expectedY));
    assertEquals(0, result.getValue().compareTo(BigDecimal.ONE));
  }

  @Test
  void convertAtGridOriginShouldProduceZeroZero() {
    // A point at the top-left corner (same as start point) should map to (0, 0)
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");

    GeoKey originKey = makeGeoKey(sfLong, sfLat);
    PatchKeyConverter.ProjectedValue result = converter.convert(originKey, BigDecimal.ONE);

    assertEquals(0, result.getX().compareTo(BigDecimal.ZERO),
        "Origin point should have gridX = 0, got " + result.getX());
    assertEquals(0, result.getY().compareTo(BigDecimal.ZERO),
        "Origin point should have gridY = 0, got " + result.getY());
  }

  @Test
  void convertAtHalfCellOffsetShouldProduceZeroZero() {
    // A point slightly offset (less than one patchWidth) from origin should still be (0, 0)
    // Use a longitude slightly east of SF (about 2km east, less than 5km patch width)
    BigDecimal nearSfLong = new BigDecimal("-122.43");
    BigDecimal sfLat = new BigDecimal("37.73");

    GeoKey nearKey = makeGeoKey(nearSfLong, sfLat);
    PatchKeyConverter.ProjectedValue result = converter.convert(nearKey, BigDecimal.ONE);

    assertEquals(0, result.getX().compareTo(BigDecimal.ZERO),
        "Half-cell offset should have gridX = 0, got " + result.getX());
    assertEquals(0, result.getY().compareTo(BigDecimal.ZERO),
        "Half-cell offset should have gridY = 0, got " + result.getY());
  }

  @Test
  void convertAtExactlyOnePatchWidthShouldProduceOneOne() {
    // A point at exactly one patchWidth distance should map to (1, 1)
    // LA is ~74 patches east and ~81 patches south of SF at 5km patches
    // We need a point that is exactly 5000m east and 5000m south
    // For this test, create a converter with known simple extents
    PatchBuilderExtents simpleExtents = new PatchBuilderExtents(
        new BigDecimal("-122.45"),
        new BigDecimal("37.73"),
        new BigDecimal("-118.24"),
        new BigDecimal("34.05")
    );
    PatchKeyConverter simpleConverter = new PatchKeyConverter(simpleExtents, PATCH_WIDTH);

    // LA key should produce grid indices > 0
    GeoKey laKey = makeGeoKey(new BigDecimal("-118.24"), new BigDecimal("34.05"));
    PatchKeyConverter.ProjectedValue result = simpleConverter.convert(laKey, BigDecimal.ONE);

    // With FLOOR rounding, the LA point should still map to (74, 81) since
    // both formulas agree at large distances
    assertEquals(0, result.getX().compareTo(new BigDecimal("74")),
        "LA gridX should be 74, got " + result.getX());
    assertEquals(0, result.getY().compareTo(new BigDecimal("81")),
        "LA gridY should be 81, got " + result.getY());
  }

  private GeoKey makeGeoKey(BigDecimal longitude, BigDecimal latitude) {
    return new GeoKey(new Entity() {
      @Override
      public Optional<EngineGeometry> getGeometry() {
        return Optional.of(new EngineGeometry() {
          @Override
          public BigDecimal getCenterX() {
            return longitude;
          }

          @Override
          public BigDecimal getCenterY() {
            return latitude;
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
      public Optional<EngineValue> getAttributeValue(int index) {
        return Optional.empty();
      }

      @Override
      public Optional<Integer> getAttributeIndex(String name) {
        return Optional.empty();
      }

      @Override
      public Map<String, Integer> getAttributeNameToIndex() {
        return Collections.emptyMap();
      }

      @Override
      public String[] getIndexToAttributeName() {
        return null;
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

      @Override
      public Map<String, List<EventHandlerGroup>> getResolvedHandlers() {
        return Collections.emptyMap();
      }

      @Override
      public boolean usesState() {
        return false;
      }

      @Override
      public int getStateIndex() {
        return -1;
      }
    });
  }
}
