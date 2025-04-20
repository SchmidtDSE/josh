package org.joshsim.engine.geometry.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for GridPoint.
 */
class GridPointTest {

  private GridPoint gridPoint1;
  private GridPoint gridPoint2;
  private GridPoint gridPoint3;

  @BeforeEach
  public void setUp() {
    BigDecimal locationX = new BigDecimal("1.23");
    BigDecimal locationY = new BigDecimal("4.56");
    gridPoint1 = new GridPoint(locationX, locationY);
    gridPoint2 = new GridPoint(locationX, locationY);
    gridPoint3 = new GridPoint(locationY, locationX);
  }

  @Test
  void testGetGridShapeType() {
    GridShapeType result = gridPoint1.getGridShapeType();
    assertEquals(GridShapeType.POINT, result);
  }

  @Test
  void testEquals() {
    assertTrue(gridPoint1.equals(gridPoint2));
    assertFalse(gridPoint1.equals(gridPoint3));
  }

  @Test
  void testHash() {
    assertEquals(gridPoint1.hashCode(), gridPoint2.hashCode());
    assertNotEquals(gridPoint1.hashCode(), gridPoint3.hashCode());
  }
}
