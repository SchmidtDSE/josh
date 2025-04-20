package org.joshsim.engine.geometry;

import org.joshsim.geo.geometry.EarthGeometry;

import java.math.BigDecimal;

/**
 * A geometric object which defines if it is in Earth space or in grid space.
 */
public interface EngineGeometry extends Spatial {

  /**
   * Get the center of this geometry as a combined point.
   *
   * @return Combined point representing the center of this geometry.
   */
  EnginePoint getCenter();

  /**
   * Determines if this geometry intersects with another spatial geometry.
   *
   * @param other the other spatial geometry to check for intersection
   * @return true if the geometries intersect, false otherwise
   */
  boolean intersects(EngineGeometry other);

  /**
   * Determines if this geometry intersects with a specific geographic point.
   *
   * @param locationX the X position (Longitude / Easting) of the point
   * @param locationY the Y position (Latitude / Northing) of the point
   * @return true if the geometry intersects with the point, false otherwise
   */
  boolean intersects(BigDecimal locationX, BigDecimal locationY);

  /**
   * Get this geometry in Earth space.
   *
   * @return A version of this geometry in Earth space.
   */
  EarthGeometry getOnEarth();

}
