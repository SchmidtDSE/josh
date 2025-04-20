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
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests for a momento structure for Geometry.
 */
@ExtendWith(MockitoExtension.class)
public class GeometryMomentoTest {

  @Mock
  private EngineGeometry mockGeometry;

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
   * @throws FactoryException if there is an error creating the Coordinate Reference System.
   * @throws NoSuchAuthorityCodeException if the EPSG code is not recognized.
   */
  @BeforeEach
  void setUp() throws NoSuchAuthorityCodeException, FactoryException {
    centerX = new BigDecimal("10.0");
    centerY = new BigDecimal("20.0");
    diameter = new BigDecimal("5.0");
    crs = CRS.decode("EPSG:32611");
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
    assertThrows(IllegalArgumentException.class, () ->
      new GeometryMomento("triangle", centerX, centerY, diameter, geometryFactory));
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
    String expectedSquareString = String.format(
        "square momento at (%.6f, %.6f) of diameter %.6f",
        centerX.doubleValue(),
        centerY.doubleValue(),
        diameter.doubleValue()
    );
    assertTrue(squareMomento.toString().contains(expectedSquareString));
  }

  @Test
  void testEquals() {
    GeometryMomento sameMomento = new GeometryMomento(
        "square",
        centerX,
        centerY,
        diameter,
        geometryFactory
    );
    assertEquals(squareMomento, sameMomento);

    GeometryMomento differentMomento = new GeometryMomento(
        "circle",
        centerX,
        centerY,
        diameter,
        geometryFactory
    );
    assertNotEquals(squareMomento, differentMomento);
  }

  @Test
  void testHashCode() {
    GeometryMomento sameMomento = new GeometryMomento(
        "square",
        centerX,
        centerY,
        diameter,
        geometryFactory
    );

    assertEquals(squareMomento.hashCode(), sameMomento.hashCode());

    GeometryMomento differentMomento = new GeometryMomento(
        "circle",
        centerX,
        centerY,
        diameter,
        geometryFactory
    );
    assertNotEquals(squareMomento.hashCode(), differentMomento.hashCode());
  }
}
