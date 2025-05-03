package org.joshsim.geo.external;

import java.math.BigDecimal;
import java.util.List;

/**
 * Container for spatial dimension information of a geospatial data source.
 */
public class ExternalSpatialDimensions {
  private final String dimensionNameX;
  private final String dimensionNameY;
  private final String dimensionNameTime;
  private final String crs;
  private final List<BigDecimal> coordinatesX;
  private final List<BigDecimal> coordinatesY;


  /**
   * Constructor for spatial dimensions.
   */
  public ExternalSpatialDimensions(
          String dimensionNameX,
          String dimensionNameY,
          String dimensionNameTime,
          String crs,
          List<BigDecimal> coordinatesX,
          List<BigDecimal> coordinatesY) {
    this.dimensionNameX = dimensionNameX;
    this.dimensionNameY = dimensionNameY;
    this.dimensionNameTime = dimensionNameTime;
    this.crs = crs;
    this.coordinatesX = coordinatesX;
    this.coordinatesY = coordinatesY;
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

  public List<BigDecimal> getCoordinatesX() {
    return coordinatesX;
  }

  public List<BigDecimal> getCoordinatesY() {
    return coordinatesY;
  }

  /**
   * Gets the coordinate bounds.
   *
   * @return Array of [minX, minY, maxX, maxY]
   */
  public BigDecimal[] getBounds() {
    if (coordinatesX.isEmpty() || coordinatesY.isEmpty()) {
      return new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }
    BigDecimal minX = coordinatesX.get(0);
    BigDecimal maxX = coordinatesX.get(coordinatesX.size() - 1);
    BigDecimal minY = coordinatesY.get(0);
    BigDecimal maxY = coordinatesY.get(coordinatesY.size() - 1);
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

  private int findClosestIndex(BigDecimal target, List<BigDecimal> coordinates) {
    if (coordinates.isEmpty()) {
      return -1;
    }

    int closestIdx = 0;
    BigDecimal closestDiff = target.subtract(coordinates.get(0)).abs();

    for (int i = 1; i < coordinates.size(); i++) {
      BigDecimal diff = target.subtract(coordinates.get(i)).abs();
      if (diff.compareTo(closestDiff) < 0) {
        closestDiff = diff;
        closestIdx = i;
      }
    }

    return closestIdx;
  }
}
