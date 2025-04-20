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
class GridSquareTest {

  private GridSquare gridSquare1;
  private GridSquare gridSquare2;
  private GridSquare gridSquare3;

  @BeforeEach
  public void setUp() {
    BigDecimal locationX = new BigDecimal("1.23");
    BigDecimal locationY = new BigDecimal("4.56");
    BigDecimal width1 = new BigDecimal("7.89");
    BigDecimal width2 = new BigDecimal("7.90");
    gridSquare1 = new GridSquare(locationX, locationY, width1);
    gridSquare2 = new GridSquare(locationX, locationY, width1);
    gridSquare3 = new GridSquare(locationX, locationY, width2);
  }

  @Test
  void testGetGridShape() {
    GridShapeType result = gridSquare1.getGridShapeType();
    assertEquals(GridShapeType.SQUARE, result);
  }

  @Test
  void testEquals() {
    assertTrue(gridSquare1.equals(gridSquare2));
    assertFalse(gridSquare1.equals(gridSquare3));
  }

  @Test
  void testHash() {
    assertEquals(gridSquare1.hashCode(), gridSquare2.hashCode());
    assertNotEquals(gridSquare1.hashCode(), gridSquare3.hashCode());
  }
}
