package org.joshsim.geo.external;

import java.math.BigDecimal;

/**
 * Container for spatial dimension information of a geospatial data source.
 */
public class ExternalSpatialDimensions {
  private final String dimensionNameX;
  private final String dimensionNameY;
  private final String dimensionNameTime;
  private final String crs;
  private final BigDecimal[] coordinatesX;
  private final BigDecimal[] coordinatesY;
  private final int width;
  private final int height;
  
  /**
   * Constructor for spatial dimensions.
   */
  public ExternalSpatialDimensions(
          String dimensionNameX,
          String dimensionNameY,
          String dimensionNameTime,
          String crs,
          BigDecimal[] coordinatesX, 
          BigDecimal[] coordinatesY) {
      
    this.dimensionNameX = dimensionNameX;
    this.dimensionNameY = dimensionNameY;
    this.dimensionNameTime = dimensionNameTime;
    this.crs = crs;
    this.coordinatesX = coordinatesX;
    this.coordinatesY = coordinatesY;
    this.width = coordinatesX.length;
    this.height = coordinatesY.length;
  }

  public String getDimensionNameX() {
    return dimensionNameX;
  }

  public String getDimensionNameY() {
    return dimensionNameY;
  }

  public String getDimensionNameTime() {
    return dimensionNameTime;
  }

  public String getCrs() {
    return crs;
  }

  public BigDecimal[] getCoordinatesX() {
    return coordinatesX;
  }

  public BigDecimal[] getCoordinatesY() {
    return coordinatesY;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /**
   * Gets the coordinate bounds.
   *
   * @return Array of [minX, minY, maxX, maxY]
   */
  public BigDecimal[] getBounds() {
    BigDecimal minX = coordinatesX[0];
    BigDecimal maxX = coordinatesX[coordinatesX.length - 1];
    BigDecimal minY = coordinatesY[0];
    BigDecimal maxY = coordinatesY[coordinatesY.length - 1];
    
    return new BigDecimal[] {minX, minY, maxX, maxY};
  }
  
  /**
   * Finds the closest X coordinate index.
   *
   * @param x The target X coordinate
   * @return Index of the closest X coordinate
   */
  public int findClosestIndexX(BigDecimal x) {
    return findClosestIndex(x, coordinatesX);
  }
  
  /**
   * Finds the closest Y coordinate index.
   *
   * @param y The target Y coordinate
   * @return Index of the closest Y coordinate
   */
  public int findClosestIndexY(BigDecimal y) {
    return findClosestIndex(y, coordinatesY);
  }
  
  private int findClosestIndex(BigDecimal target, BigDecimal[] coordinates) {
    if (coordinates.length == 0) {
      return -1;
    }
    
    int closestIdx = 0;
    BigDecimal closestDiff = target.subtract(coordinates[0]).abs();
    
    for (int i = 1; i < coordinates.length; i++) {
      BigDecimal diff = target.subtract(coordinates[i]).abs();
      if (diff.compareTo(closestDiff) < 0) {
        closestDiff = diff;
        closestIdx = i;
      }
    }
    
    return closestIdx;
  }
}