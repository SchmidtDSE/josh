/**
 * Test for grid space intersection.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.shape;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.GridGeometryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for grid space intersection.
 */
public class IntersectionDetectorTest {

  private EngineGeometryFactory factory;

  /**
   * Make common structures for intersection detector tests.
   */
  @BeforeEach
  public void setUp() {
    factory = new GridGeometryFactory();
  }

  @Test
  void testPointPointTrue() {
    EngineGeometry point1 = factory.createPoint(BigDecimal.ONE, BigDecimal.TWO);
    EngineGeometry point2 = factory.createPoint(BigDecimal.ONE, BigDecimal.TWO);
    assertTrue(point1.intersects(point2));
  }

  @Test
  void testPointPointFalse() {
    EngineGeometry point1 = factory.createPoint(BigDecimal.ONE, BigDecimal.TWO);
    EngineGeometry point3 = factory.createPoint(BigDecimal.ONE, BigDecimal.ZERO);
    assertFalse(point1.intersects(point3));
  }

  @Test
  void testSquareSquareTrue() {
    EngineGeometry square1 = factory.createSquare(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN);
    EngineGeometry square2 = factory.createSquare(BigDecimal.TWO, BigDecimal.TWO, BigDecimal.TEN);
    assertTrue(square1.intersects(square2));
  }

  @Test
  void testSquareSquareFalse() {
    EngineGeometry square1 = factory.createSquare(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TWO);
    EngineGeometry square3 = factory.createSquare(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TWO);
    assertFalse(square1.intersects(square3));
  }

  @Test
  void testPointSquareTrue() {
    EngineGeometry square = factory.createSquare(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN);
    EngineGeometry point = factory.createPoint(BigDecimal.TWO, BigDecimal.TWO);
    assertTrue(square.intersects(point));
  }

  @Test
  void testCircleCircleTrue() {
    EngineGeometry circle1 = factory.createCircle(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TWO);
    EngineGeometry circle2 = factory.createCircle(BigDecimal.TWO, BigDecimal.TWO, BigDecimal.TWO);
    assertTrue(circle1.intersects(circle2));
  }

  @Test
  void testCircleCircleFalse() {
    EngineGeometry circle1 = factory.createCircle(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TWO);
    EngineGeometry circle3 = factory.createCircle(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TWO);
    assertFalse(circle1.intersects(circle3));
  }

  @Test
  void testSquareCircleTrue() {
    EngineGeometry square = factory.createSquare(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TWO);
    EngineGeometry circle = factory.createCircle(BigDecimal.TWO, BigDecimal.TWO, BigDecimal.TWO);
    assertTrue(square.intersects(circle));
  }

  @Test
  void testSquareCircleFalse() {
    EngineGeometry square = factory.createSquare(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TWO);
    EngineGeometry circle = factory.createCircle(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TWO);
    assertFalse(square.intersects(circle));
  }

}
