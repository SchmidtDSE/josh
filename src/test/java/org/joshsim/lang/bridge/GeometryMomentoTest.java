/**
 * Tests for a momento structure for Geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;

/**
 * Tests for a momento structure for Geometry.
 */
@ExtendWith(MockitoExtension.class)
public class GeometryMomentoTest {

  @Mock private EngineGeometry mockGeometry;

  private GeometryMomento squareMomento;
  private GeometryMomento circleMomento;
  private BigDecimal centerX;
  private BigDecimal centerY;
  private BigDecimal diameter;
  private CoordinateReferenceSystem crs;
  private EarthGeometryFactory geometryFactory;

  /**
   * Create common structures for tests.
   *
   * @throws Exception if there is an error creating the Coordinate Reference System.
   */
  @BeforeEach
  void setUp() throws Exception {
    centerX = new BigDecimal("10.0");
    centerY = new BigDecimal("20.0");
    diameter = new BigDecimal("5.0");
    
    // Using Apache SIS CRS.forCode instead of GeoTools CRS.decode
    crs = CRS.forCode("EPSG:32611");
    
    geometryFactory = new EarthGeometryFactory(crs);
    squareMomento = new GeometryMomento("square", centerX, centerY, diameter, geometryFactory);
    circleMomento = new GeometryMomento("circle", centerX, centerY, diameter, geometryFactory);
  }

  @Test
  void testConstructorValidShapes() {
    assertNotNull(squareMomento);
    assertNotNull(circleMomento);
  }

  @Test
  void testConstructorInvalidShape() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GeometryMomento("triangle", centerX, centerY, diameter, geometryFactory));
  }

  @Test
  void testBuildGeometry() {
    EngineGeometry squareGeometry = squareMomento.build();
    assertNotNull(squareGeometry);

    EngineGeometry circleGeometry = circleMomento.build();
    assertNotNull(circleGeometry);
  }

  @Test
  void testToString() {
    String expectedSquareString =
        String.format(
            "square momento at (%.6f, %.6f) of diameter %.6f",
            centerX.doubleValue(), centerY.doubleValue(), diameter.doubleValue());
    assertTrue(squareMomento.toString().contains(expectedSquareString));
  }

  @Test
  void testEquals() {
    GeometryMomento sameMomento =
        new GeometryMomento("square", centerX, centerY, diameter, geometryFactory);
    assertEquals(squareMomento, sameMomento);

    GeometryMomento differentMomento =
        new GeometryMomento("circle", centerX, centerY, diameter, geometryFactory);
    assertNotEquals(squareMomento, differentMomento);
  }

  @Test
  void testHashCode() {
    GeometryMomento sameMomento =
        new GeometryMomento("square", centerX, centerY, diameter, geometryFactory);
    assertEquals(squareMomento.hashCode(), sameMomento.hashCode());

    GeometryMomento differentMomento =
        new GeometryMomento("circle", centerX, centerY, diameter, geometryFactory);
    assertNotEquals(squareMomento.hashCode(), differentMomento.hashCode());
  }
  
  @Test
  void testCrsConversions() throws Exception {
    // Test with a different CRS to confirm Grid to Target conversion works
    CoordinateReferenceSystem targetCrs = CommonCRS.WGS84.geographic();
    EarthGeometryFactory targetFactory = new EarthGeometryFactory(targetCrs);
    
    // Create momento in the target CRS
    GeometryMomento targetMomento = 
        new GeometryMomento("square", centerX, centerY, diameter, targetFactory);
    
    // Build geometry in target CRS
    EngineGeometry targetGeometry = targetMomento.build();
    assertNotNull(targetGeometry);
    
    // Verify that different CRS results in different momentos
    assertNotEquals(squareMomento, targetMomento);
  }
}