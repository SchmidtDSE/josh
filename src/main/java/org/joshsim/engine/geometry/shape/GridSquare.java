package org.joshsim.engine.geometry.shape;

import java.math.BigDecimal;

public class GridSquare extends GridShape {

  private final BigDecimal width;

  public GridSquare(BigDecimal x, BigDecimal y, BigDecimal width) {
    super(x, y);
    this.width = width;
  }

  @Override
  public GridShapeType getGridShapeType() {
    return GridShapeType.SQUARE;
  }

  @Override
  public BigDecimal getWidth() {
    return width;
  }

  @Override
  public BigDecimal getHeight() {
    return width;
  }

  @Override
  public String toString() {
    return String.format(
        "GridSquare at (%s, %s) of width %s.",
        getCenterX().toString(),
        getCenterY().toString(),
        getWidth().toString()
    );
  }

}
