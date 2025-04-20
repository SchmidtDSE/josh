package org.joshsim.engine.geometry;

import java.math.BigDecimal;

public class GridGeometryFactory implements EngineGeometryFactory {

  @Override
  public EngineGeometry createSquare(BigDecimal width, BigDecimal centerX, BigDecimal centerY) {
    return null;
  }

  @Override
  public EngineGeometry createSquare(BigDecimal topLeftX, BigDecimal topLeftY, BigDecimal bottomRightX, BigDecimal bottomRightY) {
    return null;
  }

  @Override
  public EngineGeometry createCircle(BigDecimal radius, BigDecimal centerX, BigDecimal centerY) {
    return null;
  }

  @Override
  public EngineGeometry createCircle(BigDecimal pointX, BigDecimal pointY, BigDecimal centerX, BigDecimal centerY) {
    return null;
  }

  @Override
  public EngineGeometry createPoint(BigDecimal x, BigDecimal y) {
    return null;
  }

}
