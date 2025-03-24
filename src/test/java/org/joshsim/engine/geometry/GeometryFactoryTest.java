/**
 * Test cases for the GeometryFactory class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.shape.Rectangle;

/**
 * Unit tests for the GeometryFactory class.
 */
public class GeometryFactoryTest {

  private SpatialContext geoContext;
  private SpatialContext projectedContext;

  /**
   * Set up contexts for each test.
   */
  @BeforeEach
  public void setUp() {
    // Standard WGS84 geographic context
    geoContext = SpatialContext.GEO;
    
    // Create a projected (Cartesian) context
    SpatialContextFactory factory = new SpatialContextFactory();
    factory.geo = false; // Makes it a Cartesian/projected system
    projectedContext = factory.newSpatialContext();
  }

  @Nested
  class CreateSquareWithCenterTests {
    
    @Test
    @DisplayName("Create square with default context (WGS84)")
    public void testCreateSquareWithDefaultContext() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon, 
          GeometryFactory.getDefaultSpatialContext());
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertTrue(GeometryFactory.getDefaultSpatialContext().isGeo(), "Default context should be geographic");
    }

    @Test
    @DisplayName("Create square with explicit geographic context")
    public void testCreateSquareWithGeoContext() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon, geoContext);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertTrue(geoContext.isGeo(), "Should be a geographic context");
    }
    
    @Test
    @DisplayName("Create square with projected (Cartesian) context")
    public void testCreateSquareWithProjectedContext() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon, projectedContext);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertFalse(projectedContext.isGeo(), "Should be a projected (non-geographic) context");
    }
    
    @Test
    public void testCreateSquareWithZeroWidthGeoContext() {
      // Given
      BigDecimal width = BigDecimal.ZERO;
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon, geoContext);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      
      assertEquals(20.0, rect.getMinX(), 0.000001, "minLon should equal centerLon for zero width");
      assertEquals(20.0, rect.getMaxX(), 0.000001, "maxLon should equal centerLon for zero width");
      assertEquals(10.0, rect.getMinY(), 0.000001, "minLat should equal centerLat for zero width");
      assertEquals(10.0, rect.getMaxY(), 0.000001, "maxLat should equal centerLat for zero width");
    }
    
    @Test
    public void testCreateSquareWithZeroWidthProjectedContext() {
      // Given
      BigDecimal width = BigDecimal.ZERO;
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon, projectedContext);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      
      assertEquals(20.0, rect.getMinX(), 0.000001, "minLon should equal centerLon for zero width");
      assertEquals(20.0, rect.getMaxX(), 0.000001, "maxLon should equal centerLon for zero width");
      assertEquals(10.0, rect.getMinY(), 0.000001, "minLat should equal centerLat for zero width");
      assertEquals(10.0, rect.getMaxY(), 0.000001, "maxLat should equal centerLat for zero width");
    }
    
    @Test
    public void testCreateSquareWithPrecisionGeoContext() {
      // Given
      BigDecimal width = new BigDecimal("1.123456789");
      BigDecimal centerLat = new BigDecimal("10.987654321");
      BigDecimal centerLon = new BigDecimal("20.123456789");
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon, geoContext);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      
      double halfWidth = width.doubleValue() / 2.0;
      assertEquals(20.123456789 - halfWidth, rect.getMinX(), 0.000001);
      assertEquals(20.123456789 + halfWidth, rect.getMaxX(), 0.000001);
      assertEquals(10.987654321 - halfWidth, rect.getMinY(), 0.000001);
      assertEquals(10.987654321 + halfWidth, rect.getMaxY(), 0.000001);
    }
    
    @Test
    public void testCreateSquareWithPrecisionProjectedContext() {
      // Given
      BigDecimal width = new BigDecimal("1.123456789");
      BigDecimal centerLat = new BigDecimal("10.987654321");
      BigDecimal centerLon = new BigDecimal("20.123456789");
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon, projectedContext);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      
      double halfWidth = width.doubleValue() / 2.0;
      assertEquals(20.123456789 - halfWidth, rect.getMinX(), 0.000001);
      assertEquals(20.123456789 + halfWidth, rect.getMaxX(), 0.000001);
      assertEquals(10.987654321 - halfWidth, rect.getMinY(), 0.000001);
      assertEquals(10.987654321 + halfWidth, rect.getMaxY(), 0.000001);
    }
    
    @Test
    public void testNullContextThrowsException() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      SpatialContext nullContext = null;
      
      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerLat, centerLon, nullContext);
      }, "Should throw NullPointerException when context is null");
    }
    
    @Test
    public void testCreateSquareWithNullWidth() {
      // Given
      BigDecimal width = null;
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerLat, centerLon, geoContext);
      }, "Should throw NullPointerException when width is null");
    }
    
    @Test
    public void testCreateSquareWithNullCenterLatitude() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerLat = null;
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerLat, centerLon, geoContext);
      }, "Should throw NullPointerException when centerLatitude is null");
    }
    
    @Test
    public void testCreateSquareWithNullCenterLongitude() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = null;
      
      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerLat, centerLon, geoContext);
      }, "Should throw NullPointerException when centerLongitude is null");
    }
  }
  
  @Nested
  class CreateSquareWithCornersTests {
    
    @Test
    @DisplayName("Should create square from valid corner coordinates with default context")
    public void testCreateSquareWithValidCornersDefaultContext() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(11.0);
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(
          topLeftLat, topLeftLon, bottomRightLat, bottomRightLon, 
          GeometryFactory.getDefaultSpatialContext()
      );
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
    }
    
    @Test
    @DisplayName("Should create square from valid corner coordinates with geographic context")
    public void testCreateSquareWithValidCornersGeoContext() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(11.0);
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(
          topLeftLat, topLeftLon, bottomRightLat, bottomRightLon, geoContext
      );
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
    }
    
    @Test
    @DisplayName("Should create square from valid corner coordinates with projected context")
    public void testCreateSquareWithValidCornersProjectedContext() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(11.0);
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(
          topLeftLat, topLeftLon, bottomRightLat, bottomRightLon, projectedContext
      );
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertFalse(projectedContext.isGeo(), "Should be a projected (non-geographic) context");
    }
    
    @Test
    public void testCreateSquareWithNonSquareCoordinatesGeoContext() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(12.0);  // Height = 3
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);  // Width = 2
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When/Then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        GeometryFactory.createSquare(
            topLeftLat,
            topLeftLon,
            bottomRightLat,
            bottomRightLon,
            geoContext
        );
      }, "Should throw IllegalArgumentException when coordinates don't form a square");
      
      assertTrue(exception.getMessage().contains("don't form a square"));
    }
    
    @Test
    public void testCreateSquareWithNonSquareCoordinatesProjectedContext() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(12.0);  // Height = 3
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);  // Width = 2
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When/Then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        GeometryFactory.createSquare(
            topLeftLat,
            topLeftLon,
            bottomRightLat,
            bottomRightLon,
            projectedContext
        );
      }, "Should throw IllegalArgumentException when coordinates don't form a square");
      
      assertTrue(exception.getMessage().contains("don't form a square"));
    }
    
    @Test
    public void testNullContextWithCornersThrowsException() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(11.0);
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      SpatialContext nullContext = null;
      
      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(
          topLeftLat, topLeftLon, bottomRightLat, bottomRightLon, nullContext
        );
      }, "Should throw NullPointerException when context is null");
    }
  }
  
  @Nested
  class SpatialContextTests {
    
    @Test
    public void testDefaultContextMethod() {
      // Verify the default context is SpatialContext.GEO
      SpatialContext defaultContext = GeometryFactory.getDefaultSpatialContext();
      assertEquals(SpatialContext.GEO, defaultContext);
      assertTrue(defaultContext.isGeo(), "Default context should be geographic");
    }
    
    @Test
    public void testGeoAndProjectedContextDifferences() {
      assertTrue(geoContext.isGeo(), "Geographic context should have isGeo() = true");
      assertFalse(projectedContext.isGeo(), "Projected context should have isGeo() = false");
    }
  }
}