/**
 * Structures describing a circle geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import org.locationtech.spatial4j.shape.Shape;

/**
 * Geospatial circle described in geographic coordinates.
 */
public class Circle extends Geometry {

  /**
   * Constructs a Circle instance with the specified shape.
   *
   * @param shape the geometric shape representing the circle
   */
  public Circle(Shape shape) {
    super(shape);
  }
}
