package org.joshsim.engine.geometry.shape;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.EnginePoint;


public class GridPoint extends GridShape implements EnginePoint {

  public GridPoint(BigDecimal locationX, BigDecimal locationY) {
    super(locationX, locationY);
  }

  @Override
  public GridShapeType getGridShapeType() {
    return GridShapeType.POINT;
  }

  @Override
  public BigDecimal getWidth() {
    return BigDecimal.ZERO;
  }

  @Override
  public BigDecimal getHeight() {
    return BigDecimal.ZERO;
  }

  @Override
  public BigDecimal getX() {
    return getCenterX();
  }

  @Override
  public BigDecimal getY() {
    return getCenterY();
  }

  @Override
  public String toString() {
    return String.format(
        "GridPoint at (%s, %s).",
        getCenterX().toString(),
        getCenterY().toString()
    );
  }

}
