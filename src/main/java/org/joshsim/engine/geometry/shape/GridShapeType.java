
/**
 * Enumeration of supported geometric shape types in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.shape;

/**
 * Defines the supported types of geometric shapes in the grid coordinate system.
 *
 * <p>This enumeration represents the basic geometric primitives that can be
 * created and manipulated within the grid-based spatial system.</p>
 */
public enum GridShapeType {
  /** A dimensionless point in grid space. */
  POINT,
  /** A square shape with equal sides in grid space. */
  SQUARE,
  /** A circular shape defined by center and radius in grid space. */
  CIRCLE
}
