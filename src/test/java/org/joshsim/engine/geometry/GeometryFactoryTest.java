/**
 * Test cases for the GeometryFactory class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Rectangle;




/**
 * Unit tests for the GeometryFactory class.
 */
public class GeometryFactoryTest {

  private SpatialContext ctx;

  @BeforeEach
  public void setUp() {
    ctx = SpatialContext.GEO;
  }

  @Nested
  class CreateSquareWithWidthAndCenterTests {
    
    @Test
    public void testCreateSquareWithWidth() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      assertNotNull(geometry.shape, "Shape should not be null");
      assertTrue(geometry.shape instanceof Rectangle, "Shape should be a Rectangle");
      
      Rectangle rect = (Rectangle)geometry.shape;
      assertEquals(19.0, rect.getMinX(), 0.000001, "minLon should be centerLon - halfWidth");
      assertEquals(21.0, rect.getMaxX(), 0.000001, "maxLon should be centerLon + halfWidth");
      assertEquals(9.0, rect.getMinY(), 0.000001, "minLat should be centerLat - halfWidth");
      assertEquals(11.0, rect.getMaxY(), 0.000001, "maxLat should be centerLat + halfWidth");
      
      assertEquals(
          BigDecimal.valueOf(10.0), 
          geometry.getCenterLatitude(), 
          "Center latitude should match input"
      );
      assertEquals(
          BigDecimal.valueOf(20.0), 
          geometry.getCenterLongitude(), 
          "Center longitude should match input"
      );
    }
    
    @Test
    public void testCreateSquareWithZeroWidth() {
      // Given
      BigDecimal width = BigDecimal.ZERO;
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle)geometry.shape;
      
      assertEquals(20.0, rect.getMinX(), 0.000001, "minLon should equal maxLon for zero width");
      assertEquals(20.0, rect.getMaxX(), 0.000001, "maxLon should equal minLon for zero width");
      assertEquals(10.0, rect.getMinY(), 0.000001, "minLat should equal maxLat for zero width");
      assertEquals(10.0, rect.getMaxY(), 0.000001, "maxLat should equal minLat for zero width");
    }
    
    @Test
    public void testCreateSquareWithPrecision() {
      // Given
      BigDecimal width = new BigDecimal("1.123456789");
      BigDecimal centerLat = new BigDecimal("10.987654321");
      BigDecimal centerLon = new BigDecimal("20.123456789");
      
      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerLat, centerLon);
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle)geometry.shape;
      
      double halfWidth = width.doubleValue() / 2.0;
      assertEquals(20.123456789 - halfWidth, rect.getMinX(), 0.000001);
      assertEquals(20.123456789 + halfWidth, rect.getMaxX(), 0.000001);
      assertEquals(10.987654321 - halfWidth, rect.getMinY(), 0.000001);
      assertEquals(10.987654321 + halfWidth, rect.getMaxY(), 0.000001);
    }
    
    @Test
    public void testCreateSquareWithNullWidth() {
      // Given
      BigDecimal width = null;
      BigDecimal centerLat = BigDecimal.valueOf(10.0);
      BigDecimal centerLon = BigDecimal.valueOf(20.0);
      
      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerLat, centerLon);
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
        GeometryFactory.createSquare(width, centerLat, centerLon);
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
        GeometryFactory.createSquare(width, centerLat, centerLon);
      }, "Should throw NullPointerException when centerLongitude is null");
    }
  }
  
  @Nested
  class CreateSquareWithCornersTests {
    
    @Test
    @DisplayName("Should create square from valid corner coordinates")
    public void testCreateSquareWithValidCorners() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(11.0);
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(
          topLeftLat, topLeftLon, bottomRightLat, bottomRightLon
      );
      
      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle)geometry.shape;
      
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      
      assertEquals(BigDecimal.valueOf(10.0), geometry.getCenterLatitude(), "Center latitude should be correct");
      assertEquals(BigDecimal.valueOf(20.0), geometry.getCenterLongitude(), "Center longitude should be correct");
    }
    
    @Test
    public void testCreateSquareWithNonSquareCoordinates() {
      // Given
      BigDecimal topLeftLat = BigDecimal.valueOf(12.0);  // Height = 3
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);  // Width = 2
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When/Then
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        GeometryFactory.createSquare(
          topLeftLat, topLeftLon, bottomRightLat, bottomRightLon);
      }, "Should throw IllegalArgumentException when coordinates don't form a square");
      
      assertTrue(exception.getMessage().contains("don't form a square"));
      assertTrue(exception.getMessage().contains("width=2.0, height=3.0"));
    }
    
    @Test
    public void testCreateSquareWithinTolerance() {
      // Given - height differs from width by less than tolerance
      BigDecimal topLeftLat = BigDecimal.valueOf(11.0000005);
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When
      Geometry geometry = GeometryFactory.createSquare(
        topLeftLat, topLeftLon, bottomRightLat, bottomRightLon);
      
      // Then
      assertNotNull(geometry, "Geometry should be created when within tolerance");
      // Width = 2.0, height = 2.0000005, difference is < 0.000001
    }
    
    @Test
    public void testCreateSquareWithNullTopLeftLatitude() {
      // Given
      BigDecimal topLeftLat = null;
      BigDecimal topLeftLon = BigDecimal.valueOf(19.0);
      BigDecimal bottomRightLat = BigDecimal.valueOf(9.0);
      BigDecimal bottomRightLon = BigDecimal.valueOf(21.0);
      
      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(
          topLeftLat, topLeftLon, bottomRightLat, bottomRightLon);
      }, "Should throw NullPointerException when topLeftLatitude is null");
    }
    
    @Test
    public void testCreateSquareWithNullCoordinates() {
      // Given valid values
      BigDecimal validValue = BigDecimal.valueOf(10.0);
      
      // When/Then - test each parameter being null
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(null, validValue, validValue, validValue);
      });
      
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(validValue, null, validValue, validValue);
      });
      
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(validValue, validValue, null, validValue);
      });
      
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(validValue, validValue, validValue, null);
      });
    }
  }
}
