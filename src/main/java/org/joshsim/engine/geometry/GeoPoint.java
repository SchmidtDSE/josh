/**
 * Structures describing points in geospace.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Geometry;

/**
 * Interface representing a geographical point on Earth.
 */
public class GeoPoint extends EngineGeometry {

  /**
   * Constructs a GeoPoint with the specified geometry.
   *
   * @param geometry the shape representing the geographical point
   */
  public GeoPoint(Geometry geometry, CoordinateReferenceSystem crs) {
    super(geometry, crs);
  }
}
