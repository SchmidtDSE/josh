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
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * Tests for a momento structure for Geometry.
 */
@ExtendWith(MockitoExtension.class)
public class GeometryMomentoTest {

  @Mock
  private Geometry mockGeometry;

  private GeometryMomento squareMomento;
  private GeometryMomento circleMomento;
  private BigDecimal centerX;
  private BigDecimal centerY;
  private BigDecimal diameter;
  private CoordinateReferenceSystem crs;

  /**
   * Create common structures for tests.
   *
   * @throws FactoryException
   * @throws NoSuchAuthorityCodeException
   */
  @BeforeEach
  void setUp() throws NoSuchAuthorityCodeException, FactoryException {
    centerX = new BigDecimal("10.0");
    centerY = new BigDecimal("20.0");
    diameter = new BigDecimal("5.0");
    crs = CRS.forCode("EPSG:32611");
    squareMomento = new GeometryMomento("square", centerX, centerY, diameter, crs);
    circleMomento = new GeometryMomento("circle", centerX, centerY, diameter, crs);
  }

  @Test
  void testConstructorValidShapes() {
    assertNotNull(squareMomento);
    assertNotNull(circleMomento);
  }

  @Test
  void testConstructorInvalidShape() {
    assertThrows(IllegalArgumentException.class, () ->
      new GeometryMomento("triangle", centerX, centerY, diameter, crs));
  }

  @Test
  void testBuildGeometry() {
    Geometry squareGeometry = squareMomento.build();
    assertNotNull(squareGeometry);

    Geometry circleGeometry = circleMomento.build();
    assertNotNull(circleGeometry);
  }

  @Test
  void testToString() {
    String expectedSquareString = String.format(
        "square momento at (%.6f, %.6f) of diameter %.6f.",
        centerX.doubleValue(),
        centerY.doubleValue(),
        diameter.doubleValue()
    );
    assertEquals(expectedSquareString, squareMomento.toString());
  }

  @Test
  void testEquals() {
    GeometryMomento sameMomento = new GeometryMomento(
        "square", centerX, centerY, diameter, crs
    );
    assertEquals(squareMomento, sameMomento);

    GeometryMomento differentMomento = new GeometryMomento(
        "circle", centerX, centerY, diameter, crs
    );
    assertNotEquals(squareMomento, differentMomento);
  }

  @Test
  void testHashCode() {
    GeometryMomento sameMomento = new GeometryMomento("square", centerX, centerY, diameter, crs);
    assertEquals(squareMomento.hashCode(), sameMomento.hashCode());

    GeometryMomento differentMomento = new GeometryMomento(
        "circle", centerX, centerY, diameter, crs
    );
    assertNotEquals(squareMomento.hashCode(), differentMomento.hashCode());
  }
}
