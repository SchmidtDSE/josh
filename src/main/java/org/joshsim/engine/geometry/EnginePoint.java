/**
 * Structures describing points in geospace.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;

/**
 * Interface representing a geographical point on Earth or in grid space.
 */
public interface EnginePoint extends EngineGeometry {

  /**
   * Get the horizontal or longitudinal position associated with this point.
   *
   * @return The x component of this discrete location.
   */
  BigDecimal getX();

  /**
   * Get the vertical or latitudinal position associated with this point.
   *
   * @return The y component of this discrete location.
   */
  BigDecimal getY();

}
