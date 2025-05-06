/**
 * Convienence functions which perform binary conversion in jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;


/**
 * Utility which facilitates conversion between jshd format and PrecomputedGrids.
 *
 * <p>Utility which facilitates conversions involving the jshd binary format. This has the first 64
 * bits for the minimum x coordinate followed by 64 bits for maximum x coordinate, 64 bits for
 * minimum y coordinate, 64 bits for maximum y coordinate, 64 bits for minimum timestep, and 64 bits
 * for maximum timestep to conclude the header section. After the header containing these longs, the
 * doubles of the grid through time are listed one after another in which each row is written in
 * ordered from low to high column and then each set of rows follows from the minimum to maximum
 * timestep.</p>
 */
public class JshdUtil {

  /**
   * Load a DoublePrecomputedGrid from the given bytes serialization.
   *
   * @param bytes The bytes following the jshd format specification from which to parse a
   *     PrecomputedGrid.
   * @return A DoublePrecomputedGrid parsed from the given bytes.
   */
  public static DoublePrecomputedGrid loadFromBytes(byte[] bytes) {

  }

  /**
   * Convert DoublePrecomputedGrid from the given bytes serialization.
   *
   * @param bytes The bytes following the jshd format specification from which to parse a
   *     PrecomputedGrid.
   * @return A DoublePrecomputedGrid parsed from the given bytes.
   */
  public static byte[] serializeToBytes(DoublePrecomputedGrid target) {

  }

}
