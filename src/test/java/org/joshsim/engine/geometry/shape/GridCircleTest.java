package org.joshsim.engine.geometry.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for GridCircle.
 */
class GridCircleTest {

  private GridCircle gridCircle1;
  private GridCircle gridCircle2;
  private GridCircle gridCircle3;

  @BeforeEach
  public void setUp() {
    BigDecimal locationX = new BigDecimal("1.23");
    BigDecimal locationY = new BigDecimal("4.56");
    BigDecimal radius1 = new BigDecimal("7.89");
    BigDecimal radius2 = new BigDecimal("7.90");
    gridCircle1 = new GridCircle(locationX, locationY, radius1);
    gridCircle2 = new GridCircle(locationX, locationY, radius1);
    gridCircle3 = new GridCircle(locationX, locationY, radius2);
  }

  @Test
  void testGetGridShape() {
    GridShapeType result = gridCircle1.getGridShapeType();
    assertEquals(GridShapeType.CIRCLE, result);
  }

  @Test
  void testEquals() {
    assertTrue(gridCircle1.equals(gridCircle2));
    assertFalse(gridCircle1.equals(gridCircle3));
  }

  @Test
  void testHash() {
    assertEquals(gridCircle1.hashCode(), gridCircle2.hashCode());
    assertNotEquals(gridCircle1.hashCode(), gridCircle3.hashCode());
  }
}
