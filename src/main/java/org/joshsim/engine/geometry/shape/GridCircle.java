package org.joshsim.engine.geometry.shape;

import java.math.BigDecimal;


public class GridCircle extends GridShape {

  private final BigDecimal diameter;

  public GridCircle(BigDecimal x, BigDecimal y, BigDecimal radius) {
    super(x, y);
    diameter = radius.multiply(BigDecimal.TWO);
  }

  @Override
  public GridShapeType getGridShapeType() {
    return GridShapeType.CIRCLE;
  }

  @Override
  public BigDecimal getWidth() {
    return diameter;
  }

  @Override
  public BigDecimal getHeight() {
    return diameter;
  }

  @Override
  public String toString() {
    return String.format(
        "GridCircle at (%s, %s) of diameter %s.",
        getCenterX().toString(),
        getCenterY().toString(),
        getWidth().toString()
    );
  }

}
