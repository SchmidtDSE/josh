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
import org.joshsim.engine.geometry.Geometry;
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
  private Geometry mockGeometry;

  private GeometryMomento squareMomento;
  private GeometryMomento circleMomento;
  private BigDecimal centerX;
  private BigDecimal centerY;
  private BigDecimal diameter;

  /**
   * Create common structures for tests.
   */
  @BeforeEach
  void setUp() {
    centerX = new BigDecimal("10.0");
    centerY = new BigDecimal("20.0");
    diameter = new BigDecimal("5.0");
    squareMomento = new GeometryMomento("square", centerX, centerY, diameter);
    circleMomento = new GeometryMomento("circle", centerX, centerY, diameter);
  }

  @Test
  void testConstructorValidShapes() {
    assertNotNull(squareMomento);
    assertNotNull(circleMomento);
  }

  @Test
  void testConstructorInvalidShape() {
    assertThrows(IllegalArgumentException.class, () -> 
      new GeometryMomento("triangle", centerX, centerY, diameter));
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
    GeometryMomento sameMomento = new GeometryMomento("square", centerX, centerY, diameter);
    assertEquals(squareMomento, sameMomento);

    GeometryMomento differentMomento = new GeometryMomento("circle", centerX, centerY, diameter);
    assertNotEquals(squareMomento, differentMomento);
  }

  @Test
  void testHashCode() {
    GeometryMomento sameMomento = new GeometryMomento("square", centerX, centerY, diameter);
    assertEquals(squareMomento.hashCode(), sameMomento.hashCode());

    GeometryMomento differentMomento = new GeometryMomento("circle", centerX, centerY, diameter);
    assertNotEquals(squareMomento.hashCode(), differentMomento.hashCode());
  }
}
