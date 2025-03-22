/**
 * Structures describing points in geospace.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.geometry;

import org.locationtech.spatial4j.shape.Shape;

/**
 * Interface representing a geographical point on Earth.
 */
public class GeoPoint extends Geometry {

  /**
   * Constructs a GeoPoint with the specified shape.
   *
   * @param shape the shape representing the geographical point
   */
  public GeoPoint(Shape shape) {
    super(shape);
  }
}
