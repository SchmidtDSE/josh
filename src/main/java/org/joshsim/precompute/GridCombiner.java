/**
 * Utility to combine two DataGridLayers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;


/**
 * Utility to combine two DataGridLayers.
 *
 * <p>Utility which combines two DataGridLayers by specifying which one's values should take
 * precedence if there is a collision. Note that this specifically combines into a
 * DoublePrecomputedGrid.</p>
 */
public class GridCombiner {

  /**
   * Combine two DataGridLayers into a single DataGridLayer.
   *
   * <p>Combine two DataGridLayers together into a single DataGridLayer where values from the right
   * grid are used if there is a conflict. Note that this specifically combines into a
   * DoublePrecomputedGrid so values are held in memory.</p>
   *
   * @param left The first DataGridLayer to combine and from which values are overwritten by right
   *     in the case of a conflict.
   * @param right The second DataGridLayer to combine and which takes precedence if there is a
   *     conflict.
   * @return Newly created DataGridLayer which combines the two input layers.
   */
  public static DataGridLayer combine(DataGridLayer left, DataGridLayer right) {

  }

}
